package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.application.cashflow.SensitiveDataPolicy;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementEntry;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportCommand;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportPort;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/cashflow/imports/bank-statement/simulated")
@Tag(name = "Cashflow simulated bank statement import")
public class CashflowBankStatementSimulatedController {
    private static final String CLP = "CLP";
    private static final String MANUAL_REVIEW_REASON = "Requiere clasificación manual.";
    private static final String SENSITIVE_REJECTION_REASON = "La transacción contiene datos sensibles y no fue clasificada.";

    private final ExternalStatementImportPort externalStatementImportPort;
    private final SensitiveDataPolicy sensitiveDataPolicy;

    public CashflowBankStatementSimulatedController(
            ExternalStatementImportPort externalStatementImportPort,
            SensitiveDataPolicy sensitiveDataPolicy
    ) {
        this.externalStatementImportPort = externalStatementImportPort;
        this.sensitiveDataPolicy = sensitiveDataPolicy;
    }

    /**
     * Direction loss — signed amounts become positive downstream; debit/credit direction is lost by design
     * for the MVP tradeoff.
     */
    @PostMapping
    @Operation(
            summary = "Import simulated bank statement rows",
            description = "Procesa filas simuladas con forma de cartola bancaria para validar el límite anti-corrupción. "
                    + "Tradeoff MVP: los montos con signo se convierten a valores positivos en el adaptador; "
                    + "se pierde la distinción débito/crédito."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Al menos una fila fue aceptada para importación."),
            @ApiResponse(responseCode = "400", description = "Todas las filas fueron inválidas; se retornan errores por fila.")
    })
    public ResponseEntity<SimulatedBankStatementResponse> importSimulated(
            @RequestBody SimulatedBankStatementRequest request
    ) {
        var errors = new ArrayList<RowErrorResponse>();
        var validEntries = new ArrayList<ExternalStatementEntry>();
        var validRows = new ArrayList<Integer>();
        var rows = request.rows() == null ? List.<SimulatedBankStatementRow>of() : request.rows();
        var duplicateBankTransactionIds = duplicateBankTransactionIds(rows);

        if (rows.isEmpty()) {
            errors.add(new RowErrorResponse(1, "rows", "Debe incluir al menos una fila."));
        }

        for (int index = 0; index < rows.size(); index++) {
            var rowNumber = index + 1;
            var row = rows.get(index);
            var error = validateRow(row, rowNumber, duplicateBankTransactionIds);
            if (error.isPresent()) {
                errors.add(error.orElseThrow());
                continue;
            }
            validEntries.add(toExternalStatementEntry(row));
            validRows.add(rowNumber);
        }

        var result = validEntries.isEmpty()
                ? new CashflowIngestionService.CashflowIngestionResult(List.of(), List.of(), List.of())
                : externalStatementImportPort.importStatement(new ExternalStatementImportCommand(
                new ProfileId(request.profileId()),
                request.importLabel(),
                validEntries
        ));

        var response = fromResult(request.profileId(), validEntries.size(), result, validRows, validEntries, errors);
        var status = validEntries.isEmpty() ? HttpStatus.BAD_REQUEST : HttpStatus.OK;

        return ResponseEntity.status(status).body(response);
    }

    private Optional<RowErrorResponse> validateRow(
            SimulatedBankStatementRow row,
            int rowNumber,
            Set<String> duplicateBankTransactionIds
    ) {
        if (row.bankTransactionId() == null || row.bankTransactionId().isBlank()) {
            return Optional.of(new RowErrorResponse(rowNumber, "bankTransactionId", "El identificador bancario es obligatorio."));
        }
        if (duplicateBankTransactionIds.contains(row.bankTransactionId().trim())) {
            return Optional.of(new RowErrorResponse(rowNumber, "bankTransactionId", "El identificador bancario está duplicado en la solicitud."));
        }
        if (sensitiveDataPolicy.rejectsText(row.bankTransactionId())) {
            return Optional.of(new RowErrorResponse(rowNumber, "bankTransactionId", "El identificador bancario contiene datos sensibles."));
        }
        if (row.bookingDate() == null || row.bookingDate().isBlank()) {
            return Optional.of(new RowErrorResponse(rowNumber, "bookingDate", "La fecha debe tener formato ISO yyyy-MM-dd."));
        }
        try {
            LocalDate.parse(row.bookingDate());
        } catch (DateTimeParseException exception) {
            return Optional.of(new RowErrorResponse(rowNumber, "bookingDate", "La fecha debe tener formato ISO yyyy-MM-dd."));
        }
        if (row.description() == null || row.description().isBlank()) {
            return Optional.of(new RowErrorResponse(rowNumber, "description", "La descripción es obligatoria."));
        }
        if (row.amount() == null || row.amount().compareTo(BigDecimal.ZERO) == 0) {
            return Optional.of(new RowErrorResponse(rowNumber, "amount", "El monto debe ser distinto de cero."));
        }
        if (!CLP.equals(row.currency())) {
            return Optional.of(new RowErrorResponse(rowNumber, "currency", "La única moneda soportada es CLP."));
        }
        if (row.accountAlias() == null || row.accountAlias().isBlank()) {
            return Optional.of(new RowErrorResponse(rowNumber, "accountAlias", "La cuenta de origen es obligatoria."));
        }
        return Optional.empty();
    }

    private static Set<String> duplicateBankTransactionIds(List<SimulatedBankStatementRow> rows) {
        var seen = new HashSet<String>();
        var duplicates = new HashSet<String>();
        for (var row : rows) {
            if (row.bankTransactionId() == null || row.bankTransactionId().isBlank()) {
                continue;
            }
            var bankTransactionId = row.bankTransactionId().trim();
            if (!seen.add(bankTransactionId)) {
                duplicates.add(bankTransactionId);
            }
        }
        return duplicates;
    }

    private static ExternalStatementEntry toExternalStatementEntry(SimulatedBankStatementRow row) {
        return new ExternalStatementEntry(
                row.bankTransactionId().trim(),
                LocalDate.parse(row.bookingDate()),
                row.description(),
                row.amount(),
                Currency.getInstance(CLP),
                row.counterpartyName(),
                row.accountAlias()
        );
    }

    private static SimulatedBankStatementResponse fromResult(
            String profileId,
            int accepted,
            CashflowIngestionService.CashflowIngestionResult result,
            List<Integer> validRows,
            List<ExternalStatementEntry> validEntries,
            List<RowErrorResponse> errors
    ) {
        var rowMapper = new ResultRowMapper(validRows, validEntries);
        var categorized = result.categorized().stream()
                .map(item -> CategorizedTransactionResponse.from(rowMapper.rowFor(item.sourceReference()), item))
                .toList();
        var manualReview = result.manualReview().stream()
                .map(item -> ManualReviewTransactionResponse.from(rowMapper.rowFor(item.sourceReference()), item))
                .toList();
        var rejected = result.rejected().stream()
                .map(item -> RejectedTransactionResponse.from(rowMapper.rowFor(item.sourceReference()), item))
                .toList();

        return new SimulatedBankStatementResponse(
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

    public record SimulatedBankStatementRequest(
            String profileId,
            String importLabel,
            List<SimulatedBankStatementRow> rows
    ) {
    }

    public record SimulatedBankStatementRow(
            String bankTransactionId,
            String bookingDate,
            String description,
            BigDecimal amount,
            String currency,
            String accountAlias,
            String counterpartyName
    ) {
    }

    private static final class ResultRowMapper {
        private final java.util.Map<String, Integer> rowBySourceReference;
        private final List<Integer> validRows;
        private int nextIndex;

        private ResultRowMapper(List<Integer> validRows, List<ExternalStatementEntry> validEntries) {
            this.validRows = List.copyOf(validRows);
            this.rowBySourceReference = new HashMap<>();
            for (int index = 0; index < validRows.size() && index < validEntries.size(); index++) {
                rowBySourceReference.put(validEntries.get(index).externalReference().trim(), validRows.get(index));
            }
        }

        private int rowFor(String sourceReference) {
            if (sourceReference != null && rowBySourceReference.containsKey(sourceReference.trim())) {
                return rowBySourceReference.get(sourceReference.trim());
            }
            return nextRow();
        }

        private int nextRow() {
            if (nextIndex >= validRows.size()) {
                throw new IllegalArgumentException("Result row mapping exceeded valid row count");
            }
            return validRows.get(nextIndex++);
        }
    }

    public record SimulatedBankStatementResponse(
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
