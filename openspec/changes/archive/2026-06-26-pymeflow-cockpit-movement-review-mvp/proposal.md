# Proposal: Cockpit Movement Review MVP

## Intent

Continue the MVP roadmap with user-visible cockpit functionality: let a Chilean PyME operator resolve pending manual-review movements by choosing an active profile category, using existing persisted backend endpoints without adding auth, provider, or recommendation-rule scope.

## Scope

### In Scope
- Load pending `MANUAL_REVIEW` movements and active profile categories.
- Let the user select a category and submit `POST /api/cashflow/manual-review/resolutions/{movementId}`.
- Refresh pending review, projection-ready history, recommendations, and cockpit evidence after resolution.
- Keep `DEBIT`/`CREDIT` movement direction and positive CLP amount visually explicit.
- Use copy that does not confuse movement direction with category `INFLOW`/`OUTFLOW` semantics.

### Out of Scope
- Auth, multi-user behavior, real bank/provider integrations, new ML/recommendation rules.
- Backend endpoint/model changes unless implementation proves an existing contract gap.
- Batch review, filters, custom dropdowns, projection preview, or broader workbench UX.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `pymeflow-mvp-cockpit`: add interactive manual-review categorization to the static cockpit while preserving existing evidence, accessibility, and safe-MVP constraints.

## Approach

Use the recommended UI-only persisted review flow. Separate pending review rendering from recommendations, load categories once, render accessible native category selectors per pending movement, submit the selected category, then reload review/projection-ready/recommendation evidence. Use neutral Spanish labels such as “Movimientos pendientes de revisión”, “Selecciona una categoría”, “Categorizar movimiento”, and “Listo para proyección”.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/resources/static/index.html` | Modified | Add review controls and distinct targets for recommendations vs pending movements. |
| `src/main/resources/static/app.js` | Modified | Add category state, resolution POST, refresh flow, and safe error/loading states. |
| `src/main/resources/static/styles.css` | Modified | Style selectors, action buttons, focus, and empty/error/resolved states. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Modified | Smoke static wiring/copy for the review interaction. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Shared `review-list` target overwrites sections | Med | Split DOM targets before adding controls. |
| Direction/category semantics confuse users | Med | Show movement direction separately; copy category as classification only. |
| No pending fixture rows | Low | Verify sample data path; use honest empty state. |
| Review exceeds 400 changed lines | Low | Keep UI-only; split if scope expands. |

## Review Workload Forecast

Forecast: 150–250 changed lines. Decision needed before apply: No. Chained PRs recommended: No. 400-line budget risk: Low. If backend work is discovered or diff exceeds 400 lines, split into PR1 static UI wiring and PR2 backend/tests.

## Rollback Plan

Revert static resource/test changes. Existing backend endpoints and persisted movement history remain untouched, so rollback only removes the cockpit interaction surface.

## Dependencies

- Existing endpoints for manual review, active categories, projection-ready history, recommendations, and cockpit fixture data.

## Success Criteria

- [ ] Pending movements render with positive CLP amount and visible `DEBIT`/`CREDIT`.
- [ ] Selecting a category resolves a movement through the existing POST endpoint.
- [ ] Pending, projection-ready, recommendation, and evidence sections refresh after resolution.
- [ ] UI copy avoids implying category direction changes movement direction or amount.
