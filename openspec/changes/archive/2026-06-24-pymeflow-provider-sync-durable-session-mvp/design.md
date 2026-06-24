# Design: Durable Provider Sync Sessions MVP

## Technical Approach

Replace runtime provider-sync session storage with a JDBC/Flyway adapter while keeping the application layer on `SyncSessionPort`. The main `cashflow-provider-sync` spec changes from current-process status to durable status/resume state; the in-memory adapter remains an adapter-specific test utility.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Persistence model | One `provider_sync_sessions` table keyed by `sync_id`, unique by `(profile_id, provider_type)` | Normalized error table; final-snapshot-only persistence | Satisfies resume/status durability with one reviewable migration and avoids premature audit analytics. |
| Error storage | `errors_json` text with explicit safe DTOs (`code`, `message`, `field`, `retryAfterSeconds`) | Jackson polymorphic serialization; raw exception messages | Keeps persistence provider-agnostic and prevents secrets/internal classes from leaking. |
| Updates | Per-port-method atomic JDBC statements; no transaction across provider fetch/import/session report | One long service transaction | Avoids holding DB transactions around provider I/O and preserves partial progress. |
| Wiring | Runtime `SyncSessionPort` bean becomes JDBC; `InMemorySyncSessionAdapter` stays unscanned for focused tests | Profile switch/fallback adapter | The app should be durable by default; fallback would hide persistence failures. |
| PR delivery | Forecast chained slices if implementation exceeds 400 lines | Single large PR | Migration + adapter + API/test updates may exceed review budget. |

## Data Flow

    POST /provider-syncs
      -> ProviderSyncService -> BankProviderPort fixture
      -> ExternalStatementImportPort
      -> SyncSessionPort(JDBC)
            syncId/findCursor/saveCursor/incrementEntryCount/recordReport
              -> provider_sync_sessions
    GET /provider-syncs/{syncId}
      -> ProviderSyncStatusUseCase -> SyncSessionPort(JDBC) -> safe API DTO

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/resources/db/migration/V5__create_provider_sync_sessions.sql` | Create | Table, checks, indexes, unique `(profile_id, provider_type)`. |
| `src/main/java/com/kuroneko/pymeflow/application/port/out/SyncSessionPort.java` | Modify | Add `Durability.DURABLE`; keep snapshot contract framework-free. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/persistence/JdbcSyncSessionAdapter.java` | Create | JDBC implementation of `SyncSessionPort` with atomic upserts/increments. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/persistence/ProviderErrorJsonMapper.java` | Create | Package-private safe JSON mapper for provider errors. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfiguration.java` | Modify | Replace `new InMemorySyncSessionAdapter()` runtime wiring with JDBC adapter. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/provider/InMemorySyncSessionAdapter.java` | Keep/modify | Preserve non-durable behavior for unit tests only. |
| `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncController.java` | Modify | Update OpenAPI/not-found text and fallback durability from `IN_MEMORY` to `DURABLE`. |
| `openspec/specs/cashflow-provider-sync/spec.md` | Modify later | Archive phase should merge durable session/status requirements. |
| `src/test/java/...` | Modify/Create | JDBC adapter, mapper, wiring, application, and web expectations. |

## Interfaces / Contracts

```java
enum Durability { IN_MEMORY, DURABLE }
```

Schema columns: `sync_id`, `profile_id`, `provider_type`, `status`, `pages_fetched`, `entries_fetched`, `imported_entries`, `has_more_pages`, `truncated`, `auth_aborted`, `cursor`, `last_sync_at`, `session_entry_count`, `retry_after_seconds`, `errors_json`, `created_at`, `updated_at`.

`JdbcSyncSessionAdapter` contract:
- `syncId(profileId, providerType)`: insert row with `sync-<uuid>` if absent, then return existing/new id.
- `saveCursor`: trim blank to `NULL`, set `last_sync_at=now`, update `updated_at`.
- `incrementEntryCount`: reject negatives; `session_entry_count = session_entry_count + ?` in SQL.
- `recordReport`: update the row by `sync_id`, store safe errors JSON, and return snapshots as `DURABLE`.
- `findBySyncId`: deserialize known safe error codes only; malformed/unknown error entries do not expose raw JSON.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `SyncSessionPort` validation and `ProviderErrorJsonMapper` whitelist/round-trip | JUnit/AssertJ; no Spring. |
| Integration | Migration, JDBC restart simulation, cursor resume, atomic count increments, durable snapshots | `@JdbcTest` with H2 PostgreSQL mode and `V5` script. |
| Web | `DURABLE` response/not-found wording and safe error DTOs | Existing `@WebMvcTest` with mocked use cases/status. |
| Architecture | Hexagonal boundaries | Existing ArchUnit test must remain green. |

## Migration / Rollout

Additive migration only; no data backfill. Rollout replaces runtime wiring immediately. Rollback before release reverts wiring and migration; after release, add a drop-table migration only if session data is disposable.

## Open Questions

- [ ] None blocking. Per-run immutable audit history remains explicitly out of scope.
