package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;

import java.util.Optional;

public record PersistedManualReviewResolutionResult(
        ProjectionReadyCashflowTransaction transaction,
        CashflowCategory category,
        Optional<String> safeDescription,
        Optional<String> safeSourceReference
) {
    public PersistedManualReviewResolutionResult {
        if (transaction == null) {
            throw new IllegalArgumentException("Projection-ready transaction is required");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category is required");
        }
        safeDescription = safeDescription == null ? Optional.empty() : safeDescription;
        safeSourceReference = safeSourceReference == null ? Optional.empty() : safeSourceReference;
    }
}
