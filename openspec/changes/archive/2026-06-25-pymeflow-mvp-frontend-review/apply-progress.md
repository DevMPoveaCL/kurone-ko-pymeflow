# Apply Progress: PymeFlow MVP Frontend Review

## Workload / PR Boundary

- Mode: chained PR slice (`feature-branch-chain`)
- Current work unit: PR 3 — Playwright MCP manual runtime smoke, accessibility, and responsive evidence
- Boundary: starts from uncommitted PR1 static shell plus PR2 data wiring and ends with documented Playwright MCP runtime smoke evidence in OpenSpec only. No Node/pnpm/Playwright repository tooling, app code, commits, pushes, merges, or archive.
- Estimated review budget impact: documentation-only PR3 evidence update; zero production/test tooling lines added.

## Completed Tasks

- [x] 1.1 RED: add Spring static-resource smoke for `/` expecting `src/main/resources/static/index.html` landmarks and Spanish caja/abonos/cargos copy.
- [x] 1.2 Verify existing endpoints before backend work: profiles, manual import, provider sync/status, manual-review, projection-ready, recommendations.
- [x] 1.3 RED-if-blocked: skipped after gap verification because existing `/api/cashflow/history/manual-review` and `/api/cashflow/history/projection-ready` expose safe `movementDirection`, positive CLP amounts, statuses, and enough smokeable evidence.
- [x] 2.1 GREEN: create `src/main/resources/static/index.html` with header, receipt rail, ledger, review, history, sync status, and accessible forms.
- [x] 2.2 GREEN: create `src/main/resources/static/styles.css` with receipt identity tokens, visible focus, contrast, empty/error states, and mobile stacking.
- [x] 2.3 REFACTOR: ensure copy is neutral Spanish, includes caja/abonos/cargos, and excludes `mostrador` plus real-bank claims.
- [x] 3.1 GREEN: create `src/main/resources/static/app.js` fetch/render flow for same-origin APIs, safe errors, receipts, and DEBIT/CREDIT evidence.
- [x] 3.2 Optional backend read endpoint skipped: no minimal read endpoint was needed for MVP evidence.
- [x] 3.3 REFACTOR: keep credentials, tokens, cursors, stack traces, and provider payloads out of all rendered UI.
- [x] 4.1 Accepted MVP browser smoke without adding `package.json`, `pnpm-lock.yaml`, or `playwright.config.ts`; no Node/pnpm tooling introduced for PR3.
- [x] 4.2 Used Playwright MCP first to inspect `/`, capture role/label evidence, and document the smoke path.
- [x] 4.3 Recorded Playwright MCP runtime smoke evidence for sync/manual-review visible receipt behavior instead of adding persistent `tests/cockpit/*` files in this MVP slice.
- [x] 4.4 Recorded landmark/accessibility and mobile-width smoke evidence from Playwright MCP runtime inspection.

## API Gap Result

- Existing endpoints are enough for PR2 evidence:
  - `GET /api/profiles/active`
  - `GET /api/profiles/active/categories`
  - `POST /api/cashflow/imports/manual`
  - `POST /api/cashflow/provider-syncs`
  - `GET /api/cashflow/provider-syncs/{syncId}`
  - `GET /api/cashflow/history/manual-review?profileId=pharmacy-cl`
  - `GET /api/cashflow/history/projection-ready?profileId=pharmacy-cl`
  - `GET /api/cashflow/recommendations?profileId=pharmacy-cl`
