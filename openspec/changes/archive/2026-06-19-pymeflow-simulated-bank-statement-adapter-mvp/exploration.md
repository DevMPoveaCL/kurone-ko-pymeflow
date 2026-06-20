# Exploration: PymeFlow Simulated Bank Statement Adapter MVP

## Current State

The system currently ingests cashflow movements through two endpoints:

1. **`POST /api/cashflow/ingestions`** (`CashflowIngestionController`) — all-or-nothing batch ingestion using Spring `@Valid`. Every row must pass validation or the entire batch fails.

2. **`POST /api/cashflow/imports/manual`** (`CashflowManualImportController`) — per-row-tolerant CSV-like JSON import. Validates rows programmatically, delegates valid rows to `CashflowIngestionService`, returns mixed results with row-level error tracing.

Both endpoints reuse `CashflowIngestionService.ingest()` unchanged. The ingestion pipeline handles: deduplication (via `externalReference` or deterministic `fp:v1:<sha256>` fingerprint fallback), sensitive-data rejection (`SensitiveDataPolicy`), categorization (`CashflowCategorizationPort`), and persistence (`CashflowMovementHistoryPort`).

The existing settlement feed abstraction (`SettlementFeedPort` + `MockBankSettlementAdapter`/`MockAcquirerSettlementAdapter` in `infrastructure/mock/`) demonstrates the port-and-adapter pattern for OUTBOUND (pull) bank-like data. The bank statement import is an INBOUND (push) use case, where the user submits statement rows and the system ingests them.

### Architecture Boundaries (Hexagonal)

```
interfaces/web/     ← EXISTING: CashflowManualImportController, CashflowIngestionController
application/        ← EXISTING: CashflowIngestionService, TransactionFingerprint, SensitiveDataPolicy
application/port/out/ ← EXISTING: CashflowMovementHistoryPort, CashflowCategorizationPort, SettlementFeedPort
infrastructure/     ← EXISTING: CashflowMovementHistoryJdbcAdapter, ProfileDrivenCashflowCategorizationAdapter,
                     MockBankSettlementAdapter, MockAcquirerSettlementAdapter
domain/             ← EXISTING: Transaction, ProfileId, CashflowCategory, CashflowDirection
```

### Domain Model Constraints

| Concept | Current State | Gap for Bank Statements |
|---------|--------------|------------------------|
| `Transaction.amount` | Positive `BigDecimal` only | Bank rows have signed amounts (credits positive, debits negative) |
| `Transaction.description` | Free text, no structured enrichment | Bank rows have `counterpartyName`, `accountAlias` metadata |
| `CashflowDirection` | INFLOW / OUTFLOW / TRANSFER — on `CashflowCategory`, not on `Transaction` | Direction is a property of the raw row, not the category |
| `Source Reference` | `externalReference` (explicit) or `fp:v1:...` (fingerprint) | Bank provides `bankTransactionId` — natural idempotency key |
| `CashflowMovementDraft` | Requires positive amount | Sign must be resolved before draft creation |

### Key Constraint: ArchUnit Anti-Corruption

The `ArchitectureTest.domainAndApplicationDoNotHardcodeVerticalProviderOrBankLiterals` test (`ArchitectureTest.java:45-53`) forbids string literals containing `banco`, `bank`, `acquirer`, `adquirente`, `getnet`, `tuu`, `transbank` in `domain/` and `application/` packages. `infrastructure/` and `interfaces/` are explicitly EXEMPT from this check.

