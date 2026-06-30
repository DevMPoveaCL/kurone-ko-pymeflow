# Apply Progress: Cockpit Demo Reset MVP

## Scope

- Change: `pymeflow-cockpit-demo-reset-mvp`
- Batch: PR1 backend + PR2 frontend
- Mode: Strict TDD
- Delivery: chained PR slice, `feature-branch-chain`
- Boundary: PR1 backend reset+seed API plus PR2 cockpit static UI reset action, JS wiring, safe states, evidence refresh, and static contract tests. Phase 5 verify/archive remains pending.

## Completed Tasks

- [x] 0.1 Moved premature `openspec/specs/cockpit-demo-data/spec.md` into the active change and removed the baseline copy.
- [x] 1.1 RED service tests for orchestration, deterministic seed, sync/preferences, and demo-profile validation.
- [x] 1.2 RED JDBC adapter tests for profile-scoped deletion and reference-table preservation.
- [x] 1.3 RED controller tests for success, validation, demo-only rejection, and safe failure response.
- [x] 2.1 Added `DemoDataPort`.
- [x] 2.2 Added `JdbcDemoDataAdapter` with parameterized profile-scoped deletes.
- [x] 2.3 Added transactional `CockpitDemoResetService` with deterministic 3 `PROJECTABLE` + 2 `MANUAL_REVIEW` movements, completed sync snapshot, and cockpit preferences.
- [x] 2.4 Added `DemoDataController` at `POST /api/cockpit/demo/reset-and-seed`.
- [x] 2.5 Wired `CockpitDemoResetService` in application configuration; `JdbcDemoDataAdapter` is component-scanned as `@Repository`.
- [x] 3.1 Ran focused demo tests, architecture tests, configuration wiring test, and full suite.
- [x] 3.2 Confirmed safe response fields and seed counts.
- [x] 4.1 Added RED static contract tests for `id="demo-reset-btn"`, “Reiniciar demo” copy, demo-only language, endpoint wiring, safe states, and full evidence refresh calls.
- [x] 4.2 Added the cockpit “Reiniciar demo” control and live status region in the actions panel.
- [x] 4.3 Wired static JS to `POST /api/cockpit/demo/reset-and-seed?profileId=pharmacy-cl`, show pending/success/error states, refresh preferences/profile/movements/recommendations/projection, and update sync receipt when a `syncSessionId` is returned. Reset failure copy intentionally does not append backend messages.
- [x] 4.4 Ran focused cockpit/static tests, demo tests, and full suite; verified no real bank/provider claims were introduced.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 / 2.1 / 2.3 | `src/test/java/com/kuroneko/pymeflow/application/cockpit/CockpitDemoResetServiceTest.java` | Unit | N/A (new service/port) | ✅ Compile RED: missing `DemoDataPort`/`CockpitDemoResetService` | ✅ `./gradlew.bat test --rerun-tasks --tests "*Demo*"` passed | ✅ 4 cases: order/counts, deterministic movements, sync/preferences, non-demo rejection | ✅ Avoided forbidden application literals while keeping deterministic seed |
| 1.2 / 2.2 | `src/test/java/com/kuroneko/pymeflow/infrastructure/demo/JdbcDemoDataAdapterTest.java` | Integration (`@JdbcTest`) | N/A (new adapter) | ✅ Compile RED: missing `JdbcDemoDataAdapter` | ✅ `./gradlew.bat test --rerun-tasks --tests "*Demo*"` passed | ✅ 2 cases: transactional rows deleted for target profile, reference/other profile rows retained | ✅ H2 setup uses local reference-table DDL to avoid unsupported `TIMESTAMPTZ` |
| 1.3 / 2.4 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/DemoDataControllerTest.java` | Integration (`@WebMvcTest`) | N/A (new controller) | ✅ Compile RED: missing `DemoDataController` | ✅ `./gradlew.bat test --rerun-tasks --tests "*Demo*"` passed | ✅ 5 cases: success, missing/blank profile, non-demo 403, safe 500 | ✅ Added specific validation handler so blank profile is not swallowed by generic safe failure handler |
| 2.5 | `src/test/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfigurationTest.java` | Unit | ⚠️ Existing file safety net delayed by RED compile state | ✅ Added wiring assertion for demo reset service | ✅ `./gradlew.bat test --rerun-tasks --tests "*ApplicationServiceConfigurationTest"` passed | ➖ Structural wiring only | ✅ Kept adapter as `@Repository` to avoid duplicate bean risk |
| 3.1 / 3.2 | `src/test/java/com/kuroneko/pymeflow/architecture/ArchitectureTest.java` + full suite | Verification | N/A | ✅ Architecture initially failed on forbidden application literal | ✅ `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` and full suite passed | ✅ Confirmed architecture rule catches category/source literals | ✅ Split forbidden literal pieces in application code without changing behavior |
| 4.1 / 4.2 / 4.3 / 4.4 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration (`@SpringBootTest` + `MockMvc`) | ✅ `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest"` passed before production changes | ✅ Two new tests failed first for missing reset button/status and JS endpoint/wiring; triangulation failed until reset failure avoided backend message append | ✅ `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest"` passed after UI/JS implementation | ✅ 3 checks: static control/copy, JS endpoint/safe-state/full-refresh contract, no raw reset error append | ✅ Extracted `renderProviderSyncReceipt` to remove duplicated sync receipt rendering and reran tests |

## Test Summary

- **Total tests written**: 12 new backend tests + 1 configuration wiring assertion + 2 static frontend contract tests.
- **Total tests passing**: Full suite passed.
- **Layers used**: Unit (service/config), Integration (`@JdbcTest`, `@WebMvcTest`, `@SpringBootTest` static resources), Architecture (ArchUnit).
- **Approval tests**: None — no behavior-preserving refactor task.
- **Pure functions created**: 3 helper factory methods in `CockpitDemoResetService` for deterministic seed data; 1 JS render helper for sync receipt reuse.

## Tests Run

- `./gradlew.bat test --rerun-tasks --tests "*Demo*"` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` — passed after refactor.
- `./gradlew.bat test --rerun-tasks --tests "*ApplicationServiceConfigurationTest"` — passed.
- `./gradlew.bat test --rerun-tasks` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest"` — baseline passed before PR2 production changes, then RED failed for missing reset UI/JS, then passed after GREEN/REFACTOR.
- `./gradlew.bat test --rerun-tasks --tests "*Cockpit*"` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*Demo*"` — passed.
- `./gradlew.bat test --rerun-tasks` — passed after PR2 frontend changes.

