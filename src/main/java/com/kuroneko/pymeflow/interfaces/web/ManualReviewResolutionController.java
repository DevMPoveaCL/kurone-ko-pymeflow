package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.CashflowMovementHistoryService;
import com.kuroneko.pymeflow.application.cashflow.ManualReviewResolutionCommand;
import com.kuroneko.pymeflow.application.cashflow.ManualReviewMovementResolutionCommand;
import com.kuroneko.pymeflow.application.cashflow.ManualReviewResolutionResult;
import com.kuroneko.pymeflow.application.cashflow.ManualReviewResolutionService;
import com.kuroneko.pymeflow.application.cashflow.PersistedManualReviewResolutionResult;
import com.kuroneko.pymeflow.application.cashflow.ProjectionReadyCashflowTransaction;
import com.kuroneko.pymeflow.application.cashflow.ProjectedCashflowTransaction;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/cashflow/manual-review/resolutions")
@Tag(name = "Manual review resolutions")
public class ManualReviewResolutionController {
    private static final String MANUAL_REVIEW = ManualReviewResolutionService.SOURCE_STATUS_MANUAL_REVIEW;
    private static final String PROJECTABLE = ManualReviewResolutionService.STATUS_PROJECTABLE;
    private static final String CATEGORIZED = ManualReviewResolutionService.STATUS_CATEGORIZED;

    private final ManualReviewResolutionService manualReviewResolutionService;
    private final CashflowMovementHistoryService cashflowMovementHistoryService;

    public ManualReviewResolutionController(
            ManualReviewResolutionService manualReviewResolutionService,
            CashflowMovementHistoryService cashflowMovementHistoryService
    ) {
        this.manualReviewResolutionService = manualReviewResolutionService;
        this.cashflowMovementHistoryService = cashflowMovementHistoryService;
    }

    @PostMapping
    @Operation(
            summary = "Resolver revisión manual de caja",
            description = "Convierte un movimiento en revisión manual en una transacción categorizada transitoria, sin persistir cola ni historial."
    )
    public ResponseEntity<ManualReviewResolutionResponse> resolve(
            @Valid @RequestBody ManualReviewResolutionRequest request
    ) {
        validateInterfaceRules(request);
        return ResponseEntity.ok(ManualReviewResolutionResponse.from(
                manualReviewResolutionService.resolve(request.toCommand())
        ));
    }

    @PostMapping("/{movementId}")
    @Operation(
            summary = "Resolver movimiento histórico por identificador",
            description = "Convierte una revisión manual persistida en una transacción lista para proyección, sin persistir resultados de proyección."
    )
    public ResponseEntity<PersistedManualReviewResolutionResponse> resolvePersisted(
            @PathVariable UUID movementId,
            @Valid @RequestBody PersistedManualReviewResolutionRequest request
    ) {
        return ResponseEntity.ok(PersistedManualReviewResolutionResponse.from(
                cashflowMovementHistoryService.resolveManualReview(request.toCommand(movementId))
        ));
    }

    private static void validateInterfaceRules(ManualReviewResolutionRequest request) {
        var errors = new ArrayList<ApiExceptionHandler.ValidationErrorResponse>();

        if (!request.hasManualReviewSourceStatus()) {
            errors.add(new ApiExceptionHandler.ValidationErrorResponse(
                    "sourceStatus",
                    "Solo se pueden resolver movimientos enviados a revisión manual."
            ));
        }
        if (!request.hasProjectableOutputStatus()) {
            errors.add(new ApiExceptionHandler.ValidationErrorResponse(
                    "status",
                    "El resultado debe quedar listo para proyección; no se aceptan estados pendientes o rechazados."
            ));
        }

        if (!errors.isEmpty()) {
            throw new ApiExceptionHandler.ApiValidationException(errors);
        }
    }

