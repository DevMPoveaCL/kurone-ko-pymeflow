package com.kuroneko.pymeflow.application.cashflow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

public record ProjectionReadyCashflowTransaction(
        UUID movementId,
        String categoryKey,
        BigDecimal amount,
        Currency currency,
        LocalDate date,
        CashflowMovementStatus status
) {
    public ProjectionReadyCashflowTransaction {
        if (movementId == null) {
            throw new IllegalArgumentException("Movement id is required");
        }
        if (categoryKey == null || categoryKey.isBlank()) {
            throw new IllegalArgumentException("Category key is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (status != CashflowMovementStatus.PROJECTABLE) {
            throw new IllegalArgumentException("Projection-ready transaction must be projectable");
        }
    }

    public ProjectedCashflowTransaction toProjectionTransaction() {
        return new ProjectedCashflowTransaction(categoryKey, amount, currency, date);
    }

    static ProjectionReadyCashflowTransaction from(CashflowMovementRecord record) {
        return new ProjectionReadyCashflowTransaction(
                record.id(),
                record.categoryKey(),
                record.amount(),
                record.currency(),
                record.date(),
                record.status()
        );
    }
}
