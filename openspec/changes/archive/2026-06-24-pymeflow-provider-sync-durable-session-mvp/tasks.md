# Tasks: Durable Provider Sync Sessions MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 650-850 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 migration/JDBC core → PR 2 runtime wiring/resume → PR 3 web/API semantics |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Durable storage and JDBC adapter | PR 1 | Base `main`; includes migration, mapper, adapter tests. |
| 2 | Application wiring and resume behavior | PR 2 | Base after PR 1; replaces runtime adapter and verifies restart resume. |
| 3 | Web API durable semantics | PR 3 | Base after PR 2; updates controller DTO tests and safe errors. |

## Phase 1: Storage Foundation (TDD)

- [x] 1.1 RED: Add migration test for `src/main/resources/db/migration/V5__create_provider_sync_sessions.sql` columns, checks, indexes, and unique `(profile_id, provider_type)`.
- [x] 1.2 GREEN: Create `V5__create_provider_sync_sessions.sql` with scalar session state, `errors_json`, timestamps, and additive rollback-safe DDL.
- [x] 1.3 RED: Add `ProviderErrorJsonMapperTest` for safe round-trip, unknown/malformed error filtering, and no raw credential leakage.
- [x] 1.4 GREEN: Create package-private `src/main/java/com/kuroneko/pymeflow/infrastructure/persistence/ProviderErrorJsonMapper.java` using stable safe DTO fields only.

## Phase 2: JDBC Session Adapter (TDD)

- [x] 2.1 RED: Add `JdbcSyncSessionAdapterTest` for `syncId`, cursor resume after new adapter instance, `DURABLE` snapshots, and blank cursor as null.
- [x] 2.2 GREEN: Create `src/main/java/com/kuroneko/pymeflow/infrastructure/persistence/JdbcSyncSessionAdapter.java` implementing `SyncSessionPort` with constructor injection and `JdbcTemplate`.
- [x] 2.3 RED: Add adapter tests for negative count rejection and atomic `session_entry_count = session_entry_count + ?` updates across multiple increments.
- [x] 2.4 GREEN: Implement atomic upsert/update statements for cursor, entry counts, report persistence, timestamps, and safe error serialization.

## Phase 3: Application Wiring and Resume (TDD)

- [x] 3.1 RED: Update application/service tests proving a previous cursor is used after adapter restart and reports return durable status.
- [x] 3.2 GREEN: Modify `SyncSessionPort.java` to add `Durability.DURABLE` without framework dependencies; keep `IN_MEMORY` for focused tests.
- [x] 3.3 GREEN: Modify `ApplicationServiceConfiguration.java` to wire `JdbcSyncSessionAdapter`; keep `InMemorySyncSessionAdapter.java` unscanned/test-only.
- [x] 3.4 REFACTOR: Run existing ArchUnit tests and remove any application-to-infrastructure dependency leak.

## Phase 4: Web API Semantics (TDD)

- [x] 4.1 RED: Update `CashflowProviderSyncController` WebMvc tests for trigger/status responses reporting durable/persistent durability and safe not-found wording.
- [x] 4.2 RED: Add web test for persisted safe provider errors excluding credentials, stack traces, raw payloads, and internal exception details.
- [x] 4.3 GREEN: Modify `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncController.java` DTO mapping/OpenAPI text from `IN_MEMORY` to durable semantics.
- [x] 4.4 VERIFY: Run `./gradlew.bat test --rerun-tasks` after each PR slice before marking tasks complete.

## Phase 5: Verification Fix (TDD)

- [x] 5.1 RED/GREEN: Add focused JDBC integration coverage for missing `provider_sync_sessions` storage proving durable operations fail with `DataAccessException` instead of returning a false durable snapshot.
