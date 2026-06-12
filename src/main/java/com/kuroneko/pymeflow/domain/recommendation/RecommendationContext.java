package com.kuroneko.pymeflow.domain.recommendation;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.math.BigDecimal;
import java.util.Map;

public record RecommendationContext(ProfileId profileId, BigDecimal projectedBalance, Map<String, Object> variables) {
    public RecommendationContext {
        if (profileId == null) {
            throw new IllegalArgumentException("Profile id is required");
        }
        if (projectedBalance == null) {
            throw new IllegalArgumentException("Projected balance is required");
        }
        variables = Map.copyOf(variables == null ? Map.of() : variables);
    }
}
