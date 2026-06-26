# Proposal: Cockpit Operational Preferences MVP

## Intent

Continue MVP cockpit usability by durably remembering simple operational preferences per profile, so users do not re-enter manual opening balance and 7/30-day projection period after every page load. Copy must remain honest: opening balance is user-entered, not bank-live.

## Scope

### In Scope
- Add minimal `cockpit_preferences` persistence via Flyway.
- Add pure domain/application preference model, output port, JDBC adapter, and Spring wiring.
- Add small REST `GET`/`PUT` API for cockpit preferences.
- Load preferences on cockpit startup and auto-save control changes with debounce.
- Preserve no-Node static cockpit delivery and honest manual-balance copy.

### Out of Scope
- Auth, multi-user, tenant model, broad settings module, localStorage-only persistence.
- Real bank balances, provider/account balance lookup, or frontend build tooling.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `pymeflow-mvp-cockpit`: persist and restore cockpit opening-balance/period preferences through a small backend preference API.

## Approach

Create `cockpit_preferences(profile_id PK/FK, opening_balance NUMERIC(18,2), preferred_horizon_days CHECK IN (7,30), timestamps)` in Flyway V6. Model preferences as pure Java records/value objects, expose a `CockpitPreferencesPort`, and implement idempotent JDBC load/upsert. Add a thin REST controller using constructor injection and application/service transaction boundaries. Static JS fetches preferences during initialization, applies them to existing controls, and debounces `PUT` saves on balance/period changes without changing the projection endpoint contract.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/resources/db/migration/V6__create_cockpit_preferences.sql` | New | Durable preference table. |
| `src/main/java/com/kuroneko/pymeflow/domain/cockpit/` | New | Pure preference model/validation. |
| `src/main/java/com/kuroneko/pymeflow/application/port/out/` | New | Preference persistence port. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/persistence/` | New | JDBC adapter with upsert/load. |
| `src/main/java/com/kuroneko/pymeflow/interfaces/web/` | New | `GET`/`PUT` preference API. |
| `src/main/resources/static/app.js` | Modified | Startup load and debounced auto-save. |
| `src/main/resources/static/index.html` | Modified | Preference status/copy only if needed. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Debounced saves race | Medium | Idempotent upsert; save latest UI state. |
| Preference API implies live balance | Medium | Keep copy explicit: user-entered/manual, not bank-live. |
| Review budget exceeds 400 lines | Medium | Split backend persistence/API/tests and static UI/smoke tests. |
| Future tenant isolation | Low | Keep `profile_id` boundary; defer `tenant_id` migration. |

## Rollback Plan

Revert controller/service/adapter/static changes and remove Flyway V6 before release. If already released, add a follow-up migration that drops `cockpit_preferences`; projection still works with manual input.

## Dependencies

- Existing `pharmacy-cl` profile flow, Spring Boot static cockpit, Flyway, JdbcTemplate, PostgreSQL/H2 support.

## Review Workload Forecast

- 400-line budget risk: Medium.
- Chained PRs recommended: Yes if implementation forecast exceeds 400 changed lines.
- Split guidance: PR 1 backend migration/model/port/adapter/API/tests; PR 2 cockpit JS/HTML/static smoke.

## Success Criteria

- [ ] Cockpit restores saved opening balance and 7/30-day period for the active profile.
- [ ] Changes auto-save durably without localStorage-only behavior.
- [ ] Projection copy still states opening balance is manual/user-entered, not bank-live.
- [ ] No auth, tenant, settings module, Node tooling, or real-bank balance behavior is introduced.
