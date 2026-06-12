package com.kuroneko.pymeflow.application.recommendation;

import com.kuroneko.pymeflow.application.port.out.RecommendationTemplatePort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.recommendation.RecommendationContext;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.ProfileRule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecommendationService {
    private static final String PROJECTED_BALANCE_BELOW_THRESHOLD = "projected_balance_below_threshold";
    private static final String PROJECTED_BALANCE_ABOVE_THRESHOLD = "projected_balance_above_threshold";
    private static final String OBLIGATIONS_DUE_BEFORE_CASH_INFLOW = "obligations_due_before_cash_inflow";

    private final VerticalProfileService verticalProfileService;
    private final RecommendationTemplatePort recommendationTemplatePort;

    public RecommendationService(
            VerticalProfileService verticalProfileService,
            RecommendationTemplatePort recommendationTemplatePort
    ) {
        this.verticalProfileService = verticalProfileService;
        this.recommendationTemplatePort = recommendationTemplatePort;
    }

    public List<RecommendationResult> generate(RecommendationRequest request) {
        var results = new ArrayList<RecommendationResult>();

        for (ProfileRule rule : verticalProfileService.activeRules(request.profileId())) {
            if (!matches(rule, request.state())) {
                continue;
            }

            var context = new RecommendationContext(
                    request.profileId(),
                    request.state().projectedBalance(),
                    contextVariables(request.state(), rule)
            );
            var rendered = recommendationTemplatePort.render(context, rule.actionKey());
            results.add(new RecommendationResult(rule.ruleKey(), rule.actionKey(), rendered));
        }

        return List.copyOf(results);
    }

    private static boolean matches(ProfileRule rule, MerchantRecommendationState state) {
        return switch (rule.condition()) {
            case PROJECTED_BALANCE_BELOW_THRESHOLD -> state.projectedBalance().compareTo(rule.threshold()) < 0;
            case PROJECTED_BALANCE_ABOVE_THRESHOLD -> state.projectedBalance().compareTo(rule.threshold()) > 0;
            case OBLIGATIONS_DUE_BEFORE_CASH_INFLOW -> state.nextObligationDueAt() != null
                    && state.nextExpectedInflowAt() != null
                    && state.nextObligationDueAt().isBefore(state.nextExpectedInflowAt());
            default -> false;
        };
    }

    private static Map<String, Object> contextVariables(MerchantRecommendationState state, ProfileRule rule) {
        var variables = new HashMap<>(state.variables());
        variables.put("ruleKey", rule.ruleKey());
        variables.put("actionKey", rule.actionKey());
        variables.put("condition", rule.condition());
        variables.put("threshold", rule.threshold());
        variables.put("projectedBalance", state.projectedBalance());
        return variables;
    }

    public record RecommendationRequest(ProfileId profileId, MerchantRecommendationState state) {
        public RecommendationRequest {
            if (profileId == null) {
                throw new IllegalArgumentException("Profile id is required");
            }
            if (state == null) {
                throw new IllegalArgumentException("Merchant state is required");
            }
        }
    }

    public record MerchantRecommendationState(
            BigDecimal projectedBalance,
            LocalDate nextObligationDueAt,
            LocalDate nextExpectedInflowAt,
            Map<String, Object> variables
    ) {
        public MerchantRecommendationState {
            if (projectedBalance == null) {
                throw new IllegalArgumentException("Projected balance is required");
            }
            variables = Map.copyOf(variables == null ? Map.of() : variables);
        }
    }

    public record RecommendationResult(String ruleKey, String actionKey, String text) {
        public RecommendationResult {
            if (ruleKey == null || ruleKey.isBlank()) {
                throw new IllegalArgumentException("Rule key is required");
            }
            if (actionKey == null || actionKey.isBlank()) {
                throw new IllegalArgumentException("Action key is required");
            }
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Recommendation text is required");
            }
        }
    }
}
