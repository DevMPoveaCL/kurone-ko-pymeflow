# Apply Progress: Provider Sync API MVP

## Scope Boundary

- Change: `pymeflow-provider-sync-api-mvp`
- Mode: Strict TDD
- Delivery path: `stacked-to-main`
- Current PR slice: PR #2 — web API only, based on completed PR #1 application/status snapshot.
- Explicitly out of scope for this slice: UI, real credential material, production bank dependencies, scheduling, webhooks, async jobs, and durable status storage.

## Completed Tasks

- [x] 1.1 Add failing tests in `ProviderSyncUseCaseTest` for final snapshot recording and safe error metadata.
- [x] 1.2 Add failing tests for `ProviderSyncStatusUseCase` covering found and unknown `syncId` status snapshots.
- [x] 1.3 Add failing tests for `InMemorySyncSessionAdapter` snapshot lookup, non-durable semantics, counts, cursor, and retry hints.
- [x] 2.1 Modify `SyncSessionPort` with `SyncSessionSnapshot`, `recordReport(...)`, and `findBySyncId(...)`.
- [x] 2.2 Create `ProviderSyncStatusUseCase` using only `SyncSessionPort`.
- [x] 2.3 Modify `ProviderSyncUseCase` to record the final safe snapshot after sync completion/failure.
- [x] 2.4 Modify `InMemorySyncSessionAdapter` to store concurrent snapshots by `syncId`.
- [x] 2.5 Modify `ApplicationServiceConfiguration` to wire `ProviderSyncStatusUseCase`.
- [x] 3.1 Create failing `CashflowProviderSyncControllerTest` for valid POST response with no `credentialRef` or secrets echoed.
- [x] 3.2 Add failing MVC tests for missing fields, invalid dates, unsupported provider, and sync not invoked on validation failure.
- [x] 3.3 Add failing MVC tests for GET found status, safe 404, and normalized provider errors.
- [x] 3.4 Create `CashflowProviderSyncController` with record DTOs, synchronous POST, GET, and safe error mapping.
- [x] 4.1 Refactor DTO mapping in `CashflowProviderSyncController.java` to avoid provider internals and keep `durability: "IN_MEMORY"` explicit.
- [x] 4.2 Run final verification for MVC, provider sync, architecture, full suite, and JaCoCo.
- [x] 4.3 Add focused `SyncSessionPort` record validation guard tests to resolve the pre-archive Strict TDD coverage warning.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 / 2.3 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncUseCaseTest.java` | Unit | ✅ Baseline provider sync tests passed before modifying existing files | ✅ Compile-failing tests referenced `SyncSessionSnapshot`, `recordReport`, and `entryCount` before implementation | ✅ `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest" --tests "*ProviderSyncStatusUseCaseTest" --tests "*InMemorySyncSessionAdapterTest" --tests "*ApplicationServiceConfigurationTest"` passed | ✅ Successful completion snapshot + rate-limit failure snapshot | ✅ Status calculation extracted to `statusFor(...)`; tests still passed |
| 1.2 / 2.2 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncStatusUseCaseTest.java` | Unit | N/A (new file) | ✅ Compile-failing tests referenced new `ProviderSyncStatusUseCase` before implementation | ✅ Focused status/application test command passed | ✅ Found, unknown, safe provider-error metadata, and blank-id validation cases | ➖ None needed |
| 1.3 / 2.1 / 2.4 | `src/test/java/com/kuroneko/pymeflow/infrastructure/provider/InMemorySyncSessionAdapterTest.java` | Unit | ✅ Baseline adapter tests passed before modifying existing files | ✅ Compile-failing tests referenced `recordReport(...)`, `findBySyncId(...)`, `SyncStatus`, and `Durability` before implementation | ✅ Focused status/application test command passed | ✅ Snapshot lookup with counts/cursor/retry hint + unknown/new-adapter non-durable behavior | ✅ Existing `entryCount(...)` promoted to public port method; tests still passed |
| 2.5 | `src/test/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfigurationTest.java` | Unit | ✅ Baseline configuration test included in focused GREEN run after RED compile failure | ✅ Compile-failing test referenced `providerSyncStatusUseCase(...)` bean method before implementation | ✅ Focused status/application test command passed | ➖ Single structural wiring assertion | ➖ None needed |
| 3.1 / 3.4 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncControllerTest.java` | Integration (`@WebMvcTest`) | N/A (new controller/test files) | ✅ Compile-failing test referenced non-existent `CashflowProviderSyncController` and DTO | ✅ `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncControllerTest"` passed | ✅ POST success plus provider error failure mapping and no credential echo | ✅ Mapping split into `fromReport`, `fromSnapshot`, and `fromProviderError`; tests still passed |
| 3.2 / 3.4 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncControllerTest.java` | Integration (`@WebMvcTest`) | N/A (new controller/test files) | ✅ Failing validation tests written before controller implementation | ✅ Focused controller test command passed | ✅ Missing fields, invalid ISO date, reversed range, unsupported provider, and no sync invocation cases | ✅ Validation helpers extracted; tests still passed |
| 3.3 / 4.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncControllerTest.java` | Integration (`@WebMvcTest`) | N/A (new controller/test files) | ✅ Failing GET/status and safe 404 tests written before controller implementation | ✅ Focused controller test command passed | ✅ Found snapshot, unknown non-durable status, rate-limit retry hint, and all safe provider error codes | ✅ Explicit `IN_MEMORY` durability retained in response and 404 DTO; tests still passed |
| 4.2 | Verification commands | Unit + Integration + Architecture | ✅ Full suite passed after implementation | N/A (verification task) | ✅ Required verification commands passed | ✅ Provider-focused, architecture, full suite, and coverage report commands executed | ➖ None needed |
| 4.3 | `src/test/java/com/kuroneko/pymeflow/application/port/out/SyncSessionPortTest.java` | Unit | ✅ `./gradlew.bat test --rerun-tasks --tests "*InMemorySyncSessionAdapterTest"` passed before adding coverage tests | ✅ Validation tests were added before any production change; no production change was needed because existing guards already enforce the expected behavior | ✅ `./gradlew.bat test --rerun-tasks --tests "*SyncSessionPortTest"` passed | ✅ 8 invalid snapshot cases: required fields + negative count guards | ➖ None needed |

