# Design: Cockpit Movement Review MVP

## Technical Approach

Implement the review MVP as a UI-only enhancement to the existing static cockpit. Keep Spring endpoints unchanged, split recommendation and manual-review DOM targets, cache active categories in browser state, and resolve one pending movement at a time through the persisted endpoint. The UI remains neutral Chilean Spanish for market copy, while technical contracts keep current endpoint field names.

## Architecture Decisions

| Decision | Option | Tradeoff | Choice / Rationale |
|---|---|---|---|
| Delivery surface | Static HTML/CSS/JS only | Less abstraction, but smallest diff and no tooling | Use existing `index.html`, `app.js`, `styles.css`; backend already exposes required contracts. |
| DOM ownership | Shared `review-list` vs separate targets | Shared target currently causes recommendations/manual review overwrites | Add distinct targets such as `recommendation-list` and `manual-review-list`; each renderer owns one region. |
| Category handling | Reload categories per card vs cache once | Per-card fetch is simple but noisy | Load `/api/profiles/active/categories` once into `state.categories`; render native `<select>` controls from state. |
| Direction copy | Merge movement/category direction vs separate labels | Merging risks false accounting semantics | Show movement `DEBIT/CREDIT` and positive CLP amount as primary facts; category direction `INFLOW/OUTFLOW` is supporting classification text only. |
| Controls | Native select/button vs custom dropdown | Native is less branded but accessible and low scope | Use labelled native selects and buttons; style within cockpit identity and focus system. |

## Data Flow

Initial load:

    DOMContentLoaded
      ├─ GET /api/profiles/active + /active/categories ──→ state.categories + profile copy
      ├─ GET /api/cashflow/history/projection-ready ─────→ ledger + totals
      ├─ GET /api/cashflow/history/manual-review ────────→ manual-review cards
      └─ GET /api/cashflow/recommendations ──────────────→ recommendation rail

Resolution:

    Select category ─→ POST /api/cashflow/manual-review/resolutions/{movementId}
      body: { profileId, chosenCategoryKey, description, sourceReference }
      └─ on success: refresh manual-review, projection-ready, recommendations, totals/evidence

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/resources/static/index.html` | Modify | Split `revision` into two live regions: recommendations and pending manual review; add neutral Spanish headings like “Movimientos pendientes de revisión”. |
| `src/main/resources/static/app.js` | Modify | Add UI state, category cache, manual review form rendering, per-movement submit, safe refresh flow, and independent error states. |
| `src/main/resources/static/styles.css` | Modify | Style native selects, review actions, disabled/loading states, and resolved/error/empty states using existing receipt/cockpit tokens. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Modify | Smoke-test static targets, review copy, resolution endpoint wiring, and no secret-like strings. |

## Interfaces / Contracts

No backend contract changes.

```js
const state = {
  categories: [],
  resolvingMovementIds: new Set(),
};
```

Expected category shape: `{ key, displayName, direction }`. Expected pending movement shape: `{ movementId, amount, currency, date, movementDirection, description, sourceReference, status }`. Persisted resolution body uses only `profileId`, `chosenCategoryKey`, optional `description`, optional `sourceReference`.

## UI State Model

| State | Recommendations target | Manual-review target |
|---|---|---|
| Loading | “Cargando recomendaciones.” | “Cargando movimientos pendientes de revisión.” |
| Empty | “Sin recomendaciones activas para este perfil.” | “Sin movimientos pendientes de revisión.” |
| Error | Local error; cockpit remains usable | Local error; recommendations/ledger remain usable |
| Resolving | Unchanged | Disable only selected card controls and set `aria-busy=true` |
| Success | Refresh from API | Refresh list; resolved movement disappears or empty state appears |

## Error / Empty States

Use `setState()` per owned region. Never let recommendation failures erase manual-review controls, or review failures erase recommendations. Validation/API errors display neutral messages such as “No se pudo categorizar el movimiento. Intenta nuevamente.” without echoing sensitive input.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit/static smoke | HTML targets, Chilean Spanish copy, endpoint strings | Extend `CockpitStaticResourceTest`. |
| Integration | Backend contracts | Existing controller/service tests cover endpoints; no new backend tests unless contract gap appears. |
| E2E/manual | Import sample, choose category, resolve, observe refresh | Manual browser verification; no Playwright/Node tooling added. |

## Migration / Rollout

No migration required. Rollback is reverting static resources and the smoke test.

## Review Budget Guidance

Forecast remains 150–250 changed lines. Decision needed before apply: No. Chained PRs recommended: No. 400-line budget risk: Low. If backend changes become necessary, split static UI wiring and backend contract work.

## Open Questions

- [ ] None blocking.
