## Exploration: Movement review/categorization MVP from the cockpit

### Current State
The backend already has the core persisted review flow: `GET /api/cashflow/history/manual-review?profileId=pharmacy-cl` lists pending `MANUAL_REVIEW` movements, `GET /api/profiles/active/categories` lists selectable categories, and `POST /api/cashflow/manual-review/resolutions/{movementId}` turns one pending movement into `PROJECTABLE` by setting `category_key` while preserving the stored positive amount and `movement_direction` (`DEBIT`/`CREDIT`). Projection-ready history then exposes the resolved movement through `GET /api/cashflow/history/projection-ready`, and recommendations are recomputed from persisted history.

The static cockpit already reads manual-review, projection-ready, categories, and recommendations, but it only displays movement evidence. It does not let the user choose a category or submit a persisted resolution. Also, `app.js` currently uses the same `review-list` target for recommendations and pending manual review, so whichever async render finishes later can overwrite the other section.

### Affected Areas
- `src/main/resources/static/index.html` — needs a small interactive review surface for pending movements, with category selection and clear Spanish copy.
- `src/main/resources/static/app.js` — needs state for categories, rendering of pending review controls, POST to `/api/cashflow/manual-review/resolutions/{movementId}`, and refresh of movement/recommendation data after resolution.
- `src/main/resources/static/styles.css` — likely needs minimal styles for category selectors, action buttons, and resolved/error states.
- `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowHistoryController.java` — already exposes pending/projection-ready movements with direction and positive amount; no change expected for MVP.
- `src/main/java/com/kuroneko/pymeflow/interfaces/web/ManualReviewResolutionController.java` — already exposes persisted resolution by movement id; no change expected for MVP.
- `src/main/java/com/kuroneko/pymeflow/application/cashflow/CashflowMovementHistoryService.java` — already validates profile/category, sensitive text, status, and delegates persisted resolution; no change expected for MVP.
- `src/main/java/com/kuroneko/pymeflow/infrastructure/persistence/CashflowMovementHistoryJdbcAdapter.java` — already updates only `status`, `category_key`, and timestamps, preserving amount and `movement_direction`; no change expected for MVP.
- `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` — should cover the static resource wiring for the new endpoint/copy if implemented later.

### Approaches
1. **UI-only persisted review flow** — Reuse existing endpoints: load categories once, render each pending movement with a category select and “Categorizar movimiento” action, POST the selected category, then refresh pending/projection-ready/recommendations.
   - Pros: smallest backend-safe scope; preserves hexagonal backend untouched; uses existing persistence invariants; immediately feeds projection-ready history and recommendation recomputation.
   - Cons: no per-movement recommendation to confirm; user must choose a category manually.
   - Effort: Low

2. **Backend category recommendation per pending movement** — Add a query/use case that returns suggested categories for pending movements, then let the UI accept or change the suggestion.
   - Pros: closer to “confirm recommendation” wording; can improve speed for users.
   - Cons: adds product/domain behavior not currently modeled; risks overbuilding a rules engine; pending manual reviews exist specifically because categorization did not confidently match.
   - Effort: Medium

3. **Broader movement review workbench** — Add filters, batch categorization, inline projection preview, and richer recommendation cards.
   - Pros: more complete cockpit experience.
   - Cons: too large for MVP and likely exceeds review budget; introduces state and UX complexity before the single-movement flow is proven.
   - Effort: High

### Recommendation
Use **UI-only persisted review flow** for this change. The backend already satisfies the hard invariants: `amount > 0`, `movement_direction` is stored separately, category choice changes `category_key`/`status` only, and projection-ready history plus recommendations read from persisted movement history. The MVP should not promise ML or automatic recommendations; it should say “Categoría sugerida” only if the data actually contains a suggestion, which it does not today. Use neutral Chilean Spanish such as “Movimientos pendientes de revisión”, “Selecciona una categoría”, “Categorizar movimiento”, and “Listo para proyección”.

### Risks
- The cockpit currently has one shared `review-list` target for recommendations and pending review; implementing controls without separating/ordering these renders can cause one section to overwrite the other.
- Native `<select>` is acceptable for a small static MVP, but it must keep accessible labels and clear focus states; do not introduce custom dropdown complexity now.
- Category direction can intentionally differ from movement direction; UI copy must not imply that selecting an `INFLOW` category changes a `DEBIT` into a credit or changes the positive amount invariant.
- If sample imports do not produce pending manual-review rows, the flow may appear empty; use existing fixture/manual sample data carefully rather than adding fake frontend-only rows.

### Ready for Proposal
Yes — propose a small cockpit-only change that wires existing persisted review APIs into the static UI. Explicitly keep backend endpoint/model changes out of scope unless implementation discovers an endpoint contract gap.
