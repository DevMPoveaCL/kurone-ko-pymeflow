package com.kuroneko.pymeflow.infrastructure.recommendation;

import com.kuroneko.pymeflow.domain.recommendation.RecommendationContext;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.infrastructure.config.VerticalProfileProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MustacheRecommendationTemplateAdapterTest {

    @Test
    void rendersNeutralSpanishRecommendationFromResourceTemplate() {
        var properties = new VerticalProfileProperties(
                "pharmacy-cl",
                new VerticalProfileProperties.MockProviders(false, false),
                "classpath:templates/pharmacy-recommendations.es.mustache",
                List.of("receta"),
                List.of(profileDefinition())
        );
        var adapter = new MustacheRecommendationTemplateAdapter(new DefaultResourceLoader(), properties);

        var rendered = adapter.render(
                new RecommendationContext(new ProfileId("pharmacy-cl"), BigDecimal.valueOf(100), Map.of()),
                "low-balance-warning"
        );

        assertThat(rendered)
                .contains("Revisa los próximos pagos")
                .doesNotContainIgnoringCase("prescripción")
                .doesNotContainIgnoringCase("receta");
    }

    private static VerticalProfileProperties.ProfileDefinition profileDefinition() {
        return new VerticalProfileProperties.ProfileDefinition(
                "pharmacy-cl",
                "Farmacia chilena",
                List.of(new VerticalProfileProperties.CategoryDefinition("sales", "Ventas", "INFLOW")),
                List.of(),
                List.of(),
                Map.of()
        );
    }
}