This means: bank/provider names CAN appear in interfaces/web (controllers, DTOs) and infrastructure/ (adapters), but MUST NOT appear in domain/ or application/.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `interfaces/web/CashflowBankStatementSimulatedController.java` | **New** | REST controller for `POST /api/cashflow/imports/bank-statement/simulated` |
| `interfaces/web/` (DTOs) | **New** | `BankStatementImportRequest`, `BankStatementRow`, `BankStatementImportResponse`, per-row DTOs |
| `infrastructure/bank/BankStatementAntiCorruptionMapper.java` | **New** | Anti-corruption mapper: signed amounts → positive, bankTxId → externalReference, counterparty enrichment |
| `infrastructure/bank/BankStatementAntiCorruptionMapperTest.java` | **New** | Unit tests for sign handling, idempotency key mapping, field enrichment |
| `interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | **New** | `@WebMvcTest` for the simulated bank endpoint |
| `application/cashflow/` | **Unchanged** | `CashflowIngestionService`, `SensitiveDataPolicy`, `TransactionFingerprint` reused as-is |
| `application/port/out/` | **Unchanged** | All port interfaces reused |
| `domain/` | **Unchanged** | `Transaction`, `ProfileId`, `CashflowDirection` unchanged |
| DB migrations | **None** | No new columns needed for MVP |

## Approaches

### 1. Controller-Only Anti-Corruption (Interface-Layer Mapping)

New endpoint `POST /api/cashflow/imports/bank-statement/simulated` with bank-shaped DTOs. Controller maps `BankStatementRow` → `IngestionItem` inline (sign→abs, bankTxId→externalReference), delegates to `CashflowIngestionService`. Follows the exact pattern of `CashflowManualImportController`.

**Input contract:**
```json
{
  "profileId": "pharmacy-cl",
  "importLabel": "Banco Estado Junio 2026",
  "rows": [
    {
      "bankTransactionId": "BTX-20260615-001",
      "bookingDate": "2026-06-15",
      "description": "Pago proveedor ABC",
      "amount": -450000,
      "currency": "CLP",
      "accountAlias": "Cta Cte 001",
      "counterpartyName": "Proveedor ABC S.A."
    },
    {
      "bankTransactionId": "BTX-20260615-002",
      "bookingDate": "2026-06-15",
      "description": "Depósito cliente",
      "amount": 1200000,
      "currency": "CLP",
      "accountAlias": "Cta Cte 001",
      "counterpartyName": null
    }
  ]
}
```

**Output contract:**
```json
{
  "importId": "uuid",
  "profileId": "pharmacy-cl",
  "accepted": 2,
  "categorizedCount": 2,
  "manualReviewCount": 0,
  "rejectedCount": 0,
  "invalid": 0,
  "categorized": [
    {"row": 1, "movementId": "uuid", "transaction": {...}, "category": {...}},
    {"row": 2, "movementId": "uuid", "transaction": {...}, "category": {...}}
  ],
  "manualReview": [],
  "rejected": [],
  "errors": []
}
```

**Anti-corruption mapping (inline in controller):**

| Bank Field | Target | Rule |
|---|---|---|
| `bankTransactionId` | `externalReference` | Required. Maps directly. String trimmed. |
| `bookingDate` | `Transaction.bookedAt` | `LocalDate.parse(...)` |
| `description` | `Transaction.description` | Direct if counterparty null; `"counterpartyName: description"` if counterparty present |
| `amount` (signed) | `Transaction.amount` (positive) | `amount.abs()`. Sign direction DROPPED for MVP. |
| `currency` | `Transaction.currency` | Default `CLP`. Reject non-CLP. |
| `accountAlias` | N/A | Dropped for MVP. |
| `counterpartyName` | `Transaction.description` | Prepended: `"Nombre: descripción original"` |

**Pros:**
- Minimal change surface — single new controller + DTOs (consistent with `CashflowManualImportController`)
- Zero changes to `domain/`, `application/`, `infrastructure/` layers
- Reuses 100% of existing ingestion, idempotency, categorization, and sensitive-data pipelines
- ArchUnit-compliant: bank literals confined to `interfaces/web/` and `infrastructure/bank/`
- Review size manageable: ~480 lines across 2 chained PRs
- Per-row tolerance reusing manual import validation pattern

**Cons:**
- Anti-corruption logic lives in the controller — no dedicated mapper class proving the abstraction for future Fintoc/Khipu adapters
- Counterparty name prepended to description is lossy (can't extract later)
- Direction/sign information is LOST (debit vs credit; both become positive amounts)
- No new port interface — the Fintoc/Khipu adapters would need a different integration path
- Bank `bankTransactionId` as sole idempotency key means no fingerprint fallback for rows missing the ID
- Fingerprint collision risk if `bankTransactionId` is ever optional: a debit of -5000 and credit of +5000 with same description/date would produce identical fingerprints after abs()

**Effort:** Low-Medium (~480 lines, 2 chained PRs)

---

### 2. Port + Infrastructure Anti-Corruption Adapter (RECOMMENDED)

Introduce `BankStatementImportPort` in `application/port/out/` with bank-agnostic type names, implemented by `SimulatedBankStatementAdapter` in `infrastructure/bank/`. The adapter encapsulates ALL anti-corruption mapping. The controller is thin — just receives JSON and delegates to the port.

This mirrors the existing `SettlementFeedPort` + `MockBankSettlementAdapter` pattern, but for the INBOUND (push) direction.

**Architecture:**
```
POST /api/cashflow/imports/bank-statement/simulated
  ├── interfaces/web/CashflowBankStatementSimulatedController.java
  │     Receives BankStatementImportRequest JSON
  │     Validates per-row (bankTransactionId required, amount non-zero, date ISO, currency CLP)
  │     Builds ImportStatementCommand from valid rows
  │
  ├── application/port/out/BankStatementImportPort.java
  │     importStatements(ImportStatementCommand) → CashflowIngestionResult
  │     ImportStatementCommand { profileId, List<ExternalStatementEntry> }
  │     ExternalStatementEntry { externalId, valueDate, narrative, signedAmount, currency, accountLabel, counterpartyName }
  │
  └── infrastructure/bank/SimulatedBankStatementAdapter.java  ← ANTI-CORRUPTION LAYER
        Implements BankStatementImportPort
        Maps ExternalStatementEntry → IngestionItem:
          - signedAmount.abs() → Transaction.amount
          - externalId → externalReference
          - narrative + counterpartyName → Transaction.description (enriched)
          - accountLabel → dropped for MVP
          - valueDate → Transaction.bookedAt
          - currency → Transaction.currency
        Delegates to CashflowIngestionService.ingest()
        Returns CashflowIngestionResult
