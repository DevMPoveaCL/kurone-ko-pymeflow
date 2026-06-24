package com.kuroneko.pymeflow.interfaces.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kuroneko.pymeflow.application.cashflow.ProviderSyncStatusUseCase;
import com.kuroneko.pymeflow.application.cashflow.ProviderSyncUseCase;
import com.kuroneko.pymeflow.application.port.out.ProviderAuth;
import com.kuroneko.pymeflow.application.port.out.ProviderError;
import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/cashflow/provider-syncs")
@Tag(name = "Cashflow provider sync")
public class CashflowProviderSyncController {
    private static final String DURABLE = "DURABLE";
    private static final Set<String> SUPPORTED_FIXTURE_PROVIDERS = Set.of("santander", "bancoestado");

    private final ProviderSyncUseCase providerSyncUseCase;
    private final ProviderSyncStatusUseCase providerSyncStatusUseCase;

    public CashflowProviderSyncController(
            ProviderSyncUseCase providerSyncUseCase,
            ProviderSyncStatusUseCase providerSyncStatusUseCase
    ) {
        this.providerSyncUseCase = providerSyncUseCase;
        this.providerSyncStatusUseCase = providerSyncStatusUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Trigger fixture-backed provider sync",
            description = "Runs a synchronous fixture-backed provider sync. The API accepts only safe references "
                    + "and does not accept raw credentials, secrets, passwords, or tokens. Status snapshots are "
                    + "durable and stored for status lookup and restart-safe inspection."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sync completed synchronously with a safe report."),
            @ApiResponse(responseCode = "400", description = "Request validation failed before invoking sync.")
    })
    public ProviderSyncResponse trigger(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Safe provider sync request. credentialRef is a reference only, never raw credential material.",
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            name = "fixtureProviderSync",
                            value = """
                                    {
                                      "profileId": "pharmacy-cl",
                                      "providerType": "santander",
                                      "credentialRef": "fixture-ref-santander",
                                      "dateFrom": "2026-06-01",
                                      "dateTo": "2026-06-30"
                                    }
                                    """
                    ))
            )
            @org.springframework.web.bind.annotation.RequestBody ProviderSyncRequest request
    ) {
        var command = toCommand(request);
        var report = providerSyncUseCase.sync(command);
        return providerSyncStatusUseCase.find(report.syncId())
                .map(CashflowProviderSyncController::fromSnapshot)
                .orElseGet(() -> fromReport(command, report));
    }

    @GetMapping("/{syncId}")
    @Operation(
            summary = "Read provider sync status",
            description = "Returns the last safe durable status snapshot for a syncId. Unknown values mean durable "
                    + "storage has no matching recorded session."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Durable status snapshot found."),
            @ApiResponse(responseCode = "404", description = "No durable status snapshot was found for this syncId.")
    })
    public ResponseEntity<?> status(@PathVariable String syncId) {
        return providerSyncStatusUseCase.find(syncId)
                .<ResponseEntity<?>>map(snapshot -> ResponseEntity.ok(fromSnapshot(snapshot)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new SyncStatusNotFoundResponse(
                        "SYNC_STATUS_NOT_FOUND",
                        "No durable status snapshot was found for this syncId.",
                        DURABLE
                )));
    }

    private static ProviderSyncUseCase.ProviderSyncCommand toCommand(ProviderSyncRequest request) {
        var errors = validate(request);
        if (!errors.isEmpty()) {
            throw new ApiExceptionHandler.ApiValidationException(errors);
        }
        return new ProviderSyncUseCase.ProviderSyncCommand(
                new ProfileId(request.profileId().trim()),
                LocalDate.parse(request.dateFrom().trim()),
                LocalDate.parse(request.dateTo().trim()),
                new ProviderAuth(normalizeProvider(request.providerType()), request.credentialRef().trim())
        );
    }

    private static List<ApiExceptionHandler.ValidationErrorResponse> validate(ProviderSyncRequest request) {
        var errors = new ArrayList<ApiExceptionHandler.ValidationErrorResponse>();
        if (request == null) {
            errors.add(error("request", "Request body is required."));
            return errors;
        }
        requireText(errors, "profileId", request.profileId());
        requireText(errors, "providerType", request.providerType());
        requireText(errors, "credentialRef", request.credentialRef());
        var dateFrom = parseDate(errors, "dateFrom", request.dateFrom());
        var dateTo = parseDate(errors, "dateTo", request.dateTo());
        if (hasText(request.providerType()) && !SUPPORTED_FIXTURE_PROVIDERS.contains(normalizeProvider(request.providerType()))) {
            errors.add(error("providerType", "Provider type must be one of the supported fixture providers."));
        }
        if (dateFrom.isPresent() && dateTo.isPresent() && dateTo.orElseThrow().isBefore(dateFrom.orElseThrow())) {
            errors.add(error("dateTo", "Date to must not be before date from."));
        }
        return errors;
    }

    private static void requireText(List<ApiExceptionHandler.ValidationErrorResponse> errors, String field, String value) {
        if (!hasText(value)) {
            errors.add(error(field, "Field is required."));
        }
    }

    private static Optional<LocalDate> parseDate(List<ApiExceptionHandler.ValidationErrorResponse> errors, String field, String value) {
        if (!hasText(value)) {
            errors.add(error(field, "Field is required."));
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value.trim()));
        } catch (DateTimeParseException exception) {
            errors.add(error(field, "Date must use ISO yyyy-MM-dd format."));
            return Optional.empty();
        }
    }

    private static ApiExceptionHandler.ValidationErrorResponse error(String field, String message) {
        return new ApiExceptionHandler.ValidationErrorResponse(field, message);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeProvider(String providerType) {
        return providerType.trim().toLowerCase(Locale.ROOT);
    }

    private static ProviderSyncResponse fromReport(
            ProviderSyncUseCase.ProviderSyncCommand command,
            ProviderSyncUseCase.ProviderSyncReport report
    ) {
        return new ProviderSyncResponse(
                report.syncId(),
                command.profileId().value(),
                command.auth().providerType(),
                statusFor(report).name(),
                report.pagesFetched(),
                report.entriesFetched(),
                report.importedEntries(),
                report.hasMorePages(),
                report.truncated(),
                report.authAborted(),
                Optional.empty(),
                Optional.empty(),
                report.entriesFetched(),
                report.errors().stream().map(CashflowProviderSyncController::fromProviderError).toList(),
                report.retryAfterSeconds(),
                DURABLE
        );
    }

    private static ProviderSyncResponse fromSnapshot(SyncSessionPort.SyncSessionSnapshot snapshot) {
        return new ProviderSyncResponse(
                snapshot.syncId(),
                snapshot.profileId().value(),
                snapshot.providerType(),
                snapshot.status().name(),
                snapshot.pagesFetched(),
                snapshot.entriesFetched(),
                snapshot.importedEntries(),
                snapshot.hasMorePages(),
                snapshot.truncated(),
                snapshot.authAborted(),
                snapshot.cursor(),
                snapshot.lastSyncAt(),
                snapshot.sessionEntryCount(),
                snapshot.errors().stream().map(CashflowProviderSyncController::fromProviderError).toList(),
                snapshot.retryAfterSeconds(),
                snapshot.durability().name()
        );
    }

    private static SyncSessionPort.SyncStatus statusFor(ProviderSyncUseCase.ProviderSyncReport report) {
        if (report.errors().isEmpty() && !report.truncated()) {
            return SyncSessionPort.SyncStatus.COMPLETED;
        }
        if (report.pagesFetched() > 0 || report.entriesFetched() > 0 || report.importedEntries() > 0 || report.truncated()) {
            return SyncSessionPort.SyncStatus.PARTIAL;
        }
        return SyncSessionPort.SyncStatus.FAILED;
    }

    private static ProviderErrorResponse fromProviderError(ProviderError error) {
        return switch (error) {
            case ProviderError.AuthError auth -> new ProviderErrorResponse(
                    "AUTH_ERROR",
                    auth.safeMessage(),
                    Optional.empty(),
                    Optional.empty()
            );
            case ProviderError.RateLimitError rateLimit -> new ProviderErrorResponse(
                    "RATE_LIMIT",
                    rateLimit.safeMessage(),
                    Optional.empty(),
                    Optional.of(rateLimit.retryAfterSeconds())
            );
            case ProviderError.UnavailableError unavailable -> new ProviderErrorResponse(
                    "UNAVAILABLE",
                    unavailable.safeMessage(),
                    Optional.empty(),
                    Optional.empty()
            );
            case ProviderError.DataError data -> new ProviderErrorResponse(
                    "DATA_ERROR",
                    data.detail(),
                    Optional.of(data.field()),
                    Optional.empty()
            );
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record ProviderSyncRequest(
            @Schema(example = "pharmacy-cl") String profileId,
            @Schema(description = "Fixture provider only for this MVP.", example = "santander") String providerType,
            @Schema(description = "Reference only; raw secrets are not accepted.", example = "fixture-ref-santander") String credentialRef,
            @Schema(example = "2026-06-01") String dateFrom,
            @Schema(example = "2026-06-30") String dateTo
    ) {
    }

    public record ProviderSyncResponse(
            String syncId,
            String profileId,
            String providerType,
            String status,
            int pagesFetched,
            int entriesFetched,
            int importedEntries,
            boolean hasMorePages,
            boolean truncated,
            boolean authAborted,
            Optional<String> cursor,
            Optional<Instant> lastSyncAt,
            int sessionEntryCount,
            List<ProviderErrorResponse> errors,
            Optional<Integer> retryAfterSeconds,
            String durability
    ) {
    }

    public record ProviderErrorResponse(
            String code,
            String message,
            Optional<String> field,
            Optional<Integer> retryAfterSeconds
    ) {
    }

    public record SyncStatusNotFoundResponse(String code, String message, String durability) {
    }
}
