# Apply Progress: Durable Provider Sync Sessions MVP

## Scope

- Change: `pymeflow-provider-sync-durable-session-mvp`
- Mode: Strict TDD
- Delivery: auto-chain / stacked-to-main
- Current PR slice: PR #1 migration/JDBC core
- Boundary: durable storage migration, safe provider error JSON mapper, and standalone JDBC adapter core only. Runtime wiring/resume behavior and web/API durability semantics remain out of scope for later PR slices.

## Completed Tasks

- [x] 1.1 RED: Added migration tests for `V5__create_provider_sync_sessions.sql` columns, indexes, unique profile/provider state, and status checks.
- [x] 1.2 GREEN: Created `V5__create_provider_sync_sessions.sql` with scalar session state, safe `errors_json`, timestamps, checks, and indexes.
- [x] 1.3 RED: Added `ProviderErrorJsonMapperTest` for safe round-trip, filtering unknown/malformed entries, and secret-field rejection.
- [x] 1.4 GREEN: Created package-private `ProviderErrorJsonMapper` with stable safe fields only.
- [x] 2.1 RED: Added `JdbcSyncSessionAdapterTest` for stable `syncId`, restart-style cursor resume, durable snapshots, and blank cursor normalization.
- [x] 2.2 GREEN: Created standalone `JdbcSyncSessionAdapter` implementing `SyncSessionPort` using constructor injection and `JdbcTemplate`.
- [x] 2.3 RED: Added adapter tests for negative count rejection and accumulated entry counts.
- [x] 2.4 GREEN: Implemented cursor updates, atomic SQL count increments, report persistence, timestamps, and safe error serialization.

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

## Test Summary

- Total tests written: 8
- Total tests passing: 8 focused PR #1 tests; full suite passing
- Layers used: Unit (3), Integration (5), E2E (0)
- Approval tests: None — no refactoring tasks
- Pure functions created: 0

## Tests Run

- `./gradlew.bat test --rerun-tasks --tests "*SyncSessionPortTest"` — passed safety net before modifying `SyncSessionPort`.
- `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncSessionMigrationTest" --tests "*ProviderErrorJsonMapperTest" --tests "*JdbcSyncSessionAdapterTest"` — RED first failed, then passed after implementation.
- `./gradlew.bat test --rerun-tasks --tests "*ProviderSync*Jdbc*" --tests "*Flyway*" --tests "*SyncSession*"` — passed.
- `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` — passed.
- `./gradlew.bat test --rerun-tasks` — passed.

## Deviations / Notes

- `SyncSessionPort.Durability.DURABLE` was added in PR #1 because the assigned JDBC adapter test/task requires durable snapshots. Runtime wiring remains untouched.
- `JdbcSyncSessionAdapter` is intentionally not annotated as a Spring component in PR #1, preserving the explicit boundary that runtime wiring belongs to PR #2.
- H2 does not support the PostgreSQL `ON CONFLICT` syntax used in the initial GREEN attempt, so `syncId` uses insert plus duplicate-key handling while preserving the unique profile/provider invariant.

## Remaining Tasks

- [ ] Phase 3: Application wiring and resume behavior.
- [ ] Phase 4: Web API durable semantics.

## Status

8/16 tasks complete. PR #1 is ready for verify/review; continue with PR #2 after this slice is accepted.
