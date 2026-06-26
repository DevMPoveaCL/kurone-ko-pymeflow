# Apply Progress: Cockpit Movement Review MVP

## Status

- Mode: Strict TDD
- Delivery: single work unit / single PR
- Review budget forecast: 150-250 changed lines, low risk
- Backend changes: none

## API Contract Result

Verified existing backend contracts before UI implementation:

- `GET /api/profiles/active` returns active profile `{ id, displayName }`.
- `GET /api/profiles/active/categories` returns category `{ key, displayName, direction }`.
- `GET /api/cashflow/history/manual-review?profileId=pharmacy-cl` returns pending movement `{ movementId, amount, currency, date, movementDirection, description, sourceReference, status }`.
- `GET /api/cashflow/history/projection-ready?profileId=pharmacy-cl` returns projection-ready transaction evidence.
- `GET /api/cashflow/recommendations?profileId=pharmacy-cl` returns recommendation signals.
- `POST /api/cashflow/manual-review/resolutions/{movementId}` accepts `{ profileId, chosenCategoryKey, description, sourceReference }` and returns projection-ready transaction plus category evidence.

No backend gap was found.

## Completed Tasks

- [x] 1.1 Verify existing API contracts.
- [x] 1.2 Confirm response shapes match design.
- [x] 2.1 Split recommendation and manual-review DOM targets.
- [x] 2.2 Add Spanish review copy and landmarks.
- [x] 3.1 Add category cache and resolving movement state.
- [x] 3.2 Render recommendations and manual review independently.
- [x] 3.3 Render pending movement controls with DEBIT/CREDIT and positive CLP amount.
- [x] 3.4 Add category-required validation and per-card resolving state.
- [x] 3.5 Submit selected category to existing persisted resolution endpoint.
- [x] 3.6 Refresh review, ledger/totals, recommendations, and evidence after success.
- [x] 4.1 Style review controls and states.
- [x] 5.1 Extend cockpit static resource tests.
- [x] 5.2 Run full Gradle suite.
- [ ] 5.3 Capture manual Playwright MCP smoke evidence later in verify/orchestrator step.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1-1.2 | Existing backend tests inspected: `ProfileControllerTest`, `CashflowHistoryControllerTest`, `ManualReviewResolutionControllerTest`, `HistoryRecommendationControllerTest` | Contract verification | ✅ `CockpitStaticResourceTest` baseline 3/3 passing | N/A — verification-only task, no production code | ✅ Existing contracts matched design | ➖ Existing tests cover categories, pending review, projection-ready, recommendations, and persisted resolution | ➖ None needed |
| 2.1-2.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static smoke | ✅ 3/3 baseline before modifying static resources | ✅ Added failing assertions for separate `recommendation-list` and `manual-review-list`, review copy, select/action text | ✅ Focused cockpit static tests passed | ✅ Added separate script/static assertions so split DOM and action copy are both covered | ✅ Encoding-sensitive assertions were narrowed to stable substrings while preserving behavior checks |
| 3.1-3.6 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static contract smoke | ✅ Same baseline | ✅ Added failing assertions for `state.categories`, `state.resolvingMovementIds`, persisted resolution endpoint, `chosenCategoryKey`, validation copy, positive amount via `Math.abs`, and direction literals | ✅ Focused cockpit static tests passed | ✅ Covered both endpoint/action wiring and direction/category invariant strings (`DEBIT`, `CREDIT`, `INFLOW`, `OUTFLOW`) | ✅ Extracted `refreshCockpitEvidence`, `formatPositiveMoney`, `categoryDirectionCopy`, and card busy/message helpers |
| 4.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static smoke | ✅ Same baseline | ✅ Existing RED required native select/action affordances visible in static resources | ✅ Focused cockpit static tests passed after CSS/UI implementation | ➖ Styling validated indirectly through semantic resources; no class assertions by design | ✅ Kept styling within existing cockpit tokens and semantic states |
| 5.1-5.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static smoke + full suite | ✅ Focused suite baseline captured before changes | ✅ Test additions failed before production changes | ✅ `CockpitStaticResourceTest`, `*ArchitectureTest*`, and full `./gradlew.bat test --rerun-tasks` passed | ✅ Two new focused tests cover separate DOM/copy and endpoint/direction invariants | ✅ No Node tooling or persistent Playwright tests added |

## Test Summary

- Total tests written: 2 focused static smoke tests added.
- Total tests passing: full Gradle test suite passed.
- Layers used: Static/Spring MockMvc smoke, architecture test suite.
- Approval tests: None — no refactoring-only task.
- Pure functions created: 2 (`formatPositiveMoney`, `categoryDirectionCopy`).

## Tests Run

- `./gradlew.bat test --tests "com.kuroneko.pymeflow.interfaces.web.CockpitStaticResourceTest" --rerun-tasks` — RED failed as expected after tests were added.
- `./gradlew.bat test --tests "com.kuroneko.pymeflow.interfaces.web.CockpitStaticResourceTest" --rerun-tasks` — GREEN passed.
- `./gradlew.bat test --tests "*ArchitectureTest*" --rerun-tasks` — passed.
- `./gradlew.bat test --rerun-tasks` — passed.

## Deviations

- Manual Playwright MCP smoke evidence was not captured in apply because the launch prompt said manual Playwright evidence can be documented later by orchestrator/verify.

## Remaining

- Capture browser smoke evidence for pending/empty/error-safe review behavior and DEBIT/CREDIT plus positive CLP invariants.
