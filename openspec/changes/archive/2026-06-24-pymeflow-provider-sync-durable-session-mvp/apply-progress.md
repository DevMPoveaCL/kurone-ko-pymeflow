# Apply Progress: Durable Provider Sync Sessions MVP

## Scope

- Change: `pymeflow-provider-sync-durable-session-mvp`
- Mode: Strict TDD
- Delivery: auto-chain / stacked-to-main
- Current PR slice: PR #3 web/API durable semantics
- Boundary: PR #3 starts after PR #1 storage and PR #2 runtime wiring/resume. It updates only web/API response semantics, OpenAPI wording, WebMvc expectations, safe persisted-error response coverage, and verification evidence. Storage schema, runtime wiring, UI, credentials, and production bank dependencies remain out of scope.

## Completed Tasks

- [x] 1.1 RED: Added migration tests for `V5__create_provider_sync_sessions.sql` columns, indexes, unique profile/provider state, and status checks.
- [x] 1.2 GREEN: Created `V5__create_provider_sync_sessions.sql` with scalar session state, safe `errors_json`, timestamps, checks, and indexes.
- [x] 1.3 RED: Added `ProviderErrorJsonMapperTest` for safe round-trip, filtering unknown/malformed entries, and secret-field rejection.
- [x] 1.4 GREEN: Created package-private `ProviderErrorJsonMapper` with stable safe fields only.
- [x] 2.1 RED: Added `JdbcSyncSessionAdapterTest` for stable `syncId`, restart-style cursor resume, durable snapshots, and blank cursor normalization.
- [x] 2.2 GREEN: Created standalone `JdbcSyncSessionAdapter` implementing `SyncSessionPort` using constructor injection and `JdbcTemplate`.
- [x] 2.3 RED: Added adapter tests for negative count rejection and accumulated entry counts.
- [x] 2.4 GREEN: Implemented cursor updates, atomic SQL count increments, report persistence, timestamps, and safe error serialization.
- [x] 3.1 RED: Added runtime wiring/resume integration tests proving durable status survives adapter re-instantiation and a later sync resumes from the stored `(profileId, providerType)` cursor.
- [x] 3.2 GREEN: Verified `SyncSessionPort.Durability.DURABLE` remains framework-free and `IN_MEMORY` remains available for focused in-memory adapter tests.
- [x] 3.3 GREEN: Replaced runtime `SyncSessionPort` bean wiring with `JdbcSyncSessionAdapter` via `JdbcTemplate`; left `InMemorySyncSessionAdapter` unscanned/test-only.
- [x] 3.4 REFACTOR: Ran ArchUnit and full suite; added test-context mocks where datasource auto-configuration is intentionally excluded so unrelated configuration tests do not create a JDBC runtime adapter.
- [x] 4.1 RED: Updated `CashflowProviderSyncControllerTest` to require `DURABLE` trigger/status responses and safe durable not-found wording.
- [x] 4.2 RED: Added WebMvc coverage proving persisted provider errors are exposed only as safe DTO fields and do not include credential references, secrets, stack traces, raw payload markers, or internal exception markers.
- [x] 4.3 GREEN: Updated `CashflowProviderSyncController` response fallback durability and OpenAPI operation/response wording from in-memory/non-durable semantics to durable persisted-session semantics.
- [x] 4.4 VERIFY: Ran focused provider sync/API tests, architecture tests, full Gradle suite, and JaCoCo report generation for the PR #3 slice.
- [x] 5.1 VERIFICATION FIX: Added focused JDBC integration coverage for missing `provider_sync_sessions` storage; existing production behavior fails safely with `DataAccessException` and does not return a false durable snapshot.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/FlywayProviderSyncSessionMigrationTest.java` | Integration | N/A (new) | ✅ Compile failure before migration existed | ✅ `*ProviderSyncSessionMigrationTest` passed | ✅ Columns/indexes + constraint behavior | ✅ Isolated H2 DB per test |
| 1.2 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/FlywayProviderSyncSessionMigrationTest.java` | Integration | N/A (new) | ✅ Migration test failed before DDL | ✅ Focused migration test passed | ✅ Unique profile/provider + invalid status paths | ✅ Named unique index for H2/PostgreSQL compatibility |
| 1.3 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/ProviderErrorJsonMapperTest.java` | Unit | N/A (new) | ✅ Compile failure before mapper existed | ✅ Mapper tests passed | ✅ Round-trip + malformed/unknown/secret-bearing input | ✅ Unknown JSON properties ignored safely |
| 1.4 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/ProviderErrorJsonMapperTest.java` | Unit | N/A (new) | ✅ Mapper contract test written first | ✅ Focused mapper test passed | ✅ Blank/malformed JSON returns empty | ✅ Package-private mapper kept infrastructure-local |
| 2.1 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/JdbcSyncSessionAdapterTest.java` | Integration | ✅ `*SyncSessionPortTest` passed before modifying `SyncSessionPort` | ✅ Compile failure before adapter and `DURABLE` existed | ✅ Focused adapter tests passed | ✅ Stable id, cursor resume, durable snapshot, blank cursor | ✅ Adapter left unscanned to avoid PR #1 runtime wiring |
| 2.2 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/JdbcSyncSessionAdapterTest.java` | Integration | N/A (new adapter) | ✅ Adapter tests referenced missing production class | ✅ Focused adapter tests passed | ✅ New adapter instance reads existing DB state | ✅ Constructor injection retained without Spring runtime registration |
| 2.3 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/JdbcSyncSessionAdapterTest.java` | Integration | N/A (new adapter) | ✅ Negative/accumulation tests written before logic | ✅ Focused adapter tests passed | ✅ Rejects negative and accumulates multiple increments | ✅ Count assertion verifies stored SQL state |
| 2.4 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/JdbcSyncSessionAdapterTest.java` | Integration | N/A (new adapter) | ✅ Persistence behavior tests failed before implementation | ✅ Focused adapter tests passed | ✅ Report persistence includes counts, retry hint, errors | ✅ H2-compatible insert with duplicate-key handling |
| 3.1 | `src/test/java/com/kuroneko/pymeflow/infrastructure/config/ProviderSyncRuntimeWiringIntegrationTest.java` | Integration | ✅ `*ProviderSyncUseCaseTest`, `*ApplicationServiceConfigurationTest`, `*JdbcSyncSessionAdapterTest` passed before modifying runtime wiring | ✅ Compile failure while tests expected `syncSessionPort(JdbcTemplate)` and durable restart behavior | ✅ Runtime wiring integration tests passed | ✅ Durable status after re-instantiation + resume from stored cursor | ✅ Test uses H2 V5 storage and real JDBC adapter |
| 3.2 | `src/test/java/com/kuroneko/pymeflow/application/port/out/SyncSessionPortTest.java`, `src/test/java/com/kuroneko/pymeflow/infrastructure/provider/InMemorySyncSessionAdapterTest.java` | Unit | ✅ Existing port/in-memory tests covered framework-free and test-only behavior | ✅ Covered in PR #1 RED for JDBC durable snapshots | ✅ Focused provider sync/JDBC tests passed | ✅ `DURABLE` snapshots from JDBC, `IN_MEMORY` remains in focused tests | ➖ None needed — enum already added in PR #1 |
| 3.3 | `src/test/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfigurationTest.java` | Unit | ✅ Existing configuration test passed before edit | ✅ Test failed while expecting JDBC adapter wiring instead of in-memory | ✅ Configuration focused tests passed | ✅ Direct bean assertion + runtime integration behavior | ✅ Constructor wiring uses `JdbcTemplate`; no component scanning added to in-memory adapter |
| 3.4 | `src/test/java/com/kuroneko/pymeflow/architecture/ArchitectureTest.java` | Architecture | ✅ Existing ArchUnit suite run after wiring | ✅ N/A — refactor/verification task | ✅ `*ArchitectureTest*` passed | ➖ Single verification path | ✅ No application-to-infrastructure dependency introduced |
| 4.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncControllerTest.java` | WebMvc | ✅ `*CashflowProviderSyncControllerTest` passed before controller edits | ✅ Failed while trigger/status/not-found still returned `IN_MEMORY` and in-memory text | ✅ Focused controller tests passed after controller update | ✅ Trigger fallback + status snapshot + not-found wording | ✅ OpenAPI assertions document durable status semantics |
| 4.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncControllerTest.java` | WebMvc | ✅ Same controller safety net | ✅ Added persisted safe-error response test before implementation/docs update | ✅ Focused controller tests passed | ✅ AUTH_ERROR and DATA_ERROR safe field coverage with sensitive marker exclusions | ➖ None needed — controller maps existing safe provider error DTOs |
| 4.3 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncControllerTest.java` | WebMvc | ✅ Controller baseline passed before production edit | ✅ Durable API tests failed against previous in-memory DTO/OpenAPI text | ✅ Focused and provider-sync suites passed | ✅ Response body + OpenAPI description paths | ✅ Replaced magic fallback constant with `DURABLE` |
| 4.4 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncControllerTest.java`, full suite | Verification | ✅ PR #3 focused tests green before final verification | ✅ N/A — verification task | ✅ Required focused, architecture, full suite, and JaCoCo commands passed | ➖ Single verification path | ✅ No schema/runtime/UI changes added |
| 5.1 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/JdbcSyncSessionAdapterTest.java` | Integration | ✅ `*JdbcSyncSessionAdapterTest` passed before modifying test coverage | ✅ Missing-storage scenario added before any production change; existing adapter already exposed safe `DataAccessException` behavior | ✅ Focused missing-storage test passed without production change | ✅ `syncId` and `findBySyncId` both fail against absent table instead of reporting durable state | ➖ None needed — production already fails safely; no fallback added |

## Test Summary

- Total tests written: 12 focused behavior tests across PR #1-#3 plus verification fix, including missing durable-storage failure coverage.
- Total tests passing: focused missing-storage test, focused PR #3 WebMvc tests, provider sync/JDBC focused suite, architecture suite, and full Gradle suite passing.
- Layers used: Unit (4), Integration (7), WebMvc (1 new + updated existing assertions), Architecture (1 verification), E2E (0).
- Approval tests: None — no pure refactoring tasks.
- Pure functions created: 0.

## Tests Run

- `./gradlew.bat test --rerun-tasks --tests "*SyncSessionPortTest"` — passed safety net before modifying `SyncSessionPort` in PR #1.
- `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncSessionMigrationTest" --tests "*ProviderErrorJsonMapperTest" --tests "*JdbcSyncSessionAdapterTest"` — RED first failed, then passed after PR #1 implementation.
- `./gradlew.bat test --rerun-tasks --tests "*ProviderSync*Jdbc*" --tests "*Flyway*" --tests "*SyncSession*"` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` — passed in PR #1/PR #2 and again in PR #3.
- `./gradlew.bat test --rerun-tasks` — passed in PR #1/PR #2 and again in PR #3.
- `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest" --tests "*ApplicationServiceConfigurationTest" --tests "*JdbcSyncSessionAdapterTest"` — passed as PR #2 safety net before runtime wiring.
- `./gradlew.bat test --rerun-tasks --tests "*ApplicationServiceConfigurationTest" --tests "*ProviderSyncRuntimeWiringIntegrationTest"` — failed RED before `syncSessionPort(JdbcTemplate)` existed, then passed after wiring JDBC.
- `./gradlew.bat test --rerun-tasks --tests "*ProviderSync*" --tests "*JdbcSyncSession*" --tests "*ApplicationServiceConfiguration*"` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*CashflowProviderSyncControllerTest"` — passed as PR #3 safety net before controller edits; failed RED after durable expectations were added; passed after GREEN implementation.
- `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncControllerTest" --tests "*ProviderSync*" --tests "*JdbcSyncSession*"` — passed for PR #3 focused verification.
- `./gradlew.bat jacocoTestReport` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*JdbcSyncSessionAdapterTest"` — passed as safety net before adding verification-fix coverage.
- `./gradlew.bat test --rerun-tasks --tests "*JdbcSyncSessionAdapterTest.failsSafelyWhenDurableSessionStorageIsUnavailable"` — passed; proves missing storage raises `DataAccessException` and never reports durable success.
- `./gradlew.bat test --rerun-tasks --tests "*JdbcSyncSession*" --tests "*ProviderSync*" --tests "*Flyway*"` — passed after verification fix (`BUILD SUCCESSFUL in 29s`).
- `./gradlew.bat test --rerun-tasks` — passed after verification fix (`BUILD SUCCESSFUL in 43s`).
- `./gradlew.bat jacocoTestReport` — passed after verification fix (`BUILD SUCCESSFUL in 6s`).

