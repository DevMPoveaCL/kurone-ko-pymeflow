package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.PharmacyCashflowService;
import com.kuroneko.pymeflow.application.cashflow.PharmacyCashflowService.CashflowIngestionCommand;
import com.kuroneko.pymeflow.application.cashflow.PharmacyCashflowService.CashflowIngestionResult;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

@RestController
@RequestMapping("/api/cashflow/ingestions")
@Tag(name = "Cashflow ingestion")
public class CashflowIngestionController {
    private static final String MANUAL_REVIEW_REASON = "Requiere clasificación manual.";
    private static final String SENSITIVE_REJECTION_REASON = "La transacción contiene datos sensibles y no fue clasificada.";

    private final PharmacyCashflowService pharmacyCashflowService;

    public CashflowIngestionController(PharmacyCashflowService pharmacyCashflowService) {
        this.pharmacyCashflowService = pharmacyCashflowService;
    }

    @PostMapping
    @Operation(
            summary = "Ingestar movimientos de caja simulados",
            description = "Clasifica movimientos de caja de forma transitoria, sin persistir transacciones."
    )
    public ResponseEntity<CashflowIngestionResponse> ingest(@Valid @RequestBody CashflowIngestionRequest request) {
        var command = new CashflowIngestionCommand(
                new ProfileId(request.profileId()),
                request.transactions().stream().map(CashflowTransactionRequest::toTransaction).toList()
        );

        return ResponseEntity.ok(CashflowIngestionResponse.from(pharmacyCashflowService.ingest(command)));
    }

    public record CashflowIngestionRequest(
            @NotBlank(message = "El perfil es obligatorio.")
            @Schema(example = "pharmacy-cl")
            String profileId,

            @NotEmpty(message = "Debe enviar al menos una transacción.")
            List<@Valid CashflowTransactionRequest> transactions
    ) {
    }

    public record CashflowTransactionRequest(
            @NotBlank(message = "La descripción es obligatoria.")
            @Schema(example = "Venta Caja 1")
            String description,

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
            LocalDate date
    ) {
        Transaction toTransaction() {
            return new Transaction(description, amount, Currency.getInstance(currency), date);
        }
    }

    public record CashflowIngestionResponse(
            List<CategorizedTransactionResponse> categorized,
            List<ManualReviewTransactionResponse> manualReview,
            List<RejectedTransactionResponse> rejected
    ) {
        static CashflowIngestionResponse from(CashflowIngestionResult result) {
            return new CashflowIngestionResponse(
                    result.categorized().stream().map(CategorizedTransactionResponse::from).toList(),
                    result.manualReview().stream().map(ManualReviewTransactionResponse::from).toList(),
                    result.rejected().stream().map(RejectedTransactionResponse::from).toList()
            );
        }
    }

    public record CategorizedTransactionResponse(TransactionResponse transaction, CategoryResponse category) {
        static CategorizedTransactionResponse from(PharmacyCashflowService.CategorizedTransaction result) {
            return new CategorizedTransactionResponse(
                    TransactionResponse.from(result.transaction()),
                    result.assignment().category()
                            .map(CategoryResponse::from)
                            .orElseThrow(() -> new IllegalArgumentException("Category is required"))
            );
        }
    }

    public record ManualReviewTransactionResponse(TransactionResponse transaction, String reason) {
        static ManualReviewTransactionResponse from(PharmacyCashflowService.ManualReviewTransaction result) {
            return new ManualReviewTransactionResponse(TransactionResponse.from(result.transaction()), MANUAL_REVIEW_REASON);
        }
    }

    public record RejectedTransactionResponse(BigDecimal amount, String currency, LocalDate date, String reasonCode, String reason) {
        static RejectedTransactionResponse from(PharmacyCashflowService.RejectedTransaction result) {
            var transaction = result.transaction();
            return new RejectedTransactionResponse(
                    transaction.amount(),
                    transaction.currency().getCurrencyCode(),
                    transaction.bookedAt(),
                    result.reasonCode(),
                    SENSITIVE_REJECTION_REASON
            );
        }
    }

    public record TransactionResponse(String description, BigDecimal amount, String currency, LocalDate date) {
        static TransactionResponse from(Transaction transaction) {
            return new TransactionResponse(
                    transaction.description(),
                    transaction.amount(),
                    transaction.currency().getCurrencyCode(),
                    transaction.bookedAt()
            );
        }
    }

    public record CategoryResponse(String key, String displayName, String direction) {
        static CategoryResponse from(CashflowCategory category) {
            return new CategoryResponse(category.key(), category.displayName(), category.direction().name());
        }
    }
}
