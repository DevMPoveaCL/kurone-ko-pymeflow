package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;

import java.util.Optional;

public record ManualReviewResolutionResult(
        ProjectedCashflowTransaction transaction,
        CashflowCategory category,
        Optional<String> safeDescription,
        Optional<String> safeSourceReference,
        String outputStatus
) {
    public ManualReviewResolutionResult {
        if (transaction == null) {
            throw new IllegalArgumentException("Projection transaction is required");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category is required");
        }
        safeDescription = safeDescription == null ? Optional.empty() : safeDescription;
        safeSourceReference = safeSourceReference == null ? Optional.empty() : safeSourceReference;
        if (outputStatus == null || outputStatus.isBlank()) {
            throw new IllegalArgumentException("Output status is required");
        }
    }
}
