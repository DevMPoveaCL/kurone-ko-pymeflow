package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.AppliedObligation;
import com.kuroneko.pymeflow.application.cashflow.CashflowProjectionResult;
import com.kuroneko.pymeflow.application.cashflow.CashflowProjectionService;
import com.kuroneko.pymeflow.application.cashflow.DailyProjectedBalance;
import com.kuroneko.pymeflow.application.cashflow.ProjectionAlert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashflowProjectionController.class)
class CashflowProjectionControllerTest {

    @MockBean
    private CashflowProjectionService cashflowProjectionService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsProjectionResponseShape() throws Exception {
        when(cashflowProjectionService.project(any())).thenReturn(new CashflowProjectionResult(
                List.of(new DailyProjectedBalance(
                        LocalDate.of(2026, 2, 1),
                        BigDecimal.valueOf(125000),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(1625000)
                )),
                BigDecimal.valueOf(1625000),
                List.of(new AppliedObligation(
                        "rent",
                        "Arriendo del local",
                        LocalDate.of(2026, 2, 5),
                        BigDecimal.valueOf(900000)
                )),
                List.of(new ProjectionAlert(
                        "healthy-status",
                        "healthy-status",
                        "projected_balance_above_threshold",
                        LocalDate.of(2026, 2, 1),
                        BigDecimal.valueOf(1625000)
                ))
        ));

        mockMvc.perform(post("/api/cashflow/projections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyBalances[0].date").value("2026-02-01"))
                .andExpect(jsonPath("$.dailyBalances[0].balance").value(1625000))
                .andExpect(jsonPath("$.closingProjectedBalance").value(1625000))
                .andExpect(jsonPath("$.appliedObligations[0].obligationKey").value("rent"))
                .andExpect(jsonPath("$.alerts[0].ruleKey").value("healthy-status"));
    }

    @Test
    void returnsValidationErrorsInSpanishForMalformedRequest() throws Exception {
        mockMvc.perform(post("/api/cashflow/projections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "",
                                  "openingBalance": -1,
                                  "currency": "USD",
                                  "horizonDays": 0,
                                  "transactions": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Revise los datos enviados e intente nuevamente."))
                .andExpect(content().string(containsString("El perfil es obligatorio.")))
                .andExpect(content().string(containsString("El saldo inicial no puede ser negativo.")))
                .andExpect(content().string(containsString("La moneda soportada es CLP.")))
                .andExpect(content().string(containsString("La fecha de inicio es obligatoria.")))
                .andExpect(content().string(containsString("El horizonte debe ser mayor que cero.")))
                .andExpect(jsonPath("$.errors[?(@.field == 'transactions')].message")
                        .value("Debe enviar al menos una transacción categorizada."));
    }

    @Test
    void returnsUnknownProfileAsNeutralSpanishBadRequest() throws Exception {
        when(cashflowProjectionService.project(any()))
                .thenThrow(new IllegalArgumentException("Profile not found: missing-profile"));

        mockMvc.perform(post("/api/cashflow/projections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload().replace("pharmacy-cl", "missing-profile")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("El perfil solicitado no está configurado."));
    }

    @Test
    void returnsInvalidHorizonValidationError() throws Exception {
        mockMvc.perform(post("/api/cashflow/projections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload().replace("\"horizonDays\": 3", "\"horizonDays\": -2")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(content().string(containsString("El horizonte debe ser mayor que cero.")));
    }

    @Test
    void rejectsManualAndRejectedStatusesAtInterfaceBoundary() throws Exception {
        mockMvc.perform(post("/api/cashflow/projections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload().replace("\"status\": \"PROJECTABLE\"", "\"status\": \"MANUAL_REVIEW\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("No se aceptan transacciones pendientes de revisión o rechazadas para esta proyección."));

        mockMvc.perform(post("/api/cashflow/projections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload().replace("\"status\": \"PROJECTABLE\"", "\"status\": \"REJECTED\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void acceptsProjectionWithoutSensitiveDescriptions() throws Exception {
        when(cashflowProjectionService.project(any())).thenReturn(new CashflowProjectionResult(
                List.of(new DailyProjectedBalance(
                        LocalDate.of(2026, 2, 1),
                        BigDecimal.valueOf(125000),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(1625000)
                )),
                BigDecimal.valueOf(1625000),
                List.of(),
                List.of()
        ));

        mockMvc.perform(post("/api/cashflow/projections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("description"))))
                .andExpect(content().string(not(containsString("receta"))))
                .andExpect(content().string(not(containsString("paciente"))));
    }

    @Test
    void returnsCurrencyMismatchAsNeutralSpanishBadRequest() throws Exception {
        when(cashflowProjectionService.project(any()))
                .thenThrow(new IllegalArgumentException("Transaction currency must match projection currency"));

        mockMvc.perform(post("/api/cashflow/projections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("La moneda de las transacciones debe coincidir con la moneda de la proyección."));
    }

    private static String validPayload() {
        return """
                {
                  "profileId": "pharmacy-cl",
                  "openingBalance": 1500000,
                  "currency": "CLP",
                  "startDate": "2026-02-01",
                  "horizonDays": 3,
                  "transactions": [
                    {"categoryKey": "sales", "amount": 125000, "currency": "CLP", "date": "2026-02-01", "status": "PROJECTABLE"}
                  ]
                }
                """;
    }
}
