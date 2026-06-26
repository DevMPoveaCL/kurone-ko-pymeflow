# Apply Progress: Cockpit Operational Preferences MVP

## Scope

PR1 backend slice plus PR2 frontend slice for `feature-branch-chain`. PR2 wires static cockpit UI to durable preferences: startup load/prefill, 500ms debounced autosave, safe status copy, and static-resource tests. Dynamic Playwright smoke remains for verify/orchestrator.

## Completed Tasks

- [x] 1.1 `CockpitPreferencesServiceTest`: service validation/delegation tests written first.
- [x] 1.2 `CockpitPreferencesControllerTest`: GET defaults, persisted response semantics, PUT validation tests written first.
- [x] 1.3 `JdbcCockpitPreferencesAdapterTest`: H2-backed adapter save/load/upsert tests written first.
- [x] 2.1 Flyway V6 `cockpit_preferences` table added.
- [x] 2.2 Pure `CockpitPreferences` domain record added.
- [x] 2.3 `CockpitPreferencesPort` added.
- [x] 2.4 `CockpitPreferencesService` added with transactional load/save.
- [x] 2.5 `JdbcCockpitPreferencesAdapter` added with PostgreSQL `ON CONFLICT` upsert and H2 test fallback.
- [x] 2.6 `CockpitPreferencesController` added for `GET`/`PUT /api/cashflow/cockpit/preferences`.
- [x] 2.7 `ApplicationServiceConfiguration` wires service and adapter beans.
- [x] 3.1 Backend preference tests pass.
- [x] 3.2 Architecture tests pass.
- [x] 4.1 `CockpitStaticResourceTest`: static RED tests added for preference status copy, endpoint references, load/prefill/autosave wiring, and no live-bank claim.
- [x] 5.1 `app.js`: `cockpitPreferences` API endpoint added and loaded during startup.
- [x] 5.2 `app.js`: opening balance input and 7/30 horizon radios prefill from preferences, defaulting to 7 days when empty.
- [x] 5.3 `app.js`: 500ms debounced `PUT /api/cashflow/cockpit/preferences` added for balance/horizon changes.
- [x] 5.4 `index.html`: manual opening-balance copy preserved and safe preference status region added.
- [x] 5.5 Focused Cockpit/static tests confirm preference wiring; dynamic browser smoke deferred to verify/orchestrator.
- [x] 6.1 Full Gradle test suite passed.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 / 2.2 / 2.4 | `src/test/java/com/kuroneko/pymeflow/application/cockpit/CockpitPreferencesServiceTest.java` | Unit | N/A (new) | ✅ Compile RED: missing service/domain/port | ✅ Passed in `*CockpitPreferences*` | ✅ Invalid horizon, negative balance, valid save, load existing | ✅ Domain validation centralized |
| 1.2 / 2.6 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitPreferencesControllerTest.java` | Web MVC | N/A (new controller) | ✅ Compile RED: missing controller/service/domain | ✅ Passed in `*CockpitPreferences*` | ✅ Defaults, persisted values, invalid horizon, extreme balance, non-numeric balance, valid save | ✅ Safe body parsing handled in `ApiExceptionHandler` |
| 1.3 / 2.1 / 2.5 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/JdbcCockpitPreferencesAdapterTest.java` | JDBC integration | N/A (new adapter/table) | ✅ Compile RED: missing adapter/domain/migration | ✅ Passed in `*CockpitPreferences*` | ✅ Save/load, idempotent update, empty lookup | ✅ H2 fallback isolates test dialect while prod uses `ON CONFLICT` |
| 2.7 | `src/test/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfigurationTest.java` + `*Cockpit*` context tests | Unit/context | ✅ Existing `*Cockpit*` suite exposed context wiring | ✅ Context RED: final transactional service could not be proxied | ✅ Passed in `*Cockpit*` | ✅ Direct config assertion plus context suite | ✅ Service made proxyable by removing `final` |
| 3.1 | `./gradlew.bat test --rerun-tasks --tests "*CockpitPreferences*"` | Focused backend | N/A | ✅ Initial compile failures from missing production code | ✅ 13 tests passing | ✅ Unit/Web/JDBC covered | ✅ Clean |
| 3.2 | `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` | ArchUnit | N/A | ➖ Structural verification | ✅ Passed | ➖ Single architecture gate | ✅ Domain/application boundaries preserved |
| 4.1 / 5.4 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static resource integration | ✅ Baseline `*CockpitStaticResourceTest`: 7 tests passing | ✅ Failing tests for missing `preferences-status` and manual/no-live-bank preference copy | ✅ Passed in `*CockpitStaticResourceTest` | ✅ Positive manual copy + negative bank-live claims | ✅ Reused existing accessible status pattern |
| 5.1 / 5.2 / 5.3 / 5.5 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static resource integration | ✅ Baseline `*CockpitStaticResourceTest`: 7 tests passing | ✅ Failing tests for missing preferences endpoint, load/prefill functions, debounce `500`, PUT wiring, and status messages | ✅ Passed in `*CockpitStaticResourceTest`, `*Cockpit*`, and full suite | ✅ Endpoint reference, startup load, prefill, autosave, saved/saving/error copy, no `bank-live`/`live bank` strings | ✅ Minimal JS functions extracted: load/prefill/schedule/persist |
| 6.1 | `./gradlew.bat test --rerun-tasks` | Full suite | N/A | ➖ Verification command | ✅ Full suite passed | ➖ Single full-suite gate | ✅ Clean |

## Test Summary

- **Total tests written**: 14 backend/config tests + 2 frontend static-resource tests.
- **Total tests passing**: Full suite passed via `./gradlew.bat test --rerun-tasks`.
- **Layers used**: Unit, Web MVC integration, JDBC integration, ArchUnit.
- **Approval tests**: None — no behavior-preserving refactor task.
- **Pure functions/records created**: `CockpitPreferences` domain record validation; frontend logic extracted into small load/prefill/schedule/persist functions.

## Commands Run

- `./gradlew.bat test --rerun-tasks --tests "*CockpitPreferences*"` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*Cockpit*"` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` — passed.
- `./gradlew.bat test --rerun-tasks` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest"` — baseline passed before PR2 changes, RED failed after tests were added, then GREEN passed after implementation.

## Deviations / Notes

- The public API path follows the orchestrator assignment and design path: `/api/cashflow/cockpit/preferences`. The delta spec text mentions `/api/cockpit/preferences`; implementation uses the repository's existing `/api/cashflow` cockpit namespace.
- `JdbcCockpitPreferencesAdapter` uses PostgreSQL `INSERT ... ON CONFLICT` in production and an H2-only `MERGE` fallback because H2 2.2.224 in PostgreSQL mode does not parse `ON CONFLICT`.
- `CockpitPreferencesService` is non-final because Spring transactional CGLIB proxying cannot subclass final classes.
- PR2 uses static-resource tests rather than a persistent browser test because the assignment explicitly requested no Node tooling and manual Playwright smoke by orchestrator/verify.

## Remaining Tasks

- [ ] 6.2 Playwright MCP manual smoke.
- [ ] 6.3 Archive handoff after PR2 verification.
