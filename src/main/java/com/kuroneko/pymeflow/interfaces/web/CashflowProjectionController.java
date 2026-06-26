package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.AppliedObligation;
import com.kuroneko.pymeflow.application.cashflow.CashflowProjectionCommand;
import com.kuroneko.pymeflow.application.cashflow.CashflowProjectionResult;
import com.kuroneko.pymeflow.application.cashflow.CashflowProjectionService;
import com.kuroneko.pymeflow.application.cashflow.CockpitProjectionService;
import com.kuroneko.pymeflow.application.cashflow.DailyProjectedBalance;
import com.kuroneko.pymeflow.application.cashflow.ProjectedCashflowTransaction;
import com.kuroneko.pymeflow.application.cashflow.ProjectionAlert;
import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

@RestController
@RequestMapping("/api/cashflow")
@Tag(name = "Cashflow projections")
@Validated
public class CashflowProjectionController {
    private static final String PROJECTABLE = "PROJECTABLE";
    private static final String CATEGORIZED = "CATEGORIZED";

    private final CashflowProjectionService cashflowProjectionService;
    private final CockpitProjectionService cockpitProjectionService;

    public CashflowProjectionController(
            CashflowProjectionService cashflowProjectionService,
            CockpitProjectionService cockpitProjectionService
    ) {
        this.cashflowProjectionService = cashflowProjectionService;
        this.cockpitProjectionService = cockpitProjectionService;
    }

    @PostMapping("/projections")
    @Operation(
            summary = "Proyectar flujo de caja",
            description = "Calcula una proyección transitoria desde transacciones categorizadas, sin persistir movimientos."
    )
    public ResponseEntity<CashflowProjectionResponse> project(@Valid @RequestBody CashflowProjectionRequest request) {
        validateInterfaceRules(request);
        return ResponseEntity.ok(CashflowProjectionResponse.from(cashflowProjectionService.project(request.toCommand())));
    }

    @GetMapping("/cockpit/projection")
    @Operation(
            summary = "Proyectar flujo de caja del cockpit",
            description = "Calcula una proyección desde movimientos PROJECTABLE persistidos, sin persistir cambios. Horizonte máximo MVP: 90 días."
    )
    public ResponseEntity<CashflowProjectionResponse> cockpitProjection(
            @RequestParam(required = false)
            @NotBlank(message = "El perfil es obligatorio.")
            String profileId,

            @RequestParam(required = false)
            @NotNull(message = "El saldo inicial es obligatorio.")
            @PositiveOrZero(message = "El saldo inicial no puede ser negativo.")
            BigDecimal openingBalance,

            @RequestParam(required = false)
            @NotNull(message = "La fecha de inicio es obligatoria.")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @NotNull(message = "El horizonte es obligatorio.")
            @Positive(message = "El horizonte debe ser mayor que cero.")
            @Max(value = CockpitProjectionService.MAX_HORIZON_DAYS, message = "El horizonte no puede superar 90 días.")
            Integer horizonDays
    ) {
        var result = cockpitProjectionService.projectFromHistory(
                new ProfileId(profileId),
                openingBalance,
                startDate,
                horizonDays
        );
        return ResponseEntity.ok(CashflowProjectionResponse.from(result));
    }

    private static void validateInterfaceRules(CashflowProjectionRequest request) {
        var errors = request.transactions().stream()
                .filter(transaction -> !transaction.hasProjectableStatus())
                .map(transaction -> new ApiExceptionHandler.ValidationErrorResponse(
                        "transactions.status",
                        "No se aceptan transacciones pendientes de revisión o rechazadas para esta proyección."
                ))
                .toList();
        if (!errors.isEmpty()) {
            throw new ApiExceptionHandler.ApiValidationException(errors);
        }
    }

