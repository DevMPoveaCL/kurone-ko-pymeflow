package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.util.UUID;

public record ManualReviewMovementResolutionCommand(
        UUID movementId,
        ProfileId profileId,
        String categoryKey,
        String description,
        String sourceReference
) {
    public ManualReviewMovementResolutionCommand(UUID movementId, ProfileId profileId, String categoryKey) {
        this(movementId, profileId, categoryKey, null, null);
    }

    public ManualReviewMovementResolutionCommand {
        if (movementId == null) {
            throw new IllegalArgumentException("Movement id is required");
        }
        if (profileId == null) {
            throw new IllegalArgumentException("Profile id is required");
        }
        if (categoryKey == null || categoryKey.isBlank()) {
            throw new IllegalArgumentException("Category key is required");
        }
        categoryKey = categoryKey.trim();
        if (categoryKey.length() > 80) {
            throw new IllegalArgumentException("Category key is too long");
        }
        description = normalizeOptional(description, 160, "Description is too long");
        sourceReference = normalizeOptional(sourceReference, 80, "Source reference is too long");
    }

    private static String normalizeOptional(String value, int maxLength, String message) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }
}
