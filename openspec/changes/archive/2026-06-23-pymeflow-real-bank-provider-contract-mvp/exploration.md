# Exploration: PymeFlow Real Bank Provider Contract MVP

## Current State

### What Exists Today

The system has a **proven anti-corruption boundary** for bank statement imports, built in the `pymeflow-simulated-bank-statement-adapter-mvp` change. The architecture is:

```
interfaces/web/CashflowBankStatementSimulatedController  ← PUSH endpoint (user submits rows)
       │
       ▼
application/port/out/ExternalStatementImportPort         ← Anti-corruption port (bank-agnostic)
       │
       ▼
infrastructure/bank/SimulatedBankStatementAdapter        ← Maps ExternalStatementEntry → ingestion items
       │
       ▼
application/cashflow/CashflowIngestionService            ← Idempotency, categorization, persistence
```

**Key artifacts already in place:**

| Artifact | Location | Status |
|----------|----------|--------|
| `ExternalStatementImportPort` | `application/port/out/` | ✅ Implements `importStatement(command)` → `CashflowIngestionResult` |
| `ExternalStatementEntry` | `application/port/out/` | ✅ Record with `externalReference`, `date`, `description`, `amount`, `currency`, `direction`, `counterpartyName`, `accountAlias` |
| `ExternalStatementImportCommand` | `application/port/out/` | ✅ Record with `profileId`, `importLabel`, `entries` |
| `SimulatedBankStatementAdapter` | `infrastructure/bank/` | ✅ Handles signed→positive mapping, counterparty enrichment, CLP guard |
| `CashflowBankStatementSimulatedController` | `interfaces/web/` | ✅ REST endpoint with per-row validation, row traceability |
| `Transaction.direction()` | `domain/cashflow/` | ✅ `TransactionDirection` (DEBIT/CREDIT) already in domain model |
| Adapter unit tests | `infrastructure/bank/` | ✅ 7 tests covering mapping, CLP rejection, delegation |
| Controller WebMvcTest | `interfaces/web/` | ✅ 493-line test with mixed batch, re-import, error echo protection |
| ArchUnit rules | `architecture/` | ✅ Enforces no `infrastructure.bank` dependency from domain/application |

### The Gap — What's Missing

The simulated adapter is **PUSH-only**: the controller receives rows from the user. A **real bank integration** requires **PULL**: the system fetches statements from a bank API proactively. The current architecture has:

- ❌ No **provider contract** (how to fetch from an external bank)
- ❌ No **auth abstraction** (API keys, tokens, credentials)
- ❌ No **pagination** handling (bank APIs return paginated results)
- ❌ No **rate limiting** awareness (providers impose limits)
- ❌ No **sync session** tracking (what date range was last synced, cursor state)
- ❌ No **normalized provider errors** (auth failures, timeouts, rate limits, data format errors)
- ❌ No **date-window-based** fetching (banks expose statements by date range)

### Domain Model — What Changed Since Simulated MVP

Since the simulated adapter, the `pymeflow-cashflow-direction-hardening` change upgraded the domain:

| Concept | Before | After |
|---------|--------|-------|
| `Transaction` | 4-param constructor (no direction); defaulted to CREDIT | 5-param constructor; requires explicit `TransactionDirection` |
| `CashflowMovementDraft` | Legacy constructor without direction | Now requires `TransactionDirection` |
| `ExternalStatementEntry` | No `direction` field | Now has `direction`, `counterpartyName`, `accountAlias` |
| Adapter mapping | `amount.abs()` only (direction lost) | `amount.abs()` + `directionFor(signedAmount)` (direction preserved) |

### Architecture Boundaries (Hexagonal — ArchUnit Enforced)

```
domain/                    ← PURE: Transaction, TransactionDirection, ProfileId, CategoryAssignment
                              NO framework annotations, NO infrastructure imports
                              
application/               ← ORCHESTRATION: CashflowIngestionService, VerticalProfileService
  port/out/                ← PORT INTERFACES: ExternalStatementImportPort, SettlementFeedPort,
                              CashflowCategorizationPort, CashflowMovementHistoryPort
                              Bank/provider literals FORBIDDEN (ArchUnit string scan)
                              
infrastructure/            ← ADAPTERS: Jdbc adapters, profile config, mock adapters
  bank/                    ← EXISTING: SimulatedBankStatementAdapter
  mock/                    ← EXISTING: MockBankSettlementAdapter, MockAcquirerSettlementAdapter
  config/                  ← Bean wiring: ApplicationServiceConfiguration

interfaces/web/            ← REST: Controllers, DTOs
                              Bank literals ALLOWED here
```

