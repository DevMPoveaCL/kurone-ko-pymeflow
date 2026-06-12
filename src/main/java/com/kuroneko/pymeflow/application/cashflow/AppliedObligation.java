package com.kuroneko.pymeflow.application.cashflow;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AppliedObligation(
        String obligationKey,
        String displayName,
        LocalDate dueDate,
        BigDecimal amount
) {
    public AppliedObligation {
        requireText(obligationKey, "Obligation key is required");
        requireText(displayName, "Obligation display name is required");
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date is required");
        }
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Amount must be zero or positive");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
