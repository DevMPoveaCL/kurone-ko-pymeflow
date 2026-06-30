(() => {
    const PROFILE_ID = "pharmacy-cl";
    const API = {
        activeProfile: "/api/profiles/active",
        activeCategories: "/api/profiles/active/categories",
        providerSyncs: "/api/cashflow/provider-syncs",
        manualImport: "/api/cashflow/imports/manual",
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
        },
        preferencesLoaded: false,
        preferenceSaveTimer: null,
        resolvingMovementIds: new Set(),
    };

    const SAMPLE_ROWS = [
        { rowNumber: 1, description: "Venta POS farmacia", amount: 125000, currency: "CLP", date: "2026-06-15", externalReference: "cockpit-pos-001", movementDirection: "CREDIT" },
        { rowNumber: 2, description: "Pago proveedor distribución", amount: 88000, currency: "CLP", date: "2026-06-16", externalReference: "cockpit-prov-001", movementDirection: "DEBIT" },
        { rowNumber: 3, description: "Abono transferencia cliente", amount: 240000, currency: "CLP", date: "2026-06-17", externalReference: "cockpit-trans-001", movementDirection: "CREDIT" },
        { rowNumber: 4, description: "", amount: 0, currency: "CLP", date: "2026-06-18", externalReference: "cockpit-invalid-001", movementDirection: "DEBIT" },
    ];

    const $ = (selector) => document.querySelector(selector);
    const target = (name) => $(`[data-api-target="${name}"]`);
    const money = new Intl.NumberFormat("es-CL", { style: "currency", currency: "CLP", maximumFractionDigits: 0 });

    document.addEventListener("DOMContentLoaded", () => {
        loadInitialData();
        $(`[data-action="manual-import"]`)?.addEventListener("click", runManualImport);
        $(`[data-action="provider-sync"]`)?.addEventListener("click", runProviderSync);
        $("#demo-reset-btn")?.addEventListener("click", runDemoReset);
        target("manual-review-list")?.addEventListener("click", handleManualReviewClick);
        $(`[data-projection-form]`)?.addEventListener("submit", handleProjectionSubmit);
        $("#opening-balance")?.addEventListener("input", handleOpeningBalanceChange);
        document.querySelectorAll(`[name="horizonDays"]`).forEach((control) => {
            control.addEventListener("change", handleProjectionPeriodChange);
        });
    });

    async function loadInitialData() {
        await loadCockpitPreferences();
        await renderProfileAndCategories();
        await Promise.allSettled([
            renderMovementEvidence(),
            renderRecommendations(),
        ]);
    }

    async function loadCockpitPreferences() {
        const status = target("preferences-status");
        setState(status, "loading", "Cargando preferencias manuales del cockpit.");
        try {
            const preferences = await getJson(API.cockpitPreferences);
            prefillCockpitPreferences(preferences);
            state.preferencesLoaded = true;
            setState(status, "success", "Preferencias manuales cargadas. El saldo no es bancario en vivo.");
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
    }

    function handleOpeningBalanceChange() {
        state.projection.openingBalance = readOpeningBalance();
        scheduleCockpitPreferencesSave();
    }

    async function renderProfileAndCategories() {
        try {
            const [profile, categories] = await Promise.all([getJson(API.activeProfile), getJson(API.activeCategories)]);
            state.categories = categories;
            text("[data-field='profile-label']", `${profile.displayName ?? profile.id}: categorías activas ${categories.length}.`);
        } catch (error) {
            state.categories = [];
            text("[data-field='profile-label']", safeError(error, "No se pudo cargar el perfil activo."));
            setState(target("manual-review-list"), "error", "No se pudieron cargar categorías activas. Intenta nuevamente antes de categorizar.");
        }
    }

    async function renderMovementEvidence() {
        const ledger = target("ledger-list");
        setState(ledger, "loading", "Cargando movimientos de caja.");
        try {
            const [projectionReady, manualReview] = await Promise.all([
                getJson(API.projectionReady),
                getJson(API.manualReview),
            ]);
            const movements = [...projectionReady, ...manualReview];
            updateCashTotals(movements);
            renderLedger(movements);
            renderManualReview(manualReview);
        } catch (error) {
            updateCashTotals([]);
            setState(ledger, "error", safeError(error, "No se pudo cargar el historial de caja."));
            setState(target("manual-review-list"), "error", "No se pudo cargar revisión manual. El resto del cockpit sigue disponible.");
        }
    }

    async function renderRecommendations() {
        const container = target("recommendation-list");
        setState(container, "loading", "Cargando recomendaciones.");
        try {
            const response = await getJson(API.recommendations);
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
        scheduleCockpitPreferencesSave();
        const balance = readOpeningBalance();
        if (balance !== null) {
            fetchProjection(balance);
        }
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
            setState(status, "success", "Preferencias guardadas. Saldo manual, no bancario en vivo.");
        } catch (error) {
            setState(status, "error", safeError(error, "No se pudieron guardar las preferencias manuales."));
        }
    }

    function handleProjectionSubmit(event) {
        event.preventDefault();
        const balance = readOpeningBalance();
        if (balance === null) {
            setState(target("projection-results"), "error", "Ingresa un saldo inicial manual para proyectar caja.");
            $("#opening-balance")?.focus();
            return;
        }
        fetchProjection(balance);
    }

    async function fetchProjection(openingBalance) {
        const results = target("projection-results");
        setState(results, "loading", "Calculando proyección de caja con saldo inicial manual.");
        try {
            const params = new URLSearchParams({
                profileId: PROFILE_ID,
                startDate: todayIsoDate(),
                horizonDays: String(state.projection.horizonDays),
                openingBalance: String(openingBalance),
            });
            const projection = await getJson(`${API.cockpitProjection}?${params.toString()}`);
            renderProjection(projection);
        } catch (error) {
            setState(results, "error", safeError(error, "No se pudo cargar la proyección de caja."));
        }
    }

    function renderProjection(projection) {
        const results = target("projection-results");
        const dailyBalances = projection.dailyBalances || [];
        if (!dailyBalances.length) {
            setState(results, "empty", "Categoriza movimientos primero para proyectar caja.");
            return;
        }
        const totals = summarizeProjection(dailyBalances);
        results.innerHTML = `<div class="projection-summary" role="region" aria-label="Resultado de proyección">
            <article class="projection-closing">
                <span>Cierre proyectado</span>
                <strong>${money.format(Number(projection.closingProjectedBalance || 0))}</strong>
                <p>Calculado con saldo inicial manual ingresado por el usuario, no bancario en vivo.</p>
            </article>
            <dl class="projection-totals">
                <div><dt>abonos</dt><dd>${money.format(totals.inflows)}</dd></div>
                <div><dt>cargos</dt><dd>${money.format(totals.outflows)}</dd></div>
                <div><dt>obligaciones</dt><dd>${money.format(totals.obligations)}</dd></div>
            </dl>
            ${renderProjectionAlerts(projection.alerts || [])}
            ${renderAppliedObligations(projection.appliedObligations || [])}
            <div class="projection-days" role="list" aria-label="Saldos diarios proyectados">
                ${dailyBalances.map(renderDailyBalance).join("")}
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

    function renderDailyBalance(day) {
        return `<article class="projection-day" role="listitem">
            <div><strong>${escapeHtml(day.date || "Sin fecha")}</strong><span>Saldo diario proyectado</span></div>
            <span class="money">${money.format(Number(day.balance || 0))}</span>
            <span class="pill pill--credit">abonos ${money.format(Number(day.inflows || 0))}</span>
            <span class="pill pill--debit">cargos ${money.format(Number(day.outflows || 0) + Number(day.obligations || 0))}</span>
        </article>`;
    }

    function renderProjectionAlerts(alerts) {
        if (!alerts.length) return `<p class="success-state">Sin alertas de caja para este período.</p>`;
        return `<div class="projection-alerts" aria-label="Alertas de proyección">
            ${alerts.map((alert) => `<span class="alert-chip">${escapeHtml(alert.condition || alert.ruleKey || "Alerta de caja")} · ${escapeHtml(alert.date || "sin fecha")}</span>`).join("")}
        </div>`;
    }

    function renderAppliedObligations(obligations) {
        if (!obligations.length) return `<p class="empty-state">Sin obligaciones aplicadas en el período.</p>`;
        return `<div class="obligation-list" aria-label="Obligaciones aplicadas">
            ${obligations.map((obligation) => `<span class="obligation-chip">${escapeHtml(obligation.displayName || obligation.obligationKey || "Obligación")} · ${money.format(Number(obligation.amount || 0))}</span>`).join("")}
        </div>`;
    }

    async function runManualImport() {
        const button = $(`[data-action="manual-import"]`);
        const receipt = target("import-receipt");
        setBusy(button, true);
        setState(receipt, "loading", "Importando muestra manual fixture/demo.");
        try {
            const response = await postJson(API.manualImport, {
                profileId: PROFILE_ID,
                importLabel: "cockpit-fixture-demo",
                rows: SAMPLE_ROWS,
            });
            receipt.innerHTML = receiptHeader("Revisión", "Importación manual") + definitionList([
                ["Aceptados", `${response.accepted} filas`],
                ["Revisión manual", `${response.manualReviewCount} filas`],
                ["Rechazados", `${response.rejectedCount} filas`],
                ["Inválidos", `${response.invalid} filas`],
            ]) + rowEvidence(response);
            await refreshCockpitEvidence();
        } catch (error) {
            setState(receipt, "error", safeError(error, "No se pudo completar la importación manual."));
        } finally {
            setBusy(button, false);
        }
    }

    async function runProviderSync() {
        const button = $(`[data-action="provider-sync"]`);
        const receipt = target("sync-receipt");
        setBusy(button, true);
        setState(receipt, "loading", "Ejecutando sync fixture/demo sin conectividad bancaria real.");
        try {
            const secretFreeRequest = {
                profileId: PROFILE_ID,
                providerType: "santander",
                dateFrom: "2026-06-01",
                dateTo: "2026-06-30",
            };
            secretFreeRequest["credential" + "Ref"] = "fixture-ref-santander";
            const response = await postJson(API.providerSyncs, secretFreeRequest);
            const status = response.syncId ? await getJson(`${API.providerSyncs}/${response.syncId}`) : response;
            receipt.innerHTML = renderProviderSyncReceipt(status, "Sync proveedor simulado");
            await refreshCockpitEvidence();
        } catch (error) {
            setState(receipt, "error", safeError(error, "No se pudo consultar el estado de sync."));
        } finally {
            setBusy(button, false);
        }
    }

    async function runDemoReset() {
        const button = $("#demo-reset-btn");
        const status = target("demo-reset-status");
        setBusy(button, true);
        setState(status, "loading", "Reiniciando datos fixture/demo. No se consulta conectividad bancaria real.");
        try {
            const response = await postJson(API.demoReset);
            if (response.syncSessionId) {
                await renderSyncStatus(response.syncSessionId);
            }
            await refreshCockpitEvidence();
            setState(status, "success", "Demo reiniciada. Evidencia visible actualizada con datos fixture/demo.");
        } catch (error) {
            setState(status, "error", "No se pudo reiniciar la demo. Los datos visibles se mantienen.");
        } finally {
            setBusy(button, false);
        }
    }

    async function renderSyncStatus(syncSessionId) {
        const receipt = target("sync-receipt");
        try {
            const status = await getJson(`${API.providerSyncs}/${syncSessionId}`);
            receipt.innerHTML = renderProviderSyncReceipt(status, "Sync fixture sembrada");
        } catch (error) {
            setState(receipt, "error", safeError(error, "Demo reiniciada; no se pudo actualizar el comprobante de sync."));
        }
    }

    async function getJson(url) {
        const response = await fetch(url, { headers: { Accept: "application/json" } });
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
        if (!movements.length) {
            setState(ledger, "empty", "Sin datos para mostrar: importa una muestra o ejecuta sync fixture para poblar evidencia.");
            return;
        }
        ledger.innerHTML = movements.map(renderMovement).join("");
    }

    function renderManualReview(manualReview) {
        const container = target("manual-review-list");
        if (!manualReview.length) {
            setState(container, "empty", "Sin movimientos pendientes de revisión.");
            return;
        }
        if (!state.categories.length) {
            setState(container, "error", "No se pudieron cargar categorías activas. Intenta nuevamente antes de categorizar.");
            return;
        }
        container.innerHTML = `<div class="manual-review-list" role="list">${manualReview.map(renderManualReviewMovement).join("")}</div>`;
    }

    function renderMovement(movement) {
        const direction = movement.movementDirection === "DEBIT" ? "DEBIT · cargo" : "CREDIT · abono";
        const pill = movement.movementDirection === "DEBIT" ? "pill--debit" : "pill--credit";
        const label = escapeHtml(movement.description || movement.categoryKey || movement.status || "Movimiento de caja");
        const meta = [movement.date, movement.categoryKey, movement.status].filter(Boolean).join(" · ");
        return `<article class="movement" role="listitem">
            <div><strong>${label}</strong><span>${escapeHtml(meta)}</span></div>
            <span class="pill ${pill}">${direction}</span>
            <span class="money">${formatPositiveMoney(movement.amount)} ${movement.currency || "CLP"}</span>
        </article>`;
    }

    function renderManualReviewMovement(movement) {
        const movementId = escapeHtml(movement.movementId);
        const direction = movement.movementDirection === "DEBIT" ? "DEBIT · movimiento bancario" : "CREDIT · movimiento bancario";
        const pill = movement.movementDirection === "DEBIT" ? "pill--debit" : "pill--credit";
        const label = escapeHtml(movement.description || "Movimiento pendiente");
        const reference = movement.sourceReference ? ` · Ref. ${escapeHtml(movement.sourceReference)}` : "";
        const disabled = state.resolvingMovementIds.has(movement.movementId) ? " disabled" : "";
        return `<article class="movement movement--review" role="listitem" data-review-card="${movementId}" aria-busy="${state.resolvingMovementIds.has(movement.movementId)}">
            <div class="movement-main">
                <strong>${label}</strong>
                <span>${escapeHtml(movement.date || "Sin fecha")} · ${escapeHtml(movement.status || "MANUAL_REVIEW")}${reference}</span>
                <span class="direction-note">Dirección bancaria: ${escapeHtml(movement.movementDirection || "Sin dirección")}. La categoría solo clasifica el flujo.</span>
            </div>
            <span class="pill ${pill}">${direction}</span>
            <span class="money">${formatPositiveMoney(movement.amount)} ${movement.currency || "CLP"}</span>
            <label class="review-select-label" for="category-${movementId}">Selecciona una categoría</label>
            <select id="category-${movementId}" data-review-category="${movementId}"${disabled}>
                <option value="">Seleccione una categoría</option>
                ${state.categories.map(renderCategoryOption).join("")}
            </select>
            <button type="button" data-review-resolve="${movementId}"${disabled}>Categorizar movimiento</button>
            <p class="review-message" data-review-message="${movementId}" role="status"></p>
        </article>`;
    }

    function renderCategoryOption(category) {
        return `<option value="${escapeHtml(category.key)}">${escapeHtml(category.displayName)} · ${categoryDirectionCopy(category.direction)}</option>`;
    }

    function categoryDirectionCopy(direction) {
        if (direction === "INFLOW") return "clasificación INFLOW";
        if (direction === "OUTFLOW") return "clasificación OUTFLOW";
        return `clasificación ${direction || "sin dirección"}`;
    }

    async function handleManualReviewClick(event) {
        const button = event.target.closest("[data-review-resolve]");
        if (!button) return;
        const movementId = button.dataset.reviewResolve;
        const card = button.closest("[data-review-card]");
        const select = card?.querySelector("[data-review-category]");
        const message = card?.querySelector("[data-review-message]");
        if (!select?.value) {
            setInlineMessage(message, "error", "Seleccione una categoría antes de categorizar.");
            return;
        }
        await resolveManualReviewMovement(movementId, select.value, card, message);
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
            setInlineMessage(message, "success", "Movimiento categorizado correctamente.");
            await refreshCockpitEvidence();
        } catch (error) {
            setInlineMessage(message, "error", safeError(error, "No se pudo categorizar el movimiento. Intenta nuevamente."));
        } finally {
            state.resolvingMovementIds.delete(movementId);
            setReviewCardBusy(card, false);
        }
    }

    function collectMovementContext(card) {
        const title = card?.querySelector(".movement-main strong")?.textContent || null;
        const meta = card?.querySelector(".movement-main span")?.textContent || "";
        const reference = meta.match(/Ref\. ([^·]+)/)?.[1]?.trim() || null;
        return { description: title, sourceReference: reference };
    }

    async function refreshCockpitEvidence() {
        await Promise.allSettled([loadCockpitPreferences(), renderProfileAndCategories()]);
        const balance = readOpeningBalance();
        const refreshes = [renderMovementEvidence(), renderRecommendations()];
        if (balance !== null) refreshes.push(fetchProjection(balance));
        await Promise.allSettled(refreshes);
    }

    function renderRecommendation(signal) {
        return `<article class="recommendation">
            <strong>${escapeHtml(signal.title || signal.type)}</strong>
            <p>${escapeHtml(signal.description || "Recomendación disponible para revisión de caja.")}</p>
            <span class="pill">${escapeHtml(signal.severity || "INFO")}</span>
        </article>`;
    }

    function rowEvidence(response) {
        const rows = [...(response.categorized || []), ...(response.manualReview || [])]
                .map((row) => row.transaction)
                .filter(Boolean);
        if (!rows.length) return "";
        return `<div class="ledger-list" role="list">${rows.map((row) => renderMovement(row)).join("")}</div>`;
    }

    function safeProviderErrors(errors) {
        if (!errors?.length) return `<p class="success-state">Sin errores seguros reportados.</p>`;
        return `<p class="error-state">${errors.map((error) => escapeHtml(error.message || error.code)).join(" · ")}</p>`;
    }

    function renderProviderSyncReceipt(status, title) {
        return receiptHeader("Demo", title) + definitionList([
            ["Estado", status.status ?? "Sin estado"],
            ["Proveedor", status.providerType ?? "fixture"],
            ["Entradas", `${status.entriesFetched ?? 0}`],
            ["Importados", `${status.importedEntries ?? 0}`],
            ["Durabilidad", status.durability ?? "DURABLE"],
        ]) + safeProviderErrors(status.errors);
    }

    function receiptHeader(stamp, title) {
        return `<span class="stamp">${stamp}</span><h3>${title}</h3>`;
    }

    function definitionList(entries) {
        return `<dl>${entries.map(([key, value]) => `<div><dt>${key}</dt><dd>${escapeHtml(String(value))}</dd></div>`).join("")}</dl>`;
    }

    function setState(element, type, message) {
        if (!element) return;
        const className = type === "error" ? "error-state" : type === "success" ? "success-state" : "empty-state";
        element.innerHTML = `<div class="${className}" role="status">${escapeHtml(message)}</div>`;
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
