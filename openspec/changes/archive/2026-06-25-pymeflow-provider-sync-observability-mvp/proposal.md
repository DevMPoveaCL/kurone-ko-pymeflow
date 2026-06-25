# Proposal: Provider Sync Observability MVP

## Intent

Give operators minimal, safe telemetry before sandbox/real provider integration. The MVP observes fixture-backed provider sync lifecycle and durable subsystem health without UI, credentials, provider dependency, or false bank connectivity claims.

## Scope

### In Scope
- Safe structured logs for sync start, page progress, completion, and failure, correlated by `syncId` and excluding secrets, raw payloads, stack traces, cursors, and raw exception messages.
- Micrometer counters/timers with bounded tags only: `providerType`, `status`, `errorCode`; never `syncId`, `profileId`, or message text.
- Actuator health/info for provider sync subsystem validating durable session storage reachability and reporting safe metadata.
- Tests/spec direction for safety, bounded tags, and hexagonal boundaries.

### Out of Scope
- UI, list/audit API, manual retry, scheduled recovery, or operator actions.
- Real credentials, OAuth, sandbox/production provider connectivity, or vertical-specific behavior.
- Expanding public cashflow APIs beyond existing trigger/status behavior.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `cashflow-provider-sync`: add safe observability requirements for lifecycle logs, bounded metrics, and storage-only actuator health/info.

## Approach

Preserve hexagonal architecture: application code emits lifecycle observations through an application port or infrastructure decorator, while logging, Micrometer, and Actuator contributors live in infrastructure/configuration. Health checks MUST validate `provider_sync_sessions` storage reachability only, not external bank/provider availability.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncUseCase.java` | Modified | Emit lifecycle observation points without framework coupling. |
| `src/main/java/com/kuroneko/pymeflow/application/port/out` | Modified | Add narrow observability/storage-check ports if needed. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/config` | New/Modified | Wire logging, metrics, and actuator contributors. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/persistence/JdbcSyncSessionAdapter.java` | Modified | Support storage reachability check if required. |
| `src/test/java/com/kuroneko/pymeflow/**` | Modified | Protect safe fields, bounded tags, health/info semantics, ArchUnit boundaries. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| High-cardinality metrics | Med | Allow only `providerType`, `status`, `errorCode` tags. |
| Misleading health signal | Med | Name/details must state storage-only readiness. |
| Secret/raw data leakage | Med | Test forbidden fields and sanitize exception output. |
| Review workload exceeds 400 lines | Low/Med | Forecast as medium; split logging/metrics from actuator if design/tasks exceed budget. |

## Rollback Plan

Revert the observability change set or disable/remove infrastructure beans for metrics/logging/actuator contributors. Existing trigger/status APIs, durable sessions, and fixture sync behavior remain unchanged.

## Dependencies

- Existing Spring Boot Actuator/Micrometer and durable `provider_sync_sessions` storage.
- No external provider, credential, UI, or metrics backend dependency.

## Success Criteria

- [ ] Logs cover lifecycle outcomes and never include secrets, raw payloads, cursors, stack traces, or raw messages.
- [ ] Metrics use only bounded tags: `providerType`, `status`, `errorCode`.
- [ ] Actuator health/info reports provider-sync storage reachability without claiming bank/provider connectivity.
- [ ] Hexagonal boundaries remain enforced and review plan respects the 400-line budget.
