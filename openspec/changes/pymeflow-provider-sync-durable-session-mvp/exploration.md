## Exploration: pymeflow-provider-sync-durable-session-mvp

### Current State
Provider sync is implemented as a synchronous fixture-backed API at `POST /api/cashflow/provider-syncs` and status lookup at `GET /api/cashflow/provider-syncs/{syncId}`. `ProviderSyncUseCase.ProviderSyncService` orchestrates provider fetch, import through `ExternalStatementImportPort`, cursor updates, entry counts, and final snapshot recording behind `SyncSessionPort`. The only implementation is `InMemorySyncSessionAdapter`, wired in `ApplicationServiceConfiguration`, so `SyncSessionPort.Durability` only exposes `IN_MEMORY` and status explicitly disappears on process restart. Flyway/JDBC patterns already exist for cashflow history persistence, and tests use `@JdbcTest` with H2 PostgreSQL mode plus migration scripts.

### Affected Areas
- `src/main/java/com/kuroneko/pymeflow/application/port/out/SyncSessionPort.java` — needs `Durability.DURABLE`/equivalent and possibly a clearer snapshot/state contract for durable status.
- `src/main/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncUseCase.java` — currently stamps snapshots as `IN_MEMORY`; transaction boundary/failure behavior around cursor, count, and report persistence needs to be explicit.
- `src/main/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncStatusUseCase.java` — can keep the same input port if durable lookup remains `findBySyncId`.
- `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncController.java` — response descriptions, not-found message, and expected `durability` value change after durable storage.
- `src/main/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfiguration.java` — replace or conditionally wire `InMemorySyncSessionAdapter` with a JDBC adapter.
- `src/main/java/com/kuroneko/pymeflow/infrastructure/provider/InMemorySyncSessionAdapter.java` — keep for unit tests or fallback, but production wiring should move to durable adapter.
- `src/main/java/com/kuroneko/pymeflow/infrastructure/persistence/` — natural home for a new `JdbcSyncSessionAdapter`, matching existing JDBC adapter conventions.
- `src/main/resources/db/migration/` — add a Flyway migration for provider sync session state and safe error storage.
- `src/test/java/com/kuroneko/pymeflow/infrastructure/provider/InMemorySyncSessionAdapterTest.java` — existing non-durable expectations must remain scoped to the in-memory adapter.
- `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/` — add migration/JDBC tests for restart-like adapter re-instantiation, status by `syncId`, cursor resume, duplicate id handling, and safe error round-trip.
- `src/test/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncUseCaseTest.java` — update assertions around durability and port calls without coupling application to JDBC.
- `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncControllerTest.java` — update status/not-found durability behavior and add smoke coverage for durable status response.

### Approaches
1. **Single durable session table with safe JSON error column** — Create `provider_sync_sessions` keyed by `sync_id`, with a unique `(profile_id, provider_type)` constraint for resume state, scalar status/count/cursor columns, timestamps, and an `errors_json` text/json column containing only safe error DTO data.
   - Pros: Small MVP, one migration, simple status lookup after restart, preserves current `SyncSessionPort` shape, avoids premature schema for rare provider errors.
   - Cons: JSON error assertions are less relational; future analytics over error types may require migration; H2/PostgreSQL JSON compatibility needs care if using `jsonb` directly.
   - Effort: Medium

2. **Session table plus normalized error table** — Store session scalar state in `provider_sync_sessions` and each provider error in `provider_sync_session_errors` with `code`, `safe_message`, `field`, `retry_after_seconds`, and ordering.
   - Pros: Better audit/query model, no JSON compatibility concern, easier to index error codes later.
   - Cons: More migration and adapter code, more tests, likely exceeds MVP review budget when combined with controller/application changes.
   - Effort: High

3. **Complement in-memory with write-through durable snapshots only** — Keep cursor/count in memory and only persist final status snapshots by `syncId`.
   - Pros: Minimal API status durability.
   - Cons: Does not satisfy resume/cursor durability after restart; creates split-brain state; poor foundation for real provider integration.
   - Effort: Low

### Recommendation
Use approach 1 for the MVP: a single `provider_sync_sessions` table and a `JdbcSyncSessionAdapter` implementing the existing `SyncSessionPort`. Keep the domain/application layers pure and let infrastructure own serialization of safe `ProviderError` records. Add `Durability.DURABLE` and have the JDBC adapter return snapshots with durable status; the in-memory adapter can continue returning `IN_MEMORY` for focused unit tests.

Suggested schema: `sync_id varchar primary key`, `profile_id varchar(63) not null`, `provider_type varchar(...) not null`, `status varchar not null`, page/count flags, nullable `cursor`, nullable `last_sync_at`, `session_entry_count`, nullable `retry_after_seconds`, `errors_json text not null default '[]'`, `created_at`, `updated_at`, and `unique(profile_id, provider_type)`. Generate `sync-<uuid>` in the adapter and handle duplicate sync ids by retrying a bounded number of times or relying on UUID uniqueness plus a clear duplicate-key failure. Avoid foreign key to `vertical_profiles` unless the team wants profile deletion to constrain audit history; current cursor scope is a technical provider state and can remain profile-id text for MVP.

Transaction boundaries should be conservative: make each adapter method a single DB statement/transaction; do not wrap provider network/fixture fetch plus import plus session update in one long transaction. `saveCursor` and `incrementEntryCount` should upsert the session row atomically, and `recordReport` should update the same row by `sync_id` (or insert if absent) with final status and safe errors. This preserves partial progress when imports succeed but final report recording later fails; the final failure should propagate rather than silently claim durable status.

### Risks
- JSON error storage is fastest for MVP but less queryable; if audit analytics over provider errors becomes near-term, normalized errors may be worth the extra PR slice.
- Current port separates `saveCursor` and `incrementEntryCount`, so JDBC must avoid lost updates with atomic `entry_count = entry_count + ?` upserts.
- `syncId(profileId, providerType)` currently reuses one id per profile/provider session; clarify in proposal/spec whether a new trigger should reuse the durable session id or create per-run history. The MVP can preserve current behavior but that is session-state, not immutable audit event history.
- Bounded duplicate `syncId` retry is technically prudent even with UUIDs, but overengineering it can inflate scope.
- Controller tests and existing in-memory tests currently assert `IN_MEMORY`; update only API expectations for durable wiring while keeping adapter-specific tests intact.
- Adding JDBC wiring may affect `@WebMvcTest`/configuration tests if beans are expected without a `JdbcTemplate`; keep web tests mocked and adapt `ApplicationServiceConfigurationTest` if present.
- Full durability plus tests may exceed the 400-line review budget; consider chained PRs if tasks forecast high risk.

### Ready for Proposal
Yes — propose a durable-session MVP that persists provider sync state and safe errors through JDBC/Flyway, keeps the existing application port shape mostly intact, updates API durability semantics after restart, and explicitly defers real credentials, UI, production providers, scheduling, and full immutable audit event history.