## Test Summary

- Total tests written: 24 cumulative (8 PR #1 tests + 8 PR #2 controller/OpenAPI tests + 8 `SyncSessionPort` validation guard cases).
- Total tests passing: focused `SyncSessionPort`, provider/controller suite, full suite, and JaCoCo report all passed.
- Layers used: Unit (16), Integration/WebMvc (8), E2E (0).
- Approval tests: None — no behavior-preserving refactor-only task.
- Pure functions/helpers created: status/mapping helpers in application/web layers.

## Verification

- ✅ `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncControllerTest"`
- ✅ `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncControllerTest" --tests "*ProviderSync*Test"`
- ✅ `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"`
- ✅ `./gradlew.bat test --rerun-tasks`
- ✅ `./gradlew.bat jacocoTestReport`
- ✅ `./gradlew.bat test --rerun-tasks --tests "*InMemorySyncSessionAdapterTest"`
- ✅ `./gradlew.bat test --rerun-tasks --tests "*SyncSessionPortTest"`
- ✅ `./gradlew.bat test --rerun-tasks --tests "*ProviderSync*Test" --tests "*InMemorySyncSessionAdapterTest"`
- ✅ JaCoCo after warning fix: `SyncSessionPort.java` line coverage is 100% (0 missed / 30 covered lines); `SyncSessionSnapshot` line coverage is 100% (0 missed / 24 covered lines).
- Docker/Postgres was not needed; the full suite passed with the existing test configuration.

## Deviations

- The controller derives a fallback POST response from `ProviderSyncReport` if the just-recorded snapshot cannot be read by `syncId`; when available, it returns the same snapshot-based shape as GET. This preserves synchronous API behavior while avoiding a hard dependency on durable storage.

## Remaining Tasks

- None — PR #2 web API slice is complete and ready for verification/review.