```

**Port contract (bank-agnostic type names):**
```java
// application/port/out/BankStatementImportPort.java
public interface BankStatementImportPort {
    CashflowIngestionService.CashflowIngestionResult importStatements(ImportStatementCommand command);

    record ImportStatementCommand(ProfileId profileId, List<ExternalStatementEntry> entries) {
        public ImportStatementCommand {
            if (profileId == null) throw new IllegalArgumentException("Profile id is required");
            entries = List.copyOf(entries == null ? List.of() : entries);
        }
    }

    record ExternalStatementEntry(
            String externalId,        // bank-agnostic: bankTxId, fintocId, khipuId
            LocalDate valueDate,      // bookingDate
            String narrative,         // description
            BigDecimal signedAmount,  // raw signed amount
            Currency currency,
            String accountLabel,      // accountAlias
            String counterpartyName
    ) {
        public ExternalStatementEntry {
            if (externalId == null || externalId.isBlank())
                throw new IllegalArgumentException("External id is required");
            if (valueDate == null)
                throw new IllegalArgumentException("Value date is required");
            if (narrative == null || narrative.isBlank())
                throw new IllegalArgumentException("Narrative is required");
            if (signedAmount == null || signedAmount.compareTo(BigDecimal.ZERO) == 0)
                throw new IllegalArgumentException("Amount must be non-zero");
            if (currency == null)
                throw new IllegalArgumentException("Currency is required");
        }
    }
}
```

**Idempotency design:**
- `bankTransactionId` (→ `externalId`) is **mandatory** — mapped to `externalReference` in `IngestionItem`
- No fingerprint fallback for bank statements. The bank always provides a unique transaction ID.
- Missing/null/blank `bankTransactionId` → row invalid (rejected at controller level)
- This avoids the debit/credit fingerprint collision problem entirely
- Sensitive-data policy applies to `bankTransactionId` as `externalReference` — if the TX ID contains blocked terms, it's rejected with safe fallback (same as existing behavior)

**Direction/sign handling (MVP tradeoff):**
- `Math.abs(signedAmount)` → `Transaction.amount` (positive)
- Direction (debit/credit) is LOST for MVP
- Justification: the MVP's purpose is to validate the anti-corruption layer boundary, not to build a complete bank reconciliation model
- When direction becomes necessary, the domain model (`Transaction` or `CashflowMovementDraft`) can be extended with a direction field. The anti-corruption mapper is the single place to change — controllers and ingestion service are unaffected.
- Edge case: zero-amount rows (bank fees reversed to zero) are invalid — rejected at controller level with `"amount must be non-zero"`

**Pros:**
- Clean anti-corruption LAYER — the dedicated `SimulatedBankStatementAdapter` encapsulates ALL bank-specific mapping logic
- Port interface uses bank-agnostic type names (`ExternalStatementEntry`, not `BankStatementRow`) — proves the abstraction works for Fintoc/Khipu
- Consistent with existing hexagonal patterns (`SettlementFeedPort` + `MockBankSettlementAdapter`)
- Controller is thin — only JSON deserialization + basic validation; all mapping lives in the adapter
- Future real bank integrations (Fintoc, Khipu, open banking) add new adapters behind the SAME port — zero controller changes
- ArchUnit-compliant: bank literals contained in `infrastructure/bank/` package, port uses generic names
- Adapter testable in isolation (pure function, no Spring)
- Idempotency via mandatory `externalId` avoids fingerprint collision

**Cons:**
- More files than Approach 1 (port + adapter + controller vs. controller only)
- Direction/sign is still lost (same as Approach 1 — domain model constraint)
- `ExternalStatementEntry` type in `application/port/out/` uses "statement" terminology — borderline acceptable (not "bank," but implies financial statements)
- Counterparty name prepended to description is lossy (can't extract separately later without structured description format)
- For a simulated adapter, the port abstraction adds indirection without a second implementation to justify it yet

**Effort:** Medium (~700 lines, 3 chained PRs)

---

### 3. Full Provider Abstraction (BankProviderPort + Multiple Adapters)

Design the complete bank provider abstraction now: `BankProviderPort` with methods for `fetchStatements()`, `fetchBalances()`, etc. Create a simulated provider adapter plus stub interfaces for Fintoc and Khipu. This would be the production-ready abstraction from day one.

**Pros:**
- Full future-proofing — Fintoc/Khipu adapters just implement existing interfaces
- Cleanest anti-corruption — bank concepts are fully modeled at the port boundary
- Direction/sign handling could be modeled with proper bank-specific intermediate types

**Cons:**
- Significantly over-engineered for a validation-only MVP
- Fintoc and Khipu APIs are unknown — premature abstraction risk (wrong interface design)
- The simulated adapter wouldn't meaningfully exercise real bank API concerns (OAuth, pagination, webhooks)
- Large review surface (~1200+ lines) requiring 4+ chained PRs
- Violates YAGNI — the simulated adapter exists to validate boundaries, not to be production-ready
- The "statement import" use case is fundamentally different from the "settlement feed" use case — not a good candidate for a single `BankProviderPort`

**Effort:** High (~1200 lines)

**Recommendation:** Reject for MVP. Build the abstraction incrementally: Approach 2 for MVP, evolve the port when the first real bank integration arrives.

---

## Recommendation

**Approach 2 — Port + Infrastructure Anti-Corruption Adapter** — is the right choice for this MVP:

1. **Validates the anti-corruption boundary explicitly**: The `BankStatementImportPort` (bank-agnostic types) + `SimulatedBankStatementAdapter` (bank-specific mapping) proves the abstraction works. When Fintoc or Khipu arrive, they plug in behind the same port.

2. **Architectural consistency**: Matches the existing `SettlementFeedPort` + `MockBankSettlementAdapter`/`MockAcquirerSettlementAdapter` pattern. The team already understands this structure.

3. **Proper hexagonal layering**: The anti-corruption layer lives in `infrastructure/bank/`, not in a controller. Bank concepts never leak into `application/` or `domain/`. ArchUnit test passes without modification.

4. **Testability**: The adapter is a pure function (no Spring dependencies) — fully testable in isolation. Controller tests (`@WebMvcTest`) mock the port. Unit tests for the mapper verify sign handling, idempotency key mapping, description enrichment, and edge cases.

5. **Scalable**: When Fintoc/Khipu/open banking arrive:
   - Add `FintocStatementAdapter implements BankStatementImportPort` in `infrastructure/bank/`
   - Add `KhipuStatementAdapter implements BankStatementImportPort` in `infrastructure/bank/`
   - Controller and application layer are ZERO changes

6. **MVP scope discipline**: Accept the direction/sign loss as an explicit tradeoff. Document clearly. When the domain model evolves to support direction, the adapter is the single place to change.

### Accepted MVP Tradeoffs

| Tradeoff | Impact | Mitigation |
|----------|--------|------------|
| Direction/sign lost | Debit/credit distinction gone; both become positive amounts | Documented MVP limitation. Domain model extension (direction field in `Transaction` or `CashflowMovementDraft`) planned for next iteration. |
| `accountAlias` dropped | Account-level traceability lost | Minimal impact for Chilean SMBs with single accounts. Can be prepended to description in future if needed. |
| `counterpartyName` prepended to description | Lossy — can't extract separately; pollutes categorization input | Structured description format (`"counterpartyName: originalDescription"`) preserves the data. Category engine can still match on the original description substring. |
| No fingerprint fallback for bank imports | Rows without `bankTransactionId` are rejected | Real banks ALWAYS provide transaction IDs. Document that `bankTransactionId` is mandatory. |
| `bankTransactionId` as sole dedup key | If bank changes TX ID format, re-imports create duplicates | Extremely unlikely in practice. Bank TX IDs are stable by regulatory requirement. |
| CLP-only | Non-CLP bank accounts rejected | Consistent with existing system behavior. Multi-currency is a profile/vertical concern for later. |

### Implementation Sketch

```
POST /api/cashflow/imports/bank-statement/simulated

