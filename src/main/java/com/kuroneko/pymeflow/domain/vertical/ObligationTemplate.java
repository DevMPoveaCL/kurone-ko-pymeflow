package com.kuroneko.pymeflow.domain.vertical;

import java.math.BigDecimal;
import java.time.Period;

public record ObligationTemplate(
        String key,
        String displayName,
        BigDecimal estimatedAmount,
        Period frequency,
        int dueDayOfMonth
) {
    public ObligationTemplate {
        requireText(key, "Obligation key is required");
        requireText(displayName, "Obligation display name is required");
        if (estimatedAmount == null || estimatedAmount.signum() < 0) {
            throw new IllegalArgumentException("Estimated amount must be zero or positive");
        }
        if (frequency == null || frequency.isZero() || frequency.isNegative()) {
            throw new IllegalArgumentException("Frequency must be positive");
        }
        if (dueDayOfMonth < 1 || dueDayOfMonth > 31) {
            throw new IllegalArgumentException("Due day must be between 1 and 31");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
