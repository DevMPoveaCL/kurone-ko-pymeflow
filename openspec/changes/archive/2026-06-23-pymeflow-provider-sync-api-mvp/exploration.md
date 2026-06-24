## Exploration: Provider Sync API MVP

### Current State
The provider sync core already exists behind hexagonal boundaries. `ProviderSyncUseCase` accepts a `ProviderSyncCommand` containing `ProfileId`, `dateFrom`, `dateTo`, and `ProviderAuth`, then orchestrates `BankProviderPort.fetchStatements(...)` into `ExternalStatementImportPort.importStatement(...)` while updating `SyncSessionPort`. The active infrastructure wiring uses `FakeBankProviderAdapter`, `SimulatedBankStatementAdapter`, and `InMemorySyncSessionAdapter`, with defaults from `ProviderAuthConfig`.

There is no public provider-sync API yet. Existing web adapters are controller-centric under `interfaces.web`, use Java records for DTOs, delegate into application ports/use cases, and cover behavior with `@WebMvcTest`. Existing provider session state can return a stable `syncId` for a `(profileId, providerType)` pair and stores cursor/lastSyncAt internally, but it cannot inspect a session by `syncId` and does not expose entry count through the `SyncSessionPort` interface.

### Affected Areas
- `src/main/java/com/kuroneko/pymeflow/interfaces/web/` — add a provider sync REST controller and API DTO records without leaking infrastructure concerns.
- `src/main/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncUseCase.java` — likely extend command/report only if API-specific max page/page size overrides or status lookup are required.
- `src/main/java/com/kuroneko/pymeflow/application/port/out/SyncSessionPort.java` — currently insufficient for `GET by syncId`; would need a read model/lookup method if status inspection must be independent of trigger response.
- `src/main/java/com/kuroneko/pymeflow/infrastructure/provider/InMemorySyncSessionAdapter.java` — backing store can be extended to expose safe session snapshots, but state remains in-memory/non-durable for MVP.
- `src/main/java/com/kuroneko/pymeflow/infrastructure/config/ProviderAuthConfig.java` — current global `maxPages` and `pageSize` defaults exist; per-request overrides require validation and use-case support.
- `src/test/java/com/kuroneko/pymeflow/interfaces/web/` — add `@WebMvcTest` coverage for trigger/status endpoints, safe validation, and no secret echo.
- `src/test/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfigurationTest.java` — update smoke/wiring assertions only if new application services or ports are introduced.

### Approaches
1. **Synchronous trigger with report response, no independent status endpoint** — Add `POST /api/cashflow/provider-syncs` that validates request input, builds `ProviderAuth` from `providerType` + `credentialRef`, invokes `ProviderSyncUseCase.sync(...)`, and returns the existing report fields as the API response.
   - Pros: Smallest change; reuses existing use case and fixture-backed provider; avoids pretending the in-memory session is a durable job store.
   - Cons: Does not satisfy status inspection by `syncId`; response is not truly `accepted`/async because work completes in the request.
   - Effort: Low

2. **Synchronous trigger plus best-effort status read model** — Add `POST /api/cashflow/provider-syncs` returning a report with `syncId`, counts, errors, retry hints, cursor/hasMore/truncated state, plus `GET /api/cashflow/provider-syncs/{syncId}` backed by an application-level `ProviderSyncStatusUseCase` and a `SyncSessionPort` snapshot/lookup extension.
   - Pros: Meets the MVP goal to inspect result/status by `syncId`; keeps status access behind application ports; can explicitly document in-memory/fixture-backed limitations.
   - Cons: Requires extending session contracts because current `SyncSessionPort` is keyed by `(profileId, providerType)` and does not expose lookup by `syncId`, cursor, counts, or errors.
   - Effort: Medium

3. **Async job-style API** — Make `POST` return `202 Accepted` immediately, run sync in a background executor, and expose `GET /{syncId}` as job status.
   - Pros: Best long-term API semantics for slow external providers.
   - Cons: Overbuilds the fixture-only MVP; requires concurrency, lifecycle/error state, cancellation/idempotency decisions, and more test surface.
   - Effort: High

### Recommendation
Use Approach 2 for this change. Keep execution synchronous for MVP, but persist a safe in-memory status snapshot behind an application port so `GET /api/cashflow/provider-syncs/{syncId}` can return the last observed result. The trigger endpoint should return `200 OK` or `202 Accepted` only if the team intentionally names it accepted despite synchronous completion; the response should clearly include `syncId`, `profileId`, `providerType`, `pagesFetched`, `entriesFetched`, `importedEntries`, `hasMorePages`, `truncated`, `authAborted`, normalized provider errors, `retryAfterSeconds`, and cursor/session metadata that is safe to expose.

The request should accept only safe references: `profileId`, `providerType`, `credentialRef`, `dateFrom`, `dateTo`, and optional `pageSize`/`maxPages` if the use case is extended for request-level limits. No credential material should be accepted or echoed. If per-request paging overrides are included, enforce positive values and cap them against configured maximums rather than letting clients bypass `ProviderAuthConfig`.

### Risks
- Current `SyncSessionPort` cannot look up by `syncId`; adding status inspection requires a small contract extension or a new read port.
- Current `ProviderSyncReport` exposes `ProviderError` objects, but API DTOs should map them to safe strings/codes to avoid leaking internals and to keep JSON stable.
- `InMemorySyncSessionAdapter` is non-durable; status will reset on process restart and should be documented as fixture-MVP behavior.
- `ProviderSyncUseCase.ProviderSyncCommand` currently lacks date-window validation (`ProviderSyncQuery` validates inside the loop); API validation should reject invalid windows before invoking sync.
- Per-request `pageSize`/`maxPages` are not currently supported by the command; adding them increases implementation and testing scope.

### Ready for Proposal
Yes — propose a fixture-only synchronous Provider Sync API MVP with `POST /api/cashflow/provider-syncs` and `GET /api/cashflow/provider-syncs/{syncId}`, backed by a safe in-memory status read model. Keep real credentials, real bank APIs, UI, scheduling, and durable job orchestration out of scope.
