package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.ProfileRule;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import com.kuroneko.pymeflow.infrastructure.config.VerticalProfileProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    private final VerticalProfileService verticalProfileService;
    private final VerticalProfileProperties properties;

    public ProfileController(VerticalProfileService verticalProfileService, VerticalProfileProperties properties) {
        this.verticalProfileService = verticalProfileService;
        this.properties = properties;
    }

    @GetMapping("/active")
    public ActiveProfileResponse activeProfile() {
        return ActiveProfileResponse.from(loadActiveProfile());
    }

    @GetMapping("/active/rules")
    public List<ProfileRuleResponse> activeRules() {
        return loadActiveProfile().rules().stream()
                .map(ProfileRuleResponse::from)
                .toList();
    }

    @GetMapping("/active/categories")
    public List<CategoryResponse> activeCategories() {
        return loadActiveProfile().categories().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    private VerticalProfile loadActiveProfile() {
        return verticalProfileService.loadProfile(new ProfileId(properties.activeProfileId()));
    }

    public record ActiveProfileResponse(String id, String displayName) {
        static ActiveProfileResponse from(VerticalProfile profile) {
            return new ActiveProfileResponse(profile.id().value(), profile.displayName());
        }
    }

    public record ProfileRuleResponse(String ruleKey, String condition, BigDecimal threshold, String actionKey) {
        static ProfileRuleResponse from(ProfileRule rule) {
            return new ProfileRuleResponse(rule.ruleKey(), rule.condition(), rule.threshold(), rule.actionKey());
        }
    }

    public record CategoryResponse(String key, String displayName, String direction) {
        static CategoryResponse from(CashflowCategory category) {
            return new CategoryResponse(category.key(), category.displayName(), category.direction().name());
        }
    }
}
