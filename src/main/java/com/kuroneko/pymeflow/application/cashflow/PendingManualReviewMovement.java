package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

public record PendingManualReviewMovement(
        UUID movementId,
        BigDecimal amount,
        Currency currency,
        LocalDate date,
        TransactionDirection direction,
        String description,
        String sourceReference,
        CashflowMovementStatus status
) {
    public PendingManualReviewMovement(
            UUID movementId,
            BigDecimal amount,
            Currency currency,
            LocalDate date,
            String description,
            String sourceReference,
            CashflowMovementStatus status
    ) {
        this(movementId, amount, currency, date, TransactionDirection.CREDIT, description, sourceReference, status);
    }

    public PendingManualReviewMovement {
        if (movementId == null) {
            throw new IllegalArgumentException("Movement id is required");
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
        if (direction == null) {
            throw new IllegalArgumentException("Direction is required");
        }
        if (status != CashflowMovementStatus.MANUAL_REVIEW) {
            throw new IllegalArgumentException("Pending movement must be in manual review");
        }
    }

    static PendingManualReviewMovement from(CashflowMovementRecord record) {
        return new PendingManualReviewMovement(
                record.id(),
                record.amount(),
                record.currency(),
                record.date(),
                record.direction(),
                record.safeDescription(),
                record.sourceReference(),
                record.status()
        );
    }
}
