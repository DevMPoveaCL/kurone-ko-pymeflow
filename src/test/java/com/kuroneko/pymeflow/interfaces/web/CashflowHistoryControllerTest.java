package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.CashflowMovementHistoryService;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementStatus;
import com.kuroneko.pymeflow.application.cashflow.PendingManualReviewMovement;
import com.kuroneko.pymeflow.application.cashflow.ProjectionReadyCashflowTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashflowHistoryController.class)
class CashflowHistoryControllerTest {
    private static final Currency CLP = Currency.getInstance("CLP");

    @MockBean
    private CashflowMovementHistoryService cashflowMovementHistoryService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsPendingManualReviewMovementsWithSafeFieldsOnly() throws Exception {
        var movementId = UUID.randomUUID();
        when(cashflowMovementHistoryService.pendingManualReviews(any())).thenReturn(List.of(
                new PendingManualReviewMovement(
                        movementId,
                        BigDecimal.valueOf(125000),
                        CLP,
                        LocalDate.of(2026, 6, 11),
                        "Venta Caja 1",
                        "caja-1",
                        CashflowMovementStatus.MANUAL_REVIEW
                )
        ));

        mockMvc.perform(get("/api/cashflow/history/manual-review")
                        .param("profileId", "pharmacy-cl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementId").value(movementId.toString()))
                .andExpect(jsonPath("$[0].amount").value(125000))
                .andExpect(jsonPath("$[0].currency").value("CLP"))
                .andExpect(jsonPath("$[0].date").value("2026-06-11"))
                .andExpect(jsonPath("$[0].description").value("Venta Caja 1"))
                .andExpect(jsonPath("$[0].sourceReference").value("caja-1"))
                .andExpect(jsonPath("$[0].status").value("MANUAL_REVIEW"))
                .andExpect(content().string(not(containsString("receta"))))
                .andExpect(content().string(not(containsString("paciente"))));
    }

    @Test
    void listsProjectionReadyTransactionsCompatibleWithProjectionInput() throws Exception {
        var movementId = UUID.randomUUID();
        when(cashflowMovementHistoryService.projectionReady(any(), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(List.of(new ProjectionReadyCashflowTransaction(
                        movementId,
                        "sales",
                        BigDecimal.valueOf(125000),
                        CLP,
                        LocalDate.of(2026, 6, 11),
                        CashflowMovementStatus.PROJECTABLE
                )));

        mockMvc.perform(get("/api/cashflow/history/projection-ready")
                        .param("profileId", "pharmacy-cl")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementId").value(movementId.toString()))
                .andExpect(jsonPath("$[0].categoryKey").value("sales"))
                .andExpect(jsonPath("$[0].amount").value(125000))
                .andExpect(jsonPath("$[0].currency").value("CLP"))
                .andExpect(jsonPath("$[0].date").value("2026-06-11"))
                .andExpect(jsonPath("$[0].status").value("PROJECTABLE"))
                .andExpect(content().string(not(containsString("description"))))
                .andExpect(content().string(not(containsString("sourceReference"))));

        verify(cashflowMovementHistoryService).projectionReady(any(), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30)));
    }

    @Test
    void returnsNeutralSpanishValidationForMissingProfileId() throws Exception {
        mockMvc.perform(get("/api/cashflow/history/manual-review").param("profileId", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Revise los datos enviados e intente nuevamente."))
                .andExpect(jsonPath("$.errors[0].field").value("profileId"))
                .andExpect(jsonPath("$.errors[0].message").value("El perfil es obligatorio."));

        verifyNoInteractions(cashflowMovementHistoryService);
    }

    @Test
    void returnsNeutralSpanishErrorsFromService() throws Exception {
        when(cashflowMovementHistoryService.projectionReady(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial."));

        mockMvc.perform(get("/api/cashflow/history/projection-ready")
                        .param("profileId", "pharmacy-cl")
                        .param("startDate", "2026-06-30")
                        .param("endDate", "2026-06-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("La fecha final no puede ser anterior a la fecha inicial."));
    }
}