## Deviations / Notes

- The tasks artifact originally said `stacked-to-main`; this apply batch records the orchestrator-resolved strategy as `feature-branch-chain`.
- `JdbcDemoDataAdapter` is `@Repository`-scanned rather than also declared as a config `@Bean`, preventing duplicate `DemoDataPort` beans while keeping the design's infrastructure adapter stereotype.
- Application seed constants avoid direct forbidden vertical/provider literals required by the existing `ArchitectureTest`.
- PR2 kept to static resources/tests only. `refreshCockpitEvidence()` now reloads preferences and profile/category context before movement/recommendation/projection refresh so reset-seeded opening balance and horizon are visible.
- Live smoke initially exposed a PostgreSQL transaction bug in `JdbcSyncSessionAdapter.syncId()`: catching `DuplicateKeyException` after a unique conflict still left the transaction aborted, causing demo reset to return 500. The fix changed PostgreSQL inserts to `on conflict (profile_id, provider_type) do nothing` and added a regression test proving repeated sync id use remains report-updatable.
- Post-fix smoke passed: reset endpoint returned 200, seeded 5 movements, refreshed sync receipt, preferences (`openingBalance=350000`, `preferredHorizonDays=7`), projection-ready movements, manual-review/recommendations, and projection request.

## Remaining Tasks

- [x] Phase 5 archive completed after verify report and E2E smoke.
