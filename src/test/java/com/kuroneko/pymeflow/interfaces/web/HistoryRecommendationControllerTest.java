package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.recommendation.HistoryRecommendationService;
import com.kuroneko.pymeflow.application.recommendation.HistoryRecommendationService.HistoryRecommendationResponse;
import com.kuroneko.pymeflow.application.recommendation.HistoryRecommendationService.HistorySignalResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HistoryRecommendationController.class)
class HistoryRecommendationControllerTest {

    @MockBean
    private HistoryRecommendationService historyRecommendationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsSuccessfulAndValidationErrorOpenApiResponses() throws Exception {
        Method method = HistoryRecommendationController.class.getMethod("recommendations", String.class);
        ApiResponses apiResponses = method.getAnnotation(ApiResponses.class);

        assertThat(apiResponses).isNotNull();
        assertThat(Arrays.stream(apiResponses.value()).map(response -> response.responseCode()))
                .containsExactlyInAnyOrderElementsOf(Set.of("200", "400"));
    }

    @Test
    void returnsRecommendationResponseShapeInDeterministicServiceOrder() throws Exception {
        when(historyRecommendationService.generate(any())).thenReturn(new HistoryRecommendationResponse(
                "pharmacy-cl",
                Instant.parse("2026-06-18T10:15:30Z"),
                List.of(
                        new HistorySignalResponse(
                                "HIGH_REJECTION_RATE",
                                "WARNING",
                                "Alta tasa de rechazo",
                                "Una proporción relevante del historial fue rechazada por datos inválidos o sensibles.",
                                "Revisa los motivos de rechazo para corregir la fuente de datos.",
                                Map.of(
                                        "rejectedCount", 4,
                                        "projectableCount", 6,
                                        "rejectionRatePercent", BigDecimal.valueOf(40),
                                        "topRejectionReasonCode", "SENSITIVE_DATA"
                                )
                        ),
                        new HistorySignalResponse(
                                "INSUFFICIENT_DATA",
                                "INFO",
                                "Datos insuficientes",
                                "El historial proyectable aún no alcanza el volumen mínimo para señales más robustas.",
                                "Agrega más movimientos para obtener mejores recomendaciones.",
                                Map.of("projectableCount", 6, "minimumProjectableCount", 10)
                        )
                )
        ));

        mockMvc.perform(get("/api/cashflow/recommendations")
                        .param("profileId", "pharmacy-cl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value("pharmacy-cl"))
                .andExpect(jsonPath("$.generatedAt").value("2026-06-18T10:15:30Z"))
                .andExpect(jsonPath("$.signals[0].type").value("HIGH_REJECTION_RATE"))
                .andExpect(jsonPath("$.signals[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.signals[0].title").value("Alta tasa de rechazo"))
                .andExpect(jsonPath("$.signals[0].description")
                        .value("Una proporción relevante del historial fue rechazada por datos inválidos o sensibles."))
                .andExpect(jsonPath("$.signals[0].actionHint")
                        .value("Revisa los motivos de rechazo para corregir la fuente de datos."))
                .andExpect(jsonPath("$.signals[0].metrics.rejectedCount").value(4))
                .andExpect(jsonPath("$.signals[0].metrics.projectableCount").value(6))
                .andExpect(jsonPath("$.signals[0].metrics.rejectionRatePercent").value(40))
                .andExpect(jsonPath("$.signals[0].metrics.topRejectionReasonCode").value("SENSITIVE_DATA"))
                .andExpect(jsonPath("$.signals[1].type").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.signals[1].severity").value("INFO"))
                .andExpect(jsonPath("$.signals[1].title").value("Datos insuficientes"));

        verify(historyRecommendationService).generate(any());
    }

    @Test
    void returnsValidationErrorForBlankProfileId() throws Exception {
        mockMvc.perform(get("/api/cashflow/recommendations")
                        .param("profileId", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Revise los datos enviados e intente nuevamente."))
                .andExpect(jsonPath("$.errors[0].field").value("profileId"))
                .andExpect(jsonPath("$.errors[0].message").value("El perfil es obligatorio."));

        verifyNoInteractions(historyRecommendationService);
    }

    @Test
    void returnsValidationErrorForMissingProfileId() throws Exception {
        mockMvc.perform(get("/api/cashflow/recommendations"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Revise los datos enviados e intente nuevamente."))
                .andExpect(jsonPath("$.errors[0].field").value("profileId"))
                .andExpect(jsonPath("$.errors[0].message").value("El perfil es obligatorio."));

        verifyNoInteractions(historyRecommendationService);
    }

    @Test
    void returnsUnknownProfileAsNeutralSpanishBadRequest() throws Exception {
        when(historyRecommendationService.generate(any()))
                .thenThrow(new IllegalArgumentException("Profile not found: missing-profile"));

        mockMvc.perform(get("/api/cashflow/recommendations")
                        .param("profileId", "missing-profile"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("El perfil solicitado no está configurado."));
    }

    @Test
    void omitsSensitiveRejectedFieldsAndValuesFromRecommendationResponse() throws Exception {
        when(historyRecommendationService.generate(any())).thenReturn(new HistoryRecommendationResponse(
                "pharmacy-cl",
                Instant.parse("2026-06-18T10:15:30Z"),
                List.of(new HistorySignalResponse(
                        "HIGH_REJECTION_RATE",
                        "WARNING",
                        "Alta tasa de rechazo",
                        "Una proporción relevante del historial fue rechazada por datos inválidos o sensibles.",
                        "Revisa los motivos de rechazo para corregir la fuente de datos.",
                        Map.of(
                                "rejectedCount", 2,
                                "projectableCount", 3,
                                "rejectionRatePercent", BigDecimal.valueOf(40),
                                "topRejectionReasonCode", "SENSITIVE_DATA"
                        )
                ))
        ));

        mockMvc.perform(get("/api/cashflow/recommendations")
                        .param("profileId", "pharmacy-cl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signals[0].metrics.rejectedCount").value(2))
                .andExpect(content().string(not(containsString("safeDescription"))))
                .andExpect(content().string(not(containsString("sourceReference"))))
                .andExpect(content().string(not(containsString("receta magistral"))))
                .andExpect(content().string(not(containsString("paciente"))));
    }
}