Controller (CashflowBankStatementSimulatedController):
  1. Receive BankStatementImportRequest (profileId, importLabel?, rows[])
  2. Generate importId (UUID, response-only)
  3. For each row:
     a. Validate bankTransactionId (required, non-blank)
     b. Validate amount (required, non-zero)
     c. Validate bookingDate (required, ISO format)
     d. Validate currency (CLP only, default CLP)
     e. If valid: build ExternalStatementEntry
     f. If invalid: collect RowErrorResponse
  4. Build ImportStatementCommand(profileId, validEntries)
  5. Call bankStatementImportPort.importStatements(command)
  6. Build BankStatementImportResponse with flat counts, per-row results, errors

Adapter (SimulatedBankStatementAdapter):
  For each ExternalStatementEntry:
    1. sourceReference = entry.externalId()  // bankTransactionId as dedup key
    2. description = entry.counterpartyName() != null
         ? entry.counterpartyName() + ": " + entry.narrative()
         : entry.narrative()
    3. transaction = new Transaction(
         description,
         entry.signedAmount().abs(),   // SIGN LOST - MVP tradeoff
         entry.currency(),
         entry.valueDate()
       )
    4. ingestionItem = new IngestionItem(transaction, sourceReference)
    5. Collect into list
  6. command = new CashflowIngestionCommand(profileId, items)
  7. return cashflowIngestionService.ingest(command)
