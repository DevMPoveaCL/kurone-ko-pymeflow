package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.BankProviderPort;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportCommand;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportPort;
import com.kuroneko.pymeflow.application.port.out.ProviderAuth;
import com.kuroneko.pymeflow.application.port.out.ProviderError;
import com.kuroneko.pymeflow.application.port.out.ProviderSyncQuery;
import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface ProviderSyncUseCase {
    ProviderSyncReport sync(ProviderSyncCommand command);

    record ProviderSyncCommand(
            ProfileId profileId,
            LocalDate dateFrom,
            LocalDate dateTo,
            ProviderAuth auth
    ) {
        public ProviderSyncCommand {
            if (profileId == null) {
                throw new IllegalArgumentException("Profile id is required");
            }
            if (dateFrom == null) {
                throw new IllegalArgumentException("Date from is required");
            }
            if (dateTo == null) {
                throw new IllegalArgumentException("Date to is required");
            }
            if (auth == null) {
                throw new IllegalArgumentException("Provider auth is required");
            }
        }
    }

    record ProviderSyncReport(
            String syncId,
            int pagesFetched,
            int entriesFetched,
            int importedEntries,
            boolean hasMorePages,
            boolean truncated,
            boolean authAborted,
            List<ProviderError> errors,
            Optional<Integer> retryAfterSeconds
    ) {
        public ProviderSyncReport {
            syncId = syncId == null ? "" : syncId;
            if (pagesFetched < 0) {
                throw new IllegalArgumentException("Pages fetched must not be negative");
            }
            if (entriesFetched < 0) {
                throw new IllegalArgumentException("Entries fetched must not be negative");
            }
            if (importedEntries < 0) {
                throw new IllegalArgumentException("Imported entries must not be negative");
            }
            errors = List.copyOf(errors == null ? List.of() : errors);
            retryAfterSeconds = retryAfterSeconds == null ? Optional.empty() : retryAfterSeconds;
        }
    }

    class ProviderSyncException extends RuntimeException {
        private final ProviderError error;

        public ProviderSyncException(ProviderError error) {
            super(messageFor(error));
            if (error == null) {
                throw new IllegalArgumentException("Provider error is required");
            }
            this.error = error;
        }

        public ProviderError error() {
            return error;
        }

        private static String messageFor(ProviderError error) {
            return switch (error) {
                case ProviderError.AuthError auth -> auth.safeMessage();
                case ProviderError.RateLimitError rateLimit -> rateLimit.safeMessage();
                case ProviderError.UnavailableError unavailable -> unavailable.safeMessage();
                case ProviderError.DataError data -> data.detail();
                case null -> "Provider error is required";
            };
        }
    }

    final class ProviderSyncService implements ProviderSyncUseCase {
        private final BankProviderPort bankProviderPort;
        private final ExternalStatementImportPort importPort;
        private final SyncSessionPort syncSessionPort;
        private final int maxPages;
        private final int pageSize;

        public ProviderSyncService(
                BankProviderPort bankProviderPort,
                ExternalStatementImportPort importPort,
                SyncSessionPort syncSessionPort,
                int maxPages,
                int pageSize
        ) {
            if (bankProviderPort == null) {
                throw new IllegalArgumentException("Provider port is required");
            }
            if (importPort == null) {
                throw new IllegalArgumentException("Import port is required");
            }
            if (syncSessionPort == null) {
                throw new IllegalArgumentException("Sync session port is required");
            }
            if (maxPages <= 0) {
                throw new IllegalArgumentException("Max pages must be greater than zero");
            }
            if (pageSize <= 0) {
                throw new IllegalArgumentException("Page size must be greater than zero");
            }
            this.bankProviderPort = bankProviderPort;
            this.importPort = importPort;
            this.syncSessionPort = syncSessionPort;
            this.maxPages = maxPages;
            this.pageSize = pageSize;
        }

        @Override
        public ProviderSyncReport sync(ProviderSyncCommand command) {
            var providerType = command.auth().providerType();
            var syncId = syncSessionPort.syncId(command.profileId(), providerType);
            var cursor = syncSessionPort.findCursor(command.profileId(), providerType);
            var errors = new ArrayList<ProviderError>();
            var pagesFetched = 0;
            var entriesFetched = 0;
            var importedEntries = 0;
            var hasMorePages = false;
            var authAborted = false;
            var attempts = 0;
            var stoppedAfterUnavailable = false;

            while (attempts < maxPages) {
                attempts++;
                var query = new ProviderSyncQuery(
                        command.profileId(),
                        command.dateFrom(),
                        command.dateTo(),
                        cursor,
                        pageSize
                );

                try {
                    var page = bankProviderPort.fetchStatements(query, command.auth());
                    pagesFetched++;
                    entriesFetched += page.entries().size();
                    var importResult = importPort.importStatement(new ExternalStatementImportCommand(
                            command.profileId(),
                            "provider-sync",
                            page.entries()
                    ));
                    importedEntries += importedEntryCount(importResult);
                    cursor = page.nextCursor();
                    syncSessionPort.saveCursor(command.profileId(), providerType, cursor.orElse(""));
                    syncSessionPort.incrementEntryCount(command.profileId(), providerType, page.entries().size());
                    hasMorePages = cursor.isPresent();
                    if (cursor.isEmpty()) {
                        break;
                    }
                } catch (ProviderSyncException exception) {
                    errors.add(exception.error());
                    authAborted = exception.error() instanceof ProviderError.AuthError;
                    if (authAborted || !(exception.error() instanceof ProviderError.UnavailableError)) {
                        break;
                    }
                    stoppedAfterUnavailable = attempts == maxPages;
                }
            }

            var truncated = (hasMorePages || stoppedAfterUnavailable) && attempts == maxPages;
            return new ProviderSyncReport(
                    syncId,
                    pagesFetched,
                    entriesFetched,
                    importedEntries,
                    hasMorePages,
                    truncated,
                    authAborted,
                    errors,
                    retryAfterSeconds(errors)
            );
        }

        private static int importedEntryCount(CashflowIngestionService.CashflowIngestionResult result) {
            return result.categorized().size() + result.manualReview().size() + result.rejected().size();
        }

        private static Optional<Integer> retryAfterSeconds(List<ProviderError> errors) {
            return errors.stream()
                    .filter(ProviderError.RateLimitError.class::isInstance)
                    .map(ProviderError.RateLimitError.class::cast)
                    .map(ProviderError.RateLimitError::retryAfterSeconds)
                    .findFirst();
        }
    }
}
