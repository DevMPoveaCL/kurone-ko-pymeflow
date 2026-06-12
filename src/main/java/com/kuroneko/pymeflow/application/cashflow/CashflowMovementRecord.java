package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

public record CashflowMovementRecord(
        UUID id,
        ProfileId profileId,
        BigDecimal amount,
        Currency currency,
        LocalDate date,
        CashflowMovementStatus status,
        String categoryKey,
        String safeDescription,
        String sourceReference,
        String rejectionReasonCode,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public CashflowMovementRecord {
        if (id == null) {
            throw new IllegalArgumentException("Movement id is required");
        }
        if (profileId == null) {
            throw new IllegalArgumentException("Profile id is required");
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
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Timestamps are required");
        }
    }

    public Optional<String> categoryKeyOptional() {
        return Optional.ofNullable(categoryKey);
    }

    public Optional<String> safeDescriptionOptional() {
        return Optional.ofNullable(safeDescription);
    }

    public Optional<String> sourceReferenceOptional() {
        return Optional.ofNullable(sourceReference);
    }
}
