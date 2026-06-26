# Apply Progress: Cockpit Period Cash Projection MVP

## Change

`pymeflow-cockpit-period-projection-mvp`

## Mode

Strict TDD — chained delivery. PR1 backend complete; PR2 UI implemented.

## Workload / PR Boundary

- Mode: chained PR slice (`feature-branch-chain`)
- Current work unit: PR2 cockpit static UI wiring + static resource tests
- Boundary: starts from uncommitted PR1 backend state; ends before manual Playwright MCP smoke, archive, commit, merge, or push
- Estimated review budget impact: UI-only slice kept focused on static HTML/JS/CSS plus one resource test file

## Horizon Decision

- `horizonDays` backend MVP cap: **90 days**
- Rationale: enough for 7/30-day cockpit controls and near-term cash planning while limiting accidental expensive projections.

## Completed Tasks

- [x] 0.1 Decide max `horizonDays` cap at backend
- [x] 1.1 Create `CockpitProjectionServiceTest`
- [x] 1.2 Add `cockpitProjection` controller tests
- [x] 2.1 Create `CockpitProjectionService`
- [x] 2.2 Add `GET /api/cashflow/cockpit/projection`
- [x] 3.1 Add horizon cap validation
- [x] 3.2 Run full test suite
- [x] 3.3 Verify ArchUnit compliance
- [x] 4.1 Add projection controls/results markup to `index.html`
- [x] 4.2 Wire static cockpit JS to `GET /api/cashflow/cockpit/projection`
- [x] 4.3 Add responsive projection styles/chips to `styles.css`

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 0.1 / 3.1 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/CockpitProjectionServiceTest.java`, `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProjectionControllerTest.java` | Unit + WebMvc | ✅ Existing projection/controller/config tests passed before production changes | ✅ Cap tests written first (`91` days) | ✅ Focused tests passed | ✅ Service and controller cap paths covered | ✅ Constant extracted as `MAX_HORIZON_DAYS` |
| 1.1 / 2.1 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/CockpitProjectionServiceTest.java` | Unit | N/A (new service) | ✅ Missing `CockpitProjectionService` caused compile RED | ✅ Focused tests passed | ✅ Non-empty history, empty history, and cap cases covered | ✅ Validation centralized in service |
| 1.2 / 2.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProjectionControllerTest.java` | WebMvc | ✅ `CashflowProjectionControllerTest` passed before production changes | ✅ Missing service/endpoint caused compile RED | ✅ Focused tests passed | ✅ Happy path, empty response, invalid params, missing params, and cap cases covered | ✅ Reused `CashflowProjectionResponse`; controller delegates to use case |
| Wiring | `src/test/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfigurationTest.java` | Unit | ✅ Config test passed before production changes | ⚠️ Added after minimal wiring to cover Spring bean creation | ✅ Focused tests passed | ➖ Structural wiring only | ✅ Bean wiring stays in infrastructure configuration |
| 4.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Integration/static resource | ✅ `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest"` passed before UI changes (5 existing tests) | ✅ Static resource assertions for projection landmark, manual opening-balance copy, 7/30 controls, and empty placeholder failed before markup | ✅ Focused Cockpit static tests passed after markup | ✅ Controls plus empty-state placeholder covered with separate script wiring test | ✅ Kept existing cockpit identity and navigation; no custom period control added beyond 7/30 MVP |
| 4.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Integration/static resource | ✅ Same Cockpit static safety net as 4.1 | ✅ Static script assertions for endpoint reference, query params, response fields, totals, alerts, and safe error/empty text failed before JS wiring | ✅ Focused Cockpit static tests passed after JS implementation | ✅ Request construction, required manual balance guard, empty `dailyBalances`, totals, obligations, alerts, and error copy covered | ✅ Extracted small render/summarize helpers; refresh keeps movement review behavior intact |
| 4.3 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Integration/static resource | ✅ Same Cockpit static safety net as 4.1 | ✅ Covered through markup/script resource tests before adding projection styles | ✅ Focused Cockpit static tests passed after CSS addition | ➖ Styling-only task; user-visible projection section still covered through 4.1/4.2 | ✅ Reused existing receipt/cashflow tokens and responsive media pattern |

## Test Summary

- Total tests written: 11 across PR1 + PR2 (9 backend/wiring, 2 static cockpit UI)
- Total tests passing: 11 new tests plus existing suite
- Layers used: Unit, WebMvc, Spring static-resource integration
- Approval tests: None — no refactoring task
- Pure functions created: 0

## Tests Run

- `./gradlew.bat test --rerun-tasks --tests "*CashflowProjectionControllerTest" --tests "*ApplicationServiceConfigurationTest"` — PR1 safety net passed before production changes
- `./gradlew.bat test --rerun-tasks --tests "*CockpitProjectionServiceTest" --tests "*CashflowProjectionControllerTest"` — PR1 RED compile failure before service existed, then GREEN passed after implementation
- `./gradlew.bat test --rerun-tasks --tests "*CockpitProjectionServiceTest" --tests "*CashflowProjectionControllerTest" --tests "*ApplicationServiceConfigurationTest"` — PR1 passed
- `./gradlew.bat test --rerun-tasks --tests "*Projection*"` — PR1 passed; PR2 rerun passed
- `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` — PR1 passed; PR2 rerun passed
- `./gradlew.bat test --rerun-tasks` — PR1 passed; PR2 full suite passed
- `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest"` — PR2 safety net passed before UI changes (5 tests)
- `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest"` — PR2 RED failed with 2 new static resource assertions before implementation
- `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest"` — PR2 GREEN passed after UI implementation (7 tests)
- `./gradlew.bat test --rerun-tasks --tests "*Cockpit*"` — PR2 focused Cockpit tests passed

## Deviations / Notes

- Application services in this codebase are wired via `ApplicationServiceConfiguration` instead of Spring annotations, so `CockpitProjectionService` remains framework-free despite the design sketch showing `@Service`.
- Empty history returns an empty projection directly instead of delegating to `CashflowProjectionService`, because the spec requires empty `dailyBalances` when no `PROJECTABLE` movements exist; the existing projection service would otherwise produce one row per horizon day.
- PR2 implements only the designed 7-day and 30-day controls. Custom period controls were intentionally not added to keep the PR2 slice small.
- Static resource tests avoid exact accented substrings where MockMvc renders static resources without a charset in the assertion output; assertions still target the user-visible strings and endpoint contract.
- Manual Playwright MCP smoke was run after PR2 implementation by the orchestrator.

## Remaining Tasks for PR2 / Verify

- [x] 5.1 Manual Playwright MCP smoke — exercise projection controls/API and capture evidence

## Manual Playwright MCP Smoke Evidence

- App started from current branch and health endpoint returned `UP`.
- Cockpit loaded at `/` with title `PymeFlow | Cockpit de caja diaria`.
- Projection section was visible with copy explaining that the opening balance is user-entered and not a live bank balance.
- Entered opening balance `1000000` and clicked `Calcular proyección`.
- Runtime result rendered `Resultado de proyección` with:
  - `CIERRE PROYECTADO`
  - totals for `abonos`, `cargos`, and `obligaciones`
  - `Alertas de proyección`
  - `Obligaciones aplicadas`
  - `Saldos diarios proyectados`
- The rendered copy preserved the safety boundary: saldo inicial manual, not live banking.
- Port `8080` was freed after the smoke.

## Risks

- The reused response DTO has no explicit categorization signal field; PR2 treats empty `dailyBalances` as the categorization prompt signal as planned.
- Runtime UI smoke passed via Playwright MCP. Static resource tests still cover shipped markup/script contracts.
