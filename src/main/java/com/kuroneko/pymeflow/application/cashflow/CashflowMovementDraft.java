package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;

public record CashflowMovementDraft(
        ProfileId profileId,
        BigDecimal amount,
        Currency currency,
        LocalDate date,
        TransactionDirection direction,
        CashflowMovementStatus status,
        String categoryKey,
        String safeDescription,
        String sourceReference,
        String rejectionReasonCode
) {
    public CashflowMovementDraft(
            ProfileId profileId,
            BigDecimal amount,
            Currency currency,
            LocalDate date,
            CashflowMovementStatus status,
            String categoryKey,
            String safeDescription,
            String sourceReference,
            String rejectionReasonCode
    ) {
        this(profileId, amount, currency, date, TransactionDirection.CREDIT, status, categoryKey, safeDescription, sourceReference, rejectionReasonCode);
    }

    public CashflowMovementDraft {
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
        if (direction == null) {
            throw new IllegalArgumentException("Direction is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        categoryKey = normalize(categoryKey, 80, "Category key");
        safeDescription = normalize(safeDescription, 160, "Safe description");
        sourceReference = normalize(sourceReference, 80, "Source reference");
        rejectionReasonCode = normalize(rejectionReasonCode, 80, "Rejection reason code");
        if (status == CashflowMovementStatus.PROJECTABLE && categoryKey == null) {
            throw new IllegalArgumentException("Category key is required for projectable movements");
        }
        if (status != CashflowMovementStatus.PROJECTABLE && categoryKey != null) {
            throw new IllegalArgumentException("Category key is only allowed for projectable movements");
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

    public Optional<String> rejectionReasonCodeOptional() {
        return Optional.ofNullable(rejectionReasonCode);
    }

    private static String normalize(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " is too long");
        }
        return trimmed;
    }
}
