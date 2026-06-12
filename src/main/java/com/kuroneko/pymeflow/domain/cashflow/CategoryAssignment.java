package com.kuroneko.pymeflow.domain.cashflow;

import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;

import java.util.Optional;

public record CategoryAssignment(Optional<CashflowCategory> category, boolean requiresManualReview) {
    public CategoryAssignment {
        category = category == null ? Optional.empty() : category;
    }
}