**ArchUnit anti-corruption rules (from `ArchitectureTest`):**
1. Domain must not depend on Spring, JPA, Jackson, infrastructure, or interfaces
2. Application must not depend on infrastructure or interfaces
3. Domain + application must not depend on `..infrastructure.bank..`
4. Domain + application string literals must not contain: `banco`, `bank`, `acquirer`, `adquirente`, `getnet`, `tuu`, `transbank`, `pharmacy-cl`, `farmacia`, `pharmacy`

### Existing Provider Pattern (SettlementFeedPort)

The `SettlementFeedPort` is the closest prior art for a PULL-based provider abstraction:

```java
// application/port/out/SettlementFeedPort.java
public interface SettlementFeedPort {
    SettlementFeed fetchSettlements(TenantId tenantId, LocalDate from, LocalDate to);
    
    record SettlementFeed(TenantId tenantId, List<SettlementEntry> entries) { ... }
    record SettlementEntry(String source, LocalDate settledAt, BigDecimal grossAmount, ...) { ... }
}

// infrastructure/mock/MockBankSettlementAdapter.java
@Component @Profile("mock-bank")
public class MockBankSettlementAdapter implements SettlementFeedPort { ... }
```

This pattern demonstrates: port in `application/port/out/`, mock adapter in `infrastructure/mock/`, Spring-only annotations in infrastructure. However, `SettlementFeedPort` is domain-specific (settlements, not statements) and lacks auth, pagination, error handling, or sync state.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `application/port/out/BankProviderPort.java` | **New** | Provider contract port for PULL-based statement fetching |
| `application/port/out/ProviderSyncQuery.java` | **New** | Query record: date window, profile, pagination cursor |
| `application/port/out/ProviderAuth.java` | **New** | Auth abstraction record (safe, no real secrets in MVP) |
| `application/port/out/ProviderSyncResult.java` | **New** | Result record: entries, next cursor, rate limit info |
| `application/port/out/ProviderError.java` | **New** | Sealed type hierarchy for normalized provider errors |
| `application/cashflow/ProviderSyncUseCase.java` | **New** | Application use case: orchestrates fetch → import → report |
| `infrastructure/provider/FakeBankProviderAdapter.java` | **New** | Fixture-based fake adapter proving the contract |
| `infrastructure/provider/ProviderAuthConfig.java` | **New** | `@ConfigurationProperties` for provider config |
| `infrastructure/config/ApplicationServiceConfiguration.java` | **Modify** | Bean wiring for new port + use case |
| `application/port/out/ExternalStatementImportPort.java` | **Unchanged** | Anti-corruption boundary stays as-is — receives already-mapped entries |
| `infrastructure/bank/SimulatedBankStatementAdapter.java` | **Unchanged** | Existing adapter stays — real provider maps TO this |
| `interfaces/web/CashflowBankStatementSimulatedController.java` | **Unchanged** | Existing PUSH endpoint stays; new REST endpoint optional for MVP |
| `domain/` | **Unchanged** | No domain changes needed |
| New tests | **New** | Provider contract tests, fake adapter tests, use case tests |

## Approaches

### 1. Minimal Provider Port Only (Contracts-First)

Add only the `BankProviderPort` interface + record types. No use case service, no fake adapter implementation beyond what's needed for tests. The port defines the contract; implementation comes later.

**Architecture:**
```
application/port/out/
  BankProviderPort.fetchStatements(ProviderSyncQuery, ProviderAuth) → List<ExternalStatementEntry>
  ProviderSyncQuery { profileId, dateFrom, dateTo, cursor, pageSize }
  ProviderAuth { providerType, credentialHint }  ← safe, no real secrets
  ProviderSyncResult { entries, nextCursor, rateLimitRemaining, syncId }
  
infrastructure/provider/
  FakeBankProviderAdapter (loads fixtures from JSON files)
```

