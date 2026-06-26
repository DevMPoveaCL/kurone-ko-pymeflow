package com.kuroneko.pymeflow.interfaces.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CockpitStaticResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesRootCockpitWithAccessibleChileanCashflowIdentity() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<main")))
                .andExpect(content().string(containsString("PymeFlow")))
                .andExpect(content().string(containsString("caja diaria")))
                .andExpect(content().string(containsString("abonos")))
                .andExpect(content().string(containsString("cargos")))
                .andExpect(content().string(containsString("Cartola de movimientos")))
                .andExpect(content().string(containsString("fixture/demo")))
                .andExpect(content().string(containsString("No representa conectividad bancaria real")))
                .andExpect(content().string(not(containsString("mostrador"))))
                .andExpect(content().string(not(containsString("conectividad bancaria real habilitada"))));
    }

    @Test
    void servesRootCockpitWithPrimaryRegionsAndControls() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Resumen de caja diaria\"")))
                .andExpect(content().string(containsString("Riel de comprobantes")))
                .andExpect(content().string(containsString("aria-label=\"Acciones principales del cockpit\"")))
                .andExpect(content().string(containsString("<button type=\"button\"")))
                .andExpect(content().string(containsString("Revisar abonos y cargos")));
    }

    @Test
    void servesCockpitScriptWithSameOriginApiWiringAndSafeStateTargets() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-api-target=\"cash-summary\"")))
                .andExpect(content().string(containsString("data-api-target=\"sync-receipt\"")))
                .andExpect(content().string(containsString("data-api-target=\"ledger-list\"")))
                .andExpect(content().string(containsString("data-api-target=\"recommendation-list\"")))
                .andExpect(content().string(containsString("data-api-target=\"manual-review-list\"")));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/profiles/active")))
                .andExpect(content().string(containsString("/api/profiles/active/categories")))
                .andExpect(content().string(containsString("/api/cashflow/provider-syncs")))
                .andExpect(content().string(containsString("/api/cashflow/imports/manual")))
                .andExpect(content().string(containsString("/api/cashflow/history/manual-review")))
                .andExpect(content().string(containsString("/api/cashflow/history/projection-ready")))
                .andExpect(content().string(containsString("/api/cashflow/recommendations")))
                .andExpect(content().string(containsString("Sin datos para mostrar")))
                .andExpect(content().string(containsString("No se pudo cargar")))
                .andExpect(content().string(not(containsString("credentialRef"))))
                .andExpect(content().string(not(containsString("cursor"))))
                .andExpect(content().string(not(containsString("token"))));
    }

    @Test
    void servesCockpitWithManualReviewCopyAndSeparateRecommendationRegion() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Movimientos pendientes")))
                .andExpect(content().string(containsString("Selecciona una categor")))
                .andExpect(content().string(containsString("Categorizar movimiento")))
                .andExpect(content().string(containsString("Sin movimientos pendientes")))
                .andExpect(content().string(containsString("aria-label=\"Recomendaciones de caja\"")))
                .andExpect(content().string(containsString("aria-label=\"Movimientos pendientes")));
    }

    @Test
    void servesCockpitScriptWithPersistedReviewResolutionWiringAndDirectionInvariants() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("state.categories")))
                .andExpect(content().string(containsString("state.resolvingMovementIds")))
                .andExpect(content().string(containsString("/api/cashflow/manual-review/resolutions/")))
                .andExpect(content().string(containsString("chosenCategoryKey")))
                .andExpect(content().string(containsString("Seleccione una categor")))
                .andExpect(content().string(containsString("Categorizar movimiento")))
                .andExpect(content().string(containsString("Math.abs")))
                .andExpect(content().string(containsString("DEBIT")))
                .andExpect(content().string(containsString("CREDIT")))
                .andExpect(content().string(containsString("INFLOW")))
                .andExpect(content().string(containsString("OUTFLOW")))
                .andExpect(content().string(not(containsString("INFLOW · abono"))))
                .andExpect(content().string(not(containsString("OUTFLOW · cargo"))));
    }

    @Test
    void servesCockpitWithPeriodProjectionControlsAndManualOpeningBalanceCopy() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Proyecci")))
                .andExpect(content().string(containsString("Saldo inicial manual, no bancario")))
                .andExpect(content().string(containsString("ingresado por el usuario")))
                .andExpect(content().string(containsString("value=\"7\"")))
                .andExpect(content().string(containsString("value=\"30\"")))
                .andExpect(content().string(containsString("data-api-target=\"projection-results\"")))
                .andExpect(content().string(containsString("Categoriza movimientos para proyectar la caja")));
    }

    @Test
    void servesCockpitScriptWithProjectionEndpointRenderingAndSafeStates() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/cashflow/cockpit/projection")))
                .andExpect(content().string(containsString("horizonDays")))
                .andExpect(content().string(containsString("openingBalance")))
                .andExpect(content().string(containsString("closingProjectedBalance")))
                .andExpect(content().string(containsString("dailyBalances")))
                .andExpect(content().string(containsString("appliedObligations")))
                .andExpect(content().string(containsString("alerts")))
                .andExpect(content().string(containsString("abonos")))
                .andExpect(content().string(containsString("cargos")))
                .andExpect(content().string(containsString("obligaciones")))
                .andExpect(content().string(containsString("Ingresa un saldo inicial manual para proyectar caja.")))
                .andExpect(content().string(containsString("Categoriza movimientos primero para proyectar caja.")))
                .andExpect(content().string(containsString("No se pudo cargar la proyecci")))
                .andExpect(content().string(not(containsString("bank-live"))));
    }
}
