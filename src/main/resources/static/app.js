(() => {
    const PROFILE_ID = "pharmacy-cl";
    const THEME_STORAGE_KEY = "pymeflow.theme";
    const ONBOARDING_STORAGE_KEY = "pymeflow.onboardingGuide.dismissed";
    const API = {
        activeCategories: "/api/profiles/active/categories",
        providerSyncs: "/api/cashflow/provider-syncs",
        manualReview: `/api/cashflow/history/manual-review?profileId=${PROFILE_ID}`,
        projectionReady: `/api/cashflow/history/projection-ready?profileId=${PROFILE_ID}`,
        cockpitProjection: "/api/cashflow/cockpit/projection",
        cockpitPreferences: `/api/cashflow/cockpit/preferences?profileId=${PROFILE_ID}`,
        demoReset: `/api/cockpit/demo/reset-and-seed?profileId=${PROFILE_ID}`,
        recommendations: `/api/cashflow/recommendations?profileId=${PROFILE_ID}`,
        manualReviewResolution: "/api/cashflow/manual-review/resolutions/",
    };

    const state = {
        categories: [],
        projection: {
            horizonDays: 7,
            openingBalance: null,
            projectableMovementDates: [],
            pendingManualReviewCount: 0,
            pendingCalculation: true,
            alerts: [],
            obligations: [],
            dailyBalances: [],
        },
        preferencesLoaded: false,
        preferenceSaveTimer: null,
        resolvingMovementIds: new Set(),
        demoResetComplete: false,
        demoHighlightStep: "reset",
        onboardingStepIndex: 0,
        onboardingLastFocus: null,
        categoryDialogMovementId: null,
        categoryDialogSelectedKey: null,
        categoryDialogLastFocus: null,
        projectionDetailsLastFocus: null,
        syncReceiptStatus: null,
        ledgerMovements: [],
    };

    const ONBOARDING_STEPS = ["welcome", "reset", "review", "projection", "next"];
    const DEMO_RESET_SUCCESS_DURATION = "--feedback-success-duration";

    const $ = (selector) => document.querySelector(selector);
    const target = (name) => $(`[data-api-target="${name}"]`);
    const money = new Intl.NumberFormat("es-CL", { style: "currency", currency: "CLP", maximumFractionDigits: 0 });

    document.addEventListener("DOMContentLoaded", () => {
        setupThemePreference();
        setupModuleTabs();
        setupOnboardingGuide();
        updateDemoHighlight();
        loadInitialData();
        $("#demo-reset-btn")?.addEventListener("click", runDemoReset);
        target("manual-review-list")?.addEventListener("click", handleManualReviewClick);
        setupCategoryDialog();
        setupProjectionDetailsDialog();
        target("projection-results")?.addEventListener("click", handleProjectionDetailsClick);
        target("ledger-list")?.addEventListener("click", handleProjectionDetailsClick);
        target("sync-receipt")?.addEventListener("click", handleProjectionDetailsClick);
        document.addEventListener("keydown", handleGlobalKeydown);
        $(`[data-projection-form]`)?.addEventListener("submit", handleProjectionSubmit);
        $("#opening-balance")?.addEventListener("input", handleOpeningBalanceChange);
        document.querySelectorAll(`[name="horizonDays"]`).forEach((control) => {
            control.addEventListener("change", handleProjectionPeriodChange);
        });
    });

    function setupThemePreference() {
        applyThemePreference(resolveThemePreference());
        const toggle = $("#theme-toggle");
        toggle?.addEventListener("click", () => {
            const currentTheme = document.documentElement.dataset.theme || resolveThemePreference();
            const nextTheme = currentTheme === "dark" ? "light" : "dark";
            safeLocalStorage(() => localStorage.setItem(THEME_STORAGE_KEY, nextTheme));
            applyThemePreference(nextTheme);
        });

        window.matchMedia("(prefers-color-scheme: dark)").addEventListener?.("change", () => {
            if (!readStoredTheme()) applyThemePreference(resolveThemePreference());
        });
    }

    function setupModuleTabs() {
        const tabs = [...document.querySelectorAll('[role="tab"][aria-controls]')];
        tabs.forEach((tab) => {
            tab.addEventListener("click", () => activateModuleTab(tab));
            tab.addEventListener("keydown", (event) => handleModuleTabKeydown(event, tabs));
        });
    }

    function setupOnboardingGuide() {
        const guide = $("[data-onboarding-guide]");
        if (!guide) return;
        const dismissed = safeLocalStorage(() => localStorage.getItem(ONBOARDING_STORAGE_KEY) === "true");
        guide.hidden = Boolean(dismissed);
        showOnboardingStep(0);
        guide.querySelectorAll("[data-onboarding-close]").forEach((control) => {
            control.addEventListener("click", () => dismissOnboardingGuide(guide));
        });
        guide.querySelector("[data-onboarding-prev]")?.addEventListener("click", () => showOnboardingStep(state.onboardingStepIndex - 1));
        guide.querySelector("[data-onboarding-next]")?.addEventListener("click", () => {
            const panels = document.querySelectorAll("[data-onboarding-step-panel]");
            if (state.onboardingStepIndex >= panels.length - 1) dismissOnboardingGuide(guide);
            else showOnboardingStep(state.onboardingStepIndex + 1);
        });
        document.querySelectorAll("[data-onboarding-open]").forEach((control) => {
            control.addEventListener("click", () => openOnboardingGuide(guide, control));
        });
        if (!dismissed) openOnboardingGuide(guide);
    }

    function openOnboardingGuide(guide = $("[data-onboarding-guide]"), trigger = document.activeElement) {
        if (!guide) return;
        state.onboardingLastFocus = trigger instanceof HTMLElement ? trigger : null;
        guide.hidden = false;
        syncDialogScrollLock();
        showOnboardingStep(state.onboardingStepIndex || 0);
        guide.querySelector("[data-onboarding-close]")?.focus({ preventScroll: true });
    }

    function dismissOnboardingGuide(guide = $("[data-onboarding-guide]")) {
        if (!guide) return;
        safeLocalStorage(() => localStorage.setItem(ONBOARDING_STORAGE_KEY, "true"));
        closeOnboardingGuideForSession(guide);
    }

    function closeOnboardingGuideForSession(guide = $("[data-onboarding-guide]")) {
        if (!guide) return;
        guide.hidden = true;
        const fallback = $("[data-onboarding-open]") || $("#contenido");
        (state.onboardingLastFocus || fallback)?.focus?.({ preventScroll: true });
        state.onboardingLastFocus = null;
        syncDialogScrollLock();
    }

    function showOnboardingStep(index) {
        const panels = [...document.querySelectorAll("[data-onboarding-step-panel]")];
        if (!panels.length) return;
        state.onboardingStepIndex = Math.max(0, Math.min(index, panels.length - 1));
        panels.forEach((panel, panelIndex) => {
            panel.hidden = panelIndex !== state.onboardingStepIndex;
            panel.setAttribute("aria-current", panelIndex === state.onboardingStepIndex ? "step" : "false");
        });
        const guide = $("[data-onboarding-guide]");
        guide?.setAttribute("data-onboarding-step-current", ONBOARDING_STEPS[state.onboardingStepIndex] || "welcome");
        const first = state.onboardingStepIndex === 0;
        const last = state.onboardingStepIndex === panels.length - 1;
        const previous = guide?.querySelector("[data-onboarding-prev]");
        const next = guide?.querySelector("[data-onboarding-next]");
        if (previous) previous.disabled = first;
        if (next) {
            next.textContent = last ? "Finalizar" : "Siguiente";
            next.setAttribute("aria-label", last ? "Finalizar y cerrar guía" : "Ver siguiente paso de la guía");
        }
        const progress = guide?.querySelector("[data-onboarding-progress]");
        if (progress) progress.textContent = first ? "Bienvenida" : `Paso ${state.onboardingStepIndex} de ${panels.length - 1}`;
    }

    function handleModuleTabKeydown(event, tabs) {
        if (event.currentTarget.disabled || event.currentTarget.getAttribute("aria-disabled") === "true") return;
        const nextKeys = ["ArrowRight", "ArrowDown"];
        const previousKeys = ["ArrowLeft", "ArrowUp"];
        if (![...nextKeys, ...previousKeys, "Home", "End"].includes(event.key)) return;
        event.preventDefault();
        const availableTabs = tabs.filter((tab) => !tab.disabled && tab.getAttribute("aria-disabled") !== "true");
        if (!availableTabs.length) return;
        const activeIndex = availableTabs.indexOf(event.currentTarget);
        const nextIndex = nextKeys.includes(event.key)
                ? (activeIndex + 1) % availableTabs.length
                : previousKeys.includes(event.key)
                        ? (activeIndex - 1 + availableTabs.length) % availableTabs.length
                        : event.key === "Home" ? 0 : availableTabs.length - 1;
        activateModuleTab(availableTabs[nextIndex]);
        availableTabs[nextIndex]?.focus();
    }

    function activateModuleTab(tab) {
        if (!tab || tab.disabled || tab.getAttribute("aria-disabled") === "true") return;
        document.querySelectorAll('[role="tab"][aria-controls]').forEach((current) => {
            const selected = current === tab;
            current.setAttribute("aria-selected", String(selected));
            current.tabIndex = selected ? 0 : -1;
            const panel = document.getElementById(current.getAttribute("aria-controls"));
            if (panel) panel.hidden = !selected;
        });
    }

    function resolveThemePreference() {
        return readStoredTheme() || preferredSystemTheme();
    }

    function readStoredTheme() {
        return safeLocalStorage(() => {
            const value = localStorage.getItem(THEME_STORAGE_KEY);
            return value === "light" || value === "dark" ? value : null;
        }) || null;
    }

    function preferredSystemTheme() {
        return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }

    function applyThemePreference(theme) {
        document.documentElement.dataset.theme = theme;
        const toggle = $("#theme-toggle");
        const label = $("[data-theme-toggle-label]");
        if (!toggle || !label) return;
        const dark = theme === "dark";
        toggle.setAttribute("aria-pressed", String(dark));
        toggle.setAttribute("aria-label", dark ? "Cambiar a tema claro" : "Cambiar a tema oscuro");
        label.textContent = dark ? "Modo oscuro" : "Modo claro";
    }

    function safeLocalStorage(callback) {
        try {
            return callback();
        } catch (error) {
            return null;
        }
    }

    async function loadInitialData() {
        await loadCockpitPreferences();
        await renderProfileAndCategories();
        await Promise.allSettled([
            renderMovementEvidence(),
            renderRecommendations(),
        ]);
        if (hasPendingManualReviews() || !state.demoResetComplete) setProjectionBlockedState();
        else markProjectionPending();
    }

    async function loadCockpitPreferences({ forceFresh = false } = {}) {
        const status = target("preferences-status");
        setState(status, "loading", "Cargando preferencias manuales.");
        try {
            const preferences = forceFresh ? await getJsonFresh(API.cockpitPreferences) : await getJson(API.cockpitPreferences);
            prefillCockpitPreferences(preferences);
            state.preferencesLoaded = true;
            setState(status, "success", "Preferencias manuales cargadas. Saldo manual, no bancario.");
        } catch (error) {
            state.preferencesLoaded = true;
            prefillCockpitPreferences({ preferredHorizonDays: 7 });
            setState(status, "error", safeError(error, "No se pudieron cargar las preferencias manuales."));
        }
    }

    function prefillCockpitPreferences(preferences) {
        const openingBalance = preferences.openingBalance;
        const preferredHorizonDays = Number(preferences.preferredHorizonDays || 7);
        state.projection.openingBalance = openingBalance ?? null;
        state.projection.horizonDays = preferredHorizonDays === 30 ? 30 : 7;

        const balanceInput = $("#opening-balance");
        if (balanceInput && openingBalance !== null && openingBalance !== undefined) {
            balanceInput.value = String(openingBalance);
        }
        document.querySelectorAll(`[name="horizonDays"]`).forEach((control) => {
            control.checked = Number(control.value) === state.projection.horizonDays;
        });
        updateProjectionControls();
    }

    function handleOpeningBalanceChange() {
        state.projection.openingBalance = readOpeningBalance();
        scheduleCockpitPreferencesSave();
        markProjectionPending("Ajusta los datos y calcula la proyección.");
    }

    async function renderProfileAndCategories({ forceFresh = false } = {}) {
        try {
            const categories = forceFresh ? await getJsonFresh(API.activeCategories) : await getJson(API.activeCategories);
            state.categories = categories;
        } catch (error) {
            state.categories = [];
            setState(target("manual-review-list"), "error", "No se pudieron cargar categorías activas. Intenta nuevamente antes de categorizar.");
        }
    }

    async function renderMovementEvidence({ forceFresh = false } = {}) {
        const ledger = target("ledger-list");
        setState(ledger, "loading", "Cargando movimientos de caja.");
        try {
            const [projectionReady, manualReview] = await Promise.all([
                forceFresh ? getJsonFresh(API.projectionReady) : getJson(API.projectionReady),
                forceFresh ? getJsonFresh(API.manualReview) : getJson(API.manualReview),
            ]);
            state.projection.projectableMovementDates = projectionReady.map((movement) => movement.date).filter(Boolean);
            state.projection.pendingManualReviewCount = manualReview.length;
            const movements = [...projectionReady, ...manualReview];
            updateCashTotals(projectionReady);
            renderLedger(movements);
            renderManualReview(manualReview);
            updateProjectionGate();
        } catch (error) {
            state.projection.projectableMovementDates = [];
            state.projection.pendingManualReviewCount = 0;
            updateCashTotals([]);
            setState(ledger, "error", safeError(error, "No se pudo cargar el historial de caja."));
            setState(target("manual-review-list"), "error", "No se pudo cargar revisión manual. El dashboard sigue disponible.");
        }
    }

    async function renderRecommendations({ forceFresh = false } = {}) {
        const container = target("recommendation-list");
        setState(container, "loading", "Cargando recomendaciones.");
        try {
            const response = forceFresh ? await getJsonFresh(API.recommendations) : await getJson(API.recommendations);
            if (!response.signals?.length) {
                setState(container, "empty", "Sin recomendaciones activas para este perfil.");
                return;
            }
            container.innerHTML = `<div class="recommendation-list">${response.signals.map(renderRecommendation).join("")}</div>`;
        } catch (error) {
            setState(container, "error", safeError(error, "No se pudo cargar recomendaciones."));
        }
    }

    function handleProjectionPeriodChange(event) {
        state.projection.horizonDays = Number(event.target.value || 7);
        updateProjectionControls();
        markProjectionPending("Cambios pendientes de calcular.");
    }

    function updateProjectionControls() {
        const horizonLabel = `${state.projection.horizonDays} días`;
        const submit = $(`[data-projection-form] button[type="submit"]`);
        if (submit) submit.textContent = `Calcular proyección (${horizonLabel})`;
    }

    function scheduleCockpitPreferencesSave() {
        if (!state.preferencesLoaded) return;
        window.clearTimeout(state.preferenceSaveTimer);
        const status = target("preferences-status");
        setState(status, "loading", "Guardando preferencias manuales.");
        state.preferenceSaveTimer = window.setTimeout(persistCockpitPreferences, 500);
    }

    async function persistCockpitPreferences() {
        const openingBalance = readOpeningBalance();
        const status = target("preferences-status");
        if (openingBalance === null) {
            setState(status, "error", "Ingresa un saldo inicial manual para guardar preferencias.");
            return;
        }
        try {
            const saved = await putJson("/api/cashflow/cockpit/preferences", {
                profileId: PROFILE_ID,
                openingBalance,
                preferredHorizonDays: state.projection.horizonDays,
            });
            prefillCockpitPreferences(saved);
            setState(status, "success", "Preferencias guardadas. Saldo manual, no bancario.");
        } catch (error) {
            setState(status, "error", safeError(error, "No se pudieron guardar las preferencias manuales."));
        }
    }

    function handleProjectionSubmit(event) {
        event.preventDefault();
        if (!state.demoResetComplete) {
            setProjectionBlockedState();
            return;
        }
        if (hasPendingManualReviews()) {
            setProjectionBlockedState();
            return;
        }
        const balance = readOpeningBalance();
        if (balance === null) {
            setState(target("projection-results"), "error", "Ingresa un saldo inicial manual para proyectar caja.");
            $("#opening-balance")?.focus();
            return;
        }
        fetchProjection(balance);
    }

    async function fetchProjection(openingBalance, { forceFresh = false } = {}) {
        const results = target("projection-results");
        if (!state.demoResetComplete) {
            setProjectionBlockedState();
            return;
        }
        if (hasPendingManualReviews()) {
            setProjectionBlockedState();
            return;
        }
        setState(results, "loading", "Calculando proyección con saldo manual.");
        try {
            const params = new URLSearchParams({
                profileId: PROFILE_ID,
                startDate: chooseProjectionStartDate(state.projection.projectableMovementDates, state.projection.horizonDays),
                horizonDays: String(state.projection.horizonDays),
                openingBalance: String(openingBalance),
            });
            const url = `${API.cockpitProjection}?${params.toString()}`;
            const projection = forceFresh ? await getJsonFresh(url) : await getJson(url);
            renderProjection(projection);
        } catch (error) {
            setState(results, "error", safeError(error, "No se pudo cargar la proyección de caja."));
        }
    }

    function renderProjection(projection) {
        const results = target("projection-results");
        state.projection.pendingCalculation = false;
        const dailyBalances = projection.dailyBalances || [];
        if (!dailyBalances.length) {
            const message = hasProjectableMovements()
                    ? "Hay movimientos listos, pero fuera del período seleccionado."
                    : "Categoriza movimientos para proyectar caja.";
            setState(results, "empty", message);
            return;
        }
        const totals = summarizeProjection(dailyBalances);
        results.classList.remove("empty-state");
        state.projection.dailyBalances = dailyBalances;
        state.projection.alerts = projection.alerts || [];
        state.projection.obligations = projection.appliedObligations || [];
        const horizonLabel = `${state.projection.horizonDays} días`;
        results.innerHTML = `<div class="projection-summary" role="region" aria-label="Resultado de proyección">
            <article class="projection-closing">
                <span>Cierre proyectado (estimación resultante)</span>
                <strong>${money.format(Number(projection.closingProjectedBalance || 0))}</strong>
                <p>Resultado estimado a partir del saldo inicial y los movimientos del período.</p>
            </article>
            <dl class="projection-totals">
                <div><dt>Entradas totales</dt><dd>${money.format(totals.inflows)}</dd></div>
                <div><dt>Salidas totales</dt><dd>${money.format(totals.outflows)}</dd></div>
                <div><dt>Obligaciones totales</dt><dd>${money.format(totals.obligations)}</dd></div>
            </dl>
            <div class="projection-actions" aria-label="Acciones de detalle de proyección">
                ${renderProjectionAlerts(projection.alerts || [])}
                <button type="button" class="button-secondary projection-calendar-trigger" data-projection-details-trigger="days" aria-haspopup="dialog">Ver detalle diario (${horizonLabel})</button>
            </div>
        </div>`;
    }

    function summarizeProjection(dailyBalances) {
        return dailyBalances.reduce((totals, day) => ({
            inflows: totals.inflows + Number(day.inflows || 0),
            outflows: totals.outflows + Number(day.outflows || 0),
            obligations: totals.obligations + Number(day.obligations || 0),
        }), { inflows: 0, outflows: 0, obligations: 0 });
    }

    function renderProjectionAlerts(alerts) {
        const groups = groupProjectionAlerts(alerts);
        return `<button type="button" class="button-secondary" data-projection-details-trigger="alerts" aria-haspopup="dialog">Ver alertas (${groups.length})</button>`;
    }

    function setupProjectionDetailsDialog() {
        const dialog = $("[data-projection-details-dialog]");
        if (!dialog) return;
        dialog.querySelector("[data-projection-details-close]")?.addEventListener("click", () => closeProjectionDetails());
        dialog.addEventListener("click", (event) => {
            if (event.target === dialog) closeProjectionDetails();
        });
    }

    function handleProjectionDetailsClick(event) {
        const trigger = event.target.closest("[data-projection-details-trigger]");
        if (trigger) openProjectionDetails(trigger.dataset.projectionDetailsTrigger, trigger);
    }

    function openProjectionDetails(kind, trigger) {
        const dialog = $("[data-projection-details-dialog]");
        const title = $("[data-projection-details-title]");
        const description = $("[data-projection-details-description]");
        const content = $("[data-projection-details-content]");
        if (!dialog || !title || !content) return;
        const items = kind === "receipt" ? null : kind === "ledger" ? state.ledgerMovements : kind === "alerts"
                ? groupProjectionAlerts(state.projection.alerts)
                : kind === "days" ? state.projection.dailyBalances : state.projection.obligations;
        title.textContent = kind === "receipt"
                ? "Detalles técnicos del comprobante"
                : kind === "ledger"
                ? `Movimientos de caja (${items.length})`
                : kind === "alerts"
                        ? "Alertas de proyección"
                        : kind === "days" ? `Calendario completo (${state.projection.horizonDays} días)` : "Obligaciones consideradas";
        if (description) {
            description.textContent = kind === "receipt"
                    ? "Información técnica de la carga para soporte y trazabilidad."
                    : "Detalle completo del módulo seleccionado.";
        }
        content.innerHTML = kind === "receipt"
                ? renderProviderSyncTechnicalDetails(state.syncReceiptStatus)
                : kind === "ledger"
                ? renderLedgerDetails(items)
                : kind === "alerts"
                        ? items.length
                                ? `<div class="projection-details-list" aria-label="Alertas agrupadas">${items.map(renderProjectionAlertDetail).join("")}</div>`
                                : `<p class="success-state" role="status">Sin alertas de caja para este período.</p>`
                        : kind === "days"
                                ? renderProjectionCalendar(items)
                                : `<ul class="projection-details-list" aria-label="Detalle de obligaciones">${items.map(renderProjectionObligationDetail).join("")}</ul>`;
        state.projectionDetailsLastFocus = trigger;
        dialog.hidden = false;
        syncDialogScrollLock();
        dialog.querySelector("[data-projection-details-close]")?.focus({ preventScroll: true });
    }

    function renderProjectionAlertDetail(group) {
        const copy = projectionAlertCopy(group);
        const affectedLabel = group.eventCount === 1 ? "día afectado" : "días afectados";
        return `<section class="projection-detail-item">
            <strong>${escapeHtml(copy.title)}</strong>
            <p>${escapeHtml(copy.meaning)}</p>
            <span>${escapeHtml(copy.dateText)} · ${group.eventCount} ${affectedLabel}</span>
        </section>`;
    }

    function renderProjectionObligationDetail(obligation) {
        return `<li class="projection-detail-item projection-detail-item--amount">
            <div><strong>${escapeHtml(obligation.displayName || "Obligación programada")}</strong><span>Fecha: ${escapeHtml(formatProjectionDate(obligation.date))}</span></div>
            <strong class="money">${money.format(Number(obligation.amount || 0))}</strong>
        </li>`;
    }

    function renderProjectionCalendar(days) {
        return `<div class="projection-calendar" role="region" aria-label="Calendario diario de proyección">
            <table>
                <caption>Detalle diario de la proyección</caption>
                <thead><tr><th scope="col">Fecha</th><th scope="col">Saldo al cierre</th><th scope="col">Entradas</th><th scope="col">Salidas</th></tr></thead>
                <tbody>${days.map((day) => {
                    const outflows = Number(day.outflows || 0) + Number(day.obligations || 0);
                    return `<tr>
                        <th scope="row" data-label="Fecha">${escapeHtml(formatProjectionShortDate(day.date))}</th>
                        <td data-label="Saldo al cierre" class="money">${money.format(Number(day.balance || 0))}</td>
                        <td data-label="Entradas" class="money">${money.format(Number(day.inflows || 0))}</td>
                        <td data-label="Salidas" class="money">${money.format(outflows)}</td>
                    </tr>`;
                }).join("")}</tbody>
            </table>
        </div>`;
    }

    function renderLedgerDetails(movements) {
        return `<div class="projection-ledger" role="region" aria-label="Detalle completo de movimientos de caja">
            <table class="ledger-table ledger-table--details" aria-label="Movimientos de caja completos">
                <caption>Detalle completo de movimientos de caja</caption>
                <thead>
                    <tr>
                        <th scope="col">Fecha</th>
                        <th scope="col">Movimiento/categoría</th>
                        <th scope="col">Dirección</th>
                        <th scope="col">Monto</th>
                    </tr>
                </thead>
                <tbody>${movements.map(renderMovement).join("")}</tbody>
            </table>
        </div>`;
    }

    function projectionAlertCopy(alert) {
        const key = alert?.condition || alert?.ruleKey;
        const threshold = alert?.threshold === null || alert?.threshold === undefined
                ? "el nivel configurado"
                : money.format(Number(alert.threshold));
        const lowest = money.format(Number(alert?.lowestBalance || 0));
        const highest = money.format(Number(alert?.highestBalance || 0));
        const dateText = formatProjectionDateRanges(alert?.dates || []);
        const copies = {
            projected_balance_above_threshold: {
                title: "Saldo saludable",
                meaning: `Superó ${threshold}. Fechas: ${dateText}. El saldo más alto fue ${highest}; significa que existe un colchón de liquidez por encima del nivel saludable configurado.`,
            },
            projected_balance_below_threshold: {
                title: "Saldo bajo el nivel de resguardo",
                meaning: `Quedó bajo ${threshold}. Fechas: ${dateText}. El saldo más bajo fue ${lowest}; revisa el momento de los cargos y de los ingresos para evitar un faltante.`,
            },
            obligations_due_before_cash_inflow: {
                title: "Obligaciones antes de un ingreso",
                meaning: `Las obligaciones superaron ${threshold}. Fechas: ${dateText}. El saldo más bajo asociado fue ${lowest}; revisa el momento de los cargos y de los ingresos para asegurar caja antes de los vencimientos.`,
            },
        };
        return { ...(copies[key] || {
            title: "Revisión de caja",
            meaning: `Se detectó una variación entre ${lowest} y ${highest}. Fechas: ${dateText}. Revisa el margen disponible antes de comprometer nuevos pagos.`,
        }), dateText };
    }

    function groupProjectionAlerts(alerts) {
        const groups = new Map();
        alerts.forEach((alert) => {
            const condition = alert?.condition || alert?.ruleKey || "unknown";
            const thresholdKey = alert?.threshold === null || alert?.threshold === undefined ? "none" : String(alert.threshold);
            const key = `${condition}|${thresholdKey}`;
            const group = groups.get(key) || {
                condition,
                threshold: alert?.threshold,
                dates: [],
                balances: [],
                eventCount: 0,
            };
            if (alert?.date) group.dates.push(alert.date);
            if (Number.isFinite(Number(alert?.balance))) group.balances.push(Number(alert.balance));
            group.eventCount += 1;
            groups.set(key, group);
        });
        return [...groups.values()].map((group) => ({
            ...group,
            dates: [...new Set(group.dates)].sort(),
            lowestBalance: group.balances.length ? Math.min(...group.balances) : 0,
            highestBalance: group.balances.length ? Math.max(...group.balances) : 0,
        }));
    }

    function formatProjectionDate(value) {
        if (!value) return "Fecha no disponible";
        const date = new Date(`${value}T00:00:00Z`);
        if (Number.isNaN(date.getTime())) return "Fecha no disponible";
        return new Intl.DateTimeFormat("es-CL", { day: "numeric", month: "long", year: "numeric", timeZone: "UTC" }).format(date);
    }

    function formatProjectionShortDate(value) {
        if (!value) return "Fecha no disponible";
        const date = new Date(`${value}T00:00:00Z`);
        if (Number.isNaN(date.getTime())) return "Fecha no disponible";
        return new Intl.DateTimeFormat("es-CL", { weekday: "short", day: "numeric", month: "short", timeZone: "UTC" }).format(date);
    }

    function formatProjectionDateRanges(dates) {
        const sorted = [...new Set((dates || []).filter(Boolean))].sort();
        if (!sorted.length) return "en las fechas disponibles";
        const ranges = [];
        let start = sorted[0];
        let end = sorted[0];
        sorted.slice(1).forEach((date) => {
            if (date === addDaysIso(end, 1)) end = date;
            else {
                ranges.push([start, end]);
                start = date;
                end = date;
            }
        });
        ranges.push([start, end]);
        return ranges.map(([from, to]) => {
            if (from === to) return formatProjectionDate(from);
            const startDate = new Date(`${from}T00:00:00Z`);
            const endDate = new Date(`${to}T00:00:00Z`);
            const startMonth = new Intl.DateTimeFormat("es-CL", { month: "long", timeZone: "UTC" }).format(startDate);
            const endMonth = new Intl.DateTimeFormat("es-CL", { month: "long", timeZone: "UTC" }).format(endDate);
            const startYear = startDate.getUTCFullYear();
            const endYear = endDate.getUTCFullYear();
            if (startMonth === endMonth && startYear === endYear) {
                return `del ${startDate.getUTCDate()} al ${endDate.getUTCDate()} de ${startMonth} de ${startYear}`;
            }
            return `del ${formatProjectionDate(from)} al ${formatProjectionDate(to)}`;
        }).join(", ");
    }

    function closeProjectionDetails() {
        const dialog = $("[data-projection-details-dialog]");
        if (!dialog || dialog.hidden) return false;
        dialog.hidden = true;
        syncDialogScrollLock();
        state.projectionDetailsLastFocus?.focus?.({ preventScroll: true });
        state.projectionDetailsLastFocus = null;
        return true;
    }

    async function runDemoReset() {
        const button = $("#demo-reset-btn");
        const errorMessage = target("demo-reset-error");
        setBusy(button, true);
        hideDemoResetError(errorMessage);
        try {
            const response = await postJson(API.demoReset);
            state.demoResetComplete = true;
            if (response.syncSessionId) {
                await renderSyncStatus(response.syncSessionId, { forceFresh: true });
            }
            await refreshCockpitEvidence({ forceFresh: true });
            updateProjectionGate();
            showDemoResetSuccess();
            updateDemoHighlight("review");
        } catch (error) {
            showDemoResetError(errorMessage, "No se pudo reiniciar la demo. Los datos visibles se mantienen; intenta Reiniciar demo nuevamente.");
        } finally {
            setBusy(button, false);
        }
    }

    async function renderSyncStatus(syncSessionId, { forceFresh = false } = {}) {
        const receipt = target("sync-receipt");
        try {
            const url = `${API.providerSyncs}/${syncSessionId}`;
            const status = forceFresh ? await getJsonFresh(url) : await getJson(url);
            state.syncReceiptStatus = status;
            receipt.innerHTML = renderProviderSyncReceipt(status);
        } catch (error) {
            setState(receipt, "error", safeError(error, "Demo reiniciada; no se pudo actualizar la evidencia de carga."));
        }
    }

    async function getJson(url) {
        const response = await fetch(url, { headers: { Accept: "application/json" } });
        return parseJsonResponse(response);
    }

    async function getJsonFresh(url) {
        const response = await fetch(url, { headers: { Accept: "application/json" }, cache: "no-store" });
        return parseJsonResponse(response);
    }

    async function postJson(url, body = null) {
        const options = {
            method: "POST",
            headers: { Accept: "application/json", "Content-Type": "application/json" },
        };
        if (body !== null) {
            options.body = JSON.stringify(body);
        }
        const response = await fetch(url, options);
        return parseJsonResponse(response);
    }

    async function putJson(url, body) {
        const response = await fetch(url, {
            method: "PUT",
            headers: { Accept: "application/json", "Content-Type": "application/json" },
            body: JSON.stringify(body),
        });
        return parseJsonResponse(response);
    }

    async function parseJsonResponse(response) {
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.message || "No se pudo cargar la respuesta solicitada.");
        }
        return data;
    }

    function updateCashTotals(movements) {
        const credits = movements.filter(isCredit).reduce(sumAmount, 0);
        const debits = movements.filter(isDebit).reduce(sumAmount, 0);
        text("[data-field='projected-balance']", money.format(credits - debits));
        text("[data-field='credit-total']", money.format(credits));
        text("[data-field='debit-total']", money.format(debits));
    }

    function renderLedger(movements) {
        const ledger = target("ledger-list");
        state.ledgerMovements = movements;
        if (!movements.length) {
            setState(ledger, "empty", "Sin datos para mostrar: importa una muestra o ejecuta sync fixture para poblar evidencia.");
            return;
        }
        const entries = movements.filter(isCredit);
        const exits = movements.filter(isDebit);
        ledger.removeAttribute("role");
        ledger.innerHTML = `<div class="ledger-summary" role="region" aria-label="Resumen de movimientos de caja">
            <dl class="ledger-summary__stats">
                <div><dt>Total movimientos</dt><dd>${movements.length}</dd></div>
                <div><dt>Entradas</dt><dd>${entries.length}</dd></div>
                <div><dt>Salidas</dt><dd>${exits.length}</dd></div>
                <div><dt>Total entradas</dt><dd class="money">${formatPositiveMoney(entries.reduce(sumAmount, 0))}</dd></div>
                <div><dt>Total salidas</dt><dd class="money">${formatPositiveMoney(exits.reduce(sumAmount, 0))}</dd></div>
            </dl>
            <div class="ledger-summary__action">
                <p class="help-text">Consulta fechas, categorías y montos en el detalle completo.</p>
                <button type="button" class="button-secondary" data-projection-details-trigger="ledger" aria-haspopup="dialog">Ver movimientos (${movements.length})</button>
            </div>
        </div>`;
    }

    function renderManualReview(manualReview) {
        const container = target("manual-review-list");
        if (!manualReview.length) {
            setReviewState(container, "complete");
            container.classList.add("review-complete-message");
            container.textContent = "Todos los movimientos están categorizados. La proyección está disponible.";
            updateDemoHighlight(state.demoResetComplete ? "review" : "reset");
            return;
        }
        setReviewState(container, "pending");
        container.classList.remove("review-complete-message");
        if (!state.categories.length) {
            setState(container, "error", "No se pudieron cargar categorías activas. Intenta nuevamente antes de categorizar.");
            return;
        }
        container.innerHTML = `<div class="manual-review-list" role="list">${manualReview.map(renderManualReviewMovement).join("")}</div>`;
        updateDemoHighlight(state.demoResetComplete ? "review" : "reset");
    }

    function setReviewState(container, reviewState) {
        container.closest(".review-grid")?.setAttribute("data-review-state", reviewState);
        container.closest(".review-panel")?.setAttribute("data-review-state", reviewState);
    }

    function renderMovement(movement) {
        const direction = movement.movementDirection === "DEBIT" ? "Salida" : "Entrada";
        const pill = movement.movementDirection === "DEBIT" ? "pill--debit" : "pill--credit";
        const categoryLabel = categoryLabelFor(movement);
        const movementLabel = movement.description || categoryLabel || "Movimiento de caja";
        const showCategory = categoryLabel && normalizeDisplayText(categoryLabel) !== normalizeDisplayText(movementLabel);
        return `<tr class="ledger-row">
            <td class="ledger-cell ledger-cell--date" data-label="Fecha">${escapeHtml(movement.date || "Sin fecha")}</td>
            <td class="ledger-cell ledger-cell--movement" data-label="Movimiento/categoría">
                <div class="ledger-movement-copy"><strong>${escapeHtml(movementLabel)}</strong>${showCategory ? `<span>${escapeHtml(categoryLabel)}</span>` : ""}</div>
            </td>
            <td class="ledger-cell ledger-cell--direction movement-meta" data-label="Dirección"><span class="pill ${pill}">${direction}</span></td>
            <td class="ledger-cell ledger-cell--amount" data-label="Monto"><span class="money">${formatPositiveMoney(movement.amount)} ${movement.currency || "CLP"}</span></td>
        </tr>`;
    }

    function categoryLabelFor(movement) {
        if (movement.categoryKey) {
            const category = state.categories.find((candidate) => candidate.key === movement.categoryKey);
            return category?.displayName || humanizeCategoryKey(movement.categoryKey);
        }
        return "";
    }

    function humanizeCategoryKey(categoryKey) {
        const words = String(categoryKey).replace(/[_-]+/g, " ").replace(/\s+/g, " ").trim().toLocaleLowerCase("es-CL");
        return words ? words.charAt(0).toLocaleUpperCase("es-CL") + words.slice(1) : "";
    }

    function normalizeDisplayText(value) {
        return String(value || "").trim().toLocaleLowerCase("es-CL");
    }

    function renderManualReviewMovement(movement) {
        const movementId = escapeHtml(movement.movementId);
        const direction = movement.movementDirection === "DEBIT" ? "Salida" : "Entrada";
        const pill = movement.movementDirection === "DEBIT" ? "pill--debit" : "pill--credit";
        const label = escapeHtml(movement.description || "Movimiento pendiente");
        const reference = escapeHtml(movement.sourceReference || "");
        const movementDirection = escapeHtml(movement.movementDirection || "");
        const disabled = state.resolvingMovementIds.has(movement.movementId) ? " disabled" : "";
        return `<article class="movement movement--review" role="listitem" data-review-card="${movementId}" data-review-reference="${reference}" data-review-direction="${movementDirection}" aria-busy="${state.resolvingMovementIds.has(movement.movementId)}">
            <div class="movement-main">
                <strong>${label}</strong>
                <span>${escapeHtml(movement.date || "Sin fecha")}</span>
            </div>
            <div class="movement-meta">
                <span class="pill ${pill}">${direction}</span>
                <span class="money">${formatPositiveMoney(movement.amount)} ${movement.currency || "CLP"}</span>
            </div>
            <input id="category-${movementId}" type="hidden" data-review-category="${movementId}" value="">
            <button type="button" class="category-select-trigger" data-category-modal-trigger="${movementId}" aria-haspopup="dialog" aria-expanded="false" aria-label="Categorizar ${label}"${disabled}>Categorizar</button>
            <p class="review-message" data-review-message="${movementId}" role="status"></p>
        </article>`;
    }

    async function handleManualReviewClick(event) {
        const trigger = event.target.closest("[data-category-modal-trigger]");
        if (trigger) {
            openCategoryDialog(trigger);
            return;
        }
    }

    function setupCategoryDialog() {
        const dialog = $("[data-category-dialog]");
        if (!dialog) return;
        dialog.querySelector("[data-category-dialog-close]")?.addEventListener("click", () => closeCategoryDialog());
        dialog.querySelector("[data-category-dialog-confirm]")?.addEventListener("click", submitCategoryDialog);
        dialog.addEventListener("click", (event) => {
            if (event.target === dialog) closeCategoryDialog();
        });
        dialog.addEventListener("change", (event) => {
            if (event.target.matches("[data-category-dialog-option]")) chooseCategoryFromDialog(event.target);
        });
    }

    function openCategoryDialog(trigger) {
        const movementId = trigger.dataset.categoryModalTrigger;
        const dialog = $("[data-category-dialog]");
        if (!movementId || !dialog) return;
        state.categoryDialogMovementId = movementId;
        resetCategoryDialogSelection(movementId);
        state.categoryDialogLastFocus = trigger;
        trigger.setAttribute("aria-expanded", "true");
        renderCategoryDialogOptions(movementId);
        dialog.hidden = false;
        syncDialogScrollLock();
        const initialFocus = dialog.querySelector("input[data-category-dialog-option]:checked") || dialog.querySelector("[data-category-dialog-close]");
        initialFocus?.focus({ preventScroll: true });
    }

    function renderCategoryDialogOptions(movementId) {
        const container = $("[data-category-dialog-options]");
        const card = $(`[data-review-card="${movementId}"]`);
        const movementDirection = card?.dataset.reviewDirection;
        const compatibleDirection = categoryDirectionForMovement(movementDirection);
        const group = compatibleDirection === "OUTFLOW"
                ? ["Salidas", compatibleDirection]
                : compatibleDirection === "INFLOW"
                        ? ["Entradas", compatibleDirection]
                        : null;
        const helper = $("[data-category-dialog-helper]");
        if (!container) return;
        const categories = group ? state.categories.filter((category) => category.direction === compatibleDirection) : [];
        if (helper) {
            helper.textContent = "La categoría clasifica el movimiento; no cambia si es entrada o salida.";
        }
        container.innerHTML = categories.length && group
                ? `<fieldset class="category-dialog__group"><legend>${group[0]}</legend>${categories.map((category) => {
                const key = escapeHtml(category.key);
                return `<label class="category-dialog__option"><input type="radio" name="category-dialog-choice" value="${key}" data-category-dialog-option="${key}"><span>${escapeHtml(category.displayName)}</span></label>`;
            }).join("")}</fieldset>`
                : `<div class="empty-state" role="status">No hay categorías compatibles con la dirección de este movimiento. Revisa las categorías del perfil e inténtalo nuevamente.</div>`;
        updateCategoryDialogConfirm();
    }

    function resetCategoryDialogSelection(movementId) {
        state.categoryDialogSelectedKey = null;
        document.querySelectorAll("[data-category-dialog-option]").forEach((option) => {
            option.checked = false;
        });
        const hiddenCategory = $(`[data-review-category="${movementId}"]`);
        if (hiddenCategory) hiddenCategory.value = "";
    }

    function chooseCategoryFromDialog(option) {
        state.categoryDialogSelectedKey = isCategoryDialogChoiceAvailable(option.value) ? option.value : null;
        updateCategoryDialogConfirm();
    }

    function updateCategoryDialogConfirm() {
        const confirm = $("[data-category-dialog-confirm]");
        if (confirm) confirm.disabled = !isCategoryDialogChoiceAvailable(state.categoryDialogSelectedKey);
    }

    function categoryDirectionForMovement(movementDirection) {
        return movementDirection === "DEBIT" ? "OUTFLOW" : movementDirection === "CREDIT" ? "INFLOW" : null;
    }

    function isCategoryDialogChoiceAvailable(categoryKey) {
        if (!categoryKey) return false;
        return [...document.querySelectorAll("[data-category-dialog-option]")]
                .some((option) => option.value === categoryKey && option.checked);
    }

    async function submitCategoryDialog() {
        const movementId = state.categoryDialogMovementId;
        const categoryKey = state.categoryDialogSelectedKey;
        const card = $(`[data-review-card="${movementId}"]`);
        const message = $("[data-category-dialog-status]");
        if (!movementId || !isCategoryDialogChoiceAvailable(categoryKey) || !card) return;
        const confirm = $("[data-category-dialog-confirm]");
        setBusy(confirm, true);
        setInlineMessage(message, "loading", "Aplicando categoría…");
        const resolved = await resolveManualReviewMovement(movementId, categoryKey, card, message);
        if (resolved) closeCategoryDialog({ restoreFocus: false });
        setBusy(confirm, false);
    }

    function closeCategoryDialog({ restoreFocus = true } = {}) {
        const dialog = $("[data-category-dialog]");
        if (!dialog || dialog.hidden) return false;
        dialog.hidden = true;
        state.categoryDialogMovementId = null;
        state.categoryDialogSelectedKey = null;
        state.categoryDialogLastFocus?.setAttribute?.("aria-expanded", "false");
        if (restoreFocus) state.categoryDialogLastFocus?.focus?.({ preventScroll: true });
        else target("manual-review-list")?.focus?.({ preventScroll: true });
        state.categoryDialogLastFocus = null;
        syncDialogScrollLock();
        return true;
    }

    function syncDialogScrollLock() {
        const locked = Boolean($("[data-category-dialog]:not([hidden]), [data-projection-details-dialog]:not([hidden]), [data-onboarding-guide]:not([hidden])"));
        document.documentElement.classList.toggle("dialog-open", locked);
        document.body?.classList.toggle("dialog-open", locked);
    }

    function handleGlobalKeydown(event) {
        const activeDialog = $("[data-category-dialog]:not([hidden]), [data-projection-details-dialog]:not([hidden]), [data-onboarding-guide]:not([hidden])");
        if (event.key === "Tab" && activeDialog) {
            trapDialogFocus(event, activeDialog);
            return;
        }
        if (event.key !== "Escape") return;
        if (closeCategoryDialog()) return;
        if (closeProjectionDetails()) return;
        const guide = $("[data-onboarding-guide]");
        if (guide && !guide.hidden) closeOnboardingGuideForSession(guide);
    }

    function trapDialogFocus(event, dialog) {
        const controls = [...dialog.querySelectorAll("button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex='-1'])")]
                .filter((control) => !control.hidden && control.getClientRects().length);
        if (!controls.length) {
            event.preventDefault();
            return;
        }
        const first = controls[0];
        const last = controls[controls.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }

    async function resolveManualReviewMovement(movementId, chosenCategoryKey, card, message) {
        setReviewCardBusy(card, true);
        state.resolvingMovementIds.add(movementId);
        setInlineMessage(message, "loading", "Categorizando movimiento.");
        try {
            const movement = collectMovementContext(card);
            await postJson(`${API.manualReviewResolution}${movementId}`, {
                profileId: PROFILE_ID,
                chosenCategoryKey,
                description: movement.description,
                sourceReference: movement.sourceReference,
            });
            await refreshCockpitEvidence({ forceFresh: true });
            const remaining = state.projection.pendingManualReviewCount;
            const status = target("review-status");
            setState(status, "success", remaining
                    ? `Movimiento categorizado. Quedan ${remaining} pendientes antes de proyectar.`
                    : "Movimiento categorizado. La proyección ya está disponible.");
            return true;
        } catch (error) {
            setInlineMessage(message, "error", safeError(error, "No se pudo categorizar el movimiento. Intenta nuevamente."));
        } finally {
            state.resolvingMovementIds.delete(movementId);
            setReviewCardBusy(card, false);
        }
        return false;
    }

    function collectMovementContext(card) {
        const title = card?.querySelector(".movement-main strong")?.textContent || null;
        const reference = card?.dataset.reviewReference || null;
        return { description: title, sourceReference: reference };
    }

    async function refreshCockpitEvidence({ forceFresh = false } = {}) {
        await Promise.allSettled([loadCockpitPreferences({ forceFresh }), renderProfileAndCategories({ forceFresh })]);
        const movementEvidence = renderMovementEvidence({ forceFresh });
        const recommendations = renderRecommendations({ forceFresh });
        await movementEvidence;
        const refreshes = [recommendations];
        await Promise.allSettled(refreshes);
        if (hasPendingManualReviews() || !state.demoResetComplete) setProjectionBlockedState();
        else markProjectionPending("Evidencia actualizada. Calcula la proyección para ver el resultado.");
    }

    function updateDemoHighlight(nextStep = state.demoHighlightStep) {
        state.demoHighlightStep = nextStep;
        document.querySelectorAll("[data-demo-highlight-step]").forEach((control) => {
            if (control.dataset.demoHighlightStep === nextStep) {
                control.dataset.demoHighlight = "current";
            } else {
                delete control.dataset.demoHighlight;
            }
        });
    }

    function updateProjectionGate() {
        const projectionTab = $('[role="tab"][aria-controls="proyeccion"]');
        const blocked = !state.demoResetComplete || hasPendingManualReviews();
        if (projectionTab) {
            projectionTab.disabled = blocked;
            projectionTab.setAttribute("aria-disabled", String(blocked));
            if (blocked) projectionTab.tabIndex = -1;
        }
        if (blocked) setProjectionBlockedState();
    }

    function setProjectionBlockedState() {
        const count = state.projection.pendingManualReviewCount;
        const message = !state.demoResetComplete
                ? "Reinicia la demo para habilitar la proyección."
                : `Categoriza los ${count} movimientos pendientes para habilitar la proyección.`;
        markProjectionPending(message);
    }

    function markProjectionPending(message = "Cambios pendientes de calcular.") {
        state.projection.pendingCalculation = true;
        state.projection.dailyBalances = [];
        state.projection.alerts = [];
        state.projection.obligations = [];
        setState(target("projection-results"), "empty", message);
    }

    function renderRecommendation(signal) {
        const severity = signal.severity || "INFO";
        const severityLabel = { WARNING: "Atención", ERROR: "Importante" }[severity] || severity;
        const severityPill = severity === "INFO" ? "" : `<span class="pill recommendation-severity">${escapeHtml(severityLabel)}</span>`;
        return `<article class="recommendation">
            <strong>${escapeHtml(signal.title || signal.type)}</strong>
            <p>${escapeHtml(signal.description || "Recomendación disponible para revisión de caja.")}</p>
            ${severityPill}
        </article>`;
    }

    function safeProviderErrors(errors) {
        if (!errors?.length) return `<p class="success-state">Sin incidencias</p>`;
        return `<p class="error-state"><strong>Incidencias:</strong> ${errors.map((error) => escapeHtml(error.message || error.code)).join(" · ")}</p>`;
    }

    function renderProviderSyncReceipt(status) {
        const received = status.entriesFetched ?? 0;
        const imported = status.importedEntries ?? 0;
        return receiptHeader(status.status === "COMPLETED" ? "Carga de demostración completada" : "Resultado de carga de demostración") + definitionList([
            ["Estado", humanSyncStatus(status.status)],
            ["Origen", humanSyncOrigin(status.providerType)],
            ["Movimientos recibidos/importados", `${received} recibidos · ${imported} importados`],
            ["Persistencia", humanSyncPersistence(status.durability)],
        ]) + safeProviderErrors(status.errors) + `<button type="button" class="button-secondary receipt-details-trigger" data-projection-details-trigger="receipt" aria-haspopup="dialog">Ver detalles técnicos</button>`;
    }

    function renderProviderSyncTechnicalDetails(status) {
        return definitionList([
            ["Estado técnico", status?.status ?? "No disponible"],
            ["Proveedor", status?.providerType ?? "No disponible"],
            ["sync ID", status?.syncId ?? "No disponible"],
            ["Durabilidad", status?.durability ?? "No disponible"],
        ]);
    }

    function humanSyncStatus(status) {
        return {
            COMPLETED: "Carga completada",
            PARTIAL: "Carga parcial",
            FAILED: "Carga no completada",
        }[status] || "Estado no disponible";
    }

    function humanSyncOrigin(providerType) {
        return providerType === "fixture-demo" ? "Datos de demostración" : "Origen no disponible";
    }

    function humanSyncPersistence(durability) {
        return durability === "DURABLE" ? "Guardada correctamente" : "Persistencia no confirmada";
    }

    function receiptHeader(title) {
        return `<h3 id="sync-title">${escapeHtml(title)}</h3>`;
    }

    function definitionList(entries) {
        return `<dl>${entries.map(([key, value]) => `<div><dt>${key}</dt><dd>${escapeHtml(String(value))}</dd></div>`).join("")}</dl>`;
    }

    function setState(element, type, message) {
        if (!element) return;
        const className = type === "error" ? "error-state" : type === "success" ? "success-state" : "empty-state";
        element.innerHTML = `<div class="${className}" role="status">${escapeHtml(message)}</div>`;
    }

    function showDemoResetSuccess() {
        const overlay = $("[data-demo-reset-success]");
        if (!overlay) return;
        window.clearTimeout(state.demoResetSuccessTimer);
        overlay.hidden = false;
        state.demoResetSuccessTimer = window.setTimeout(() => {
            overlay.hidden = true;
            state.demoResetSuccessTimer = null;
        }, feedbackDurationMs());
    }

    function feedbackDurationMs() {
        const duration = getComputedStyle(document.documentElement).getPropertyValue(DEMO_RESET_SUCCESS_DURATION).trim();
        const amount = Number.parseFloat(duration);
        if (!Number.isFinite(amount)) return 2400;
        return duration.endsWith("ms") ? amount : amount * 1000;
    }

    function showDemoResetError(element, message) {
        if (!element) return;
        element.textContent = message;
        element.hidden = false;
    }

    function hideDemoResetError(element) {
        if (!element) return;
        element.hidden = true;
        element.textContent = "";
    }

    function setBusy(button, busy) {
        if (!button) return;
        button.disabled = busy;
        button.setAttribute("aria-busy", String(busy));
    }

    function setReviewCardBusy(card, busy) {
        if (!card) return;
        card.setAttribute("aria-busy", String(busy));
        card.querySelectorAll("select, button").forEach((control) => {
            control.disabled = busy;
        });
    }

    function setInlineMessage(element, type, message) {
        if (!element) return;
        element.className = `review-message ${type === "error" ? "error-state" : type === "success" ? "success-state" : ""}`.trim();
        element.textContent = message;
    }

    function formatPositiveMoney(amount) {
        return money.format(Math.abs(Number(amount || 0)));
    }

    function readOpeningBalance() {
        const value = $("#opening-balance")?.value;
        if (!value) return null;
        const amount = Number(value);
        return Number.isFinite(amount) && amount >= 0 ? amount : null;
    }

    function todayIsoDate() {
        return new Date().toISOString().slice(0, 10);
    }

    function chooseProjectionStartDate(projectableMovementDates, horizonDays) {
        const today = todayIsoDate();
        const validDates = projectableMovementDates
                .filter((date) => /^\d{4}-\d{2}-\d{2}$/.test(date))
                .sort();
        if (!validDates.length) return today;
        const horizonEnd = addDaysIso(today, Number(horizonDays || 7) - 1);
        const hasDateInSelectedPeriod = validDates.some((date) => date >= today && date <= horizonEnd);
        return hasDateInSelectedPeriod ? today : validDates[0];
    }

    function addDaysIso(isoDate, days) {
        const date = new Date(`${isoDate}T00:00:00.000Z`);
        date.setUTCDate(date.getUTCDate() + days);
        return date.toISOString().slice(0, 10);
    }

    function hasProjectableMovements() {
        return state.projection.projectableMovementDates.length > 0;
    }

    function hasPendingManualReviews() {
        return state.projection.pendingManualReviewCount > 0;
    }

    function text(selector, value) {
        const element = $(selector);
        if (element) element.textContent = value;
    }

    function safeError(error, fallback) {
        return error?.message ? `${fallback} ${error.message}` : fallback;
    }

    function isCredit(movement) {
        return movement.movementDirection === "CREDIT";
    }

    function isDebit(movement) {
        return movement.movementDirection === "DEBIT";
    }

    function sumAmount(total, movement) {
        return total + Number(movement.amount || 0);
    }

    function escapeHtml(value) {
        return String(value)
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll('"', "&quot;")
                .replaceAll("'", "&#39;");
    }
})();
