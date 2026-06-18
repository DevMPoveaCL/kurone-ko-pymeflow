package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashflowIngestionController.class)
class CashflowIngestionControllerTest {

    @MockBean
    private CashflowIngestionService cashflowIngestionService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsCategorizedTransaction() throws Exception {
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        var transaction = transaction("Venta Caja 1", 125000);
        when(cashflowIngestionService.ingest(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(new CashflowIngestionService.CategorizedTransaction(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        transaction,
                        new CategoryAssignment(Optional.of(category), false)
                )),
                List.of(),
                List.of()
        ));

        mockMvc.perform(post("/api/cashflow/ingestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("Venta Caja 1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorized[0].movementId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.categorized[0].transaction.description").value("Venta Caja 1"))
                .andExpect(jsonPath("$.categorized[0].category.key").value("sales"))
                .andExpect(jsonPath("$.manualReview").isEmpty())
                .andExpect(jsonPath("$.rejected").isEmpty());
    }

    @Test
    void returnsMalformedValidationErrors() throws Exception {
        mockMvc.perform(post("/api/cashflow/ingestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "",
                                  "transactions": [
                                    {"description": "", "amount": -1, "currency": "USD"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Revise los datos enviados e intente nuevamente."))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("La moneda soportada es CLP.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("El monto debe ser mayor que cero.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("La fecha es obligatoria.")));
    }

    @Test
    void returnsManualReviewTransaction() throws Exception {
        var transaction = transaction("Venta Caja 2", 88000);
        when(cashflowIngestionService.ingest(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(),
                List.of(new CashflowIngestionService.ManualReviewTransaction(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        transaction,
                        new CategoryAssignment(Optional.empty(), true)
                )),
                List.of()
        ));

        mockMvc.perform(post("/api/cashflow/ingestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("Venta Caja 2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manualReview[0].movementId").value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$.manualReview[0].transaction.description").value("Venta Caja 2"))
                .andExpect(jsonPath("$.manualReview[0].reason").value("Requiere clasificación manual."));
    }

    @Test
    void returnsSensitiveRejectionWithoutEchoingSensitiveDescription() throws Exception {
        var sensitiveDescription = "Venta Caja 1 receta 12345";
        when(cashflowIngestionService.ingest(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(),
                List.of(),
                List.of(new CashflowIngestionService.RejectedTransaction(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        transaction(sensitiveDescription, 42000),
                        "SENSITIVE_IDENTIFIER_REJECTED"
                ))
        ));

        mockMvc.perform(post("/api/cashflow/ingestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(sensitiveDescription)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejected[0].movementId").value("33333333-3333-3333-3333-333333333333"))
                .andExpect(jsonPath("$.rejected[0].reasonCode").value("SENSITIVE_IDENTIFIER_REJECTED"))
                .andExpect(jsonPath("$.rejected[0].reason").value("La transacción contiene datos sensibles y no fue clasificada."))
                .andExpect(jsonPath("$.rejected[0].description").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(sensitiveDescription))));
    }

    private static String validPayload(String description) {
        return """
                {
                  "profileId": "pharmacy-cl",
                  "transactions": [
                    {"description": "%s", "amount": 125000, "currency": "CLP", "date": "2026-06-11"}
                  ]
                }
                """.formatted(description);
    }

    private static Transaction transaction(String description, int amount) {
        return new Transaction(
                description,
                BigDecimal.valueOf(amount),
                Currency.getInstance("CLP"),
                LocalDate.of(2026, 6, 11)
        );
    }
}
