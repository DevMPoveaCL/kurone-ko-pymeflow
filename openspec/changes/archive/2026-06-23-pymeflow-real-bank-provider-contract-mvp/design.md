# Design: PymeFlow Real Bank Provider Contract MVP

## Technical Approach

Add a PULL-based provider contract one layer above the existing `ExternalStatementImportPort` anti-corruption boundary. A new `BankProviderPort` in `application/port/out/` fetches paginated statement pages using date windows and cursors. `ProviderSyncUseCase` in `application/cashflow/` orchestrates the loop: read sync cursor, fetch page, map to `ExternalStatementImportCommand`, delegate to `ExternalStatementImportPort`, update cursor, and aggregate a report. A `FakeBankProviderAdapter` in `infrastructure/provider/` loads JSON fixtures to prove mapping and pagination without real bank APIs. Java 21 records with compact constructors enforce validation; a sealed `ProviderError` hierarchy enables exhaustive error handling. All provider-specific literals remain in `infrastructure/provider/`.

## Architecture Decisions

| Decision | Options | Tradeoffs | Choice |
|---|---|---|---|
| Provider port name | `BankProviderPort` vs `StatementProviderPort` | "Bank" in name is a type identifier, not a string literal, so ArchUnit string scan is unaffected | `BankProviderPort` in `application/port/out/` |
| Error representation | Sealed interface vs runtime exceptions | Sealed types add type count but give compile-time exhaustiveness | Sealed `ProviderError` with `AuthError`, `RateLimitError`, `UnavailableError`, `DataError` |
| Sync session | `SyncSessionPort` + in-memory adapter vs no tracking | In-memory loses state on restart; no tracking loses cursor proof | `SyncSessionPort` interface with `InMemorySyncSessionAdapter` for MVP |
| Fixture format | JSON files vs Java code | JSON adds resource dependency but mimics real API payloads | JSON fixtures under `src/test/resources/fixtures/provider/` |
| Pagination safety | Max-page guard vs trust provider | Guard adds loop complexity but prevents infinite cursors | Max-page guard (e.g., 50 pages) inside `ProviderSyncUseCase` |
| Delivery | 3 chained PRs vs single PR | Chained adds overhead but stays under 400-line review budget | 3 chained PRs (contracts → adapter → use case) |

## Data Flow

```
ProviderSyncUseCase
       │
       ├─► SyncSessionPort (load cursor)
       │
       ├─► LOOP (pagination guard)
       │     │
       │     ├─► BankProviderPort.fetchStatements(query, auth)
       │     │       → FakeBankProviderAdapter (fixture JSON → ExternalStatementEntry)
       │     │
       │     ├─► ExternalStatementImportPort.importStatement(command)
       │     │       → SimulatedBankStatementAdapter → CashflowIngestionService
       │     │
       │     └─► SyncSessionPort (save next cursor)
       │
       └─► ProviderSyncReport (pages, entries, imported, errors)
```

The use case aggregates `CashflowIngestionResult` from each page into the final report. Provider errors are collected per page; auth failures abort the sync immediately.

## File Changes

| File | Action | Description |
|---|---|---|
| `application/port/out/BankProviderPort.java` | Create | `fetchStatements(query, auth) → ProviderSyncPage` |
| `application/port/out/ProviderSyncQuery.java` | Create | Date window, profile, cursor, page size record |
| `application/port/out/ProviderSyncPage.java` | Create | Entries, next cursor, rate limit hints record |
| `application/port/out/ProviderAuth.java` | Create | Safe auth descriptor (provider type, credential ref) |
| `application/port/out/ProviderError.java` | Create | Sealed error hierarchy |
| `application/port/out/SyncSessionPort.java` | Create | Read/write sync cursor port |
| `application/cashflow/ProviderSyncUseCase.java` | Create | Sync orchestration interface + nested command/report records |
| `infrastructure/provider/FakeBankProviderAdapter.java` | Create | Fixture-backed adapter implementing `BankProviderPort` |
| `infrastructure/provider/InMemorySyncSessionAdapter.java` | Create | In-memory `SyncSessionPort` implementation |
| `infrastructure/config/ProviderAuthConfig.java` | Create | `@ConfigurationProperties` for provider settings |
| `infrastructure/config/ApplicationServiceConfiguration.java` | Modify | Wire new ports, use case, and session adapter |
| `src/test/resources/fixtures/provider/*.json` | Create | Fixture pages for Santander and BancoEstado |
| `application/port/out/BankProviderPortContractTest.java` | Create | Abstract contract validation test |
| `infrastructure/provider/FakeBankProviderAdapterTest.java` | Create | Fixture loading, mapping, and pagination tests |
| `application/cashflow/ProviderSyncUseCaseTest.java` | Create | Mocked orchestration, error aggregation, guard tests |
| `architecture/ArchitectureTest.java` | Modify | Add rule blocking `domain/application` → `infrastructure.provider` |

## Interfaces / Contracts

```java
public interface BankProviderPort {
    ProviderSyncPage fetchStatements(ProviderSyncQuery query, ProviderAuth auth);
}

public record ProviderSyncQuery(ProfileId profileId, LocalDate dateFrom,
                                LocalDate dateTo, Optional<String> cursor, int pageSize) { ... }

public record ProviderAuth(String providerType, String credentialRef) { }

public sealed interface ProviderError
    permits ProviderError.AuthError, ProviderError.RateLimitError,
           ProviderError.UnavailableError, ProviderError.DataError { ... }
```

`ProviderSyncUseCase` consumes `ProviderSyncCommand` and returns `ProviderSyncReport`, delegating page mapping to the existing `ExternalStatementImportPort`. `SyncSessionPort` hides traceability and cursor persistence behind `syncId(profileId, providerType)`, `findCursor(profileId, providerType)`, and `saveCursor(profileId, providerType, cursor)` so resume state is isolated per provider for the same profile.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Port contract | Query validation, page immutability, auth safety, reusable provider-port contract base | JUnit 5 unit tests |
| Sealed errors | Exhaustive switch coverage, message safety | JUnit 5 unit tests |
| Fake adapter | Fixture JSON → `ExternalStatementEntry` mapping, cursor passthrough, CLP enforcement | JUnit 5 unit tests with classpath JSON |
| Use case | Pagination loop termination, error aggregation, max-page guard, sync session updates | Mockito-based unit tests |
| Architecture | No illegal cross-layer dependencies | ArchUnit (add `infrastructure.provider` rule) |

## Migration / Rollout

No migration required. New beans are additive; existing `ExternalStatementImportPort` and `SimulatedBankStatementAdapter` remain untouched.

## Open Questions

- [ ] Confirm REST sync trigger endpoint stays out of MVP scope (proposal excludes UI; exploration marks it optional).
