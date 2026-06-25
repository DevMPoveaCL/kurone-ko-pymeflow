# Design: Provider Sync Observability MVP

## Technical Approach

Add safe observability at the application boundary and keep framework-specific telemetry in infrastructure. `ProviderSyncUseCase.ProviderSyncService` will emit lifecycle events through a narrow application output port; an infrastructure adapter will convert those events into structured logs and Micrometer metrics. Actuator health/info will be storage-only and implemented in infrastructure against durable `provider_sync_sessions` reachability/capability, never external bank connectivity.

No delta spec exists yet for this change; this design maps to the proposal and the existing `cashflow-provider-sync` capability.

## Architecture Decisions

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Direct Micrometer/logger calls in `ProviderSyncService` | Smallest code, but leaks framework concerns into application orchestration. | Reject. Keep application free of Spring/Micrometer/logging APIs. |
| Use-case decorator only | Clean wiring, but cannot observe page progress without changing the service contract. | Reject for MVP because page progress is required. |
| Application observation port + infrastructure adapter | Small application change, supports start/page/final events, preserves hexagonal direction. | Choose. Add a safe `ProviderSyncObservationPort`. |
| Actuator via `SyncSessionPort` | Abstract, but expands an application port with actuator-specific needs. | Reject. Storage health is infrastructure-only. |
| Actuator via JDBC storage contributor | Direct durable storage check; no provider call; simple to test. | Choose. Use `JdbcTemplate` to validate `provider_sync_sessions` only. |

## Data Flow

```text
ProviderSyncService ──safe events──> ProviderSyncObservationPort
        │                                  │
        │                                  └─> infrastructure observability adapter
        │                                            ├─ structured logs
        │                                            └─ Micrometer meters
        └─ SyncSessionPort ──> JdbcSyncSessionAdapter ──> provider_sync_sessions

Actuator health/info ──> JDBC storage check ──> provider_sync_sessions
```

Normal lifecycle logs MUST include only safe fields such as `syncId`, `providerType`, `status`, counts, `hasMorePages`, `truncated`, `authAborted`, `errorCode`, and `retryAfterSeconds`. They MUST NOT include `credentialRef`, secrets, raw payloads, cursors, raw exception messages, or stack traces.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/com/kuroneko/pymeflow/application/port/out/ProviderSyncObservationPort.java` | Create | Pure application port with lifecycle event records and safe error/status enums. |
| `src/main/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncUseCase.java` | Modify | Inject observation port, retain existing constructor via no-op delegate, emit start/page/completion/failure events. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/observability/ProviderSyncObservabilityAdapter.java` | Create | Implements the port with structured SLF4J logs and Micrometer counters/timers using bounded tags only. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/observability/ProviderSyncStorageHealthIndicator.java` | Create | Actuator health contributor checking `provider_sync_sessions` reachability/capability only. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/observability/ProviderSyncInfoContributor.java` | Create | Adds safe provider-sync subsystem metadata; no credentials, payloads, cursors, or provider connectivity claims. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfiguration.java` | Modify | Wire the observation adapter and pass the port into `ProviderSyncService`. |
| `src/test/java/com/kuroneko/pymeflow/**` | Modify/Create | Add application, observability, actuator, and architecture regression tests. |

## Interfaces / Contracts

```java
public interface ProviderSyncObservationPort {
    void observe(ProviderSyncObservation event);
}
```

`ProviderSyncObservation` should be a Java 21 record with safe fields only: `syncId`, `providerType`, `status`, numeric counts, flags, `errorCode`, `retryAfterSeconds`, and optional duration. Metric tags are restricted to `providerType`, `status`, `errorCode`; `syncId` is log-only correlation, never a meter tag.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Event order and safe payloads from `ProviderSyncService`. | Mock/capturing `ProviderSyncObservationPort`; assert no `credentialRef`, cursor, raw messages. |
| Unit | Bounded metrics and sanitized logs adapter behavior. | `SimpleMeterRegistry`; assert only `providerType`, `status`, `errorCode` tags. |
| Integration | Storage-only actuator health/info. | H2 migrated/unmigrated JDBC tests; assert no external provider calls or misleading bank readiness wording. |
| Architecture | Hexagonal boundaries. | Extend ArchUnit to forbid Spring/Micrometer/logging dependencies in domain/application. |

## Migration / Rollout

No migration required. Existing `provider_sync_sessions` storage is reused. If implementation forecast exceeds 400 changed lines, split PRs as: (1) observation port + logs/metrics, (2) actuator health/info + tests.

## Open Questions

None.