**Pros:**
- Minimal surface (~200 lines) — pure contract definition
- No orchestration complexity — just the interface
- Can be validated with contract tests immediately
- Clear separation: `BankProviderPort` (PULL from bank) vs `ExternalStatementImportPort` (PUSH to ingestion)
- ArchUnit-compliant: port uses bank-agnostic type names, bank literals in `infrastructure/provider/`

**Cons:**
- No sync orchestration — controller or caller must chain fetch+import manually
- No sync session tracking — caller must manage cursors
- No provider error taxonomy — errors are ad-hoc exceptions
- The "fake adapter" with JSON fixtures is a TEST artifact, not a real infrastructure adapter
- Doesn't prove the end-to-end PULL flow

**Effort:** Low (~250 lines, single PR)

---

### 2. Provider Port + Sync Use Case + Error Taxonomy (RECOMMENDED)

Add `BankProviderPort` with normalized errors, a `ProviderSyncUseCase` application service that orchestrates fetch → import → report, and a fake adapter with fixture files. Sync session tracking via a lightweight port (even if in-memory for MVP).

**Architecture:**
```
┌─────────────────────────────────────────────────────────────────┐
│ interfaces/web/ (NEW endpoint, optional for MVP)                 │
│   ProviderSyncController                                         │
│     POST /api/cashflow/providers/{provider}/sync                 │
│       { "profileId", "dateFrom", "dateTo", "pageSize": 50 }      │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│ application/cashflow/ProviderSyncUseCase.java                    │
│   1. Load ProviderAuth for profile + provider                    │
│   2. Build ProviderSyncQuery (date window, cursor from session)  │
│   3. bankProviderPort.fetchStatements(query, auth)               │
│      → catches ProviderError subclasses, normalizes              │
│   4. For each page:                                              │
│      a. externalStatementImportPort.importStatement(command)     │
│      b. Collect results                                          │
│   5. Update sync session (cursor, lastSyncAt)                    │
│   6. Return ProviderSyncReport                                   │
└───────────────┬─────────────────────────────────────────────────┘
                │
        ┌───────┴───────────┐
        ▼                   ▼
┌───────────────┐   ┌───────────────────────────┐
│ BankProvider  │   │ ExternalStatementImport   │
│ Port (NEW)    │   │ Port (EXISTING)           │
│ PULL from     │   │ PUSH to ingestion         │
│ bank API      │   │ Anti-corruption layer     │
└───────┬───────┘   └───────────┬───────────────┘
        │                       │
        ▼                       ▼
┌───────────────┐   ┌───────────────────────────┐
│ FakeProvider  │   │ SimulatedBankStatement    │
│ Adapter       │   │ Adapter (EXISTING)        │
│ (fixture-     │   │ infrastructure/bank/      │
│  based)       │   │                           │
└───────────────┘   └───────────────────────────┘
```

**Port contracts (Java 21 — sealed types + records):**

```java
// application/port/out/BankProviderPort.java
public interface BankProviderPort {
    ProviderSyncPage fetchStatements(ProviderSyncQuery query, ProviderAuth auth)
        throws ProviderAuthException, ProviderRateLimitException, 
               ProviderUnavailableException, ProviderDataException;
}

public record ProviderSyncQuery(
    ProfileId profileId,
    LocalDate dateFrom,
    LocalDate dateTo,
    Optional<String> cursor,    // pagination token
    int pageSize
) {
    public ProviderSyncQuery {
        if (profileId == null) throw new IllegalArgumentException("Profile id is required");
        if (dateFrom == null) throw new IllegalArgumentException("Date from is required");
        if (dateTo == null) throw new IllegalArgumentException("Date to is required");
        if (dateFrom.isAfter(dateTo)) throw new IllegalArgumentException("Date from must be before date to");
        if (pageSize <= 0) throw new IllegalArgumentException("Page size must be positive");
        cursor = (cursor == null) ? Optional.empty() : cursor;
    }
}

public record ProviderSyncPage(
    List<ExternalStatementEntry> entries,   // Already anti-corruption-mapped
    Optional<String> nextCursor,            // Null if last page
    int totalPagesEstimate,                 // Provider hint, may be approximate
    Optional<Instant> rateLimitResetsAt
) {
    public ProviderSyncPage {
        entries = List.copyOf(entries == null ? List.of() : entries);
    }
}

// ProviderAuth — safe, no real secrets for MVP
public record ProviderAuth(
    String providerType,     // "santander", "banco_estado", "fintoc"
    String credentialRef     // Reference to config, not the secret itself
) { }

// Provider errors — sealed hierarchy (Java 21)
public sealed interface ProviderError 
    permits ProviderError.AuthError, 
           ProviderError.RateLimitError,
           ProviderError.UnavailableError,
           ProviderError.DataError {
    
    record AuthError(String provider, String reason) implements ProviderError { }
    record RateLimitError(String provider, long retryAfterSeconds) implements ProviderError { }
    record UnavailableError(String provider, int httpStatus, String message) implements ProviderError { }
    record DataError(String provider, String field, String detail) implements ProviderError { }
}
```

