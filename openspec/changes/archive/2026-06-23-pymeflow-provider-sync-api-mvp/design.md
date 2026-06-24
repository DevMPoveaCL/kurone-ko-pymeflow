# Design: Provider Sync API MVP

## Technical Approach

Expose the existing fixture-backed `ProviderSyncUseCase` through a synchronous Spring MVC API. `POST /api/cashflow/provider-syncs` validates only safe references, builds `ProviderAuth`, runs the existing use case, records an in-memory safe status snapshot, and returns the same normalized response shape used by `GET /api/cashflow/provider-syncs/{syncId}`. This extends the `cashflow-provider-sync` spec without adding real credential storage, production bank dependencies, scheduling, or async job semantics.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| API semantics | Return `200 OK` after synchronous sync completion | `202 Accepted` async job style | The current use case is synchronous and fixture-only; `202` would imply lifecycle guarantees the MVP does not provide. |
| Status lookup | Add application `ProviderSyncStatusUseCase` plus `SyncSessionPort.findBySyncId(...)` snapshot extension | Let web read `InMemorySyncSessionAdapter` directly | Preserves hexagonal boundaries: web depends on application, infrastructure remains a port implementation. |
| Status storage | Keep non-durable in-memory snapshots in `InMemorySyncSessionAdapter` | Database-backed job/status table | Matches MVP scope and avoids pretending fixture status survives restart. |
| Request shape | Accept `profileId`, `providerType`, `credentialRef`, `dateFrom`, `dateTo` only | Accept secrets or per-request paging overrides | Keeps examples safe and avoids bypassing configured `ProviderAuthConfig` limits. |

## Data Flow

```text
POST /api/cashflow/provider-syncs
  -> CashflowProviderSyncController
  -> ProviderSyncUseCase.sync(command)
  -> BankProviderPort(FakeBankProviderAdapter)
  -> ExternalStatementImportPort(SimulatedBankStatementAdapter)
  -> SyncSessionPort(InMemorySyncSessionAdapter snapshot)
  -> ProviderSyncResponse

GET /api/cashflow/provider-syncs/{syncId}
  -> CashflowProviderSyncController
  -> ProviderSyncStatusUseCase.find(syncId)
  -> SyncSessionPort.findBySyncId(syncId)
  -> ProviderSyncResponse | 404
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncController.java` | Create | REST controller, DTO records, manual validation, safe OpenAPI examples. |
| `src/main/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncStatusUseCase.java` | Create | Application read use case for status lookup by `syncId`. |
| `src/main/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncUseCase.java` | Modify | Persist final safe status snapshot after sync completes, including error/retry metadata. |
| `src/main/java/com/kuroneko/pymeflow/application/port/out/SyncSessionPort.java` | Modify | Add immutable `SyncSessionSnapshot`, `recordReport(...)`, and `findBySyncId(...)`. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/provider/InMemorySyncSessionAdapter.java` | Modify | Store snapshot fields in concurrent in-memory state; lookup by `syncId`. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfiguration.java` | Modify | Wire `ProviderSyncStatusUseCase` with the existing `SyncSessionPort`. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncControllerTest.java` | Create | `@WebMvcTest` for POST/GET, validation, OpenAPI examples, and no credential echo. |
| Existing provider sync tests | Modify | Cover snapshot recording, status lookup, wiring, and adapter behavior. |

## Interfaces / Contracts

```java
record ProviderSyncRequest(String profileId, String providerType,
        String credentialRef, String dateFrom, String dateTo) {}

record ProviderSyncResponse(String syncId, String profileId, String providerType,
        String status, int pagesFetched, int entriesFetched, int importedEntries,
        boolean hasMorePages, boolean truncated, boolean authAborted,
        Optional<String> cursor, Optional<Instant> lastSyncAt,
        int sessionEntryCount, List<ProviderErrorResponse> errors,
        Optional<Integer> retryAfterSeconds, String durability) {}
```

Provider errors map to safe API codes: `AUTH_ERROR`, `RATE_LIMIT`, `UNAVAILABLE`, `DATA_ERROR`; messages come only from `ProviderError` safe fields. `credentialRef` is accepted to build `ProviderAuth` but is never returned or logged by the API DTO.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Snapshot recording, lookup by `syncId`, normalized status/error mapping | JUnit/AssertJ tests for use case, status use case, adapter. |
| Integration | Controller POST/GET, validation, 404, no credential echo, OpenAPI examples | `@WebMvcTest` with mocked application use cases and reflection checks. |
| E2E | Not available | Covered by Spring MVC and application tests per project config. |

## Migration / Rollout

No migration required. Rollout is additive and fixture-only. Status is explicitly non-durable (`durability: "IN_MEMORY"`) and resets on process restart.

## PR Split Forecast

Estimated changed lines: 350-500. 400-line budget risk: Medium. Chained PRs recommended: Yes if implementation crosses 400 lines. Suggested split: PR #1 application/status port + adapter tests; PR #2 web API/OpenAPI + MVC tests.

## Open Questions

None.
