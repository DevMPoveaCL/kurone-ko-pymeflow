package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.recommendation.RecommendationContext;

public interface RecommendationTemplatePort {
    String render(RecommendationContext context, String templateKey);
}
