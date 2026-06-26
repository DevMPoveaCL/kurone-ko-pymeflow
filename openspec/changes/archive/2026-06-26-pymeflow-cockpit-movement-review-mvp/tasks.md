# Tasks: Cockpit Movement Review MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 150-250 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR / single work unit |
| Delivery strategy | auto-chain |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | UI-only manual movement review with static smoke and manual browser evidence | PR 1 | Keep tests with code; no Node tooling. |

## Phase 1: Contract Verification

- [x] 1.1 Verify existing API contracts in backend code/tests before UI coding: active profile, active categories, manual-review history, projection-ready history, recommendations, and `POST /api/cashflow/manual-review/resolutions/{movementId}`.
- [x] 1.2 Confirm response shapes match `design.md`: category `{ key, displayName, direction }`, movement `{ movementId, amount, currency, date, movementDirection, description, sourceReference, status }`, and resolution body fields.

## Phase 2: Static DOM Foundation

- [x] 2.1 Modify `src/main/resources/static/index.html` to split `data-api-target="review-list"` into separate recommendation and manual-review live regions.
- [x] 2.2 Add Spanish review copy and landmarks for “Movimientos pendientes de revisión”, category selection, safe empty/error states, and direction/amount evidence.

## Phase 3: Review State and Actions

- [x] 3.1 Modify `src/main/resources/static/app.js` to add `state.categories` and `state.resolvingMovementIds`, loading active categories once.
- [x] 3.2 Render recommendations and manual-review cards through separate functions/targets so one region never overwrites the other.
- [x] 3.3 Render each pending movement with visible `DEBIT`/`CREDIT`, positive CLP amount, labelled native category `<select>`, and “Categorizar movimiento” action.
- [x] 3.4 Add category-required validation and per-card resolving state that disables only the selected card and preserves retry context on failure.
- [x] 3.5 Submit selected category to the existing resolution endpoint with `profileId`, `chosenCategoryKey`, `description`, and `sourceReference` only.
- [x] 3.6 After success, refresh manual review, projection-ready ledger/totals, recommendations, and cockpit evidence without backend changes.

## Phase 4: Styling

- [x] 4.1 Modify `src/main/resources/static/styles.css` to style native selects, review actions, focus, disabled/loading, empty, error, and success states within existing cockpit tokens.

## Phase 5: Verification

- [x] 5.1 Extend `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` for split targets, review copy, category/action wiring, resolution endpoint string, safe messages, and no secret-like strings.
- [x] 5.2 Run `./gradlew.bat test --rerun-tasks`; do not add Node tooling.
- [ ] 5.3 Capture manual Playwright MCP smoke evidence against the running app for pending/empty/error-safe review behavior and unchanged positive amount plus `DEBIT`/`CREDIT` invariants.
