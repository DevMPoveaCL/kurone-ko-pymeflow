# Tasks: Cockpit Period Cash Projection MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 440–480 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Backend endpoint + CockpitProjectionService + unit/integration tests | PR 1 | Base: `feat/cockpit-period-projection-mvp`; verifiable via `./gradlew.bat test --rerun-tasks` |
| 2 | Cockpit UI (HTML/JS/CSS) + Playwright MCP smoke evidence | PR 2 | Base: PR 1 branch; verifiable via manual Playwright snapshot |

## Phase 0: HorizonDays Decision (before code)

- [x] 0.1 Decide max `horizonDays` cap at backend (90 days recommended per design open question)

## Phase 1: Backend RED — Failing Tests

- [x] 1.1 Create `src/test/.../application/cashflow/CockpitProjectionServiceTest.java` — mock `CashflowMovementHistoryService` + `CashflowProjectionService`, assert command construction (CLP default, profileId, openingBalance), empty-history returns empty projection
- [x] 1.2 Add `cockpitProjection` tests to existing `CashflowProjectionControllerTest.java` — GET validation (negative balance, zero horizon, missing params → 400 + neutral Spanish messages), happy path 200 with response shape, empty movements 200

## Phase 2: Backend GREEN — Implementation

- [x] 2.1 Create `src/main/.../application/cashflow/CockpitProjectionService.java` — `projectFromHistory(ProfileId, BigDecimal, LocalDate, int)`: calls `historyService.projectionReady()`, maps via `ProjectionReadyCashflowTransaction::toProjectionTransaction`, builds `CashflowProjectionCommand` with `Currency.getInstance("CLP")`, delegates to `projectionService.project()`
- [x] 2.2 Modify `CashflowProjectionController.java` — add `@GetMapping("/cockpit/projection")` with `@RequestParam` bindings (`profileId`, `openingBalance`, `startDate`, `horizonDays`), delegate to `CockpitProjectionService`, return existing `CashflowProjectionResponse`

## Phase 3: Backend REFACTOR + Verify

- [x] 3.1 Add horizonDays cap validation in `CockpitProjectionService` (max per decision 0.1) — reject > cap with neutral Spanish `IllegalArgumentException`
- [x] 3.2 Run full test suite: `./gradlew.bat test --rerun-tasks` — all RED→GREEN pass
- [x] 3.3 Verify ArchUnit compliance — no domain→framework or application→infrastructure imports

## Phase 4: Cockpit UI

- [x] 4.1 Modify `src/main/resources/static/index.html` — add projection section: opening-balance input with Spanish label "Saldo inicial (manual, no bancario)", 7d/30d period `<select>`, results container (`#projection-results`), empty-state placeholder
- [x] 4.2 Modify `src/main/resources/static/app.js` — period state object, `fetchProjection()` calling `GET /api/cashflow/cockpit/projection`, render function for daily balances table, totals (abonos/cargos/obligaciones), alerts chips, neutral Spanish empty-state message
- [x] 4.3 Modify `src/main/resources/static/styles.css` — responsive projection panel, table layout, alert/obligation chips following existing design tokens

## Phase 5: Smoke Verification

- [x] 5.1 Manual Playwright MCP smoke — start app with fixture data, exercise period controls + opening balance, verify rendered daily balances/closing/totals/alerts, verify empty state when no PROJECTABLE movements exist, capture snapshot evidence