    public record ManualReviewResolutionRequest(
            @NotBlank(message = "El perfil es obligatorio.")
            @Schema(example = "pharmacy-cl")
            String profileId,

            @NotBlank(message = "La categoría seleccionada es obligatoria.")
            @Schema(example = "sales")
            String chosenCategoryKey,

            @NotNull(message = "El monto es obligatorio.")
            @Positive(message = "El monto debe ser mayor que cero.")
            @Schema(example = "125000")
            BigDecimal amount,

            @NotBlank(message = "La moneda es obligatoria.")
            @Pattern(regexp = "CLP", message = "La moneda soportada es CLP.")
            @Schema(example = "CLP")
            String currency,

            @NotNull(message = "La fecha es obligatoria.")
            @Schema(example = "2026-06-11")
            LocalDate date,

            @Size(max = 160, message = "La descripción no puede superar 160 caracteres.")
            @Schema(description = "Descripción opcional no sensible.", example = "Venta Caja 1")
            String description,

            @Size(max = 80, message = "La referencia de origen no puede superar 80 caracteres.")
            @Schema(description = "Referencia opcional no sensible del origen.", example = "caja-1")
            String sourceReference,

            @Schema(description = "Opcional. Si se informa, debe ser MANUAL_REVIEW.", example = "MANUAL_REVIEW")
            String sourceStatus,

            @Schema(description = "Opcional. Si se informa, debe ser PROJECTABLE o CATEGORIZED.", example = "PROJECTABLE")
            String status
    ) {
        ManualReviewResolutionCommand toCommand() {
            return new ManualReviewResolutionCommand(
                    new ProfileId(profileId),
                    chosenCategoryKey,
                    amount,
                    Currency.getInstance(currency),
                    date,
                    description,
                    sourceReference,
                    sourceStatus,
                    status
            );
        }

        boolean hasManualReviewSourceStatus() {
            return normalized(sourceStatus)
                    .map(MANUAL_REVIEW::equals)
                    .orElse(true);
        }

        boolean hasProjectableOutputStatus() {
            return normalized(status)
                    .map(value -> PROJECTABLE.equals(value) || CATEGORIZED.equals(value))
                    .orElse(true);
        }

        private static Optional<String> normalized(String value) {
            return Optional.ofNullable(value)
                    .map(String::trim)
                    .filter(candidate -> !candidate.isBlank())
                    .map(candidate -> candidate.toUpperCase(java.util.Locale.ROOT));
        }
    }

    public record ManualReviewResolutionResponse(
            ProjectableTransactionResponse transaction,
            CategoryResponse category,
            String description,
            String sourceReference
    ) {
        static ManualReviewResolutionResponse from(ManualReviewResolutionResult result) {
            return new ManualReviewResolutionResponse(
                    ProjectableTransactionResponse.from(result.transaction(), result.outputStatus()),
                    CategoryResponse.from(result.category()),
                    result.safeDescription().orElse(null),
                    result.safeSourceReference().orElse(null)
            );
        }
    }

    public record PersistedManualReviewResolutionRequest(
            @NotBlank(message = "El perfil es obligatorio.")
            @Schema(example = "pharmacy-cl")
            String profileId,

            @NotBlank(message = "La categoría seleccionada es obligatoria.")
            @Schema(example = "sales")
            String chosenCategoryKey,

            @Size(max = 160, message = "La descripción no puede superar 160 caracteres.")
            @Schema(description = "Descripción opcional no sensible para validar contexto de caja.", example = "Venta Caja 1")
            String description,

            @Size(max = 80, message = "La referencia de origen no puede superar 80 caracteres.")
            @Schema(description = "Referencia opcional no sensible del origen.", example = "caja-1")
            String sourceReference
    ) {
        ManualReviewMovementResolutionCommand toCommand(UUID movementId) {
            return new ManualReviewMovementResolutionCommand(
                    movementId,
                    new ProfileId(profileId),
                    chosenCategoryKey,
                    description,
                    sourceReference
            );
        }
    }

    public record PersistedManualReviewResolutionResponse(
            ProjectionReadyTransactionResponse transaction,
            CategoryResponse category,
            String description,
            String sourceReference
    ) {
        static PersistedManualReviewResolutionResponse from(PersistedManualReviewResolutionResult result) {
            return new PersistedManualReviewResolutionResponse(
                    ProjectionReadyTransactionResponse.from(result.transaction()),
                    CategoryResponse.from(result.category()),
                    result.safeDescription().orElse(null),
                    result.safeSourceReference().orElse(null)
            );
        }
    }

    public record ProjectionReadyTransactionResponse(
            UUID movementId,
            String categoryKey,
            BigDecimal amount,
            String currency,
            LocalDate date,
            String status
    ) {
        static ProjectionReadyTransactionResponse from(ProjectionReadyCashflowTransaction transaction) {
            return new ProjectionReadyTransactionResponse(
                    transaction.movementId(),
                    transaction.categoryKey(),
                    transaction.amount(),
                    transaction.currency().getCurrencyCode(),
                    transaction.date(),
                    transaction.status().name()
            );
        }
    }

    public record ProjectableTransactionResponse(
            String categoryKey,
            BigDecimal amount,
            String currency,
            LocalDate date,
            String status
    ) {
        static ProjectableTransactionResponse from(ProjectedCashflowTransaction transaction, String outputStatus) {
            return new ProjectableTransactionResponse(
                    transaction.categoryKey(),
                    transaction.amount(),
                    transaction.currency().getCurrencyCode(),
                    transaction.date(),
                    outputStatus
            );
        }
    }

    public record CategoryResponse(String key, String displayName, String direction) {
        static CategoryResponse from(CashflowCategory category) {
            return new CategoryResponse(category.key(), category.displayName(), category.direction().name());
        }
    }
}