    public record CashflowProjectionRequest(
            @NotBlank(message = "El perfil es obligatorio.")
            @Schema(example = "pharmacy-cl")
            String profileId,

            @NotNull(message = "El saldo inicial es obligatorio.")
            @PositiveOrZero(message = "El saldo inicial no puede ser negativo.")
            @Schema(example = "1500000")
            BigDecimal openingBalance,

            @NotBlank(message = "La moneda es obligatoria.")
            @Pattern(regexp = "CLP", message = "La moneda soportada es CLP.")
            @Schema(example = "CLP")
            String currency,

            @NotNull(message = "La fecha de inicio es obligatoria.")
            @Schema(example = "2026-02-01")
            LocalDate startDate,

            @Positive(message = "El horizonte debe ser mayor que cero.")
            @Schema(example = "30")
            int horizonDays,

            @NotEmpty(message = "Debe enviar al menos una transacción categorizada.")
            List<@Valid ProjectedTransactionRequest> transactions
    ) {
        CashflowProjectionCommand toCommand() {
            return new CashflowProjectionCommand(
                    new ProfileId(profileId),
                    openingBalance,
                    Currency.getInstance(currency),
                    startDate,
                    horizonDays,
                    transactions.stream().map(ProjectedTransactionRequest::toTransaction).toList()
            );
        }
    }

    public record ProjectedTransactionRequest(
            @NotBlank(message = "La categoría es obligatoria.")
            @Schema(example = "sales")
            String categoryKey,

            @NotNull(message = "El monto es obligatorio.")
            @Positive(message = "El monto debe ser mayor que cero.")
            @Schema(example = "125000")
            BigDecimal amount,

            @NotBlank(message = "La moneda de la transacción es obligatoria.")
            @Pattern(regexp = "CLP", message = "La moneda soportada es CLP.")
            @Schema(example = "CLP")
            String currency,

            @NotNull(message = "La fecha de la transacción es obligatoria.")
            @Schema(example = "2026-02-01")
            LocalDate date,

            @Schema(description = "Opcional. Si se omite, se asume CREDIT.", example = "CREDIT")
            String movementDirection,

            @Schema(description = "Opcional. Si se informa, debe ser PROJECTABLE o CATEGORIZED.", example = "PROJECTABLE")
            String status
    ) {
        ProjectedCashflowTransaction toTransaction() {
            return new ProjectedCashflowTransaction(categoryKey, amount, Currency.getInstance(currency), date, resolveMovementDirection());
        }

        private TransactionDirection resolveMovementDirection() {
            if (movementDirection == null) {
                return TransactionDirection.CREDIT;
            }
            return TransactionDirection.valueOf(movementDirection.trim().toUpperCase(java.util.Locale.ROOT));
        }

        boolean hasProjectableStatus() {
            if (status == null || status.isBlank()) {
                return true;
            }
            var normalized = status.trim().toUpperCase();
            return PROJECTABLE.equals(normalized) || CATEGORIZED.equals(normalized);
        }
    }

    public record CashflowProjectionResponse(
            List<DailyProjectedBalanceResponse> dailyBalances,
            BigDecimal closingProjectedBalance,
            List<AppliedObligationResponse> appliedObligations,
            List<ProjectionAlertResponse> alerts
    ) {
        static CashflowProjectionResponse from(CashflowProjectionResult result) {
            return new CashflowProjectionResponse(
                    result.dailyBalances().stream().map(DailyProjectedBalanceResponse::from).toList(),
                    result.closingProjectedBalance(),
                    result.appliedObligations().stream().map(AppliedObligationResponse::from).toList(),
                    result.alerts().stream().map(ProjectionAlertResponse::from).toList()
            );
        }
    }

    public record DailyProjectedBalanceResponse(
            LocalDate date,
            BigDecimal inflows,
            BigDecimal outflows,
            BigDecimal obligations,
            BigDecimal balance
    ) {
        static DailyProjectedBalanceResponse from(DailyProjectedBalance balance) {
            return new DailyProjectedBalanceResponse(
                    balance.date(),
                    balance.inflows(),
                    balance.outflows(),
                    balance.obligations(),
                    balance.balance()
            );
        }
    }

    public record AppliedObligationResponse(String obligationKey, String displayName, LocalDate dueDate, BigDecimal amount) {
        static AppliedObligationResponse from(AppliedObligation obligation) {
            return new AppliedObligationResponse(
                    obligation.obligationKey(),
                    obligation.displayName(),
                    obligation.dueDate(),
                    obligation.amount()
            );
        }
    }

    public record ProjectionAlertResponse(
            String ruleKey,
            String actionKey,
            String condition,
            LocalDate date,
            BigDecimal balance
    ) {
        static ProjectionAlertResponse from(ProjectionAlert alert) {
            return new ProjectionAlertResponse(
                    alert.ruleKey(),
                    alert.actionKey(),
                    alert.condition(),
                    alert.date(),
                    alert.balance()
            );
        }
    }
}
