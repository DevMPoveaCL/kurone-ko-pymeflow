# Tasks: PymeFlow MVP Frontend Review

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 650-900 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 static shell → PR 2 data wiring/API gap → PR 3 smoke/accessibility |
| Delivery strategy | auto-chain (auto-forecast) |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Static cockpit shell and visual system | PR 1 | Base = feature/tracker branch; WebMvc/static smoke included. |
| 2 | Existing API data wiring plus backend gap decision | PR 2 | Base = PR 1 branch; add endpoint only if gap verification blocks evidence. |
| 3 | Playwright MCP manual runtime smoke, accessibility, responsive evidence | PR 3 | Base = PR 2 branch; no Node/pnpm tooling for MVP; record manual browser smoke evidence. |

## Phase 1: TDD Foundation / API Gap

- [x] 1.1 RED: add Spring static-resource smoke for `/` expecting `src/main/resources/static/index.html` landmarks and Spanish caja/abonos/cargos copy.
- [x] 1.2 Verify existing endpoints before backend work: profiles, manual import, provider sync/status, manual-review, projection-ready, recommendations.
- [x] 1.3 RED-if-blocked: extend `src/test/java/.../CashflowHistoryControllerTest.java` for `GET /api/cashflow/history/movements` safe fields, direction, positive CLP. Skipped after gap verification: existing history APIs provide smokeable movement evidence.

## Phase 2: Static Cockpit Shell

- [x] 2.1 GREEN: create `src/main/resources/static/index.html` with header, receipt rail, ledger, review, history, sync status, and accessible forms.
- [x] 2.2 GREEN: create `src/main/resources/static/styles.css` with receipt identity tokens, visible focus, contrast, empty/error states, and mobile stacking.
- [x] 2.3 REFACTOR: ensure copy is neutral Spanish, includes caja/abonos/cargos, and excludes `mostrador` plus real-bank claims.

## Phase 3: Data Wiring / Optional Backend

- [x] 3.1 GREEN: create `src/main/resources/static/app.js` fetch/render flow for same-origin APIs, safe errors, receipts, and DEBIT/CREDIT evidence.
- [x] 3.2 If 1.3 is needed, add read-only history support in `CashflowHistoryController.java`, service, port, JDBC adapter, and matching tests. Skipped after gap verification: no backend endpoint needed for PR2.
- [x] 3.3 REFACTOR: keep credentials, tokens, cursors, stack traces, and provider payloads out of all rendered UI.

## Phase 4: Smoke / Accessibility

- [x] 4.1 Accept MVP browser smoke without adding `package.json`, `pnpm-lock.yaml`, or `playwright.config.ts`; no Node/pnpm tooling introduced for PR3.
- [x] 4.2 Use Playwright MCP first to inspect `/`, capture role/label evidence, and document the smoke path.
- [x] 4.3 Record Playwright MCP runtime smoke evidence for sync/manual-review visible receipt behavior instead of adding persistent `tests/cockpit/*` files in this MVP slice.
- [x] 4.4 Record landmark/accessibility and mobile-width smoke evidence from Playwright MCP runtime inspection.
