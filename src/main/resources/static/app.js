(() => {
    const PROFILE_ID = "pharmacy-cl";
    const API = {
        activeProfile: "/api/profiles/active",
        activeCategories: "/api/profiles/active/categories",
        providerSyncs: "/api/cashflow/provider-syncs",
        manualImport: "/api/cashflow/imports/manual",
        manualReview: `/api/cashflow/history/manual-review?profileId=${PROFILE_ID}`,
        projectionReady: `/api/cashflow/history/projection-ready?profileId=${PROFILE_ID}`,
        recommendations: `/api/cashflow/recommendations?profileId=${PROFILE_ID}`,
        manualReviewResolution: "/api/cashflow/manual-review/resolutions/",
    };

    const state = {
        categories: [],
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
        target("manual-review-list")?.addEventListener("click", handleManualReviewClick);
    });

    async function loadInitialData() {
        await renderProfileAndCategories();
        await Promise.allSettled([
            renderMovementEvidence(),
            renderRecommendations(),
        ]);
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
            receipt.innerHTML = receiptHeader("Demo", "Sync proveedor simulado") + definitionList([
                ["Estado", status.status ?? "Sin estado"],
                ["Proveedor", status.providerType ?? "fixture"],
                ["Entradas", `${status.entriesFetched ?? 0}`],
                ["Importados", `${status.importedEntries ?? 0}`],
                ["Durabilidad", status.durability ?? "DURABLE"],
            ]) + safeProviderErrors(status.errors);
            await refreshCockpitEvidence();
        } catch (error) {
            setState(receipt, "error", safeError(error, "No se pudo consultar el estado de sync."));
        } finally {
            setBusy(button, false);
        }
    }

    async function getJson(url) {
        const response = await fetch(url, { headers: { Accept: "application/json" } });
        return parseJsonResponse(response);
    }

    async function postJson(url, body) {
        const response = await fetch(url, {
            method: "POST",
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
        await Promise.allSettled([renderMovementEvidence(), renderRecommendations()]);
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
