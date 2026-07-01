package com.kuroneko.pymeflow.interfaces.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void servesCockpitWithPreferenceStatusCopyAndManualBalanceSemantics() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-api-target=\"preferences-status\"")))
                .andExpect(content().string(containsString("Saldo inicial manual, no bancario")))
                .andExpect(content().string(containsString("No es saldo bancario en vivo ni saldo bancario real.")))
                .andExpect(content().string(not(containsString("saldo bancario disponible"))))
                .andExpect(content().string(not(containsString("saldo bancario actualizado"))));
    }

    @Test
    void servesCockpitScriptWithPreferenceLoadPrefillAndAutosaveWiring() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cockpitPreferences")))
                .andExpect(content().string(containsString("/api/cashflow/cockpit/preferences?profileId=")))
                .andExpect(content().string(containsString("loadCockpitPreferences")))
                .andExpect(content().string(containsString("prefillCockpitPreferences")))
                .andExpect(content().string(containsString("persistCockpitPreferences")))
                .andExpect(content().string(containsString("preferredHorizonDays")))
                .andExpect(content().string(containsString("openingBalance")))
                .andExpect(content().string(containsString("500")))
                .andExpect(content().string(containsString("method: \"PUT\"")))
                .andExpect(content().string(containsString("Preferencias guardadas")))
                .andExpect(content().string(containsString("Guardando preferencias")))
                .andExpect(content().string(containsString("No se pudieron guardar las preferencias")))
                .andExpect(content().string(not(containsString("bank-live"))))
                .andExpect(content().string(not(containsString("live bank"))));
    }

    @Test
    void servesCockpitWithDemoResetControlAndDemoOnlyCopy() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"demo-reset-btn\"")))
                .andExpect(content().string(containsString("Reiniciar demo")))
                .andExpect(content().string(containsString("data-api-target=\"demo-reset-status\"")))
                .andExpect(content().string(containsString("solo reinicia datos fixture/demo")))
                .andExpect(content().string(not(containsString("conectividad bancaria real habilitada"))))
                .andExpect(content().string(not(containsString("proveedor real conectado"))));
    }

    @Test
    void servesCockpitScriptWithDemoResetEndpointSafeStatesAndFullEvidenceRefresh() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("demoReset: `/api/cockpit/demo/reset-and-seed?profileId=${PROFILE_ID}`")))
                .andExpect(content().string(containsString("#demo-reset-btn")))
                .andExpect(content().string(containsString("runDemoReset")))
                .andExpect(content().string(containsString("Reiniciando datos fixture/demo")))
                .andExpect(content().string(containsString("Demo reiniciada")))
                .andExpect(content().string(containsString("No se pudo reiniciar la demo. Los datos visibles se mantienen.")))
                .andExpect(content().string(containsString("loadCockpitPreferences()")))
                .andExpect(content().string(containsString("renderProfileAndCategories()")))
                .andExpect(content().string(containsString("renderMovementEvidence()")))
                .andExpect(content().string(containsString("renderRecommendations()")))
                .andExpect(content().string(containsString("fetchProjection(balance)")))
                .andExpect(content().string(not(containsString("safeError(error, \"No se pudo reiniciar la demo"))))
                .andExpect(content().string(not(containsString("stack"))))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void servesCockpitWithGuidedDemoStepsInRequiredOrderAndTargets() throws Exception {
        String html = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Gu")))
                .andExpect(content().string(containsString("id=\"demo-guide\"")))
                .andExpect(content().string(containsString("data-guide-step=\"reset\"")))
                .andExpect(content().string(containsString("data-guide-step=\"review\"")))
                .andExpect(content().string(containsString("data-guide-step=\"categorize\"")))
                .andExpect(content().string(containsString("data-guide-step=\"project\"")))
                .andExpect(content().string(containsString("href=\"#demo-reset-btn\"")))
                .andExpect(content().string(containsString("href=\"#revision\"")))
                .andExpect(content().string(containsString("href=\"#revision\"")))
                .andExpect(content().string(containsString("href=\"#proyeccion\"")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html.indexOf("Reiniciar demo")).isLessThan(html.indexOf("Revisar pendientes"));
        assertThat(html.indexOf("Revisar pendientes")).isLessThan(html.indexOf("Categorizar"));
        assertThat(html.indexOf("Categorizar")).isLessThan(html.indexOf("Proyectar caja"));
    }

    @Test
    void servesGuidedDemoWithSafeSpanishCopyAndNoLiveProviderClaims() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("demo-guide-title")))
                .andExpect(content().string(containsString("datos simulados")))
                .andExpect(content().string(containsString("solo orienta la demo")))
                .andExpect(content().string(containsString("No guarda avance")))
                .andExpect(content().string(not(containsString("conectividad bancaria real habilitada"))))
                .andExpect(content().string(not(containsString("proveedor real conectado"))))
                .andExpect(content().string(not(containsString("bank-live"))))
                .andExpect(content().string(not(containsString("live bank"))));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("actualizada")))
                .andExpect(content().string(not(containsString("conectividad bancaria real habilitada"))))
                .andExpect(content().string(not(containsString("proveedor real conectado"))))
                .andExpect(content().string(not(containsString("bank-live"))))
                .andExpect(content().string(not(containsString("live bank"))));
    }

    @Test
    void servesGuidedDemoScriptWithSessionOnlyStateAndNoFrontendDrift() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("guide: {")))
                .andExpect(content().string(containsString("completed: new Set()")))
                .andExpect(content().string(containsString("currentStep: \"reset\"")))
                .andExpect(content().string(containsString("GUIDE_STEPS")))
                .andExpect(content().string(containsString("markGuideStepComplete")))
                .andExpect(content().string(containsString("renderGuideProgress")))
                .andExpect(content().string(containsString("handleGuideClick")))
                .andExpect(content().string(containsString("[data-guide-status-message]")))
                .andExpect(content().string(not(containsString("localStorage"))))
                .andExpect(content().string(not(containsString("sessionStorage"))))
                .andExpect(content().string(not(containsString("/api/cockpit/demo/guide"))))
                .andExpect(content().string(not(containsString("new WebSocket"))))
                .andExpect(content().string(not(containsString("npm"))));
    }

    @Test
    void servesGuidedDemoWithAccessibleStatusAndNonBlockingControls() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h2 id=\"demo-guide-title\">Gu")))
                .andExpect(content().string(containsString("<ol class=\"guide-steps\"")))
                .andExpect(content().string(containsString("aria-current=\"step\"")))
                .andExpect(content().string(containsString("data-guide-status")))
                .andExpect(content().string(containsString("data-guide-status-message")))
                .andExpect(content().string(containsString("role=\"status\"")))
                .andExpect(content().string(containsString("aria-live=\"polite\"")))
                .andExpect(content().string(containsString("data-guide-target")))
                .andExpect(content().string(containsString("data-action=\"manual-import\"")))
                .andExpect(content().string(containsString("id=\"demo-reset-btn\"")))
                .andExpect(content().string(containsString("data-projection-form")));
    }
}
