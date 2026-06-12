package com.kuroneko.pymeflow.domain.vertical;

public record CashflowCategory(String key, String displayName, CashflowDirection direction) {
    public CashflowCategory {
        requireText(key, "Category key is required");
        requireText(displayName, "Category display name is required");
        if (direction == null) {
            throw new IllegalArgumentException("Category direction is required");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
