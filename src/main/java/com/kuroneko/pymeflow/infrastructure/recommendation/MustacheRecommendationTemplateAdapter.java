package com.kuroneko.pymeflow.infrastructure.recommendation;

import com.kuroneko.pymeflow.application.port.out.RecommendationTemplatePort;
import com.kuroneko.pymeflow.domain.recommendation.RecommendationContext;
import com.kuroneko.pymeflow.infrastructure.config.VerticalProfileProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MustacheRecommendationTemplateAdapter implements RecommendationTemplatePort {
    private final Map<String, String> templates;

    public MustacheRecommendationTemplateAdapter(
            ResourceLoader resourceLoader,
            VerticalProfileProperties properties
    ) {
        this.templates = loadTemplates(resourceLoader, properties.recommendationTemplatePath());
    }

    @Override
    public String render(RecommendationContext context, String templateKey) {
        var template = templates.get(templateKey);
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("Recommendation template not found: " + templateKey);
        }
        var rendered = template;
        for (Map.Entry<String, Object> variable : context.variables().entrySet()) {
            rendered = rendered.replace("{{" + variable.getKey() + "}}", String.valueOf(variable.getValue()));
        }
        return rendered;
    }

    private static Map<String, String> loadTemplates(ResourceLoader resourceLoader, String templatePath) {
        try (var reader = new BufferedReader(new InputStreamReader(
                resourceLoader.getResource(templatePath).getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.isBlank())
                    .filter(line -> !line.trim().startsWith("#"))
                    .map(line -> line.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toUnmodifiableMap(parts -> parts[0].trim(), parts -> parts[1].trim()));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load recommendation templates", exception);
        }
    }
}
