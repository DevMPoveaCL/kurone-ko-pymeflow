package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService.CashflowIngestionCommand;
import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService.CashflowIngestionCommand.IngestionItem;
import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService.CashflowIngestionResult;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/cashflow/imports/manual")
@Tag(name = "Cashflow manual import")
public class CashflowManualImportController {
    private static final String CLP = "CLP";
    private static final String MANUAL_REVIEW_REASON = "Requiere clasificación manual.";
    private static final String SENSITIVE_REJECTION_REASON = "La transacción contiene datos sensibles y no fue clasificada.";

    private final CashflowIngestionService cashflowIngestionService;

    public CashflowManualImportController(CashflowIngestionService cashflowIngestionService) {
        this.cashflowIngestionService = cashflowIngestionService;
    }

    @PostMapping
    @Operation(
            summary = "Import manual cashflow rows",
            description = "Processes CSV-like rows with row-level tolerance. Provided rowNumber values are echoed "
                    + "in successful and validation-error response entries; missing rowNumber values fall back to "
                    + "the submitted 1-based position."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "At least one row was accepted for ingestion."),
            @ApiResponse(responseCode = "400", description = "All rows were invalid; row-level errors are returned.")
    })
    public ResponseEntity<ManualImportResponse> importManual(@RequestBody ManualImportRequest request) {
        var errors = new ArrayList<RowErrorResponse>();
        var validItems = new ArrayList<IngestionItem>();
        var validRows = new ArrayList<ValidRow>();
        var rows = request.rows() == null ? List.<ManualImportRow>of() : request.rows();

        for (int index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            var rowNumber = responseRowNumber(row, index);
            var error = validateRow(row, rowNumber);
            if (error.isPresent()) {
                errors.add(error.orElseThrow());
                continue;
            }
            var item = toIngestionItem(row);
            validItems.add(item);
            validRows.add(new ValidRow(rowNumber, item.transaction()));
        }

        var result = validItems.isEmpty()
                ? new CashflowIngestionResult(List.of(), List.of(), List.of())
                : cashflowIngestionService.ingest(new CashflowIngestionCommand(new ProfileId(request.profileId()), validItems));

        var response = fromResult(request.profileId(), validItems.size(), result, validRows, errors);
        var status = validItems.isEmpty() ? HttpStatus.BAD_REQUEST : HttpStatus.OK;

        return ResponseEntity.status(status).body(response);
    }

    private static Optional<RowErrorResponse> validateRow(ManualImportRow row, int rowNumber) {
        if (row.description() == null || row.description().isBlank()) {
            return Optional.of(new RowErrorResponse(rowNumber, "description", "La descripción es obligatoria."));
        }
        if (row.amount() == null || row.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.of(new RowErrorResponse(rowNumber, "amount", "El monto debe ser mayor que cero."));
        }
        if (row.date() == null || row.date().isBlank()) {
            return Optional.of(new RowErrorResponse(rowNumber, "date", "La fecha debe tener formato ISO yyyy-MM-dd."));
        }
        try {
            LocalDate.parse(row.date());
        } catch (DateTimeParseException exception) {
            return Optional.of(new RowErrorResponse(rowNumber, "date", "La fecha debe tener formato ISO yyyy-MM-dd."));
        }
        if (!currencyOrDefault(row).equals(CLP)) {
            return Optional.of(new RowErrorResponse(rowNumber, "currency", "La única moneda soportada es CLP."));
        }
        return Optional.empty();
    }

    private static IngestionItem toIngestionItem(ManualImportRow row) {
        return new IngestionItem(
                new Transaction(
                        row.description(),
                        row.amount(),
                        Currency.getInstance(currencyOrDefault(row)),
                        LocalDate.parse(row.date())
                ),
                row.externalReference()
        );
    }

    private static String currencyOrDefault(ManualImportRow row) {
        return row.currency() == null || row.currency().isBlank() ? CLP : row.currency();
    }

    private static int responseRowNumber(ManualImportRow row, int index) {
        return row.rowNumber() == null ? index + 1 : row.rowNumber();
    }

    private static ManualImportResponse fromResult(
            String profileId,
            int accepted,
            CashflowIngestionResult result,
            List<ValidRow> validRows,
            List<RowErrorResponse> errors
    ) {
        var rowMapper = new ResultRowMapper(validRows);
        var categorized = result.categorized().stream()
                .map(item -> CategorizedTransactionResponse.from(rowMapper.rowFor(item.transaction()), item))
                .toList();
        var manualReview = result.manualReview().stream()
                .map(item -> ManualReviewTransactionResponse.from(rowMapper.rowFor(item.transaction()), item))
                .toList();
        var rejected = result.rejected().stream()
                .map(item -> RejectedTransactionResponse.from(rowMapper.rowFor(item.transaction()), item))
                .toList();

        return new ManualImportResponse(
                UUID.randomUUID(),
                profileId,
                accepted,
                categorized.size(),
                manualReview.size(),
                rejected.size(),
                errors.size(),
                categorized,
                manualReview,
                rejected,
                List.copyOf(errors)
        );
    }

    public record ManualImportRequest(String profileId, String importLabel, List<ManualImportRow> rows) {
    }

    public record ManualImportRow(Integer rowNumber, String description, BigDecimal amount, String currency, String date, String externalReference) {
    }

    private record ValidRow(int row, Transaction transaction) {
        boolean matches(Transaction candidate) {
            return transaction.description().equals(candidate.description())
                    && transaction.amount().compareTo(candidate.amount()) == 0
                    && transaction.currency().equals(candidate.currency())
                    && transaction.bookedAt().equals(candidate.bookedAt());
        }
    }

    private static final class ResultRowMapper {
        private final List<ValidRow> validRows;
        private final boolean[] used;

        private ResultRowMapper(List<ValidRow> validRows) {
            this.validRows = List.copyOf(validRows);
            this.used = new boolean[validRows.size()];
        }

        private int rowFor(Transaction transaction) {
            for (int index = 0; index < validRows.size(); index++) {
                if (!used[index] && validRows.get(index).matches(transaction)) {
                    used[index] = true;
                    return validRows.get(index).row();
                }
            }
            for (int index = 0; index < validRows.size(); index++) {
                if (!used[index]) {
                    used[index] = true;
                    return validRows.get(index).row();
                }
            }
            throw new IllegalArgumentException("Result row mapping exceeded valid row count");
        }
    }

    public record ManualImportResponse(
            UUID importId,
            String profileId,
            int accepted,
            int categorizedCount,
            int manualReviewCount,
            int rejectedCount,
            int invalid,
            List<CategorizedTransactionResponse> categorized,
            List<ManualReviewTransactionResponse> manualReview,
            List<RejectedTransactionResponse> rejected,
            List<RowErrorResponse> errors
    ) {
    }

    public record CategorizedTransactionResponse(int row, UUID movementId, TransactionResponse transaction, CategoryResponse category) {
        static CategorizedTransactionResponse from(int row, CashflowIngestionService.CategorizedTransaction result) {
            return new CategorizedTransactionResponse(
                    row,
                    result.movementId(),
                    TransactionResponse.from(result.transaction()),
                    result.assignment().category()
                            .map(CategoryResponse::from)
                            .orElseThrow(() -> new IllegalArgumentException("Category is required"))
            );
        }
    }

    public record ManualReviewTransactionResponse(int row, UUID movementId, TransactionResponse transaction, String reason) {
        static ManualReviewTransactionResponse from(int row, CashflowIngestionService.ManualReviewTransaction result) {
            return new ManualReviewTransactionResponse(row, result.movementId(), TransactionResponse.from(result.transaction()), MANUAL_REVIEW_REASON);
        }
    }

    public record RejectedTransactionResponse(int row, UUID movementId, BigDecimal amount, String currency, LocalDate date, String reasonCode, String reason) {
        static RejectedTransactionResponse from(int row, CashflowIngestionService.RejectedTransaction result) {
            var transaction = result.transaction();
            return new RejectedTransactionResponse(
                    row,
                    result.movementId(),
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

    public record RowErrorResponse(int row, String field, String message) {
    }
}
