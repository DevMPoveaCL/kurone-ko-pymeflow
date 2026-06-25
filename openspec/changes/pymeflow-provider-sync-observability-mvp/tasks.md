# Tasks: Provider Sync Observability MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 520-750 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 observation port + safe logs + metrics + tests; PR 2 actuator health/info + tests/docs |
| Delivery strategy | auto-chain |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Emit safe lifecycle observations, logs, and metrics | PR 1 | Base `feat/provider-sync-observability-mvp`; includes application, adapter, wiring, unit and ArchUnit tests. |
| 2 | Expose storage-only actuator health/info | PR 2 | Base PR 1 branch; includes JDBC health/info, integration tests, and docs if needed. |

## Phase 1: RED Tests for Observation MVP

- [x] 1.1 Add failing cases in `src/test/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncUseCaseTest.java` for start/page/completion/failure events and sanitized payloads.
- [x] 1.2 Create failing `src/test/java/com/kuroneko/pymeflow/infrastructure/observability/ProviderSyncObservabilityAdapterTest.java` for bounded Micrometer tags and safe logs.
- [x] 1.3 Extend `src/test/java/com/kuroneko/pymeflow/architecture/ArchitectureTest.java` to reject Spring, Micrometer, and logging dependencies from application/domain.

## Phase 2: GREEN Observation Implementation

- [x] 2.1 Create `src/main/java/com/kuroneko/pymeflow/application/port/out/ProviderSyncObservationPort.java` with Java 21 records/enums for safe lifecycle events.
- [x] 2.2 Modify `src/main/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncUseCase.java` to emit observation events via the port and preserve a no-op default constructor path.
- [x] 2.3 Create `src/main/java/com/kuroneko/pymeflow/infrastructure/observability/ProviderSyncObservabilityAdapter.java` with structured safe logs and metrics tagged only by `providerType`, `status`, `errorCode`.
- [x] 2.4 Update `src/main/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfiguration.java` to inject `ProviderSyncObservationPort` into `ProviderSyncService`.

## Phase 3: RED Tests for Actuator Boundary

- [ ] 3.1 Create failing `src/test/java/com/kuroneko/pymeflow/infrastructure/observability/ProviderSyncStorageHealthIndicatorTest.java` for reachable/unreachable `provider_sync_sessions` health only.
- [ ] 3.2 Create failing `src/test/java/com/kuroneko/pymeflow/infrastructure/observability/ProviderSyncInfoContributorTest.java` for safe capability metadata and no provider connectivity claim.

## Phase 4: GREEN Actuator Implementation

- [ ] 4.1 Create `src/main/java/com/kuroneko/pymeflow/infrastructure/observability/ProviderSyncStorageHealthIndicator.java` using `JdbcTemplate` against `provider_sync_sessions` only.
- [ ] 4.2 Create `src/main/java/com/kuroneko/pymeflow/infrastructure/observability/ProviderSyncInfoContributor.java` with storage/capability metadata only.
- [ ] 4.3 Verify no UI, list/audit API, retry, credential, or real provider dependency is introduced.

## Phase 5: Verification / Refactor

- [ ] 5.1 Run `./gradlew.bat test --rerun-tasks` and keep tests with each work-unit commit.
- [ ] 5.2 Refactor names/package boundaries only after green tests; keep application free of infrastructure imports.
