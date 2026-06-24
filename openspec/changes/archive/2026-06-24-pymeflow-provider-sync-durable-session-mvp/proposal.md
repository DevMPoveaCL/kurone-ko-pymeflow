# Proposal: Durable Provider Sync Sessions MVP

## Intent

Provider sync status and resume state currently disappear on app restart because `SyncSessionPort` is backed only by memory. This change makes fixture-backed provider sync sessions durable before introducing real provider credentials or production bank dependencies.

## Scope

### In Scope
- Add a JDBC-backed `SyncSessionPort` and Flyway table for provider sync session state.
- Persist status, cursor, entry counts, timestamps, durability, and safe provider errors without secrets.
- Update API semantics from `IN_MEMORY`/restart-lost status to persistent durable lookup.
- Keep fixture provider as the only provider implementation.

### Out of Scope
- UI, scheduling, webhooks, real credentials, OAuth, token storage, or production bank APIs.
- Immutable per-run audit history or normalized provider-error analytics.
- Replacing the existing import anti-corruption boundary.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `cashflow-provider-sync`: Change sync session traceability and status API requirements from current-process in-memory snapshots to durable JDBC-backed session state.

## Approach

Use the exploration-recommended single `provider_sync_sessions` table plus `JdbcSyncSessionAdapter`. Keep domain/application pure: application continues depending on `SyncSessionPort`, while infrastructure owns JDBC persistence and safe error serialization. Add `Durability.DURABLE`; keep `InMemorySyncSessionAdapter` scoped to focused tests/fallback behavior. Use atomic upsert/increment operations for cursor and entry counts.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `application/port/out/SyncSessionPort.java` | Modified | Add durable semantics and snapshot contract. |
| `application/cashflow/*ProviderSync*UseCase.java` | Modified | Preserve orchestration while recording durable status. |
| `interfaces/web/CashflowProviderSyncController.java` | Modified | Return durable status and restart-safe not-found semantics. |
| `infrastructure/config/ApplicationServiceConfiguration.java` | Modified | Wire JDBC adapter for application use. |
| `infrastructure/persistence/` | New | Add JDBC adapter and mapper/serialization support. |
| `src/main/resources/db/migration/` | New | Add `provider_sync_sessions` Flyway migration. |
| `src/test/java/...` | Modified/New | Add JDBC, application, and web coverage for durability. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Lost entry counts from separate port calls | Med | Use atomic DB increments/upserts. |
| JSON error storage becomes hard to query | Low | Accept for MVP; defer normalized error table. |
| Review budget exceeds 400 lines | Med | Split into chained PR slices if task forecast is high. |

## Rollback Plan

Revert JDBC adapter wiring to `InMemorySyncSessionAdapter`, revert API durability expectations, and remove the new Flyway migration before release. If already released, add a follow-up migration to drop `provider_sync_sessions` only after confirming no required session data remains.

## Dependencies

- Existing JDBC/Flyway setup and H2 PostgreSQL-mode test pattern.
- Existing fixture provider and import boundary.

## Success Criteria

- [ ] Status lookup by `syncId` survives adapter/application restart simulation.
- [ ] API responses report durable semantics and never expose credentials or provider internals.
- [ ] Fixture provider sync continues to import through `ExternalStatementImportPort`.
- [ ] Tests cover migration, JDBC adapter, application orchestration, and web API behavior.
