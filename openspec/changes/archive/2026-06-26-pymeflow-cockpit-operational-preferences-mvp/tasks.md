# Tasks: Cockpit Operational Preferences MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 500–700 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (backend) → PR 2 (frontend + smoke) |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Flyway V6 + domain + port + service + adapter + controller + config + 3 test suites (~450 lines) | PR 1 | Base: `feat/cockpit-operational-preferences-mvp` |
| 2 | app.js load/prefill/debounce + HTML copy + smoke test (~150 lines) | PR 2 | Base: PR 1 branch |

## Phase 1: RED — Backend Contract Tests

- [x] 1.1 `CockpitPreferencesServiceTest`: failing test for horizon=15 rejected, balance non-negative
- [x] 1.2 `CockpitPreferencesControllerTest`: failing @WebMvcTest for GET 200 defaults, PUT 400 on invalid horizon
- [x] 1.3 `JdbcCockpitPreferencesAdapterTest`: failing @JdbcTest save-then-load roundtrip with H2

## Phase 2: GREEN — Backend Implementation

- [x] 2.1 `V6__create_cockpit_preferences.sql`: profile_id PK/FK, opening_balance NUMERIC(18,2), preferred_horizon_days INTEGER CHECK IN (7,30), timestamps
- [x] 2.2 `domain/cockpit/CockpitPreferences.java`: record with compact constructor validation in Spanish (non-negative balance, horizon 7 or 30)
- [x] 2.3 `application/port/out/CockpitPreferencesPort.java`: `findByProfile(ProfileId)`, `save(ProfileId, CockpitPreferences)`
- [x] 2.4 `application/cockpit/CockpitPreferencesService.java`: @Transactional save/load delegating to port
- [x] 2.5 `infrastructure/persistence/JdbcCockpitPreferencesAdapter.java`: idempotent upsert via INSERT ON CONFLICT UPDATE + SELECT load
- [x] 2.6 `interfaces/web/CockpitPreferencesController.java`: GET/PUT `/api/cashflow/cockpit/preferences` with DTO records, Spanish validation errors
- [x] 2.7 `ApplicationServiceConfiguration.java`: add CockpitPreferencesService and JdbcCockpitPreferencesAdapter beans

## Phase 3: GREEN — Tests Pass + ArchUnit

- [x] 3.1 Verify Phase 1 RED tests pass with Phase 2 implementation
- [x] 3.2 Run `ArchitectureTest` — confirm domain has zero framework imports, port stays in application layer

## Phase 4: RED — Frontend Smoke Test

- [x] 4.1 `CockpitStaticResourceTest`: static RED tests for preferences endpoint references, status copy, debounced autosave wiring, and no live-bank claim

## Phase 5: GREEN — Frontend Implementation

- [x] 5.1 `app.js`: add `cockpitPreferences` to API, implement `loadCockpitPreferences()` called on DOMContentLoaded
- [x] 5.2 `app.js`: pre-fill #opening-balance and horizonDays radio from loaded prefs; default 7 days if empty
- [x] 5.3 `app.js`: add 500ms debounced PUT on balance/period change with `persistCockpitPreferences()`
- [x] 5.4 `index.html`: verify opening-balance help text keeps "manual, no bancario" wording and add safe preference status copy
- [x] 5.5 Run focused Cockpit/static-resource tests to verify load→prefill/autosave wiring; dynamic Playwright smoke remains for verify/orchestrator

## Phase 6: Verification & Archive

- [x] 6.1 Run full suite: `./gradlew.bat test --rerun-tasks`
- [ ] 6.2 Playwright MCP manual smoke: open cockpit, change balance/period, refresh, assert controls pre-filled
- [ ] 6.3 Hand off to sdd-archive