- No `GET /api/cashflow/history/movements` endpoint was added.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 / 2.1 / 2.2 / 2.3 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Integration (`@SpringBootTest` + `MockMvc`) | N/A (new static resources) | ✅ Written first; failed because `/` forwarded to missing/static-empty `index.html` content | ✅ Passed with `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest*"` | ✅ 2 cases: identity/copy/no real-bank claim and landmarks/controls/root forwarding | ✅ Adjusted assertions for Spring MockMvc root forwarding (`/` forwards to `index.html`) and avoided charset-fragile accented substrings while keeping behavior-specific copy checks |
| 1.2 / 1.3 / 3.2 | Existing endpoint tests listed below | API gap verification | ✅ Existing controller tests passed before backend decision | ✅ Gap decision made before backend production code; no new endpoint test written because not blocked | ✅ Existing endpoint tests passed and proved available contracts | ✅ Existing history tests cover both manual-review and projection-ready movement directions | ✅ No backend refactor needed |
| 3.1 / 3.3 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Integration/static resource contract | ✅ `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest*"` passed before modifying PR1 static files | ✅ Added `servesCockpitScriptWithSameOriginApiWiringAndSafeStateTargets`; failed because `index.html` lacked API targets and `app.js` lacked same-origin endpoint wiring | ✅ Passed after adding API targets, fetch/render flow, loading/empty/error states, receipts, and DEBIT/CREDIT evidence rendering | ✅ Contract checks multiple endpoints and states: profile, categories, provider sync trigger/status, manual import, manual-review, projection-ready, recommendations, safe empty/error text | ✅ Removed sensitive technical terms from rendered UI copy and kept provider status rendering to safe fields only |
| 4.1 / 4.2 / 4.3 / 4.4 | Playwright MCP runtime smoke evidence documented in this artifact | Manual E2E smoke/accessibility | ✅ Prior JUnit cockpit/API suites were already green before PR3 documentation update | ➖ Documentation-only task; no new production code or repository test introduced by accepted MVP decision | ✅ Manual Playwright MCP runtime smoke passed against `http://localhost:8080/` | ✅ Covered desktop identity/copy, provider sync, manual review movement evidence, landmarks/regions, and 390x844 mobile viewport | ✅ Re-scoped tasks to avoid Node/pnpm tooling while preserving smoke/accessibility evidence |

## PR3 Playwright MCP Manual Runtime Smoke Evidence

- No `package.json`, `pnpm-lock.yaml`, `playwright.config.ts`, or `tests/cockpit/*` files were added for this MVP slice.
- Spring Boot app was run locally for the smoke; PID `15984` was stopped afterward and port `8080` was freed.
- Smoke URL: `http://localhost:8080/`.
- Page title observed: `PymeFlow | Cockpit de caja diaria`.
- Identity/copy observed: `Caja diaria para PyMEs chilenas` and `PymeFlow · MVP cockpit`.
- Safe demo warning observed: `Modo seguro demo` and `No representa conectividad bancaria real`.
- Accessibility landmarks observed: `banner`, `navigation`, `main`, plus regions for caja, comprobantes, cartola, and revisión.
- Provider sync button worked and rendered durable evidence: `DURABLE`.
- Manual review button worked and rendered movement evidence: `DEBIT`, `CREDIT`, `abono`, `cargo`, and positive CLP amounts.
- Mobile viewport `390x844` preserved accessible structure and task flow.

## Test Summary

- Total tests written this session: 1 new integration/static contract test from prior PR2 work; PR3 added documentation-only manual smoke evidence.
- Total tests passing: focused cockpit contract, provider sync suite, API gap controller suite, architecture test, and full Gradle suite passed.
- Layers used: Integration/static resource contract and existing WebMvc/controller tests.
- Approval tests: None — no refactoring-only production task.
- Pure functions created: 8 small static JS helpers for rendering, totals, API parsing, escaping, and movement classification.

## Tests Run

- `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest*"` — safety net passed before PR2 changes.
- `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest*"` — RED failed as expected on missing API targets/wiring.
- `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest*"` — GREEN passed after implementation and again after refactor.
- `./gradlew.bat test --rerun-tasks --tests "*ProviderSync*"` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*Cockpit*"` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*CashflowHistoryControllerTest" --tests "*CashflowManualImportControllerTest" --tests "*HistoryRecommendationControllerTest" --tests "*ProfileControllerTest"` — passed.
- `./gradlew.bat test --rerun-tasks` — passed.
- PR3 verification rerun: `./gradlew.bat test --rerun-tasks --tests "*Cockpit*"` — passed.
- PR3 verification rerun: `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` — passed.
- PR3 verification rerun: `./gradlew.bat test --rerun-tasks` — passed.

## Remaining Tasks

- None for the OpenSpec apply scope. Focused cockpit, architecture, and full Gradle test suites passed after this PR3 documentation update.

## Deviations from Design

- PR3 intentionally does not add persistent pnpm/Playwright tooling. This follows the design/proposal wording that browser smoke tooling is optional/if-needed and the orchestrator-accepted MVP decision to document Playwright MCP manual runtime smoke instead.
- The fallback read-only movement endpoint was intentionally not added because existing history APIs provide required evidence.

## Issues Found

- Existing history endpoints already expose `movementDirection`; a broad dashboard or movement aggregate API would be unnecessary review weight for PR2.
- Playwright MCP smoke proved real browser interaction for PR3, but the evidence is manual/documented rather than a persistent CI test. Future hardening can add repository Playwright tooling as a separate review slice if needed.

## Status

13/13 task rows complete. Ready for verification; do not archive until verify passes and the orchestrator approves.