## Deviations / Notes

- `SyncSessionPort.Durability.DURABLE` was added in PR #1 because the assigned JDBC adapter test/task required durable snapshots; PR #2 wired that durable adapter at runtime.
- `JdbcSyncSessionAdapter` is intentionally not annotated as a Spring component in PR #1, preserving explicit configuration ownership in PR #2.
- H2 does not support the PostgreSQL `ON CONFLICT` syntax used in the initial GREEN attempt, so `syncId` uses insert plus duplicate-key handling while preserving the unique profile/provider invariant.
- Runtime wiring uses explicit `@Bean` construction with `JdbcTemplate` rather than annotating `JdbcSyncSessionAdapter`; this keeps infrastructure wiring centralized and preserves the in-memory adapter as a focused test utility.
- `VerticalProfilePropertiesTest` intentionally excludes datasource auto-configuration, so it now mocks provider sync use cases and `JdbcTemplate` to keep the test scoped to vertical profile properties instead of durable sync runtime wiring.
- PR #3 intentionally did not change storage schema or runtime service wiring. `ProviderSyncUseCase` still records reports through the port with a default snapshot durability value; the JDBC adapter persists/returns durable snapshots, and the web fallback now reports durable semantics for the runtime contract.
- Verification fix intentionally changed only test coverage and artifacts. The existing JDBC adapter already fails safely when the durable table is absent; no fallback-to-memory behavior was added because the design explicitly says fallback would hide persistence failures.

## Remaining Tasks

- [x] All planned PR #1, PR #2, and PR #3 tasks are complete.
- [x] Verification failure for “Migration failure prevents false durability” is covered by `JdbcSyncSessionAdapterTest.failsSafelyWhenDurableSessionStorageIsUnavailable`.

## Status

17/17 tasks complete. Verification-fix coverage is ready for verify/review.
