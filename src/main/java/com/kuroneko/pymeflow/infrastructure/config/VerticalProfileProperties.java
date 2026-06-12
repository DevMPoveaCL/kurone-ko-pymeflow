package com.kuroneko.pymeflow.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "pymeflow.vertical-profile")
public record VerticalProfileProperties(
        @NotBlank String activeProfileId,
        MockProviders mockProviders,
        @NotBlank String recommendationTemplatePath,
        List<String> sensitiveIdentifiers,
        @NotEmpty List<@Valid ProfileDefinition> profiles
) {
    public record MockProviders(boolean bankSettlements, boolean acquirerSettlements) {
    }

    public record ProfileDefinition(
            @NotBlank String id,
            @NotBlank String displayName,
            @NotEmpty List<@Valid CategoryDefinition> categories,
            List<@Valid RuleDefinition> rules,
            List<@Valid ObligationDefinition> obligations,
            Map<String, String> templates
    ) {
    }

    public record CategoryDefinition(@NotBlank String key, @NotBlank String displayName, @NotBlank String direction) {
    }

    public record RuleDefinition(
            @NotBlank String key,
            @NotBlank String condition,
            BigDecimal threshold,
            @NotBlank String actionKey
    ) {
    }

    public record ObligationDefinition(
            @NotBlank String key,
            @NotBlank String displayName,
            BigDecimal estimatedAmount,
            @NotBlank String frequency,
            int dueDayOfMonth
    ) {
    }
}
