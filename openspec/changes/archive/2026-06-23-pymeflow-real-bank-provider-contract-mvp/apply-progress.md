# Apply Progress: PymeFlow Real Bank Provider Contract MVP

## Status

| Field | Value |
|-------|-------|
| Current PR | PR #3 — Integration |
| Status | PR #1, PR #2, PR #3, and verification fix cycle complete |
| Scope boundary | Clean: provider contracts, fake adapter, in-memory session, sync use case, Spring wiring, architecture guard, and tests only |
| Artifact store | OpenSpec |
| Strict TDD | Active |
| Delivery strategy | auto-chain / feature-branch-chain |

## Completed Tasks

- [x] 1.1 Create `BankProviderPort`.
- [x] 1.2 Create `ProviderSyncQuery` with date/page validation.
- [x] 1.3 Create immutable `ProviderSyncPage`.
- [x] 1.4 Create safe `ProviderAuth` descriptor.
- [x] 1.5 Create sealed `ProviderError` taxonomy.
- [x] 1.6 Create `SyncSessionPort`.
- [x] 2.1 Create `BankProviderPortContractTest` query validation coverage.
- [x] 2.2 Cover immutable page entries and empty last-page cursor.
- [x] 2.3 Cover sealed provider error exhaustive switch and safe fields.
- [x] 2.4 Cover ProviderAuth validation.
- [x] 3.1 Create `FakeBankProviderAdapter` fixture-backed provider implementation.
- [x] 3.2 Create `InMemorySyncSessionAdapter`.
- [x] 3.3 Create `ProviderAuthConfig` with safe paging defaults.
- [x] 4.1 Create Santander fixture with signed CLP amounts and pagination cursor.
- [x] 4.2 Create BancoEstado fixture with counterparty metadata.
- [x] 4.3 Create fake adapter tests for mapping, CLP enforcement, missing fixture, and cursor passthrough.
- [x] 5.1 Create `ProviderSyncUseCase` and `ProviderSyncService` pagination/import/session/error orchestration.
- [x] 6.1 Wire provider sync beans in `ApplicationServiceConfiguration`.
- [x] 6.2 Add architecture guard blocking `domain/application` dependencies on `infrastructure.provider`.
- [x] 7.1 Cover single-page sync, multi-page cursor chain, and max-page guard.
- [x] 7.2 Cover auth abort, rate-limit retry hint, unavailable aggregation, and data error field/detail.
- [x] 7.3 Cover cursor resume and save after each page.
- [x] Verification fix: `UnavailableError` is collected without aborting recoverable sync flow; auth abort and rate-limit retry hint remain preserved.
- [x] Verification fix: `SyncSessionPort` now exposes `syncId` traceability and scopes cursor/session state by `profileId + providerType`.
- [x] Verification fix: `BankProviderPortContractTest` now uses a reusable abstract contract base with a concrete fixture implementation.

## TDD Cycle Evidence

