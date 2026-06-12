package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.ManualReviewResolutionResult;
import com.kuroneko.pymeflow.application.cashflow.ManualReviewResolutionService;
import com.kuroneko.pymeflow.application.cashflow.ProjectedCashflowTransaction;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManualReviewResolutionController.class)
class ManualReviewResolutionControllerTest {

    @MockBean
    private ManualReviewResolutionService manualReviewResolutionService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void resolvesManualReviewMovementIntoProjectionCompatibleResponse() throws Exception {
        when(manualReviewResolutionService.resolve(any())).thenReturn(result(
                ManualReviewResolutionService.STATUS_PROJECTABLE,
                Optional.of("Venta Caja 1"),
                Optional.of("caja-1")
        ));

        mockMvc.perform(post("/api/cashflow/manual-review/resolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.categoryKey").value("sales"))
                .andExpect(jsonPath("$.transaction.amount").value(125000))
                .andExpect(jsonPath("$.transaction.currency").value("CLP"))
                .andExpect(jsonPath("$.transaction.date").value("2026-06-11"))
                .andExpect(jsonPath("$.transaction.status").value("PROJECTABLE"))
                .andExpect(jsonPath("$.category.key").value("sales"))
                .andExpect(jsonPath("$.category.displayName").value("Ventas"))
                .andExpect(jsonPath("$.category.direction").value("INFLOW"))
                .andExpect(jsonPath("$.description").value("Venta Caja 1"))
                .andExpect(jsonPath("$.sourceReference").value("caja-1"));

        verify(manualReviewResolutionService).resolve(any());
    }

    @Test
    void returnsUnknownProfileAndCategoryAsNeutralSpanishErrors() throws Exception {
        when(manualReviewResolutionService.resolve(any()))
                .thenThrow(new IllegalArgumentException("El perfil indicado no está configurado."));

        mockMvc.perform(post("/api/cashflow/manual-review/resolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload().replace("pharmacy-cl", "missing-profile")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("El perfil solicitado no está configurado."));

        reset(manualReviewResolutionService);
        when(manualReviewResolutionService.resolve(any()))
                .thenThrow(new IllegalArgumentException("La categoría seleccionada no existe para el perfil indicado."));

        mockMvc.perform(post("/api/cashflow/manual-review/resolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload().replace("sales", "unknown")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La categoría enviada no está configurada para el perfil."));
    }

    @Test
    void returnsValidationErrorsForInvalidAmountCurrencyAndDate() throws Exception {
        mockMvc.perform(post("/api/cashflow/manual-review/resolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "chosenCategoryKey": "sales",
                                  "amount": -1,
                                  "currency": "USD"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(content().string(containsString("El monto debe ser mayor que cero.")))
                .andExpect(content().string(containsString("La moneda soportada es CLP.")))
                .andExpect(content().string(containsString("La fecha es obligatoria.")));

        verifyNoInteractions(manualReviewResolutionService);
    }

    @Test
    void rejectsSensitiveDescriptionWithoutEchoingRequestText() throws Exception {
        var sensitiveDescription = "Venta Caja 1 receta 12345";
        when(manualReviewResolutionService.resolve(any()))
                .thenThrow(new IllegalArgumentException("La descripción contiene información sensible y no puede proyectarse."));

        mockMvc.perform(post("/api/cashflow/manual-review/resolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload().replace("Venta Caja 1", sensitiveDescription)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La información enviada contiene datos sensibles y no puede proyectarse."))
                .andExpect(content().string(not(containsString(sensitiveDescription))))
                .andExpect(content().string(not(containsString("receta 12345"))));
    }

    @Test
    void rejectsSensitiveSourceReferenceWithoutEchoingRequestText() throws Exception {
        var sensitiveReference = "paciente-987654321";
        when(manualReviewResolutionService.resolve(any()))
                .thenThrow(new IllegalArgumentException("La referencia de origen contiene información sensible y no puede proyectarse."));

        mockMvc.perform(post("/api/cashflow/manual-review/resolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload().replace("caja-1", sensitiveReference)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La información enviada contiene datos sensibles y no puede proyectarse."))
                .andExpect(content().string(not(containsString(sensitiveReference))));
    }

    @Test
    void rejectsRejectedOrInvalidStatusMisuseAtInterfaceBoundary() throws Exception {
        mockMvc.perform(post("/api/cashflow/manual-review/resolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload().replace("\"sourceStatus\": \"MANUAL_REVIEW\"", "\"sourceStatus\": \"REJECTED\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("sourceStatus"))
                .andExpect(jsonPath("$.errors[0].message").value("Solo se pueden resolver movimientos enviados a revisión manual."));

        mockMvc.perform(post("/api/cashflow/manual-review/resolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload().replace("\"status\": \"PROJECTABLE\"", "\"status\": \"REJECTED\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("status"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("El resultado debe quedar listo para proyección; no se aceptan estados pendientes o rechazados."));

        verifyNoInteractions(manualReviewResolutionService);
    }

    @Test
    void omitsOptionalContextWhenServiceReturnsNoSafeText() throws Exception {
        when(manualReviewResolutionService.resolve(any())).thenReturn(result(
                ManualReviewResolutionService.STATUS_CATEGORIZED,
                Optional.empty(),
                Optional.empty()
        ));

        mockMvc.perform(post("/api/cashflow/manual-review/resolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.status").value("CATEGORIZED"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.sourceReference").doesNotExist());
    }

    private static ManualReviewResolutionResult result(
            String outputStatus,
            Optional<String> description,
            Optional<String> sourceReference
    ) {
        return new ManualReviewResolutionResult(
                new ProjectedCashflowTransaction(
                        "sales",
                        BigDecimal.valueOf(125000),
                        Currency.getInstance("CLP"),
                        LocalDate.of(2026, 6, 11)
                ),
                new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW),
                description,
                sourceReference,
                outputStatus
        );
    }

    private static String validPayload() {
        return """
                {
                  "profileId": "pharmacy-cl",
                  "chosenCategoryKey": "sales",
                  "amount": 125000,
                  "currency": "CLP",
                  "date": "2026-06-11",
                  "description": "Venta Caja 1",
                  "sourceReference": "caja-1",
                  "sourceStatus": "MANUAL_REVIEW",
                  "status": "PROJECTABLE"
                }
                """;
    }

    private static String minimalPayload() {
        return """
                {
                  "profileId": "pharmacy-cl",
                  "chosenCategoryKey": "sales",
                  "amount": 125000,
                  "currency": "CLP",
                  "date": "2026-06-11",
                  "sourceStatus": "MANUAL_REVIEW",
                  "status": "CATEGORIZED"
                }
                """;
    }
}
