# Proposal: Provider Sync API MVP

## Intent

Expose the existing fixture-backed provider sync core through a small REST API so clients can trigger a synchronous sync and inspect the last safe in-memory status by `syncId`, without accepting credentials or depending on production banks.

## Scope

### In Scope
- Add `POST /api/cashflow/provider-syncs` using `ProviderSyncUseCase` with `profileId`, `providerType`, `credentialRef`, `dateFrom`, and `dateTo`.
- Add `GET /api/cashflow/provider-syncs/{syncId}` backed by a safe application read model/port extension.
- Return stable DTOs with `syncId`, counts, cursor/session metadata, normalized provider errors, and retry hints.
- Document fixture-only provider behavior and in-memory/non-durable status as MVP limitations.

### Out of Scope
- Real credential material, OAuth, token rotation, encryption, or secret echoing.
- Production bank APIs, UI, scheduling, webhooks, async job orchestration, or durable status storage.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `cashflow-provider-sync`: add public synchronous trigger and status inspection API for the existing fixture-backed provider sync capability.

## Approach

Add a Spring MVC controller in `interfaces.web` with Java record DTOs and request validation. The POST endpoint builds safe `ProviderAuth` from `providerType` + `credentialRef`, delegates to `ProviderSyncUseCase`, and maps the report to API DTOs. Add a small application-level status query use case or `SyncSessionPort` read extension for lookup by `syncId`; implement it in `InMemorySyncSessionAdapter` as a non-durable snapshot. Map provider errors to safe codes/messages/retry hints only.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/com/kuroneko/pymeflow/interfaces/web/` | New | Provider sync controller and DTO records. |
| `application/port/out/SyncSessionPort.java` | Modified | Safe status snapshot lookup by `syncId` if needed. |
| `infrastructure/provider/InMemorySyncSessionAdapter.java` | Modified | Store/read last safe in-memory status. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/` | New | `@WebMvcTest` coverage for trigger/status, validation, and no secret leakage. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Status appears durable although it is in-memory | Medium | Document MVP limitation and response semantics clearly. |
| Provider internals leak through errors | Medium | Map to explicit safe DTO fields and test no credential echo. |
| Port extension weakens hexagonal boundaries | Low | Keep lookup in application port/use case; infrastructure only implements it. |

## Rollback Plan

Revert the controller/DTOs, web tests, status read use case or port extension, and in-memory snapshot changes. Existing provider sync core remains behind `ProviderSyncUseCase`.

## Dependencies

- Existing `ProviderSyncUseCase`, `FakeBankProviderAdapter`, `SimulatedBankStatementAdapter`, and `InMemorySyncSessionAdapter`.

## Success Criteria

- [ ] POST triggers fixture-backed sync and returns safe report data.
- [ ] GET returns last in-memory status by `syncId` without sensitive data.
- [ ] Invalid requests and provider failures return safe DTOs with retry hints where applicable.
- [ ] Tests cover validation, no secret leakage, and non-durable status behavior.
