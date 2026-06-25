package com.kuroneko.pymeflow.application.port.out;

import java.time.Duration;
import java.util.Optional;

public interface ProviderSyncObservationPort {
    void observe(ProviderSyncObservation observation);

    static ProviderSyncObservationPort noop() {
        return observation -> { };
    }

    enum LifecycleEvent {
        STARTED,
        PAGE_FETCHED,
        COMPLETED,
        FAILED
    }

    enum SyncStatus {
        STARTED,
        IN_PROGRESS,
        COMPLETED,
        PARTIAL,
        FAILED
    }

    enum ErrorCode {
        NONE,
        AUTH,
        RATE_LIMIT,
        UNAVAILABLE,
        DATA,
        UNKNOWN
    }

    record ProviderSyncObservation(
            LifecycleEvent event,
            String syncId,
            String profileId,
            String providerType,
            SyncStatus status,
            int pagesFetched,
            int entriesFetched,
            int importedEntries,
            boolean hasMorePages,
            boolean truncated,
            boolean authAborted,
            ErrorCode errorCode,
            Optional<Integer> retryAfterSeconds,
            Optional<Duration> duration
    ) {
        public ProviderSyncObservation {
            if (event == null) {
                throw new IllegalArgumentException("Lifecycle event is required");
            }
            syncId = safeText(syncId);
            profileId = safeText(profileId);
            providerType = safeText(providerType);
            if (status == null) {
                throw new IllegalArgumentException("Sync status is required");
            }
            if (pagesFetched < 0) {
                throw new IllegalArgumentException("Pages fetched must not be negative");
            }
            if (entriesFetched < 0) {
                throw new IllegalArgumentException("Entries fetched must not be negative");
            }
            if (importedEntries < 0) {
                throw new IllegalArgumentException("Imported entries must not be negative");
            }
            errorCode = errorCode == null ? ErrorCode.NONE : errorCode;
            retryAfterSeconds = retryAfterSeconds == null ? Optional.empty() : retryAfterSeconds;
            duration = duration == null ? Optional.empty() : duration.filter(value -> !value.isNegative());
        }

        private static String safeText(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
