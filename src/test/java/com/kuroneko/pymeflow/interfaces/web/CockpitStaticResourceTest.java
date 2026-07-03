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

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:cockpit-static-resource;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
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
                .andExpect(content().string(containsString("Cartola")))
                .andExpect(content().string(containsString("fixture/demo")))
                .andExpect(content().string(containsString("Datos demo/manuales")))
                .andExpect(content().string(not(containsString("mostrador"))))
                .andExpect(content().string(not(containsString("conectividad bancaria real habilitada"))));
    }

    @Test
    void servesCockpitBrandAssetsFromStaticResourcesAndWiresHeadMetadata() throws Exception {
        mockMvc.perform(get("/favicon.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        mockMvc.perform(get("/branding.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<meta name=\"color-scheme\" content=\"light dark\">")))
                .andExpect(content().string(containsString("<link rel=\"icon\" type=\"image/png\" href=\"/favicon.png\">")))
                .andExpect(content().string(containsString("class=\"brand-lockup\"")))
                .andExpect(content().string(containsString("src=\"/branding.png\"")))
                .andExpect(content().string(containsString("width=\"240\"")))
                .andExpect(content().string(containsString("height=\"72\"")))
                .andExpect(content().string(containsString("alt=\"PymeFlow\"")));
    }

    @Test
    void servesCockpitStylesWithFarmaciaUniaccPaletteDarkModeAndResponsiveBrandContracts() throws Exception {
        mockMvc.perform(get("/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("--flow-canvas: #fbf8ff")))
                .andExpect(content().string(containsString("--flow-surface: #f4effb")))
                .andExpect(content().string(containsString("--flow-elevated: #ffffff")))
                .andExpect(content().string(containsString("--flow-inset: #f0e8fa")))
                .andExpect(content().string(containsString("--flow-violet: #7a4db7")))
                .andExpect(content().string(containsString("--flow-cyan: #009fe3")))
                .andExpect(content().string(containsString("--flow-magenta: #c72a8c")))
                .andExpect(content().string(containsString("@media (prefers-color-scheme: dark)")))
                .andExpect(content().string(containsString(":root[data-theme=\"light\"]")))
                .andExpect(content().string(containsString(":root[data-theme=\"dark\"]")))
                .andExpect(content().string(containsString("--flow-canvas: #20262e")))
                .andExpect(content().string(containsString("--flow-surface: #151f29")))
                .andExpect(content().string(containsString("--flow-elevated: #162331")))
                .andExpect(content().string(containsString("--flow-raised: #1c2b38")))
                .andExpect(content().string(containsString("--flow-inset: #101820")))
                .andExpect(content().string(containsString("--flow-cyan: #7adfff")))
                .andExpect(content().string(containsString("--flow-magenta: #7adfff")))
                .andExpect(content().string(containsString(".brand-lockup")))
                .andExpect(content().string(containsString("object-fit: contain")))
                .andExpect(content().string(containsString("max-inline-size")))
                .andExpect(content().string(containsString("@media (max-width: 860px)")))
                .andExpect(content().string(containsString("@media (max-width: 520px)")))
                .andExpect(content().string(containsString("overflow-x: hidden")))
                .andExpect(content().string(containsString("color-scheme: dark;")));
    }

    @Test
    void servesCockpitWithAccessibleThemeSwitchMarkup() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"topbar")))
                .andExpect(content().string(containsString("id=\"theme-toggle\"")))
                .andExpect(content().string(containsString("data-theme-toggle")))
                .andExpect(content().string(containsString("aria-pressed=\"false\"")))
                .andExpect(content().string(containsString("aria-label=\"Cambiar tema visual")))
                .andExpect(content().string(containsString("data-theme-toggle-label")))
                .andExpect(content().string(containsString("Modo claro")))
                .andExpect(content().string(not(containsString("Preferencia visual"))))
                .andExpect(content().string(not(containsString("Sigue el sistema"))))
                .andExpect(content().string(not(containsString("No afecta datos ni avance demo"))));
    }

    @Test
    void servesCockpitScriptWithThemePreferenceWiring() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("THEME_STORAGE_KEY")))
                .andExpect(content().string(containsString("pymeflow.theme")))
                .andExpect(content().string(containsString("window.matchMedia(\"(prefers-color-scheme: dark)\")")))
                .andExpect(content().string(containsString("document.documentElement.dataset.theme")))
                .andExpect(content().string(containsString("localStorage.getItem(THEME_STORAGE_KEY)")))
                .andExpect(content().string(containsString("localStorage.setItem(THEME_STORAGE_KEY, nextTheme)")))
                .andExpect(content().string(containsString("#theme-toggle")))
                .andExpect(content().string(containsString("aria-pressed")))
                .andExpect(content().string(containsString("Cambiar a tema claro")))
                .andExpect(content().string(containsString("Cambiar a tema oscuro")));
    }

    @Test
    void servesCockpitStylesWithDarkThemeOverrideUsingOnlyBlueCyanBrandAccent() throws Exception {
        String css = mockMvc.perform(get("/styles.css"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int darkOverrideStart = css.indexOf(":root[data-theme=\"dark\"]");
        assertThat(darkOverrideStart).isGreaterThanOrEqualTo(0);
        String darkOverride = css.substring(darkOverrideStart, css.indexOf("* { box-sizing", darkOverrideStart));

        assertThat(darkOverride).contains("--flow-canvas: #20262e");
        assertThat(darkOverride).contains("--flow-violet: #0477a0");
        assertThat(darkOverride).contains("--flow-cyan: #7adfff");
        assertThat(darkOverride).contains("--flow-magenta: #7adfff");
        assertThat(darkOverride).doesNotContain("#c72a8c");
        assertThat(darkOverride).doesNotContain("rgba(199, 42, 140");
    }

    @Test
    void servesCockpitStylesWithMobileSafeReviewGridContracts() throws Exception {
        String css = mockMvc.perform(get("/styles.css"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(css).contains(".review-grid { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 0.75fr);");
        assertThat(css).contains(".review-column { display: grid; min-width: 0;");
        assertThat(css).contains(".button-row { display: flex; flex-wrap: wrap; min-width: 0;");
        assertThat(css).contains("@media (max-width: 860px)");
        assertThat(css).contains(".review-grid { grid-template-columns: minmax(0, 1fr); }");
        assertThat(css).contains("@media (max-width: 520px)");
        assertThat(css).contains(".review-grid,");
        assertThat(css).contains(".review-column,");
        assertThat(css).doesNotContain(".review-grid { display: grid; grid-template-columns: minmax(320px");
    }

    @Test
    void servesCockpitWithoutFrontendToolingOrApiSelectorDrift() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-api-target=\"cash-summary\"")))
                .andExpect(content().string(containsString("data-api-target=\"sync-receipt\"")))
                .andExpect(content().string(containsString("data-guide-step=\"reset\"")))
                .andExpect(content().string(containsString("data-guide-target=\"#demo-reset-btn\"")))
                .andExpect(content().string(not(containsString("/src/"))))
                .andExpect(content().string(not(containsString("vite"))))
                .andExpect(content().string(not(containsString("npm"))));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/profiles/active")))
                .andExpect(content().string(containsString("/api/cashflow/provider-syncs")))
                .andExpect(content().string(containsString("/api/cashflow/imports/manual")))
                .andExpect(content().string(containsString("/api/cockpit/demo/reset-and-seed?profileId=${PROFILE_ID}")))
                .andExpect(content().string(containsString("pymeflow.theme")))
                .andExpect(content().string(not(containsString("sessionStorage"))))
                .andExpect(content().string(not(containsString("new WebSocket"))));
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
                .andExpect(content().string(containsString("aria-label=\"Acciones principales del dashboard\"")))
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
                .andExpect(content().string(containsString("Elige categor")))
                .andExpect(content().string(containsString("proyecta")))
                .andExpect(content().string(containsString("Cargando movimientos pendientes")))
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
                .andExpect(content().string(containsString("Saldo manual, no bancario")))
                .andExpect(content().string(containsString("value=\"7\"")))
                .andExpect(content().string(containsString("value=\"30\"")))
                .andExpect(content().string(containsString("data-api-target=\"projection-results\"")))
                .andExpect(content().string(containsString("Categoriza movimientos para proyectar caja")));
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
                .andExpect(content().string(containsString("Hay movimientos listos")))
                .andExpect(content().string(containsString("Categoriza movimientos para proyectar la caja.")))
                .andExpect(content().string(containsString("No se pudo cargar la proyecci")))
                .andExpect(content().string(not(containsString("bank-live"))));
    }

    @Test
    void servesCockpitScriptWithDemoUsefulProjectionStartAndPreciseEmptyState() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("projectableMovementDates")))
                .andExpect(content().string(containsString("state.projection.projectableMovementDates = projectionReady.map((movement) => movement.date).filter(Boolean)")))
                .andExpect(content().string(containsString("chooseProjectionStartDate(state.projection.projectableMovementDates, state.projection.horizonDays)")))
                .andExpect(content().string(containsString("hasProjectableMovements()")))
                .andExpect(content().string(containsString("Hay movimientos listos")))
                .andExpect(content().string(containsString("Categoriza movimientos para proyectar la caja.")))
                .andExpect(content().string(not(containsString("startDate: todayIsoDate()"))))
                .andExpect(content().string(not(containsString("Categoriza movimientos primero para proyectar caja."))));
    }

    @Test
    void servesCockpitWithShortDidacticDashboardCopyScale() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>PymeFlow | Dashboard de caja</title>")))
                .andExpect(content().string(containsString("<h1>Dashboard de caja</h1>")))
                .andExpect(content().string(containsString("id=\"tab-revision\"")))
                .andExpect(content().string(containsString("id=\"tab-proyeccion\"")))
                .andExpect(content().string(containsString("id=\"tab-cartola\"")))
                .andExpect(content().string(containsString("<p class=\"card-label\">Caja</p>")))
                .andExpect(content().string(containsString("<p class=\"card-label\">Entradas</p>")))
                .andExpect(content().string(containsString("<p class=\"card-label\">Salidas</p>")))
                .andExpect(content().string(not(containsString("Cómo se ve la caja esta semana o este mes"))))
                .andExpect(content().string(not(containsString("Una vista operativa para revisar abonos, cargos, cartola"))));

        mockMvc.perform(get("/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("font-size: clamp(1.75rem, 4vw, 3rem)")))
                .andExpect(content().string(containsString("font-size: clamp(1.2rem, 2.4vw, 1.65rem)")))
                .andExpect(content().string(containsString("font-size: clamp(1.45rem, 3vw, 2.2rem)")))
                .andExpect(content().string(not(containsString("font-size: clamp(2rem, 5vw, 4rem)"))))
                .andExpect(content().string(not(containsString("font-size: clamp(1.4rem, 3vw, 2.25rem)"))));
    }

    @Test
    void servesCockpitWithPreferenceStatusCopyAndManualBalanceSemantics() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-api-target=\"preferences-status\"")))
                .andExpect(content().string(containsString("Saldo inicial manual, no bancario")))
                .andExpect(content().string(containsString("Saldo manual, no bancario.")))
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
                .andExpect(content().string(containsString("Solo reinicia datos demo")))
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
    void servesCockpitScriptWithProjectionReadyOnlyCashSummarySemantics() throws Exception {
        String script = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(script).contains("updateCashTotals(projectionReady)");
        assertThat(script).contains("const movements = [...projectionReady, ...manualReview]");
        assertThat(script).contains("renderLedger(movements)");
        assertThat(script).contains("renderManualReview(manualReview)");
        assertThat(script).contains("Caja proyectada usa solo movimientos listos");
        assertThat(script).doesNotContain("            updateCashTotals(movements);");
    }

    @Test
    void servesCockpitWithProminentDemoResetStatusContract() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"state-line status-text demo-reset-status\"")))
                .andExpect(content().string(containsString("tabindex=\"-1\"")))
                .andExpect(content().string(containsString("aria-live=\"polite\"")))
                .andExpect(content().string(containsString("Solo reinicia datos demo")));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Caja proyectada actualizada solo con movimientos listos")))
                .andExpect(content().string(containsString("focusStatus(status)")))
                .andExpect(content().string(containsString("scrollIntoView({ behavior: \"smooth\", block: \"center\" })")))
                .andExpect(content().string(containsString("Demo reiniciada con datos fixture/demo. Caja proyectada usa solo movimientos listos")));

        mockMvc.perform(get("/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".demo-reset-status .success-state")))
                .andExpect(content().string(containsString("border: 2px solid color-mix(in srgb, var(--flow-success) 56%, transparent)")))
                .andExpect(content().string(containsString("box-shadow: 0 0 0 4px color-mix(in srgb, var(--flow-success) 14%, transparent)")));
    }

    @Test
    void servesCockpitWithGuidedDemoStepsInRequiredOrderAndTargets() throws Exception {
        String html = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Ayuda demo")))
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
                .andExpect(content().string(containsString("Datos simulados")))
                .andExpect(content().string(containsString("bancaria")))
                .andExpect(content().string(containsString("Avance visible")))
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
                .andExpect(content().string(containsString("pymeflow.theme")))
                .andExpect(content().string(not(containsString("sessionStorage"))))
                .andExpect(content().string(not(containsString("/api/cockpit/demo/guide"))))
                .andExpect(content().string(not(containsString("new WebSocket"))))
                .andExpect(content().string(not(containsString("npm"))));
    }

    @Test
    void servesGuidedDemoWithAccessibleStatusAndNonBlockingControls() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"demo-guide-title\">Flujo demo")))
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
