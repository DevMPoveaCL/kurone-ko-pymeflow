package com.kuroneko.pymeflow.application.cashflow;

import java.util.Locale;

public enum CashflowMovementStatus {
    MANUAL_REVIEW,
    PROJECTABLE,
    REJECTED;

    public static CashflowMovementStatus fromApiStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        var normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("CATEGORIZED".equals(normalized)) {
            return PROJECTABLE;
        }
        return CashflowMovementStatus.valueOf(normalized);
    }

    public boolean canTransitionTo(CashflowMovementStatus target) {
        return this == MANUAL_REVIEW && target == PROJECTABLE;
    }

    public boolean isProjectionReady() {
        return this == PROJECTABLE;
    }

    public boolean isPendingManualReview() {
        return this == MANUAL_REVIEW;
    }
}
