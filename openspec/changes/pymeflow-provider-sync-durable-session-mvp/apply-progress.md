# Apply Progress: Durable Provider Sync Sessions MVP

## Scope

- Change: `pymeflow-provider-sync-durable-session-mvp`
- Mode: Strict TDD
- Delivery: auto-chain / stacked-to-main
- Current PR slice: PR #2 runtime wiring/resume
- Boundary: runtime `SyncSessionPort` wiring now uses the JDBC adapter and provider sync resume/status behavior is verified through restart-style adapter re-instantiation. Web/API durability wording and response semantics remain out of scope for PR #3.

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

## Test Summary

- Total tests written: 10
- Total tests passing: 10 focused PR #1/PR #2 behavior tests; full suite passing
- Layers used: Unit (4), Integration (6), Architecture (1 verification), E2E (0)
- Approval tests: None — no refactoring tasks
- Pure functions created: 0

## Tests Run

- `./gradlew.bat test --rerun-tasks --tests "*SyncSessionPortTest"` — passed safety net before modifying `SyncSessionPort`.
- `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncSessionMigrationTest" --tests "*ProviderErrorJsonMapperTest" --tests "*JdbcSyncSessionAdapterTest"` — RED first failed, then passed after implementation.
- `./gradlew.bat test --rerun-tasks --tests "*ProviderSync*Jdbc*" --tests "*Flyway*" --tests "*SyncSession*"` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` — passed.
- `./gradlew.bat test --rerun-tasks` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest" --tests "*ApplicationServiceConfigurationTest" --tests "*JdbcSyncSessionAdapterTest"` — passed as PR #2 safety net before runtime wiring.
- `./gradlew.bat test --rerun-tasks --tests "*ApplicationServiceConfigurationTest" --tests "*ProviderSyncRuntimeWiringIntegrationTest"` — failed RED before `syncSessionPort(JdbcTemplate)` existed, then passed after wiring JDBC.
- `./gradlew.bat test --rerun-tasks --tests "*ProviderSync*" --tests "*JdbcSyncSession*" --tests "*ApplicationServiceConfiguration*"` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` — passed.
- `./gradlew.bat test --rerun-tasks` — passed.

## Deviations / Notes

- `SyncSessionPort.Durability.DURABLE` was added in PR #1 because the assigned JDBC adapter test/task required durable snapshots; PR #2 now wires that durable adapter at runtime.
- `JdbcSyncSessionAdapter` is intentionally not annotated as a Spring component in PR #1, preserving the explicit boundary that runtime wiring belongs to PR #2.
- H2 does not support the PostgreSQL `ON CONFLICT` syntax used in the initial GREEN attempt, so `syncId` uses insert plus duplicate-key handling while preserving the unique profile/provider invariant.
- Runtime wiring uses explicit `@Bean` construction with `JdbcTemplate` rather than annotating `JdbcSyncSessionAdapter`; this keeps infrastructure wiring centralized and preserves the in-memory adapter as a focused test utility.
- `VerticalProfilePropertiesTest` intentionally excludes datasource auto-configuration, so it now mocks provider sync use cases and `JdbcTemplate` to keep the test scoped to vertical profile properties instead of durable sync runtime wiring.

## Remaining Tasks

- [ ] Phase 4: Web API durable semantics.

## Status

12/16 tasks complete. PR #2 runtime wiring/resume is ready for review; continue with PR #3 web/API durable semantics after this slice is accepted.
