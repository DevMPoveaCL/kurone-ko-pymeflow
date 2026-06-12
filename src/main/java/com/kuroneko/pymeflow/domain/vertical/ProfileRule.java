package com.kuroneko.pymeflow.domain.vertical;

import java.math.BigDecimal;

public record ProfileRule(
        String ruleKey,
        String condition,
        BigDecimal threshold,
        String actionKey
) {
    public ProfileRule {
        requireText(ruleKey, "Rule key is required");
        requireText(condition, "Condition is required");
        requireText(actionKey, "Action key is required");
        if (threshold == null) {
            throw new IllegalArgumentException("Threshold is required");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
