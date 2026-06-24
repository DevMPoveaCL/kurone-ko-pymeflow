# Tasks: PymeFlow Real Bank Provider Contract MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 580–680 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Provider contracts, error taxonomy, port interfaces, contract tests | PR 1 | Base: `feature/pymeflow-provider-contract-mvp`; ~205 lines |
| 2 | Fake adapter, in-memory session, fixture resources, adapter tests | PR 2 | Base: PR 1 branch; ~250 lines |
| 3 | Sync use case, Spring wiring, architecture guard, use case tests | PR 3 | Base: PR 2 branch; ~210 lines |

## Phase 1: Provider Contracts (PR 1)

- [x] 1.1 Create `application/port/out/BankProviderPort.java` — single-method interface: `ProviderSyncPage fetchStatements(ProviderSyncQuery, ProviderAuth)`
- [x] 1.2 Create `application/port/out/ProviderSyncQuery.java` — record with compact constructor validating non-null profileId/dates, dateFrom ≤ dateTo, pageSize > 0
- [x] 1.3 Create `application/port/out/ProviderSyncPage.java` — record: immutable `List<ExternalStatementEntry> entries`, `Optional<String> nextCursor`, `Optional<Instant> rateLimitResetsAt`
- [x] 1.4 Create `application/port/out/ProviderAuth.java` — record: `String providerType`, `String credentialRef` with non-null validation
- [x] 1.5 Create `application/port/out/ProviderError.java` — sealed interface permitting `AuthError`, `RateLimitError`(retryAfterSeconds), `UnavailableError`, `DataError`(field, detail) records
- [x] 1.6 Create `application/port/out/SyncSessionPort.java` — interface: `findCursor(ProfileId)`, `saveCursor(ProfileId, String)`, `lastSyncAt(ProfileId)`, `incrementEntryCount(ProfileId, int)`

## Phase 2: Contract Tests (PR 1)

- [x] 2.1 Create `application/port/out/BankProviderPortContractTest.java` — test query rejects null profileId/dateFrom/dateTo and dateFrom > dateTo (Spec: Invalid date window, Missing required fields)
- [x] 2.2 Add test: ProviderSyncPage entries list is immutable, nextCursor empty when last page
- [x] 2.3 Add test: sealed ProviderError exhaustive switch compiles, each subtype carries correct fields, message is safe (Spec: Auth/RateLimit/Unavailable/Data)
- [x] 2.4 Add test: ProviderAuth rejects null providerType

## Phase 3: Adapter Implementation (PR 2)

- [x] 3.1 Create `infrastructure/provider/FakeBankProviderAdapter.java` — implements BankProviderPort: load JSON fixtures from classpath, map signed amounts to `TransactionDirection`, enforce CLP-only, pass-through pagination cursors
- [x] 3.2 Create `infrastructure/provider/InMemorySyncSessionAdapter.java` — implements SyncSessionPort with `ConcurrentHashMap<ProfileId, SyncSessionState>` record
- [x] 3.3 Create `infrastructure/config/ProviderAuthConfig.java` — `@ConfigurationProperties(prefix = "provider")` record with maxPages, pageSize defaults

## Phase 4: Adapter Tests + Fixtures (PR 2)

- [x] 4.1 Create `src/test/resources/fixtures/provider/santander-page-1.json` — CLP entries with signed amounts and pagination cursor
- [x] 4.2 Create `src/test/resources/fixtures/provider/bancoestado-page-1.json` — CLP entries with counterparty metadata
- [x] 4.3 Create `infrastructure/provider/FakeBankProviderAdapterTest.java` — test fixture loading maps direction from sign, CLP-only rejects non-CLP with DataError, missing fixture returns empty page, cursor passthrough (Spec: Fixture mapped, Missing fixture, CLP-only enforcement)

## Phase 5: Use Case Implementation (PR 3)

- [x] 5.1 Create `application/cashflow/ProviderSyncUseCase.java` — interface `sync(ProviderSyncCommand) → ProviderSyncReport` with `ProviderSyncService` implementation: pagination loop with max-page guard, error aggregation (auth aborts, others collected), delegate to `ExternalStatementImportPort.importStatement()`, update session after each page

## Phase 6: Wiring + Architecture Guard (PR 3)

- [x] 6.1 Modify `infrastructure/config/ApplicationServiceConfiguration.java` — add `@Bean` for `ProviderSyncService` wiring `BankProviderPort`, `SyncSessionPort`, `ExternalStatementImportPort`; add `@Bean` for `SyncSessionPort → InMemorySyncSessionAdapter` and `@ConfigurationPropertiesScan` if missing
- [x] 6.2 Modify `architecture/ArchitectureTest.java` — add explicit rule: `..domain..` and `..application..` must not depend on `..infrastructure.provider..`

## Phase 7: Use Case Tests (PR 3)

- [x] 7.1 Create `application/cashflow/ProviderSyncUseCaseTest.java` — Mockito test single-page sync imports all entries, multi-page sync follows cursor chain, max-page guard stops at configured limit (Spec: Single-page, Multi-page, Max page guard)
- [x] 7.2 Add test: AuthError aborts sync and records error; RateLimitError surfaces retryAfterSeconds in report; UnavailableError collects and continues; DataError maps field/detail (Spec: Auth failure, Rate limit, Unavailable, Data error)
- [x] 7.3 Add test: session cursor saved after each page, previous cursor used for resume (Spec: Session updated, Session available for resume)
