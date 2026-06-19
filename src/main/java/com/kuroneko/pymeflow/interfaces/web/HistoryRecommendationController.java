package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.recommendation.HistoryRecommendationService;
import com.kuroneko.pymeflow.application.recommendation.HistoryRecommendationService.HistoryRecommendationResponse;
import com.kuroneko.pymeflow.application.recommendation.HistoryRecommendationService.HistorySignalResponse;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cashflow/recommendations")
@Tag(name = "Cashflow recommendations")
public class HistoryRecommendationController {
    private final HistoryRecommendationService historyRecommendationService;

    public HistoryRecommendationController(HistoryRecommendationService historyRecommendationService) {
        this.historyRecommendationService = historyRecommendationService;
    }

    @GetMapping
    @Operation(
            summary = "Obtener recomendaciones del historial de caja",
            description = "Entrega recomendaciones determinísticas desde el historial persistido, sin guardar resultados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recomendaciones generadas desde el historial persistido."),
            @ApiResponse(responseCode = "400", description = "El perfil es obligatorio o no es válido.")
    })
    public ResponseEntity<RecommendationResponse> recommendations(@RequestParam(required = false) String profileId) {
        validateProfileId(profileId);
        return ResponseEntity.ok(RecommendationResponse.from(
                historyRecommendationService.generate(new ProfileId(profileId))
        ));
    }

    private static void validateProfileId(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            throw new ApiExceptionHandler.ApiValidationException(List.of(
                    new ApiExceptionHandler.ValidationErrorResponse("profileId", "El perfil es obligatorio.")
            ));
        }
    }

    public record RecommendationResponse(
            @Schema(example = "pharmacy-cl")
            String profileId,
            Instant generatedAt,
            List<RecommendationSignalResponse> signals
    ) {
        static RecommendationResponse from(HistoryRecommendationResponse response) {
            return new RecommendationResponse(
                    response.profileId(),
                    response.generatedAt(),
                    response.signals().stream().map(RecommendationSignalResponse::from).toList()
            );
        }
    }

    public record RecommendationSignalResponse(
            @Schema(example = "HIGH_REJECTION_RATE")
            String type,
            @Schema(example = "WARNING")
            String severity,
            String title,
            String description,
            String actionHint,
            Map<String, Object> metrics
    ) {
        static RecommendationSignalResponse from(HistorySignalResponse signal) {
            return new RecommendationSignalResponse(
                    signal.type(),
                    signal.severity(),
                    signal.title(),
                    signal.description(),
                    signal.actionHint(),
                    signal.metrics()
            );
        }
    }
}