| Task(s) | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---------|------------|-----|-------|-------------|----------|
| 1.1–1.6, 2.1–2.4 | Historical recovery from cancelled PR #1; no independent safety-net command was recorded before the recovered RED. | `BankProviderPortContractTest` was left by a cancelled sub-agent and failed compilation with 22 missing-symbol errors for the provider contracts. | Implemented the minimal contract types and reran `./gradlew.bat test --rerun-tasks --tests "*BankProviderPortContractTest"` successfully. | Contract tests covered query validation, immutable page entries, auth descriptors, and all sealed error variants. | Scope kept to application port contracts only; no adapter, use case, Spring wiring, REST endpoint, fixture, or infrastructure implementation added. |
| 3.1–3.3, 4.1–4.3 | Historical recovery from cancelled PR #2; focused adapter/config/session tests were executed before accepting the recovered implementation. | PR #2 implementation was left by a cancelled sub-agent and required audit before acceptance. Focused adapter/config/session tests existed and were executed to confirm behavior. | `ProviderAuthConfigTest`, `FakeBankProviderAdapterTest`, `InMemorySyncSessionAdapterTest`, and `BankProviderPortContractTest` pass. | Adapter tests covered mapped fixture rows, missing fixture, CLP rejection, cursor passthrough, and session cursor/count behavior. | Scope stayed adapter-only: no `ProviderSyncUseCase`, Spring wiring, REST endpoint, or PR #3 architecture guard added. |
| 5.1, 7.1–7.3 | PR #3 baseline was recovered from existing focused tests before verify; command evidence recorded below. | Added `ProviderSyncUseCaseTest` first. RED failed compilation because `ProviderSyncUseCase`, `ProviderSyncService`, `ProviderSyncCommand`, `ProviderSyncReport`, and `ProviderSyncException` did not exist. | Implemented the use case service and ran `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest" --tests "*ApplicationServiceConfigurationTest"` successfully. | Tests covered single-page, multi-page, max-page guard, auth abort, rate-limit retry hint, unavailable/data errors, resume cursor, and save after each page. | Generalized the loop for resume cursor, next cursor, max-page truncation, imported count aggregation, retry hint extraction, and provider-agnostic exception handling. |
| 6.1 | PR #3 baseline was recovered from existing focused tests before verify; command evidence recorded below. | Extended `ApplicationServiceConfigurationTest` before production wiring. RED failed compilation because `syncSessionPort()`, `providerSyncUseCase(...)`, and `bankProviderPort()` beans did not exist. | Added beans for `BankProviderPort → FakeBankProviderAdapter`, `SyncSessionPort → InMemorySyncSessionAdapter`, and `ProviderSyncUseCase.ProviderSyncService`; focused config tests pass. | Config tests exercised provider/session/use-case bean creation with concrete dependencies. | Reused existing `@ConfigurationPropertiesScan` from `PymeFlowApplication`; no duplicate scan annotation added. |
| 6.2 | PR #3 baseline was recovered from existing focused tests before verify; command evidence recorded below. | Added an ArchUnit guard in `ArchitectureTest` for `..domain..` and `..application..` not depending on `..infrastructure.provider..`. This structural guard has no intentional violation case. | `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest" --tests "*ArchitectureTest*"` passes. | Architecture suite includes multiple layer rules; this guard is structural and has no data-input triangulation path. | Kept application provider-agnostic; `FakeBankProviderAdapter.ProviderDataException` now extends the application-level `ProviderSyncException` so use-case aggregation works without importing infrastructure. |
| Verification fix cycle | `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest" --tests "*BankProviderPortContractTest" --tests "*InMemorySyncSessionAdapterTest"` passed before production changes. | Added failing tests first for provider-scoped session APIs, `syncId` in `ProviderSyncReport`, unavailable recovery, and repeated unavailable max-attempt guard. RED failed compilation on new `SyncSessionPort` signatures/`syncId`, then exposed the unavailable infinite-retry behavior. | Implemented provider-scoped `SyncSessionPort`, in-memory `SyncSessionKey`, stable generated `syncId`, report traceability, unavailable-only continuation, and max-attempt truncation; focused tests pass. | Added distinct cases: unavailable then recovered page, repeated unavailable until limit, provider A/B cursor isolation, and `syncId` exposure. | Refactored `BankProviderPortContractTest` into reusable abstract contract base plus concrete fixture implementation; no production dependency direction changed. |

## Verification Evidence

| Command | Result |
|---------|--------|
| `./gradlew.bat test --rerun-tasks --tests "*BankProviderPortContractTest"` | PASS (PR #1 evidence) |
| `./gradlew.bat test --rerun-tasks --tests "*ProviderAuthConfigTest" --tests "*FakeBankProviderAdapterTest" --tests "*InMemorySyncSessionAdapterTest" --tests "*BankProviderPortContractTest"` | PASS (PR #2 evidence) |
| `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` | PASS (PR #2 evidence) |
| `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest" --tests "*ApplicationServiceConfigurationTest" --tests "*ArchitectureTest*"` | PASS |
| `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest" --tests "*ArchitectureTest*"` | PASS |
| `docker compose up -d postgres` | PASS (`pymeflow-postgres` already running) |
| `./gradlew.bat test --rerun-tasks` | PASS |
| `./gradlew.bat jacocoTestReport` | PASS |
| `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest" --tests "*BankProviderPortContractTest" --tests "*InMemorySyncSessionAdapterTest"` | PASS (verification fix safety net) |
| `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest" --tests "*InMemorySyncSessionAdapterTest"` | RED (new provider-scoped session and `syncId` tests failed compilation before implementation) |
| `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest.repeatedUnavailableErrorsStopAtMaxAttemptsWithoutImporting"` | RED (timed out due existing unavailable infinite-retry behavior before max-attempt guard) |
| `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest" --tests "*InMemorySyncSessionAdapterTest" --tests "*BankProviderPortContractTest"` | PASS (verification fix GREEN) |
| `./gradlew.bat test --rerun-tasks --tests "*ProviderSyncUseCaseTest" --tests "*BankProviderPortContractTest" --tests "*FakeBankProviderAdapterTest" --tests "*InMemorySyncSessionAdapterTest"` | PASS |
| `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` | PASS |
| `docker compose up -d postgres` | PASS (`pymeflow-postgres` running) |
| `./gradlew.bat test --rerun-tasks` | PASS |
| `./gradlew.bat jacocoTestReport` | PASS |

## Remaining Tasks

- [x] None — all PR #1, PR #2, and PR #3 tasks are complete for this change.

## Risks

- The RED phase for PR #1 and PR #2 was recovered from earlier cancelled/partial work, as previously recorded.
- PR #3 intentionally remains provider-agnostic and fixture-backed; real bank OAuth/credential storage, production API dependencies, scheduling, REST endpoint, and UI remain out of scope.
- The sync session adapter is in-memory by design for MVP; cursor state is not durable across application restarts.
- Verification fix cycle changed the `SyncSessionPort` contract from profile-scoped to profile/provider-scoped; this is provider-agnostic but source-incompatible with old call sites.
