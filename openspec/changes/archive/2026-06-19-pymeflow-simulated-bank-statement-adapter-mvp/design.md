# Design: PymeFlow Simulated Bank Statement Adapter MVP

## Technical Approach

Add a simulated bank-statement import endpoint that proves the anti-corruption boundary before real bank providers. A bank-agnostic application port (`ExternalStatementImportPort`) keeps provider concepts out of domain/application. An infrastructure adapter (`SimulatedBankStatementAdapter`) maps simulated bank rows into the existing `CashflowIngestionService.ingest(...)` contract. The controller lives in `interfaces/web` and owns HTTP/JSON validation and response shaping.

## Architecture Decisions

| Decision | Option | Tradeoff | Rationale |
|----------|--------|----------|-----------|
| Port package | `application/port/out` | `port/in` is conceptually inbound, but the codebase has **no** `port/in` directory and all ports live in `port/out` | Follow existing convention; ArchUnit does not enforce `in` vs `out` naming |
| Adapter return type | Adapter returns `CashflowIngestionResult` directly | Duplicating result records adds boilerplate with no boundary benefit | The adapter is explicitly a bridge to ingestion; reusing the canonical result keeps the port minimal |
| `bankTransactionId` handling | Mandatory at controller; no fingerprint fallback | Breaks if provider omits IDs | Proposal requires mandatory ID for idempotency; fingerprint fallback is explicitly excluded for this endpoint |
| Direction loss | Accept `amount.abs()` with documented tradeoff | Loses debit/credit semantics | MVP scope; direction hardening is a future domain iteration |
| `accountAlias` | Silently dropped | User cannot reconcile by account in MVP | Out of scope per proposal |
| Duplicate `bankTransactionId` values | Reject every row sharing a duplicate nonblank ID before adapter delegation | A repeated valid-looking row is not imported in the same request | Bank statement IDs are expected to identify one source row; rejecting all duplicated occurrences avoids ambiguous row traceability and idempotency behavior |

## Data Flow

    HTTP POST /api/cashflow/imports/bank-statement/simulated
                    │
                    ▼
    CashflowBankStatementSimulatedController
      ── validates rows, including duplicate bankTransactionId detection ──► row-level errors (no echo)
      ── builds ExternalStatementImportCommand ──►
                    │
                    ▼
    SimulatedBankStatementAdapter (infrastructure/bank)
      ── maps ExternalStatementEntry ──► CashflowIngestionCommand.IngestionItem
      ── delegates ──► CashflowIngestionService.ingest(...)
      ── maps result back ──► CashflowIngestionResult
                    │
                    ▼
    Controller maps result + row metadata ──► SimulatedBankStatementResponse

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `application/port/out/ExternalStatementImportPort.java` | Create | Port interface: `importStatement(command)` |
| `application/port/out/ExternalStatementImportCommand.java` | Create | Command record: `profileId`, `importLabel`, `entries` |
| `application/port/out/ExternalStatementEntry.java` | Create | Entry record: `externalReference`, `date`, `description`, `amount`, `currency` |
| `infrastructure/bank/SimulatedBankStatementAdapter.java` | Create | Maps bank rows to ingestion items, delegates to `CashflowIngestionService`, returns `CashflowIngestionResult` |
| `interfaces/web/CashflowBankStatementSimulatedController.java` | Create | Endpoint, row validation, response shaping with row traceability |
| `infrastructure/config/ApplicationServiceConfiguration.java` | Modify | Bean wiring for `SimulatedBankStatementAdapter` |
| `infrastructure/bank/SimulatedBankStatementAdapterTest.java` | Create | Adapter unit tests: mapping, enrichment, abs(), CLP guard, delegation |
| `interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Create | WebMvc tests: mixed batch, all-invalid 400, no-echo, idempotency smoke, row traceability |
| `architecture/ArchitectureTest.java` | Modify | Smoke rule confirming `infrastructure.bank` does not leak into `domain` or `application` |

## Interfaces / Contracts

```java
package com.kuroneko.pymeflow.application.port.out;

public interface ExternalStatementImportPort {
    CashflowIngestionService.CashflowIngestionResult importStatement(
            ExternalStatementImportCommand command);
}
```

```java
public record ExternalStatementImportCommand(
        ProfileId profileId,
        String importLabel,
        List<ExternalStatementEntry> entries) {
    public ExternalStatementImportCommand {
        if (profileId == null) throw new IllegalArgumentException("Profile id is required");
        entries = List.copyOf(entries == null ? List.of() : entries);
    }
}
```

```java
public record ExternalStatementEntry(
        String externalReference,
        LocalDate date,
        String description,
        BigDecimal amount,
        Currency currency) {
    public ExternalStatementEntry {
        if (externalReference == null || externalReference.isBlank())
            throw new IllegalArgumentException("External reference is required");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("Description is required");
        if (date == null) throw new IllegalArgumentException("Date is required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0)
            throw new IllegalArgumentException("Amount must be non-zero");
        if (currency == null) throw new IllegalArgumentException("Currency is required");
    }
}
```

**Mapping rules in adapter:**
- `externalReference` = `bankTransactionId.trim()` (mandatory)
- `amount` = `signedAmount.abs()`
- `description` = `counterpartyName` present ? `counterpartyName + " — " + description` : `description`
- `date` = `bookingDate`
- `currency` = `Currency.getInstance(currencyCode.toUpperCase())`; only `"CLP"` accepted
- `accountAlias` dropped

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (adapter) | Mapping rules, enrichment, abs(), CLP-only guard, delegation to ingestion service, result passthrough | Plain JUnit 5 + Mockito; mock `CashflowIngestionService` |
| Integration (controller) | Mixed valid/invalid batch, duplicate `bankTransactionId` partial success, all-duplicate/all-invalid 400 without adapter delegation, all-invalid returns 400, row-level errors without echoing sensitive values, idempotency via re-import, row traceability (1-based position or provided `rowNumber`), OpenApi response codes | `@WebMvcTest` with `@MockBean` for `ExternalStatementImportPort` |
| Architecture | `infrastructure.bank` does not leak into `domain`/`application` | Add ArchUnit rule in `ArchitectureTest` |

## Migration / Rollout

No migration required. The MVP adds no DB schema changes or persisted batch state.

## Chained PR / Work-Unit Split

Forecast exceeds 400 review lines. Split into **3 chained PRs**:

1. **Contracts + port** (`ExternalStatementImportPort`, `ExternalStatementImportCommand`, `ExternalStatementEntry`, config wiring, ArchUnit smoke) — ~120 lines.
2. **Adapter + adapter tests** (`SimulatedBankStatementAdapter` + unit tests) — ~350 lines.
3. **Controller + controller tests** (`CashflowBankStatementSimulatedController` + `@WebMvcTest`) — ~450 lines; if review tools flag this, split controller and controller tests into separate PRs.

## Open Questions

- None.
