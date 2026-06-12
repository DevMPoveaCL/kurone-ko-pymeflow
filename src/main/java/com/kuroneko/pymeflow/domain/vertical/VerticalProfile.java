package com.kuroneko.pymeflow.domain.vertical;

import java.util.List;

public record VerticalProfile(
        ProfileId id,
        String displayName,
        List<ProfileRule> rules,
        List<CashflowCategory> categories,
        List<ObligationTemplate> obligations
) {
    public VerticalProfile {
        if (id == null) {
            throw new IllegalArgumentException("Profile id is required");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Profile display name is required");
        }
        rules = List.copyOf(rules == null ? List.of() : rules);
        categories = List.copyOf(categories == null ? List.of() : categories);
        obligations = List.copyOf(obligations == null ? List.of() : obligations);
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("At least one category is required");
        }
    }
}
