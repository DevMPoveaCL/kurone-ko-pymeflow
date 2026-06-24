# Exploration: PymeFlow Cashflow Direction Hardening

## Current State

### Direction conflation: two orthogonal concepts currently merged

The system has **category direction** (`CashflowDirection`: INFLOW, OUTFLOW, TRANSFER) as a property of `CashflowCategory`. The projection engine (`CashflowProjectionService.sumByDirection`) treats all transactions within an INFLOW category as inflows and all OUTFLOW-category transactions as outflows — regardless of the bank's actual debit/credit signal.

Meanwhile, **movement direction** (the bank's debit/credit) is completely discarded — `SimulatedBankStatementAdapter` calls `entry.amount().abs()` at line 28, and `CashflowMovementDraft`/`CashflowMovementRecord` both enforce `amount.signum() > 0` (strictly positive). The `ExternalStatementEntry` record accepts signed amounts but has no direction field.

The OpenSpec spec `cashflow-bank-statement-import/spec.md` explicitly documents this as an "accepted MVP tradeoff" in requirement "Direction Loss Documentation" (lines 67-75).

### Key domain constraints

| Artifact | Amount constraint | Has direction? |
|---|---|---|
| `Transaction` (domain) | No sign constraint | No |
| `CashflowMovementDraft` | `amount.signum() <= 0` throws | No |
| `CashflowMovementRecord` | `amount.signum() <= 0` throws | No |
| `ProjectedCashflowTransaction` | `amount.signum() < 0` throws | No |
| `ProjectionReadyCashflowTransaction` | `amount.signum() <= 0` throws | No |
| `ExternalStatementEntry` | Non-zero (signed ok) | No |
| DB `cashflow_movement_history.amount` | `CHECK (amount > 0)` | No column |

### How projection uses direction

`CashflowProjectionService.sumByDirection()` (line 102-111) resolves direction by looking up `categories.get(transaction.categoryKey()).direction()` — direction comes from the category, not the transaction. The running balance formula is:

```
runningBalance = runningBalance + inflows - outflows - obligations
```

This means the projection engine cannot distinguish between a bank credit mapped to an INFLOW category (correct) and a bank debit mapped to an INFLOW category (semantic contradiction that should be flagged).

### Simulated bank adapter: direction loss

`SimulatedBankStatementAdapter.importStatement()` (line 20-43):
- Receives `ExternalStatementEntry` with signed `amount` (survives controller validation)
- Calls `entry.amount().abs()` — strips sign
- No direction field exists on `Transaction` to preserve the sign/debit-credit signal
- Test `mapsSignedNegativeToPositive()` explicitly validates this behavior

## Affected Areas

### Domain (package `domain.cashflow`, `domain.vertical`)
- `Transaction.java` — no direction field; would need one
- `CashflowDirection.java` — currently category-only; may need `TransactionDirection` companion enum with DEBIT/CREDIT semantics
- `CashflowCategory.java` — unchanged (category direction is correct and stays)
- `CategoryAssignment.java` — unchanged

### Application (package `application.cashflow`, `application.port.out`)
- `CashflowMovementDraft.java` — requires positive amount; needs direction field
- `CashflowMovementRecord.java` — requires positive amount; needs direction field
- `CashflowIngestionService.java` — propagates `Transaction` → `Draft`; needs direction propagation (+ `IngestionOutcome` inner record)
- `CashflowMovementHistoryService.java` — queries records; projectionReady must carry direction
- `CashflowProjectionService.java` — `sumByDirection` uses category direction; could add cross-validation
- `CashflowProjectionCommand.java` — `ProjectedCashflowTransaction` needs direction
- `ProjectedCashflowTransaction.java` — needs direction
- `ProjectionReadyCashflowTransaction.java` — needs direction
- `DailyProjectedBalance.java` — unchanged (direction resolved in sums)
- `ExternalStatementEntry.java` — port record; needs `TransactionDirection` field
- `ExternalStatementImportCommand.java` — unchanged
- `ExternalStatementImportPort.java` — unchanged
- `CashflowMovementHistoryPort.java` — unchanged (JDBC impl handles internally)
- `CashflowCategorizationPort.java` — `categorize(Transaction, VerticalProfile)` — categorization may use direction for smarter matching

### Application (other)
- `ManualReviewResolutionService.java` — validates amount > 0; needs direction in resolution command
- `ManualReviewResolutionCommand.java` — needs direction field
- `AccountantExportService.java` — `buildSummary` uses `transaction.amount().abs()` + category direction; should use movement direction
- `HistoryRecommendationService.java` — category concentration uses amounts; direction mismatch could be a new signal

### Infrastructure
- `SimulatedBankStatementAdapter.java` — `abs()` call at line 28 must be replaced with direction mapping; sign → direction
- `CashflowMovementHistoryJdbcAdapter.java` — SELECT/INSERT columns need `movement_direction` column
- `ProfileDrivenCashflowCategorizationAdapter.java` — text-matching only; may benefit from direction-aware rules
- `CsvAccountantExportAdapter.java` — CSV format may include direction column

### Interfaces (API controllers)
- `CashflowBankStatementSimulatedController.java` — request accepts signed; response records need direction in `TransactionResponse`
- `CashflowManualImportController.java` — `ManualImportRow` may accept optional direction; response records need direction
- `CashflowHistoryController.java` — `PendingManualReviewMovementResponse` and `ProjectionReadyTransactionResponse` need direction
- `CashflowProjectionController.java` — `ProjectedTransactionRequest` may accept direction
- `CashflowIngestionController.java` — if exists, same changes

### DB Schema (Flyway)
- `V2__create_cashflow_movement_history.sql` — `CHECK (amount > 0)` stays; new `movement_direction` column needed
- New `V4__add_movement_direction.sql` migration required
- `V3__unique_partial_index_source_reference.sql` — unchanged
- `R__seed_pharmacy_categories.sql` — unchanged

### Test files (all corresponding `*Test.java`)
- ~15 test files need updates for new direction fields in builders/factories/assertions
- `ArchitectureTest.java` — may need new rule if new domain type added
- Integration test for new migration

### OpenSpec specs
- `specs/cashflow-bank-statement-import/spec.md` — remove "Direction Loss Documentation" requirement, add direction preservation scenarios
- `specs/cashflow-manual-import/spec.md` — add direction to row validation
- `specs/cashflow-ingestion-idempotency/spec.md` — direction included in fingerprint hash
- `specs/cashflow-history-recommendations/spec.md` — add direction-mismatch signal

## Approaches

### 1. Direction only on external statement boundary (NOT recommended)

Add `TransactionDirection` to `ExternalStatementEntry` and the bank adapter, but keep domain `Transaction` direction-free. Bank adapters would populate direction, but the ingestion service would discard it when creating drafts.

- **Pros**: Minimal change — 2 files changed. No migration. No API break.
- **Cons**: Direction is still lost in the domain. Projection, history, exports still cannot distinguish debit/credit. Category-movement-direction cross-validation impossible. **Does not solve the roadmap requirement** — the direction hardening must survive into the domain for real bank providers.
- **Effort**: Low

### 2. Full direction propagation through domain (RECOMMENDED)

Add a new `TransactionDirection` enum (DEBIT, CREDIT) to the domain, propagate through `Transaction` → `CashflowMovementDraft` → `CashflowMovementRecord` → DB → projections/history/exports. Keep `CashflowDirection` as-is for categories. Bank adapters map sign → direction. Manual import optionally accepts direction. Projection uses category direction for sums (unchanged) but adds cross-validation.

- **Pros**: Preserves bank signal end-to-end. Enables direction-mismatch detection. Provider-agnostic — real Santander, BCI, etc. can each normalize to DEBIT/CREDIT. Backward-compatible: existing persisted rows default to CREDIT. Domain stays clean — `TransactionDirection` is a pure domain concept. Category direction and movement direction are clearly separated concepts.
- **Cons**: Large change — ~30 files touched. DB migration with default value. API contracts change. Manual import API gets a new optional field. Review size will exceed 400 lines (needs chained PRs).
- **Effort**: High

### 3. Signed amounts internally (NOT recommended)

Remove positive-amount constraint across the board. Use negative amounts for outflows, positive for inflows. Eliminate need for explicit direction field.

- **Pros**: One less field. More compact representation.
- **Cons**: Fundamental semantic conflict — `CashflowDirection` on categories already assigns direction. Would have to change EVERY amount constraint (5 records + DB CHECK). Existing persisted rows with positive amounts would become ambiguous. The projection engine's dual sum (inflows vs outflows) would need a complete rewrite. Category-movement-direction mismatch would be invisible (both encoded in sign). Breaks the existing convention where amount is always an absolute magnitude. **Worst approach** — conflates two orthogonal concepts into one numeric field.
- **Effort**: High (rewrite, not add)

## Recommendation

**Approach 2: Full direction propagation.** Rationale:

1. **Correct separation of concerns**: Category direction (`CashflowDirection`) answers "what does this category mean for cashflow?" — a categorization concern. Movement direction (`TransactionDirection`) answers "what did the bank/provider say?" — a recording concern. These MUST be separate.
2. **Provider-agnostic**: `TransactionDirection.DEBIT`/`CREDIT` is a neutral normalization that any bank adapter can map to regardless of the provider's native convention (signed amounts, separate debit/credit columns, ISO 20022 codes).
3. **Backward-compatible**: Existing persisted rows default to `CREDIT` (the majority case for SMB cashflow). Migration is additive — no data loss.
4. **MVP-safe**: The projection engine's `sumByDirection` still uses category direction (unchanged behavior). Direction hardening ADDS cross-validation but doesn't change projection math.

### Direction type design

```java
// domain/cashflow/TransactionDirection.java
public enum TransactionDirection {
    DEBIT,   // money leaving the account (bank-speak: debit, negative amount)
    CREDIT   // money entering the account (bank-speak: credit, positive amount)
}
```

Why NOT reuse `CashflowDirection`?
- `CashflowDirection.INFLOW/OUTFLOW` is a CATEGORY concern — "Sales is an inflow category"
- `TransactionDirection.DEBIT/CREDIT` is a PROVIDER concern — "The bank says this was a debit"
- Using the same enum for both would create ambiguity: is INFLOW on a movement the same as INFLOW on a category? No. A bank CREDIT might map to a REFUND category (OUTFLOW direction). These can legitimately differ.
- `TRANSFER` makes no sense for a bank transaction — internal transfers aren't represented in external statements.

### Migration strategy for existing rows

```sql
ALTER TABLE cashflow_movement_history
ADD COLUMN movement_direction VARCHAR(8) NOT NULL DEFAULT 'CREDIT'
CHECK (movement_direction IN ('DEBIT', 'CREDIT'));
```

Existing rows get `CREDIT` — the safe default since most cashflow movements for SMBs are credits (sales, settlements). Debits (supplier payments, fees) will be under-counted until re-imported, but no data is lost. A follow-up migration could backfill from the source material if available.

### Bank adapter mapping

```java
// SimulatedBankStatementAdapter — replace abs() with direction mapping
var direction = entry.amount().signum() >= 0 
    ? TransactionDirection.CREDIT 
    : TransactionDirection.DEBIT;
var transaction = new Transaction(
    descriptionFor(entry.counterpartyName(), entry.description()),
    entry.amount().abs(),       // amount stays positive
    TransactionDirection.CREDIT, // TODO: direction from bank
    entry.currency(),
    entry.date()
);
```

Wait — the adapter currently receives `ExternalStatementEntry` which has signed amounts. But in the hardened design, `ExternalStatementEntry` should carry an explicit `TransactionDirection` field rather than relying on sign. The controller already validates non-zero signed amounts; the adapter should map sign → direction. But for real providers, the adapter may receive an explicit debit/credit flag, not a sign.

Best practice: `ExternalStatementEntry` gets `TransactionDirection direction`. The controller maps sign → direction. The adapter no longer needs to interpret sign — it trusts the direction field.

### Cross-validation in projection (optional, can be deferred)

If a `DEBIT` movement is categorized under an `INFLOW` category, the projection service could:
- Force manual review
- Log a warning
- Auto-correct (risky)

For MVP hardening: just preserve direction, add a `directionMismatch` field to projection alerts but don't block projection.

## Risks

1. **Review size blowout**: Touching ~30 files + migration + specs could exceed 400 lines easily. Mitigation: chained PRs — slice into domain-only, persistence, adapter, API slices (see PR strategy below).
2. **Fingerprint hash change**: `TransactionFingerprint` includes normalized fields. Adding direction changes the hash, breaking idempotency for existing no-reference transactions. **Decision needed**: include direction in fingerprint or not? If included, re-imported transactions with no direction will get new fingerprints (BREAKS idempotency). If excluded, direction doesn't affect dedup (SAFER). Recommendation: exclude direction from fingerprint for backward compatibility, document as accepted tradeoff.
3. **Manual review resolution**: `ManualReviewMovementResolutionCommand` currently has no direction. Users resolving manual reviews don't know the original bank direction. Decision: add direction to the resolution command so reviewers can see it, but don't require it as input (direction is immutable from the bank).
4. **Export format change**: CSV accountant export currently has no direction column. Adding one changes the format. Mitigation: add a separate column, don't change existing columns.
5. **API backward compatibility**: Adding `direction` to `ManualImportRow` as optional is safe. Adding it to history/projection responses may break clients that don't expect it. Mitigation: document as additive field, not breaking.
6. **Category-movement-direction mismatch**: What happens when a DEBIT movement gets an INFLOW category? This is a legitimate scenario in bank reversal/chargeback cases. The system should preserve both directions and expose the mismatch, not silently correct it. The projection engine should continue using category direction (existing behavior) and emit a warning for mismatches.

## PR Strategy (chained)

Given the ~30 files affected and the 400-line review budget, this change MUST be delivered as chained PRs:

| Slice | Scope | Files ~ | Lines ~ | Autonomous |
|-------|-------|---------|---------|------------|
| PR #1 | Domain model: `TransactionDirection` enum, add to `Transaction`, `CashflowMovementDraft`, `CashflowMovementRecord` | 5 | ~80 | Yes — domain only, no integration |
| PR #2 | DB migration + persistence adapter: Flyway V4, JDBC adapter column | 3 | ~60 | Yes — builds on PR #1 |
| PR #3 | External statement layer: `ExternalStatementEntry`, bank adapter, ingestion service propagation | 5 | ~120 | Yes — builds on PR #1-2 |
| PR #4 | API contracts: history/projection/manual import controllers, response DTOs | 6 | ~150 | Yes — builds on PR #1-3 |
| PR #5 | Cross-validation + recommendations: projection alerts for mismatch, new history signal | 4 | ~80 | Yes — builds on PR #1-4 |
| PR #6 | OpenSpec specs update: remove direction-loss tradeoff, add preservation scenarios | 5 | ~100 | Yes — builds on all PRs |

Each slice is self-contained, testable independently, and produces a clean diff under 200 lines.

## Ready for Proposal

Yes. The exploration identifies the correct approach (full domain propagation with `TransactionDirection`), maps all affected files, defines the migration strategy, and forecasts the PR delivery chain. The orchestrator should proceed to `sdd-propose` to formalize scope, risks, and delivery constraints.