**Application use case contract:**
```java
// application/cashflow/ProviderSyncUseCase.java
public interface ProviderSyncUseCase {
    ProviderSyncReport sync(ProviderSyncCommand command);
}

public record ProviderSyncCommand(ProfileId profileId, String provider, LocalDate dateFrom, LocalDate dateTo) { }

public record ProviderSyncReport(
    UUID syncId,
    ProfileId profileId,
    String provider,
    int pagesFetched,
    int totalEntries,
    int imported,
    int duplicates,
    int rejected,
    List<ProviderError> providerErrors,
    boolean hasMorePages
) { }
```

**Testing strategy (fixture-based contract tests):**

| Layer | Test | Approach |
|-------|------|----------|
| Port contract | `BankProviderPortContractTest` | Abstract test class — each adapter implementation runs the same suite |
| Fake adapter | `FakeBankProviderAdapterTest` | Loads JSON fixture files, verifies mapping to `ExternalStatementEntry` |
| Provider error | `ProviderErrorTest` | Sealed type exhaustiveness, serialization, error message safety |
| Use case | `ProviderSyncUseCaseTest` | Mock provider port + mock import port, verify orchestration |
| Integration | `ProviderSyncControllerTest` (optional) | `@WebMvcTest` with mocked ports |

**Fixture file example** (`src/test/resources/fixtures/provider/santander-page-1.json`):
```json
{
  "provider": "santander",
  "page": 1,
  "nextCursor": "cursor-abc-123",
  "entries": [
    {
      "transactionId": "SANT-20260601-001",
      "bookingDate": "2026-06-01",
      "valueDate": "2026-06-02",
      "description": "Transferencia recibida",
      "amount": 450000,
      "currency": "CLP",
      "accountNumber": "001-1234567-89",
      "counterparty": {
        "name": "Farmacia Cruz Verde",
        "rut": "96.951.530-7"
      },
      "type": "CREDITO"
    }
  ]
}
```

**Pros:**
- Complete orchestration — the use case handles pagination loops, error aggregation, sync state
- Normalized errors as sealed types — callers can `switch` exhaustively
- Fixture-based tests validate the provider contract without real bank APIs
- Sync session tracking proves the full PULL lifecycle
- Clean separation: `BankProviderPort` (fetch) + `ExternalStatementImportPort` (ingest) + `ProviderSyncUseCase` (orchestrate)
- Scales to multiple providers: add `SantanderProviderAdapter`, `BancoEstadoProviderAdapter`, `FintocProviderAdapter` behind the same port
- ArchUnit-compliant: port uses `ExternalStatementEntry` (bank-agnostic), provider names in `infrastructure/provider/`
- Review size manageable (~600 lines across 2-3 chained PRs)

**Cons:**
- More files than Approach 1 (use case + sync session + error types)
- `ProviderSyncUseCase` adds orchestration that could live in the controller for MVP
- The fake adapter loads fixtures — adds a test resource dependency
- Sync session tracking minimal for MVP — in-memory only, no DB table yet
- No real REST endpoint unless specifically included (optional for MVP)

**Effort:** Medium (~600 lines, 2-3 chained PRs)

---

### 3. Full Provider Abstraction (BankProviderPort + Multiple Stubs + OAuth Flow)

Design the complete bank provider abstraction with OAuth token management, retry policies, rate limit backoff, sync session persistence, and stub adapters for Santander, BancoEstado, and Fintoc. This would be the production-ready abstraction from day one.

**Pros:**
- Full future-proofing — real bank adapters plug in directly
- Most complete error handling and resilience
- Clear path to production with multiple Chilean banks

