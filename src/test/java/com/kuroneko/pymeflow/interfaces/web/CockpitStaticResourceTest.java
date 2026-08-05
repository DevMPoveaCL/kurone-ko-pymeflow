package com.kuroneko.pymeflow.interfaces.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    void servesRootAndCohesiveCashSummaryIdentity() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        String html = resource("/index.html");
        assertThat(html).contains("<title>PymeFlow | Resumen de caja</title>");
        assertThat(html).contains("<h1>RESUMEN DE CAJA</h1>");
        assertThat(html).contains("aria-label=\"Resumen de caja\"");
        assertThat(html).doesNotContain("PymeFlow · MVP");
        assertThat(html).doesNotContain("Dashboard de caja");
        assertThat(html).contains("id=\"tab-revision\"")
                .contains("id=\"tab-proyeccion\"")
                .contains("id=\"tab-cartola\"")
                .contains("id=\"tab-comprobantes\"");
    }

    @Test
    void servesAccessibleSingleStepCategoryConfirmationFlow() throws Exception {
        String html = resource("/index.html");
        String script = resource("/app.js");

        assertThat(html).contains("id=\"category-dialog\"")
                .contains("role=\"dialog\"")
                .contains("aria-modal=\"true\"")
                .contains("data-category-dialog-confirm disabled")
                .contains("data-category-dialog-status")
                .contains("data-category-dialog-helper")
                .contains("La categoría clasifica el movimiento; no cambia si es entrada o salida.");
        assertThat(script).contains("categoryDialogSelectedKey")
                .contains("type=\"radio\"")
                .contains("name=\"category-dialog-choice\"")
                .contains("submitCategoryDialog")
                .contains("await resolveManualReviewMovement(movementId, categoryKey, card, message)")
                .contains("await refreshCockpitEvidence({ forceFresh: true })")
                .contains("closeCategoryDialog({ restoreFocus: false })")
                .contains("Categorizar</button>")
                .doesNotContain("Categorizar movimiento</button>")
                .doesNotContain("MANUAL_REVIEW")
                .doesNotContain("DEBIT · cargo")
                .doesNotContain("CREDIT · abono");
        assertThat(functionBody(script, "renderManualReviewMovement"))
                .contains("data-review-direction");
        assertThat(functionBody(script, "renderCategoryDialogOptions"))
                .contains("categoryDirectionForMovement(movementDirection)")
                .contains("category.direction === compatibleDirection")
                .contains("No hay categorías compatibles con la dirección de este movimiento")
                .contains("updateCategoryDialogConfirm()");
        assertThat(functionBody(script, "categoryDirectionForMovement"))
                .contains("movementDirection === \"DEBIT\" ? \"OUTFLOW\"")
                .contains("movementDirection === \"CREDIT\" ? \"INFLOW\"");
        assertThat(functionBody(script, "submitCategoryDialog"))
                .contains("isCategoryDialogChoiceAvailable(categoryKey)");
    }

    @Test
    void filtersCategoryOptionsUsingTheRenderedMovementDirection() throws Exception {
        String script = resource("/app.js");

        assertThat(functionBody(script, "renderManualReviewMovement"))
                .contains("data-review-direction=\"${movementDirection}\"");
        assertThat(functionBody(script, "renderCategoryDialogOptions"))
                .contains("const movementDirection = card?.dataset.reviewDirection;")
                .contains("const compatibleDirection = categoryDirectionForMovement(movementDirection);")
                .contains("state.categories.filter((category) => category.direction === compatibleDirection)");
    }

    @Test
    void clearsStaleCategorySelectionWhenTheDialogOpens() throws Exception {
        String script = resource("/app.js");

        assertThat(functionBody(script, "openCategoryDialog"))
                .contains("resetCategoryDialogSelection(movementId);");
        assertThat(functionBody(script, "resetCategoryDialogSelection"))
                .contains("state.categoryDialogSelectedKey = null;")
                .contains("document.querySelectorAll(\"[data-category-dialog-option]\")")
                .contains("option.checked = false;")
                .contains("hiddenCategory.value = \"\";");
    }

    @Test
    void disablesCategoryApplyWhenNoCompatibleOptionExists() throws Exception {
        String script = resource("/app.js");

        assertThat(functionBody(script, "renderCategoryDialogOptions"))
                .contains("No hay categorías compatibles con la dirección de este movimiento")
                .contains("updateCategoryDialogConfirm();");
        assertThat(functionBody(script, "updateCategoryDialogConfirm"))
                .contains("confirm.disabled = !isCategoryDialogChoiceAvailable(state.categoryDialogSelectedKey);");
        assertThat(functionBody(script, "isCategoryDialogChoiceAvailable"))
                .contains("if (!categoryKey) return false;")
                .contains("option.value === categoryKey && option.checked");
    }

    @Test
    void servesSemanticScrollbarAndDirectionTokensInBothThemes() throws Exception {
        String css = resource("/styles.css");

        assertThat(css).contains("--flow-inflow-bg:")
                .contains("--flow-outflow-bg:")
                .contains("--flow-scrollbar-track:")
                .contains("--flow-scrollbar-thumb:")
                .contains(":root[data-theme=\"dark\"]")
                .contains("scrollbar-width: thin")
                .contains("scrollbar-color: var(--flow-scrollbar-thumb) var(--flow-scrollbar-track)")
                .contains("::-webkit-scrollbar-thumb")
                .contains("::-webkit-scrollbar-track")
                .contains("overflow: auto")
                .contains(".pill--credit { border: 1px solid var(--flow-inflow-border)")
                .contains(".pill--debit { border: 1px solid var(--flow-outflow-border)")
                .doesNotContain(".pill--debit { background: var(--flow-muted)");
    }

    @Test
    void servesCompactAccessibleCartolaLedgerAcrossViewportModes() throws Exception {
        String css = resource("/styles.css");
        String script = resource("/app.js");

        assertThat(functionBody(script, "renderLedger"))
                .contains("ledger.removeAttribute(\"role\")")
                .contains("state.ledgerMovements = movements")
                .contains("<div class=\"ledger-summary\" role=\"region\" aria-label=\"Resumen de movimientos de caja\">")
                .contains("Total movimientos")
                .contains("entries.length")
                .contains("exits.length")
                .contains("data-projection-details-trigger=\"ledger\"")
                .contains("Ver movimientos (${movements.length})");
        assertThat(functionBody(script, "renderMovement"))
                .contains("<tr class=\"ledger-row\">")
                .contains("data-label=\"Fecha\"")
                .contains("data-label=\"Movimiento/categoría\"")
                .contains("data-label=\"Dirección\"")
                .contains("data-label=\"Monto\"")
                .contains("categoryLabelFor(movement)");
        assertThat(functionBody(script, "renderLedgerDetails"))
                .contains("<table class=\"ledger-table ledger-table--details\" aria-label=\"Movimientos de caja completos\">")
                .contains("<th scope=\"col\">Fecha</th>")
                .contains("<th scope=\"col\">Movimiento/categoría</th>")
                .contains("<th scope=\"col\">Dirección</th>")
                .contains("<th scope=\"col\">Monto</th>");
        assertThat(functionBody(script, "categoryLabelFor"))
                .contains("state.categories.find((candidate) => candidate.key === movement.categoryKey)")
                .contains("humanizeCategoryKey(movement.categoryKey)");
        assertThat(css)
                .contains(".ledger-table {")
                .contains(".ledger-row { height: 52px; }")
                .contains(".ledger-table th:nth-child(1) { width: 15%; }")
                .contains(".ledger-table th:nth-child(2) { width: 47%; }")
                .contains(".ledger-table thead { position: absolute;")
                .contains(".ledger-table td::before { content: attr(data-label);")
                .contains(".ledger-table--details .ledger-cell--amount .money { white-space: normal; }")
                .contains(".ledger-table caption { display: block; width: 100%; }")
                .contains(".ledger-panel { min-height: clamp(18rem, calc(100dvh - 26rem), 32rem); }")
                .contains(".ledger-summary__stats")
                .contains(".ledger-summary__action")
                .contains("--flow-scrollbar-track: var(--flow-surface);")
                .contains("--flow-scrollbar-thumb: var(--flow-violet);")
                .contains("--flow-scrollbar-thumb-hover: var(--flow-magenta);")
                .contains(":root,\nhtml,\nbody,")
                .contains("::-webkit-scrollbar,")
                .contains("::-webkit-scrollbar-thumb:hover,");
        assertThat(functionBody(script, "openProjectionDetails"))
                .contains("kind === \"ledger\"")
                .contains("renderLedgerDetails(items)");
        assertThat(script).contains("target(\"ledger-list\")?.addEventListener(\"click\", handleProjectionDetailsClick)");
    }

    @Test
    void servesContainerResponsiveLedgerSummaryContracts() throws Exception {
        String css = resource("/styles.css");

        assertThat(css)
                .contains(".ledger-summary { display: grid; container-name: ledger-summary; container-type: inline-size;")
                .contains(".ledger-summary__stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 12rem), 1fr));")
                .contains(".ledger-summary__stats dt { min-inline-size: 0;")
                .contains("overflow-wrap: normal; word-break: normal;")
                .contains("font-size: clamp(0.875rem, 5cqi, 1.35rem);")
                .contains(".ledger-summary__stats .money { font-variant-numeric: tabular-nums; white-space: nowrap; }")
                .contains("@container ledger-summary (max-width: 32rem)")
                .contains(".ledger-summary__stats > div:nth-child(n + 4) { grid-column: 1 / -1; }")
                .contains(".ledger-summary__action button { inline-size: fit-content; max-inline-size: 100%; }")
                .contains(".ledger-summary__action button { max-inline-size: min(100%, 22rem); }")
                .doesNotContain(".ledger-summary__stats { grid-template-columns: repeat(2, minmax(0, 1fr));")
                .doesNotContain(".ledger-summary__action button { width: 100%; }");
    }

    @Test
    void servesMobileAdaptiveNavigationAndDocumentFlowContracts() throws Exception {
        String css = resource("/styles.css");
        int mobileStart = css.indexOf("@media (max-width: 520px)");
        assertThat(mobileStart).isGreaterThanOrEqualTo(0);
        String mobile = css.substring(mobileStart);

        assertThat(mobile).contains(".dashboard-shell { height: auto; min-height: 100dvh; overflow: visible;")
                .contains(":root { --mobile-inner-gutter: max(var(--shell-gutter), 0.75rem); --mobile-section-space: 1.25rem; }")
                .contains(".shell-metrics { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); overflow: visible;")
                .contains(".cash-card--primary { grid-column: 1 / -1; }")
                .contains(".module-tabs { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); overflow: visible;")
                .contains(".module-nav-row { grid-template-columns: minmax(0, 1fr); padding: var(--mobile-inner-gutter); }")
                .contains(".nav-guide-slot button, .nav-actions button, .nav-reset-slot button { inline-size: fit-content; min-inline-size: min-content; max-inline-size: 100%; min-block-size: 44px; padding-inline: 1rem; }")
                 .contains(".nav-guide-slot button { padding-inline: 0.875rem; }")
                 .contains(".primary-shell, .module-workspace, .module-panel")
                 .contains("overflow: visible;")
                .contains(".primary-shell { display: block; }")
                .contains(".primary-shell { padding-bottom: calc(var(--mobile-section-space) + env(safe-area-inset-bottom)); }")
                .contains(".review-column { justify-items: center; border: 0; border-radius: 0; background: transparent;")
                .contains(".review-column--recommendations > [data-api-target].empty-state,")
                .contains(".manual-review-list, .recommendation-list { padding-right: 0; }")
                .contains(".category-dialog__panel { max-height: calc(100dvh - 1.5rem);")
                .doesNotContain("width: 100vw");

        assertThat(resource("/index.html")).doesNotContain("nav-feedback-region")
                .doesNotContain("demo-reset-status");
        assertThat(css).doesNotContain("nav-feedback-region")
                .doesNotContain("feedback-region-block-size");
    }

    @Test
    void servesSemanticNetFlowAndProjectionLabels() throws Exception {
        String html = resource("/index.html");

        assertThat(html).contains("<p class=\"card-label\">Flujo neto</p>")
                .contains("Entradas − Salidas = Flujo neto")
                .doesNotContain("data-field=\"profile-label\"")
                .contains("<label for=\"opening-balance\">Saldo inicial (supuesto de partida)</label>")
                .contains("Supuesto de partida para estimar el período; no representa saldo bancario.");
        assertThat(functionBody(resource("/app.js"), "renderProjection"))
                .contains("Cierre proyectado (estimación resultante)")
                .contains("Resultado estimado a partir del saldo inicial y los movimientos del período.");
    }

    @Test
    void keepsProjectionCalculationExplicitAfterChangesAndEvidenceRefreshes() throws Exception {
        String script = resource("/app.js");
        String submit = functionBody(script, "handleProjectionSubmit");

        assertThat(submit).contains("fetchProjection(balance);");
        assertThat(countOccurrences(submit, "fetchProjection(")).isEqualTo(1);
        assertThat(countOccurrences(script, "fetchProjection(")).isEqualTo(2);
        assertThat(functionBody(script, "handleProjectionPeriodChange"))
                .contains("state.projection.horizonDays")
                .contains("updateProjectionControls()")
                .contains("markProjectionPending(\"Cambios pendientes de calcular.\")")
                .doesNotContain("scheduleCockpitPreferencesSave()")
                .doesNotContain("fetchProjection(");
        assertThat(functionBody(script, "handleOpeningBalanceChange"))
                .contains("markProjectionPending(\"Ajusta los datos y calcula la proyección.\")")
                .doesNotContain("fetchProjection(");
        assertThat(functionBody(script, "refreshCockpitEvidence"))
                .contains("markProjectionPending(\"Evidencia actualizada. Calcula la proyección para ver el resultado.\")")
                .doesNotContain("fetchProjection(");
        assertThat(functionBody(script, "updateProjectionControls"))
                .contains("Calcular proyección (${horizonLabel})");
        assertThat(functionBody(script, "markProjectionPending"))
                .contains("pendingCalculation = true")
                .contains("setState(target(\"projection-results\"), \"empty\", message)");
    }

    @Test
    void gatesProjectionOnDemoSetupAndPreservesFirstUseOnboarding() throws Exception {
        String html = resource("/index.html");
        String script = resource("/app.js");

        assertThat(html).contains("id=\"tab-proyeccion\"")
                .contains("aria-disabled=\"true\" disabled tabindex=\"-1\"");
        assertThat(functionBody(script, "loadInitialData"))
                .contains("hasPendingManualReviews() || !state.demoResetComplete")
                .contains("setProjectionBlockedState()");
        assertThat(functionBody(script, "runDemoReset"))
                .contains("state.demoResetComplete = true;")
                .contains("await refreshCockpitEvidence({ forceFresh: true })")
                .contains("updateProjectionGate();");
        assertThat(functionBody(script, "updateProjectionGate"))
                .contains("const blocked = !state.demoResetComplete || hasPendingManualReviews();")
                .contains("projectionTab.disabled = blocked")
                .contains("projectionTab.setAttribute(\"aria-disabled\", String(blocked))");
        assertThat(functionBody(script, "setProjectionBlockedState"))
                .contains("Reinicia la demo para habilitar la proyección.")
                .contains("Categoriza los ${count} movimientos pendientes para habilitar la proyección.");
        assertThat(functionBody(script, "handleModuleTabKeydown"))
                .contains("event.currentTarget.disabled")
                .contains("event.currentTarget.getAttribute(\"aria-disabled\") === \"true\"");
        assertThat(functionBody(script, "activateModuleTab"))
                .contains("if (!tab || tab.disabled || tab.getAttribute(\"aria-disabled\") === \"true\") return;");
        assertThat(functionBody(script, "setupOnboardingGuide"))
                .contains("localStorage.getItem(ONBOARDING_STORAGE_KEY) === \"true\"")
                .contains("guide.hidden = Boolean(dismissed);")
                .contains("if (!dismissed) openOnboardingGuide(guide);")
                .doesNotContain("openOnboardingGuide(guide);\n        }");
    }

    @Test
    void servesHumanReadableDemoLoadEvidenceWithTechnicalDetailsDisclosed() throws Exception {
        String html = resource("/index.html");
        String script = resource("/app.js");
        String css = resource("/styles.css");

        assertThat(html).contains("<h2>Comprobantes</h2>")
                .contains("Confirma que los datos de demostración se cargaron correctamente sin exponer información sensible.")
                .contains("<h3 id=\"sync-title\">Carga de demostración</h3>")
                .doesNotContain("<p class=\"eyebrow\">Comprobantes</p>")
                .doesNotContain("Comprobante fixture");
        assertThat(functionBody(script, "renderProviderSyncReceipt"))
                .contains("Carga de demostración completada")
                .contains("[\"Estado\", humanSyncStatus(status.status)]")
                .contains("[\"Origen\", humanSyncOrigin(status.providerType)]")
                .contains("[\"Movimientos recibidos/importados\"")
                .contains("[\"Persistencia\", humanSyncPersistence(status.durability)]")
                .contains("<button type=\"button\" class=\"button-secondary receipt-details-trigger\"")
                .contains("data-projection-details-trigger=\"receipt\"")
                .contains("Ver detalles técnicos")
                .doesNotContain("<details")
                .doesNotContain("<summary");
        assertThat(functionBody(script, "renderProviderSyncTechnicalDetails"))
                .contains("[\"Estado técnico\", status?.status ?? \"No disponible\"]")
                .contains("[\"Proveedor\", status?.providerType ?? \"No disponible\"]")
                .contains("[\"sync ID\", status?.syncId ?? \"No disponible\"]")
                .contains("[\"Durabilidad\", status?.durability ?? \"No disponible\"]");
        assertThat(functionBody(script, "safeProviderErrors"))
                .contains("Sin incidencias")
                .contains("Incidencias:");
        assertThat(css).contains(".receipt-details-trigger { margin-top: 1rem;")
                .doesNotContain(".receipt-details {")
                .doesNotContain(".receipt-details summary");
    }

    @Test
    void servesFocusedProjectionDetailsWithoutTechnicalAlertKeysInMainRender() throws Exception {
        String html = resource("/index.html");
        String script = resource("/app.js");
        String css = resource("/styles.css");

        assertThat(html).contains("data-projection-details-dialog")
                .contains("data-projection-details-title")
                .contains("data-projection-details-description")
                .contains("data-projection-details-content")
                .contains("data-projection-details-close");
        assertThat(functionBody(script, "renderProjection"))
                .contains("renderProjectionAlerts(projection.alerts || [])")
                .contains("Ver detalle diario (${horizonLabel})")
                .contains("data-projection-details-trigger=\"days\"")
                .doesNotContain("renderProjectionDays")
                .doesNotContain("projection-day")
                .doesNotContain("alert.condition")
                .doesNotContain("alert.ruleKey")
                .doesNotContain("data-projection-details-trigger=\"obligations\"");
        assertThat(countOccurrences(functionBody(script, "renderProjection"), "data-projection-details-trigger=\"days\""))
                .isEqualTo(1);
        assertThat(functionBody(script, "renderProjectionAlerts"))
                .contains("const groups = groupProjectionAlerts(alerts)")
                .contains("data-projection-details-trigger=\"alerts\"")
                .contains("Ver alertas (${groups.length})")
                .doesNotContain("alert-chip");
        assertThat(functionBody(script, "projectionAlertCopy"))
                .contains("projected_balance_above_threshold")
                .contains("Saldo saludable")
                .contains("Saldo bajo el nivel de resguardo")
                .contains("colchón de liquidez por encima del nivel saludable configurado")
                .contains("formatProjectionDateRanges");
        assertThat(functionBody(script, "renderProjectionCalendar"))
                .contains("<th scope=\"col\">Fecha</th>")
                .contains("<th scope=\"col\">Saldo al cierre</th>")
                .contains("<th scope=\"col\">Entradas</th>")
                .contains("<th scope=\"col\">Salidas</th>")
                .contains("data-label=\"Fecha\"")
                .contains("data-label=\"Saldo al cierre\"")
                .contains("data-label=\"Entradas\"")
                .contains("data-label=\"Salidas\"");
        assertThat(css).contains(".projection-calendar table")
                .contains(".projection-details-dialog__content")
                .contains("min-block-size: var(--control-min-block-size);");
    }

    @Test
    void keepsDashboardContentOnDocumentScrollAndReservesInternalScrollForDialogs() throws Exception {
        String css = resource("/styles.css");
        String script = resource("/app.js");

        assertThat(css)
                .contains(".dashboard-shell {")
                .contains("height: auto;")
                .contains("min-height: 100dvh;")
                .contains("overflow: visible;")
                .contains(".module-workspace { width: 100%; min-width: 0; }")
                .contains(".module-panel { width: 100%; min-width: 0; }")
                .contains(".manual-review-list, .recommendation-list { padding-inline: 0; }")
                .doesNotContain(".module-panel { min-height: 0; max-height: 100%; overflow: auto;")
                .doesNotContain(".manual-review-list, .recommendation-list { min-height: 0; max-height:")
                .contains(".projection-details-dialog__content { min-height: 0; max-height: 100%; overflow-y: auto;")
                .contains("html.dialog-open,")
                .contains("body.dialog-open { overflow: hidden; }");
        assertThat(script)
                .contains("function syncDialogScrollLock()")
                .contains("document.documentElement.classList.toggle(\"dialog-open\", locked)")
                .contains("document.body?.classList.toggle(\"dialog-open\", locked)");
    }

    @Test
    void keepsAllDashboardRegionsIntrinsicAndBoundsOnlyOverlayScrollAreas() throws Exception {
        String css = resource("/styles.css");

        assertThat(css)
                .contains(".dashboard-shell {")
                .contains("min-height: 100dvh;")
                .contains("height: auto;")
                .contains("overflow: visible;")
                .contains("padding-bottom: max(1rem, env(safe-area-inset-bottom));")
                .contains("/* The page owns dashboard scrolling; main-content regions grow with their content. */")
                .contains(".primary-shell,\n.module-workspace,\n.module-panel,")
                .contains("height: auto;\n    max-height: none;\n    overflow: visible;")
                .doesNotMatch("(?s)(?<![-\\w])height\\s*:\\s*100dvh\\s*;")
                .doesNotContain(".shell-metrics { display: flex; overflow-x: auto;")
                .contains(".category-dialog__options")
                .contains("overflow-y: auto;")
                .contains(".projection-details-dialog__content { min-height: 0; max-height: 100%; overflow-y: auto;")
                .contains(".onboarding-card")
                .contains("max-height: min(90dvh, 42rem);");

        assertIntrinsicDashboardRules(css);
        assertThat(css).doesNotContain(".dashboard-shell::-webkit-scrollbar")
                .doesNotContain(".primary-shell::-webkit-scrollbar")
                .doesNotContain(".module-workspace::-webkit-scrollbar")
                .doesNotContain(".module-panel::-webkit-scrollbar")
                .doesNotContain(".review-grid::-webkit-scrollbar")
                .doesNotContain(".review-column::-webkit-scrollbar")
                .doesNotContain(".manual-review-list::-webkit-scrollbar")
                .doesNotContain(".recommendation-list::-webkit-scrollbar")
                .doesNotContain(".receipt-rail::-webkit-scrollbar");
    }

    @Test
    void servesResponsiveDailyDetailsDisclosureWithoutMainDailyCards() throws Exception {
        String html = resource("/index.html");
        String script = resource("/app.js");
        String css = resource("/styles.css");

        assertThat(functionBody(script, "renderProjection"))
                .contains("Ver detalle diario (${horizonLabel})")
                .contains("data-projection-details-trigger=\"days\"")
                .doesNotContain("projection-day");
        assertThat(functionBody(script, "openProjectionDetails"))
                .contains("state.projection.dailyBalances")
                .contains("kind === \"receipt\"")
                .contains("renderProviderSyncTechnicalDetails(state.syncReceiptStatus)")
                .contains("Calendario completo (${state.projection.horizonDays} días)")
                .contains("renderProjectionCalendar(items)");
        assertThat(functionBody(script, "renderProjectionCalendar"))
                .contains("<table>")
                .contains("<th scope=\"col\">Fecha</th>")
                .contains("<th scope=\"col\">Saldo al cierre</th>")
                .contains("<th scope=\"col\">Entradas</th>")
                .contains("<th scope=\"col\">Salidas</th>")
                .contains("data-label=\"Fecha\"")
                .contains("data-label=\"Saldo al cierre\"")
                .contains("data-label=\"Entradas\"")
                .contains("data-label=\"Salidas\"");
        assertThat(html).contains("class=\"projection-top-grid\"");
        assertThat(css)
                .contains(".projection-top-grid { display: grid;")
                .contains("grid-template-columns: repeat(2, minmax(0, 1fr));")
                .contains(".projection-calendar tbody td::before")
                .contains("display: flex; justify-content: space-between;");
    }

    @Test
    void groupsAlertsByConditionAndThresholdWithHumanConsequencesAndDateRanges() throws Exception {
        String script = resource("/app.js");

        assertThat(functionBody(script, "groupProjectionAlerts"))
                .contains("const condition = alert?.condition || alert?.ruleKey")
                .contains("const thresholdKey")
                .contains("const key = `${condition}|${thresholdKey}`")
                .contains("lowestBalance")
                .contains("highestBalance");
        assertThat(functionBody(script, "projectionAlertCopy"))
                .contains("Superó ${threshold}")
                .contains("Quedó bajo ${threshold}")
                .contains("money.format(Number(alert.threshold))")
                .contains("colchón de liquidez por encima del nivel saludable configurado")
                .contains("revisa el momento de los cargos y de los ingresos")
                .contains("Obligaciones antes de un ingreso")
                .contains("alert?.dates");
        assertThat(functionBody(script, "formatProjectionDateRanges"))
                .contains("del ${startDate.getUTCDate()} al ${endDate.getUTCDate()} de ${startMonth} de ${startYear}")
                .contains("ranges.push([start, end])");
    }

    @Test
    void keepsProjectionDetailsKeyboardAccessibleAndReturnsFocus() throws Exception {
        String script = resource("/app.js");

        assertThat(functionBody(script, "openProjectionDetails"))
                .contains("state.projectionDetailsLastFocus = trigger")
                .contains("data-projection-details-close")
                .contains("dialog.hidden = false")
                .contains("groupProjectionAlerts(state.projection.alerts)")
                .contains("items.map(renderProjectionAlertDetail)")
                .doesNotContain("state.projection.alerts.map(renderProjectionAlertDetail)");
        assertThat(functionBody(script, "closeProjectionDetails"))
                .contains("dialog.hidden = true")
                .contains("state.projectionDetailsLastFocus?.focus")
                .contains("state.projectionDetailsLastFocus = null");
        assertThat(functionBody(script, "handleGlobalKeydown"))
                .contains("activeDialog")
                .contains("closeProjectionDetails()");
        assertThat(functionBody(script, "syncDialogScrollLock"))
                .contains("data-projection-details-dialog]:not([hidden])")
                .contains("document.documentElement.classList.toggle(\"dialog-open\", locked)")
                .contains("document.body?.classList.toggle(\"dialog-open\", locked)");
    }

    @Test
    void disablesProjectionAndBlocksFetchWhileManualReviewsArePending() throws Exception {
        String script = resource("/app.js");

        assertThat(functionBody(script, "renderMovementEvidence"))
                .contains("state.projection.pendingManualReviewCount = manualReview.length")
                .contains("updateProjectionGate()");
        assertThat(functionBody(script, "updateProjectionGate"))
                .contains("projectionTab.disabled = blocked")
                .contains("projectionTab.setAttribute(\"aria-disabled\", String(blocked))")
                .contains("if (blocked) setProjectionBlockedState()");
        assertThat(functionBody(script, "fetchProjection"))
                .contains("if (hasPendingManualReviews())")
                .contains("setProjectionBlockedState()")
                .contains("return;");
        assertThat(functionBody(script, "refreshCockpitEvidence"))
                .contains("markProjectionPending(\"Evidencia actualizada. Calcula la proyección para ver el resultado.\")")
                .doesNotContain("fetchProjection(");
        assertThat(functionBody(script, "setProjectionBlockedState"))
                .contains("Categoriza los ${count} movimientos pendientes");
    }

    @Test
    void keepsReviewActiveAfterCategorizationAndEnablesProjectionAfterTheLastPendingMovement() throws Exception {
        String script = resource("/app.js");

        assertThat(functionBody(script, "renderManualReview"))
                .contains("updateDemoHighlight(state.demoResetComplete ? \"review\" : \"reset\")")
                .doesNotContain("\"project\"");
        assertThat(functionBody(script, "resolveManualReviewMovement"))
                .contains("Movimiento categorizado. La proyecci")
                .doesNotContain("updateDemoHighlight(\"project\")");
        assertThat(functionBody(script, "updateProjectionGate"))
                .contains("const blocked = !state.demoResetComplete || hasPendingManualReviews()")
                .contains("projectionTab.disabled = blocked");
        assertThat(functionBody(script, "runDemoReset"))
                .contains("await refreshCockpitEvidence({ forceFresh: true })");
        assertThat(functionBody(script, "renderMovement"))
                .contains("movement.movementDirection === \"DEBIT\" ? \"Salida\" : \"Entrada\"")
                .contains("formatPositiveMoney(movement.amount)");
        assertThat(functionBody(script, "renderManualReviewMovement"))
                .contains("movement.movementDirection === \"DEBIT\" ? \"Salida\" : \"Entrada\"")
                .contains("formatPositiveMoney(movement.amount)");
    }

    @Test
    void marksCompletedManualReviewAndCollapsesItsMovementSurface() throws Exception {
        String script = resource("/app.js");
        String css = resource("/styles.css");

        assertThat(functionBody(script, "renderManualReview"))
                .contains("setReviewState(container, \"complete\")")
                .contains("setReviewState(container, \"pending\")")
                .contains("container.classList.add(\"review-complete-message\")")
                .contains("container.classList.remove(\"review-complete-message\")")
                .contains("Todos los movimientos están categorizados. La proyección está disponible.")
                .doesNotContain("Sin movimientos pendientes de revisión.");
        assertThat(functionBody(script, "setReviewState"))
                .contains("closest(\".review-grid\")?.setAttribute(\"data-review-state\", reviewState)")
                .contains("closest(\".review-panel\")?.setAttribute(\"data-review-state\", reviewState)");
        assertThat(css)
                .contains(".review-panel[data-review-state=\"pending\"] { align-content: start; }")
                .contains(".review-grid[data-review-state=\"pending\"] { grid-template-columns: minmax(0, 3fr) minmax(18rem, 2fr); align-items: start; }")
                .contains(".review-panel[data-review-state=\"complete\"] { grid-template-rows: auto; align-content: start; }")
                .contains(".review-grid[data-review-state=\"complete\"] { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); align-items: start; }")
                .contains(".review-grid[data-review-state=\"complete\"] .review-column--movements")
                .contains(".review-grid[data-review-state=\"complete\"] .review-column--recommendations { grid-column: auto;")
                .contains(".review-complete-message { margin: 0; border: 1px solid var(--flow-border);")
                .contains(".review-column > [data-api-target].empty-state { border: 0; border-radius: 0; background: transparent;")
                .contains(".review-grid[data-review-state=\"pending\"],\n    .review-grid[data-review-state=\"complete\"] { grid-template-columns: minmax(0, 1fr); }");
    }

    @Test
    void servesAuditedReviewCompositionWithUsefulViewportOccupationContracts() throws Exception {
        String css = resource("/styles.css");

        assertThat(css)
                .contains("/* Audited manual-review composition: content determines height; the shell does not. */")
                .contains(".review-grid[data-review-state=\"pending\"] {\n        grid-template-columns: minmax(0, 3fr) minmax(0, 2fr);")
                .contains("grid-template-columns: repeat(auto-fit, minmax(min(100%, 18rem), 1fr));")
                .contains(".review-grid[data-review-state=\"pending\"] .manual-review-list {\n        grid-template-columns:")
                .contains(".review-panel {\n        grid-template-rows: auto;\n        align-content: start;\n        border: 0;")
                .contains(".review-grid[data-review-state=\"complete\"] {\n        grid-template-columns: minmax(0, 1fr);")
                .contains(".review-grid[data-review-state=\"complete\"] .review-column--movements > h3,")
                .contains(".movement--review .review-message:empty { display: none; }")
                .contains(".movement--review .category-select-trigger {\n        grid-area: action;")
                .doesNotContain(".review-grid[data-review-state=\"pending\"] .manual-review-list { max-height:");
    }

    @Test
    void servesTabletFlowAndIntrinsicCompactControlContracts() throws Exception {
        String css = resource("/styles.css");

        int tabletStart = css.indexOf("@media (min-width: 521px) and (max-width: 860px)");
        assertThat(tabletStart).isGreaterThanOrEqualTo(0);
        String tablet = css.substring(tabletStart);

        assertThat(tablet).contains(".dashboard-shell { height: auto; min-height: 100dvh; overflow: visible;")
                .contains(".topbar { grid-template-columns: 1fr; grid-template-areas: \"brand\" \"title\" \"actions\"; justify-items: center;")
                .contains(".hero-ledger { grid-template-columns: repeat(3, minmax(0, 1fr)); align-items: start; gap: 0.75rem; }")
                .contains(".cash-card { min-block-size: 0; padding: 0.9rem; }")
                .contains(".amount { font-size: clamp(1.3rem, 3vw, 1.8rem); }")
                 .contains(".module-tabs { flex-wrap: wrap; overflow: visible;")
                 .contains(".review-grid { grid-template-columns: minmax(0, 1fr); gap: 0.75rem; }")
                 .contains("height: auto; min-height: 0; overflow: visible;")
                .contains("inline-size: fit-content; min-inline-size: min-content; max-inline-size: 100%; min-block-size: 44px;")
                .contains("env(safe-area-inset-bottom)");
    }

    @Test
    void servesIntrinsicControlsAndTransientResetSuccessFeedback() throws Exception {
        String html = resource("/index.html");
        String css = resource("/styles.css");
        String script = resource("/app.js");

        assertThat(css).contains("inline-size: fit-content;")
                .contains("max-inline-size: 100%;")
                .contains("min-block-size: var(--control-min-block-size);")
                .contains(".control--block { inline-size: 100%; }")
                .contains(".category-select-trigger {")
                .contains("justify-self: center;")
                .contains("--feedback-success-duration: 2400ms;")
                .contains("--z-feedback-overlay: 70;")
                .contains("backdrop-filter: blur(12px);")
                .contains("@media (prefers-reduced-motion: reduce)");
        assertThat(html).contains("data-demo-reset-success hidden")
                .contains("role=\"status\"")
                .doesNotContain("aria-modal=\"true\" data-demo-reset-success");
        assertThat(functionBody(script, "runDemoReset"))
                .contains("showDemoResetSuccess()")
                .doesNotContain("focusStatus")
                .doesNotContain("scrollIntoView");
        assertThat(functionBody(script, "showDemoResetSuccess"))
                .contains("window.clearTimeout(state.demoResetSuccessTimer)")
                .contains("overlay.hidden = false")
                .contains("overlay.hidden = true");
        assertThat(script).doesNotContain("function focusStatus");
    }

    @Test
    void suppressesRedundantInfoSeverityPillsForInformationalRecommendations() throws Exception {
        String recommendationRenderer = functionBody(resource("/app.js"), "renderRecommendation");

        assertThat(recommendationRenderer)
                .contains("severity === \"INFO\" ? \"\"")
                .contains("severityLabel")
                .contains("WARNING: \"Atención\"")
                .contains("recommendation-severity");
    }

    @Test
    void servesProportionalDashboardCompositionContracts() throws Exception {
        String html = resource("/index.html");
        String css = resource("/styles.css");
        String script = resource("/app.js");

        assertThat(html).contains("class=\"module-nav-row\"")
                .contains("class=\"nav-guide-slot\"")
                .contains("class=\"nav-actions nav-reset-slot\"");
        assertThat(css).contains("--density-control-padding:")
                .contains("--density-card-padding:")
                .contains("--density-section-gap:")
                .contains("--content-measure:")
                .contains("--metric-min-height:")
                .contains("grid-template-columns: repeat(3, minmax(0, 1fr));")
                .contains("container-type: inline-size;")
                .contains("min-block-size: var(--metric-min-height);")
                .contains(".movement-meta")
                .contains("inline-size: fit-content;")
                .contains(".recommendation-severity")
                .doesNotContain("grid-template-columns: 1.2fr repeat(2, minmax(0, 0.9fr));");
        assertThat(script).contains("class=\"movement-meta\"")
                .contains("Atención");
    }

    @Test
    void servesReadableBrandAndCenteredProjectionControlContracts() throws Exception {
        String html = resource("/index.html");
        String css = resource("/styles.css");

        assertThat(html).contains("<h1>RESUMEN DE CAJA</h1>")
                .contains("class=\"brand-crop-box\"")
                .contains("class=\"brand-lockup brand-lockup--prominent\"")
                .contains("class=\"projection-controls\"")
                .contains("class=\"control--block\"");
        assertThat(css).contains("--brand-slot-inline-size: clamp(160px, 24cqi, 336px);")
                 .contains("--field-measure: 20rem;")
                .contains("font-family: Raleway, system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif;")
                .contains("font-weight: 900;")
                .contains("font-size: clamp(1.05rem, 2.5cqi, 2.25rem);")
                .contains("letter-spacing: clamp(0.035em, 0.06em, 0.08em);")
                .contains("text-transform: uppercase;")
                .contains("white-space: nowrap;")
                .contains("aspect-ratio: 744 / 230;")
                .contains("block-size: auto;")
                .contains("object-fit: contain;")
                .doesNotContain("object-fit: cover;")
                .doesNotContain("object-position: center 52%;")
                 .contains("inline-size: var(--field-measure);")
                 .contains("max-inline-size: 100%;")
                .contains(".projection-controls {\n    justify-items: center;\n    text-align: center;\n}")
                .contains(".projection-controls fieldset {\n    inline-size: fit-content;")
                 .contains(".projection-controls > button.control--block {\n    inline-size: fit-content;")
                 .contains("padding: var(--control-padding);")
                .contains(".control--block { inline-size: 100%; }")
                .contains(".category-dialog__footer .control--block { inline-size: 100%;")
                 .contains("min-block-size: 44px;")
                .contains("@media (min-width: 521px) and (max-width: 860px)")
                .contains("@media (max-width: 520px)")
                .doesNotContain(".projection-controls > button.control--block { inline-size: 100%;");
    }

    @Test
    void keepsStaticAssetsAndNoFrontendBuildToolingContract() throws Exception {
        mockMvc.perform(get("/favicon.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        mockMvc.perform(get("/branding.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("vite"))))
                .andExpect(content().string(not(containsString("npm"))));
    }

    private String resource(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private String functionBody(String script, String functionName) {
        String signature = "function " + functionName + "(";
        int signatureStart = script.indexOf(signature);
        assertThat(signatureStart).as("function %s is present", functionName).isGreaterThanOrEqualTo(0);
        int parameterEnd = script.indexOf(')', signatureStart);
        int bodyStart = script.indexOf('{', parameterEnd);
        assertThat(bodyStart).as("function %s has a body", functionName).isGreaterThanOrEqualTo(0);

        int depth = 0;
        for (int index = bodyStart; index < script.length(); index++) {
            char character = script.charAt(index);
            if (character == '{') depth++;
            if (character == '}' && --depth == 0) return script.substring(bodyStart, index + 1);
        }
        throw new AssertionError("Function " + functionName + " is not closed");
    }

    private void assertIntrinsicDashboardRules(String css) {
        List<String> dashboardRegions = List.of(
                ".dashboard-shell",
                ".primary-shell",
                ".module-workspace",
                ".module-panel",
                ".review-grid",
                ".review-column",
                ".manual-review-list",
                ".recommendation-list",
                ".receipt-rail",
                ".shell-metrics"
        );
        Matcher rules = Pattern.compile("(?s)([^{}]+)\\{([^{}]*)\\}").matcher(css);

        while (rules.find()) {
            String selector = rules.group(1).replaceAll("/\\*.*?\\*/", "").trim();
            boolean appliesToDashboardRegion = dashboardRegions.stream()
                    .anyMatch(region -> List.of(selector.split(","))
                            .stream()
                            .map(String::trim)
                            .anyMatch(part -> part.equals(region)
                                    || part.startsWith(region + ".")
                                    || part.startsWith(region + "[")
                                    || part.startsWith(region + ":")
                                    || part.startsWith(region + "-")
                                    || part.startsWith(region + " ")
                                    || part.startsWith(region + ">")));
            if (!appliesToDashboardRegion) continue;

            String declarations = rules.group(2);
            assertThat(declarations)
                    .as("dashboard selector %s must not create a nested scroll context", selector)
                    .doesNotMatch("(?is)\\boverflow(?:-[xy])?\\s*:\\s*(?:auto|scroll)")
                    .doesNotMatch("(?is)(?<![-\\w])max-height\\s*:")
                    .doesNotMatch("(?is)(?<![-\\w])height\\s*:\\s*(?!auto\\s*;)");
        }
    }

    private int countOccurrences(String value, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