```

### Row Validation vs Service Processing

| Stage | What fails | Where | Result |
|-------|-----------|-------|--------|
| Row validation | Missing `bankTransactionId`, zero amount, bad date, non-CLP | Controller | `errors[]` in response |
| Port validation | `ExternalStatementEntry` contract violations | `ImportStatementCommand` | `IllegalArgumentException` → 400 |
| Anti-corruption | N/A | Adapter | Pure mapping, no failures |
| Sensitive data | TX ID or description contains blocked terms | `CashflowIngestionService` | `rejected[]` in response |
| Category match | Description matched a category | `CashflowCategorizationPort` | `categorized[]` in response |
| No category match | Description unmatched | `CashflowCategorizationPort` | `manualReview[]` in response |
| Duplicate | Same `bankTransactionId` re-imported | `CashflowMovementHistoryPort` | Returns existing movement |

## Risks

1. **Direction/sign data loss (HIGH probability, MEDIUM impact)**: Bank debits and credits both become positive amounts. Category matching may misclassify debits as inflows. Mitigation: document as explicit MVP tradeoff. The next iteration MUST add direction to the domain model. The anti-corruption adapter is designed to be the single change point.

2. **Counterparty name polluting description (MEDIUM probability, LOW impact)**: Prepending `"Proveedor ABC S.A.: Pago proveedor"` changes the text the categorization engine sees. If categories are tuned to exact descriptions, they may fail to match. Mitigation: the category engine (`ProfileDrivenCashflowCategorizationAdapter`) uses keyword matching — the original description is still a substring. Real risk if counterparty names overlap with category keywords.

3. **No port implementation diversity (MEDIUM probability, LOW impact)**: The port has only ONE implementation until Fintoc/Khipu arrive. The abstraction is "validated" but not "proven" by multiple adapters. Mitigation: this is the nature of incremental architecture. The `SettlementFeedPort` also started with a single mock before gaining `MockBankSettlementAdapter` and `MockAcquirerSettlementAdapter`.

4. **`ExternalStatementEntry` terminology in application layer (LOW probability, LOW impact)**: The port interface uses "statement" which is financial-domain language. ArchUnit checks for "banco"/"bank" literals, not "statement." Mitigation: "statement" is generic enough (credit card statements, invoice statements, not inherently bank-specific). If it becomes a concern, rename to `ExternalTransactionEntry`.

5. **Chained PR complexity (MEDIUM probability, LOW impact)**: 3 PRs for a simulated adapter may feel heavy. Mitigation: the force-chained strategy keeps each PR under 400 lines. PR1 (contracts + test) compiles independently. PR2 (adapter) makes PR1 green. PR3 (controller + test) completes the feature. Each PR is reviewable in one sitting.

6. **Sensitive `bankTransactionId` rejected silently (LOW probability, LOW impact)**: If a bank uses customer-sensitive data in transaction IDs, the `SensitiveDataPolicy` rejects them. The user sees a rejection with no explanation of what was sensitive. Mitigation: same behavior as existing ingestion — consistent UX. Bank TX IDs rarely contain PII.

## Review Size Forecast

| Component | File | Est. Lines |
|-----------|------|-----------|
| Port interface | `application/port/out/BankStatementImportPort.java` | ~45 |
| Port contract test | `application/port/out/BankStatementImportPortTest.java` | ~50 |
| Anti-corruption adapter | `infrastructure/bank/SimulatedBankStatementAdapter.java` | ~90 |
| Adapter unit tests | `infrastructure/bank/SimulatedBankStatementAdapterTest.java` | ~180 |
| Controller + DTOs | `interfaces/web/CashflowBankStatementSimulatedController.java` | ~180 |
| Controller WebMvcTest | `interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | ~250 |
| **Total** | | **~795** |

