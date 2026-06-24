package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.CashflowMovementHistoryService;
import com.kuroneko.pymeflow.application.cashflow.PendingManualReviewMovement;
import com.kuroneko.pymeflow.application.cashflow.ProjectionReadyCashflowTransaction;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cashflow/history")
@Tag(name = "Cashflow history")
public class CashflowHistoryController {
    private final CashflowMovementHistoryService cashflowMovementHistoryService;

    public CashflowHistoryController(CashflowMovementHistoryService cashflowMovementHistoryService) {
        this.cashflowMovementHistoryService = cashflowMovementHistoryService;
    }

    @GetMapping("/manual-review")
    @Operation(
            summary = "Listar movimientos pendientes de revisión manual",
            description = "Entrega movimientos seguros de caja pendientes de categorización para el perfil indicado."
    )
    public ResponseEntity<List<PendingManualReviewMovementResponse>> pendingManualReviews(
            @RequestParam String profileId
    ) {
        validateProfileId(profileId);
        return ResponseEntity.ok(cashflowMovementHistoryService.pendingManualReviews(new ProfileId(profileId)).stream()
                .map(PendingManualReviewMovementResponse::from)
                .toList());
    }

    @GetMapping("/projection-ready")
    @Operation(
            summary = "Listar transacciones listas para proyección",
            description = "Entrega transacciones categorizadas compatibles con la entrada de proyección, sin persistir resultados."
    )
    public ResponseEntity<List<ProjectionReadyTransactionResponse>> projectionReady(
            @RequestParam String profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        validateProfileId(profileId);
        return ResponseEntity.ok(cashflowMovementHistoryService.projectionReady(new ProfileId(profileId), startDate, endDate).stream()
                .map(ProjectionReadyTransactionResponse::from)
                .toList());
    }

    private static void validateProfileId(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            throw new ApiExceptionHandler.ApiValidationException(List.of(
                    new ApiExceptionHandler.ValidationErrorResponse("profileId", "El perfil es obligatorio.")
            ));
        }
    }

    public record PendingManualReviewMovementResponse(
            UUID movementId,
            BigDecimal amount,
            String currency,
            LocalDate date,
            String movementDirection,
            String description,
            String sourceReference,
            String status
    ) {
        static PendingManualReviewMovementResponse from(PendingManualReviewMovement movement) {
            return new PendingManualReviewMovementResponse(
                    movement.movementId(),
                    movement.amount(),
                    movement.currency().getCurrencyCode(),
                    movement.date(),
                    movement.direction().name(),
                    movement.description(),
                    movement.sourceReference(),
                    movement.status().name()
            );
        }
    }

    public record ProjectionReadyTransactionResponse(
            UUID movementId,
            @Schema(example = "sales")
            String categoryKey,
            BigDecimal amount,
            String currency,
            LocalDate date,
            String movementDirection,
            String status
    ) {
        static ProjectionReadyTransactionResponse from(ProjectionReadyCashflowTransaction transaction) {
            return new ProjectionReadyTransactionResponse(
                    transaction.movementId(),
                    transaction.categoryKey(),
                    transaction.amount(),
                    transaction.currency().getCurrencyCode(),
                    transaction.date(),
                    transaction.direction().name(),
                    transaction.status().name()
            );
        }
    }
}