**Cons:**
- **Significantly over-engineered** for MVP — the goal is contract validation, not production readiness
- Real bank APIs (Santander, BancoEstado) are undocumented or require commercial agreements
- OAuth flow design without real endpoints is speculative
- ~1200+ lines requiring 4+ chained PRs
- Violates YAGNI — the provider contract can evolve incrementally
- The simulated adapter already proved the anti-corruption boundary; the provider contract should be validated, not perfected

**Effort:** High (~1200 lines)

**Recommendation:** Reject for MVP. Same rationale as the original simulated adapter exploration: build incrementally, validate the contract with one fake adapter, evolve when the first real bank arrives.

---

## Recommendation

**Approach 2 — Provider Port + Sync Use Case + Error Taxonomy** — is the right choice for this MVP:

1. **Builds directly on proven work**: The `ExternalStatementImportPort` + `SimulatedBankStatementAdapter` anti-corruption boundary is already tested and deployed. This change adds the provider contract ONE LAYER ABOVE it, handling PULL concerns (auth, pagination, errors, sync state).

2. **Clean architectural layering**: 
   - `BankProviderPort` (PULL) → fetches from provider, returns already-mapped `ExternalStatementEntry`
   - `ExternalStatementImportPort` (PUSH) → ingests mapped entries into the system
   - `ProviderSyncUseCase` → orchestrates the two, handles pagination loops and error aggregation
   - Each layer has a single responsibility.

3. **Validates the contract with fixtures**: The fake adapter loads realistic bank-like JSON fixtures, exercises the `BankProviderPort` contract, and proves the mapping to `ExternalStatementEntry` works. When a real bank arrives, the contract is already proven.

4. **Normalized errors with Java 21 sealed types**: `ProviderError` is exhaustively matchable, prevents unhandled error cases at compile time, and maps provider-specific errors to canonical categories.

5. **Sync session tracking positions the system for production**: Even if MVP uses in-memory sync state, the `SyncSessionPort` abstraction can be swapped for a JDBC adapter later without touching the use case.

6. **ArchUnit-compliant**: The port interface lives in `application/port/out/` with bank-agnostic types. Provider names and bank literals are confined to `infrastructure/provider/`. String literal scan passes unchanged.

7. **Scalable to multiple banks**: Add `SantanderProviderAdapter implements BankProviderPort`, `FintocProviderAdapter implements BankProviderPort` — zero changes to the use case or anti-corruption layer.

### MVP Scope Boundaries

| In MVP Scope | Out of MVP Scope (Future) |
|---|---|
| `BankProviderPort` interface + record types | Real HTTP client calls to bank APIs |
| `ProviderError` sealed hierarchy | OAuth token rotation and refresh |
| `ProviderSyncUseCase` orchestration | Production rate limiting / backoff policy |
| `FakeBankProviderAdapter` with JSON fixtures | Multi-currency (CLP only) |
| `ProviderAuth` safe abstraction (credential ref, no secrets) | Webhook-based bank data push |
| `SyncSessionPort` (in-memory implementation) | Real sync session persistence (JDBC) |
| Fixture-based contract tests | Integration tests with real bank sandboxes |
| Optional REST endpoint for sync trigger | Scheduled/cron-based auto-sync |

### Implementation Sketch

```
1. PR1: Provider Contracts (~220 lines)
   - BankProviderPort with fetchStatements()
   - ProviderSyncQuery, ProviderSyncPage records
   - ProviderAuth record
   - ProviderError sealed hierarchy
   - ExternalStatementEntryContractsTest (extends existing pattern, verifies new port doesn't introduce bank literals)
   
2. PR2: Fake Adapter + Fixtures (~200 lines)
   - FakeBankProviderAdapter (loads JSON fixtures, maps to ExternalStatementEntry)
   - Fixture files: santander-page-1.json, santander-page-2.json, banco-estado-page-1.json
   - BankProviderPortContractTest (abstract test class)
   - FakeBankProviderAdapterTest (fixture-based)
   - ProviderErrorTest (sealed type exhaustiveness)

3. PR3: Sync Use Case + Wiring (~250 lines)
   - ProviderSyncUseCase interface + implementation
   - SyncSessionPort (in-memory for MVP)
   - ProviderAuthConfig (@ConfigurationProperties)
   - ApplicationServiceConfiguration wiring
   - ProviderSyncUseCaseTest (mocked ports)
   - Optional: ProviderSyncController + WebMvcTest
```

