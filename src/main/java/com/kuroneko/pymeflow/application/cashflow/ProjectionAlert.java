package com.kuroneko.pymeflow.application.cashflow;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectionAlert(
        String ruleKey,
        String actionKey,
        String condition,
        LocalDate date,
        BigDecimal balance
) {
    public ProjectionAlert {
        requireText(ruleKey, "Rule key is required");
        requireText(actionKey, "Action key is required");
        requireText(condition, "Condition is required");
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (balance == null) {
            throw new IllegalArgumentException("Balance is required");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
