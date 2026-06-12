package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.util.UUID;

public record ManualReviewMovementResolutionCommand(
        UUID movementId,
        ProfileId profileId,
        String categoryKey
) {
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
    }
}
