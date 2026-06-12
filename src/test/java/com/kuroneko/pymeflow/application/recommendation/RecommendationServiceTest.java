package com.kuroneko.pymeflow.application.recommendation;

import com.kuroneko.pymeflow.application.port.out.RecommendationTemplatePort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.recommendation.RecommendationContext;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.ProfileRule;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationServiceTest {

    @Test
    void rendersTemplateForMatchingProfileRule() {
        var profileId = new ProfileId("retail-cl");
        var rule = new ProfileRule("low-balance", "projected_balance_below_threshold", BigDecimal.valueOf(500), "low-balance-template");
        var profileService = new VerticalProfileService(id -> Optional.of(profile(profileId, List.of(rule))));
        RecommendationTemplatePort templatePort = (context, templateKey) -> "rendered:" + templateKey;
        var service = new RecommendationService(profileService, templatePort);

        var results = service.generate(new RecommendationService.RecommendationRequest(
                profileId,
                new RecommendationService.MerchantRecommendationState(BigDecimal.valueOf(100), null, null, Map.of())
        ));

        assertThat(results).singleElement()
                .extracting(RecommendationService.RecommendationResult::text)
                .isEqualTo("rendered:low-balance-template");
    }

    @Test
    void skipsUnknownConditions() {
        var profileId = new ProfileId("retail-cl");
        var rule = new ProfileRule("unknown", "not_configured", BigDecimal.ZERO, "template");
        var profileService = new VerticalProfileService(id -> Optional.of(profile(profileId, List.of(rule))));
        RecommendationTemplatePort templatePort = this::failIfRendered;
        var service = new RecommendationService(profileService, templatePort);

        var results = service.generate(new RecommendationService.RecommendationRequest(
                profileId,
                new RecommendationService.MerchantRecommendationState(BigDecimal.valueOf(100), null, null, Map.of())
        ));

        assertThat(results).isEmpty();
    }

    @Test
    void evaluatesDateOrderingRuleBeforeRenderingTemplate() {
        var profileId = new ProfileId("retail-cl");
        var rule = new ProfileRule("due-before-inflow", "obligations_due_before_cash_inflow", BigDecimal.ZERO, "timing-alert");
        var profileService = new VerticalProfileService(id -> Optional.of(profile(profileId, List.of(rule))));
        RecommendationTemplatePort templatePort = (context, templateKey) -> "rendered:" + templateKey;
        var service = new RecommendationService(profileService, templatePort);

        var results = service.generate(new RecommendationService.RecommendationRequest(
                profileId,
                new RecommendationService.MerchantRecommendationState(
                        BigDecimal.valueOf(1000),
                        java.time.LocalDate.of(2026, 6, 5),
                        java.time.LocalDate.of(2026, 6, 10),
                        Map.of()
                )
        ));

        assertThat(results).singleElement()
                .extracting(RecommendationService.RecommendationResult::text)
                .isEqualTo("rendered:timing-alert");
    }

    private String failIfRendered(RecommendationContext context, String templateKey) {
        throw new AssertionError("Template should not be rendered");
    }

    private static VerticalProfile profile(ProfileId profileId, List<ProfileRule> rules) {
        return new VerticalProfile(
                profileId,
                "Retail",
                rules,
                List.of(new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW)),
                List.of()
        );
    }
}