### PULL Flow Sequence (Real Bank Scenario)

```
User/Controller triggers sync for profile "pharmacy-cl", provider "santander"
       │
       ▼
ProviderSyncUseCase.sync(command)
       │
       ├─► Load ProviderAuth from config (credentialRef)
       │
       ├─► Load last sync cursor from SyncSessionPort
       │
       ├─► LOOP (pagination):
       │     │
       │     ├─► bankProviderPort.fetchStatements(query, auth)
       │     │     → FakeBankProviderAdapter loads fixture page
       │     │     → Maps Santander JSON → ExternalStatementEntry list
       │     │     → Returns ProviderSyncPage (entries, nextCursor, rateLimitResetsAt)
       │     │
       │     ├─► On ProviderRateLimitException: wait, retry (MVP: surface to caller)
       │     ├─► On ProviderAuthException: abort sync
       │     ├─► On ProviderUnavailableException: collect error, continue if partial
       │     │
       │     ├─► externalStatementImportPort.importStatement(command)
       │     │     → SimulatedBankStatementAdapter maps → ingestion items
       │     │     → CashflowIngestionService deduplicates, categorizes, persists
       │     │     → Returns CashflowIngestionResult (categorized, manualReview, rejected)
       │     │
       │     └─► Update page totals, next cursor
       │
       ├─► Update sync session (cursor, lastSyncAt, entryCount)
       │
       └─► Return ProviderSyncReport (pages, entries, imported, duplicates, errors)
```

## Risks

1. **Provider contract designed without real bank API knowledge (HIGH probability, MEDIUM impact)**: Santander, BancoEstado, and Fintoc APIs are either undocumented or require commercial agreements. The contract may need adjustment when real APIs are available. **Mitigation**: The port is designed with common bank API patterns (pagination cursors, date windows, auth tokens). Keep it simple — it's easier to evolve a simple contract than to fix an over-engineered one.

2. **Fixture-based tests don't validate real API behavior (HIGH probability, LOW impact)**: The fake adapter returns controlled data. Real APIs have unexpected edge cases (missing fields, encoding quirks, timeouts mid-page). **Mitigation**: This is explicitly MVP scope — contract validation, not production integration. When real APIs arrive, add integration tests with sandbox environments.

3. **`ProviderAuth` too abstract — credential storage not designed (MEDIUM probability, MEDIUM impact)**: MVP uses `credentialRef` (a config key reference), avoiding real secrets. But the real auth flow (API keys, OAuth client credentials, token rotation) needs encryption at rest and secure injection. **Mitigation**: The `ProviderAuth` record is deliberately minimal. When real auth arrives, add a `ProviderAuthResolver` infrastructure service that loads/decrypts credentials. The port contract stays unchanged.

4. **Sync session in-memory loses state on restart (MEDIUM probability, LOW impact)**: MVP uses an in-memory `SyncSessionPort` implementation. On restart, cursors are lost and full date windows would be re-fetched. **Mitigation**: Accept for MVP. The `SyncSessionPort` interface supports JDBC swap later. In-memory is fine for testing and demo.

5. **Pagination loop complexity — provider may return inconsistent cursors (LOW probability, MEDIUM impact)**: Bank APIs are notorious for cursor bugs (duplicate pages, skipped pages, stale cursors). **Mitigation**: The `ProviderSyncUseCase` can include a max-page guard (e.g., max 50 pages per sync) and duplicate detection via `externalReference` in the ingestion idempotency layer (already proven). This prevents infinite loops.

6. **ArchUnit risk — `ExternalStatementEntry` already imported in `application/port/out/` (LOW probability, LOW impact)**: The port interface uses `ExternalStatementEntry`, which contains `direction` (not a bank term). The ArchUnit string literal scan checks for `banco|bank|acquirer|...` — `ExternalStatementEntry` passes. No changes to ArchUnit rules needed.

7. **Chained PR complexity (LOW probability, LOW impact)**: 2-3 PRs for a contract validation may feel heavy. **Mitigation**: PR1 (contracts only) is independently reviewable and compiles. PR2 (fake adapter) makes PR1 testable. PR3 (use case) completes the flow. Each slice under 400 lines.

