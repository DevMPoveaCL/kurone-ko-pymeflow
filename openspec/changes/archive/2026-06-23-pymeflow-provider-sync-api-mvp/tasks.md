# Tasks: Provider Sync API MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 350-500 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 application/status snapshot → PR 2 web API |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Status snapshot port/use cases | PR 1 | Base `main`; include adapter/use-case tests. |
| 2 | REST trigger/status API | PR 2 | Base after PR 1; include MVC validation/no-secret tests. |

## Phase 1: RED — Application Status Snapshot

- [x] 1.1 Add failing tests in `src/test/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncUseCaseTest.java` for final snapshot recording and safe error metadata.
- [x] 1.2 Add failing tests for `ProviderSyncStatusUseCase` covering found and unknown `syncId` status snapshots.
- [x] 1.3 Add failing tests for `InMemorySyncSessionAdapter` snapshot lookup, non-durable semantics, counts, cursor, and retry hints.

## Phase 2: GREEN — Application and Adapter

- [x] 2.1 Modify `src/main/java/com/kuroneko/pymeflow/application/port/out/SyncSessionPort.java` with `SyncSessionSnapshot`, `recordReport(...)`, and `findBySyncId(...)`.
- [x] 2.2 Create `src/main/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncStatusUseCase.java` using only `SyncSessionPort`.
- [x] 2.3 Modify `src/main/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncUseCase.java` to record the final safe snapshot after sync completion/failure.
- [x] 2.4 Modify `src/main/java/com/kuroneko/pymeflow/infrastructure/provider/InMemorySyncSessionAdapter.java` to store concurrent snapshots by `syncId`.
- [x] 2.5 Modify `src/main/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfiguration.java` to wire `ProviderSyncStatusUseCase`.

## Phase 3: RED/GREEN — Web API

- [x] 3.1 Create failing `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncControllerTest.java` for valid POST response with no `credentialRef` or secrets echoed.
- [x] 3.2 Add failing MVC tests for missing fields, invalid dates, unsupported provider, and sync not invoked on validation failure.
- [x] 3.3 Add failing MVC tests for GET found status, safe 404 for unknown/expired `syncId`, and normalized provider errors.
- [x] 3.4 Create `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncController.java` with record DTOs, synchronous POST, GET, and safe error mapping.

## Phase 4: REFACTOR / Verification

- [x] 4.1 Refactor DTO mapping in `CashflowProviderSyncController.java` to avoid provider internals and keep `durability: "IN_MEMORY"` explicit.
- [x] 4.2 Run `./gradlew.bat test --rerun-tasks` and fix ArchUnit, MVC, and application test failures.