### Chained PR Strategy (force-chained, 400-line budget)

| PR | Contents | Est. Lines | Depends on |
|----|----------|-----------|------------|
| **PR1**: Contracts + anti-corruption unit tests | `BankStatementImportPort` + `ExternalStatementEntry` + `SimulatedBankStatementAdapterTest` (RED — adapter doesn't exist yet) | ~275 | — |
| **PR2**: Anti-corruption adapter implementation | `SimulatedBankStatementAdapter` (GREEN — makes PR1 pass) | ~90 | PR1 |
| **PR3**: Controller + WebMvc test | `CashflowBankStatementSimulatedController` + DTOs + `@WebMvcTest` | ~430 | PR2 |

PR3 is borderline at ~430 lines. Split further if exact measurement confirms >400:
- **PR3a**: Controller test only (~250 lines, RED)
- **PR3b**: Controller + DTOs (~180 lines, GREEN)

Final: 3-4 chained PRs.

## Testing Strategy

| Layer | Test Type | What it Covers |
|-------|-----------|---------------|
| **Port contract** | Unit (no Spring) | `ExternalStatementEntry` validation: null externalId, zero amount, blank narrative |
| **Adapter** | Unit (no Spring) | Sign→positive mapping, counterparty enrichment, idempotency key passthrough, edge cases (negative zero, large amounts, special chars in TX ID) |
| **Controller** | `@WebMvcTest` (mocked port) | Mixed batch tolerance, all-invalid→400, missing bankTransactionId, zero amount, non-CLP currency, non-ISO date, re-import returns existing, sensitive TX ID rejection without echo, counterparty null→description passthrough, importId present in response |
| **Architecture** | ArchUnit (existing) | No bank literals in domain/application (existing test, should pass unchanged) |
| **Smoke** | `./gradlew.bat test --rerun-tasks` | All tests green, JaCoCo coverage acceptable |

### Smoke Path
```
1. POST with 2 valid rows (one debit, one credit) → 200, accepted=2, categorizedCount=?
2. POST with mixed valid/invalid rows → 200, accepted=N, invalid=M, errors[M]
3. POST with all-invalid → 400, accepted=0, invalid=N
4. POST with duplicate bankTransactionId → 200, returns existing movement IDs
5. POST with bankTransactionId = "   " → 400, error "external id is required"
6. POST with non-CLP currency → rejected with CLP-only error
```

## Ready for Proposal

**Yes** — The exploration confirms:

- **Gap**: No bank-statement-shaped import exists. The existing manual import can't handle signed amounts, bank transaction IDs as dedup keys, or counterparty metadata.
- **Approach**: Port + infrastructure anti-corruption adapter (Approach 2) — validates the model boundary explicitly and scales to Fintoc/Khipu with minimal changes.
- **Architecture**: New `BankStatementImportPort` (bank-agnostic types) + `SimulatedBankStatementAdapter` (anti-corruption mapping) + thin controller. Zero changes to domain/application core.
- **Tradeoffs documented**: Direction/sign loss, counterparty prepended to description, no fingerprint fallback, accountAlias dropped. All are explicit MVP scope decisions.
- **Review strategy**: 3-4 chained PRs under the 400-line budget.

Key decisions needed before proposal:
- Confirm Approach 2 (port + adapter) is preferred over Approach 1 (controller-only)
- Confirm direction/sign loss is acceptable for MVP
- Confirm `bankTransactionId` as mandatory idempotency key (no fingerprint fallback)
- Confirm 3-4 chained PR split strategy