## Review Size Forecast

| Component | Est. Lines |
|-----------|-----------|
| `BankProviderPort` + records | ~80 |
| `ProviderError` sealed hierarchy | ~60 |
| Port contract tests (abstract) | ~70 |
| `FakeBankProviderAdapter` | ~90 |
| Fixture JSON files (3 files × ~20 lines) | ~60 |
| `FakeBankProviderAdapterTest` + contract test impl | ~150 |
| `ProviderSyncUseCase` interface + impl | ~80 |
| `SyncSessionPort` + in-memory impl | ~50 |
| `ProviderAuthConfig` | ~30 |
| `ApplicationServiceConfiguration` wiring changes | ~10 |
| `ProviderSyncUseCaseTest` (mocked ports) | ~120 |
| **Total** | **~600** |

### Chained PR Strategy (force-chained, 400-line budget)

| PR | Contents | Est. Lines | Depends on |
|----|----------|-----------|------------|
| **PR1**: Provider contracts | `BankProviderPort`, `ProviderSyncQuery`, `ProviderSyncPage`, `ProviderAuth`, `ProviderError` + sealed hierarchy tests | ~220 | — |
| **PR2**: Fake adapter + fixtures | `FakeBankProviderAdapter`, fixture JSON files, abstract contract test, adapter unit tests | ~200 | PR1 |
| **PR3**: Sync use case + wiring | `ProviderSyncUseCase`, `SyncSessionPort` (in-memory), `ProviderAuthConfig`, `ApplicationServiceConfiguration`, use case tests | ~250 | PR2 |

PR3 can optionally include a REST controller (~100 lines) if a trigger endpoint is desired for MVP. Total would stay under 400 if the controller is excluded or split.

## Testing Strategy

| Layer | Test Type | What it Covers |
|-------|-----------|---------------|
| Port contract | Abstract test class | `ProviderSyncQuery` validation (null profile, inverted dates), `ProviderSyncPage` immutability, `ProviderAuth` equals/hashCode |
| Sealed types | Unit (no Spring) | `ProviderError` exhaustiveness in switch, error message safety (no echo of secrets), serialization round-trip |
| Fake adapter | Unit (no Spring) | Fixture loading, field mapping to `ExternalStatementEntry`, pagination cursor passthrough, CLP enforcement, empty fixture handling, missing fixture file behavior |
| Use case | Unit (Mockito) | Pagination loop terminates, errors aggregated, sync session updated, duplicate detection via ingestion, partial failure handling, max-page guard |
| Controller (optional) | `@WebMvcTest` | Trigger endpoint, sync report response shape, error responses |
| Architecture | ArchUnit (existing) | No `infrastructure.provider` dependency from domain/application (add rule), no bank literals in domain/application (existing, should pass) |

## Ready for Proposal

**Yes** — The exploration confirms:

- **Gap**: The existing `ExternalStatementImportPort` + `SimulatedBankStatementAdapter` handles PUSH-based anti-corruption mapping. Missing: PULL-based provider contract, auth abstraction, pagination, sync sessions, normalized errors.
- **Approach**: Provider Port + Sync Use Case + Error Taxonomy (Approach 2) — adds the provider contract one layer above the proven anti-corruption boundary, validates with fixture-based tests.
- **Architecture**: New `BankProviderPort` (PULL from bank → `ExternalStatementEntry`) + `ProviderSyncUseCase` (orchestrates fetch+import+sync state) + `ProviderError` (sealed type normalization). `ExternalStatementImportPort` and `SimulatedBankStatementAdapter` unchanged.
- **Zero domain changes**: `Transaction`, `TransactionDirection`, `ProfileId` — all stable since the direction hardening.
- **Tradeoffs documented**: In-memory sync sessions, no real bank API calls, no OAuth flow, provider contract based on common patterns (not real API specs). All explicit MVP scope decisions.
- **Review strategy**: 2-3 chained PRs under the 400-line budget.

Key decisions needed before proposal:
- Confirm Approach 2 (port + use case + error taxonomy) is preferred
- Confirm fixture-based tests (not real bank sandboxes) are sufficient for MVP
- Confirm in-memory sync sessions are acceptable for MVP
- Confirm whether a REST endpoint for sync triggering is in or out of MVP scope
- Confirm 2-3 chained PR split strategy
