# Tasks: PymeFlow Cashflow Direction Hardening

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~640 (6 chained PRs) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR #1 → #2 → #3 → #4 → #5 → #6 |
| Delivery strategy | force-chained |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Domain model + records get `direction` | PR #1 | Base: `cashflow/direction-hardening` |
| 2 | DB migration + JDBC read/write column | PR #2 | Base: PR #1 branch |
| 3 | Adapter sign→direction + ingestion propagation | PR #3 | Base: PR #2 branch |
| 4 | Controller DTOs expose direction additively | PR #4 | Base: PR #3 branch |
| 5 | Mismatch recommendation signal | PR #5 | Base: PR #4 branch |
| 6 | Spec docs + direction-loss cleanup | PR #6 | Base: PR #5 branch |

---

## PR #1 — Domain

### RED

- [x] 1.1 Write `TransactionDirectionTest`: enum values `DEBIT`, `CREDIT`; not equal to `CashflowDirection`.
- [x] 1.2 Write `TransactionTest` for new canonical constructor: `direction` required; overload defaults to `CREDIT`; null validation.
- [x] 1.3 Write `CashflowMovementDraftTest` for new `direction` field: required, not null; amount must stay positive.
- [x] 1.4 Write `CashflowMovementRecordTest` for new `direction` field: round-tripped from draft; nullable disallowed.
- [x] 1.5 Write `ExternalStatementEntryTest`: accepts signed amounts unchanged (adapter owns mapping).

### GREEN

- [x] 1.6 Create `domain/cashflow/TransactionDirection.java`: `public enum TransactionDirection { DEBIT, CREDIT }`.
- [x] 1.7 Add `TransactionDirection direction` to `Transaction` canonical ctor; overload defaults to `CREDIT`; validate not null.
- [x] 1.8 Add `TransactionDirection direction` to `CashflowMovementDraft` record; validate not null.
- [x] 1.9 Add `TransactionDirection direction` to `CashflowMovementRecord` record; validate not null, propagate from draft in `IngestionOutcome` static factories.
- [x] 1.10 Add `TransactionDirection direction` to `ExternalStatementEntry` (default `null` in old overload; adapter populates).
- [x] 1.11 Add `TransactionDirection direction` to `ProjectionReadyCashflowTransaction`, `ProjectedCashflowTransaction`, `PendingManualReviewMovement` records.

### REFACTOR

- [x] 1.12 Run `./gradlew test` — all PR #1 tests green. Verify `ArchitectureTest` still passes.

**AC**: Tests for direction presence pass. Domain records carry `TransactionDirection`.

**PR #1 scope note**: Full ingestion propagation is intentionally deferred to PR #3 (`Adapter sign→direction + ingestion propagation`). PR #1 keeps compatibility defaults (`CREDIT`) where older constructors are still used.

---

## PR #2 — Persistence

### RED

- [x] 2.1 Write migration integration test: Flyway V4 applies cleanly; `movement_direction` column exists with `NOT NULL DEFAULT 'CREDIT'`.
- [x] 2.2 Extend `CashflowMovementHistoryJdbcAdapterTest`: INSERT with `DEBIT` reads back `DEBIT`; old rows without column default to `CREDIT`.

### GREEN

- [x] 2.3 Create `db/migration/V4__add_movement_direction.sql`: `ALTER TABLE cashflow_movement_history ADD COLUMN movement_direction VARCHAR(6) NOT NULL DEFAULT 'CREDIT' CHECK (movement_direction IN ('DEBIT','CREDIT'))`.
- [x] 2.4 Add `movement_direction` to `CashflowMovementHistoryJdbcAdapter` INSERT (values list, column list); map from `draft.direction().name()`.
- [x] 2.5 Add `movement_direction` to SELECT column list; map in `mapRow()` via `rs.getString("movement_direction")` → `TransactionDirection.valueOf(...)`.

### REFACTOR

- [x] 2.6 Run `./gradlew test --tests "*CashflowMovementHistoryJdbcAdapter*"` — all green. Verify Flyway migration test.

**AC**: Migration applies; DEBIT/CREDIT round-trips through DB read/write.

---

## PR #3 — Ingestion + Fingerprint

### RED

- [x] 3.1 Write `SimulatedBankStatementAdapterTest`: `amount=-15000` → `IngestionItem` has `direction=DEBIT, amount=15000`; `amount=15000` → `CREDIT, amount=15000`.
- [x] 3.2 Write `TransactionFingerprintTest`: direction excluded — same `(profileId,amount,currency,date,desc)` produces identical hash regardless of `DEBIT`/`CREDIT`.
- [x] 3.3 Write `CashflowIngestionServiceTest`: draft carries direction from adapter through to persisted draft.

### GREEN

- [x] 3.4 In `SimulatedBankStatementAdapter`: replace `amount.abs()` with sign→direction mapping: `signum() < 0 → DEBIT`, `≥ 0 → CREDIT`; pass positive `abs()` amount.
- [x] 3.5 In `CashflowIngestionService.IngestionOutcome` factories: pass `transaction.direction()` to draft constructors.
- [x] 3.6 Document in `TransactionFingerprint.java` javadoc: direction excluded from `fp:v1` by design (no code change needed — fields unchanged).

### REFACTOR

- [x] 3.7 Run `./gradlew test --tests "*SimulatedBankStatement*" --tests "*TransactionFingerprint*" --tests "*CashflowIngestionService*"`. Verify green.

**AC**: Negative bank amounts → DEBIT draft; fingerprint stable across direction changes.

---

## PR #4 — API Contracts

### RED

- [x] 4.1 Extend `CashflowManualImportControllerTest`: optional `movementDirection` on row; omitted defaults `CREDIT`; invalid value rejected with field error.
- [x] 4.2 Extend `CashflowBankStatementSimulatedControllerTest`: response entries include `movementDirection`.
- [x] 4.3 Extend `CashflowHistoryControllerTest`: `PendingManualReviewMovementResponse` and `ProjectionReadyTransactionResponse` include `movementDirection`.
- [x] 4.4 Extend `CashflowProjectionControllerTest`: response DTOs include optional `movementDirection`.
- [x] 4.5 Extend `ManualReviewResolutionControllerTest`: resolution response exposes direction.

### GREEN

- [x] 4.6 Add `String movementDirection` to `CashflowManualImportController.ManualImportRow`; default `null` → resolve to `CREDIT`; validate enum.
- [x] 4.7 Add `movementDirection` to all controller response DTOs: `CashflowBankStatementSimulatedController`, `CashflowHistoryController` (both responses), `CashflowProjectionController`, `ManualReviewResolutionController`.
- [x] 4.8 Map `TransactionDirection` to string in response `.from()` methods; default direction on manual import row resolution.

### REFACTOR

- [x] 4.9 Run `./gradlew test --tests "*Controller*" --tests "*Composition*"`. Verify all controller tests green.

**AC**: All API responses include `movementDirection`; manual import accepts optional direction with CREDIT default.

---

## PR #5 — Mismatch Recommendation

### RED

- [x] 5.1 Write test in `HistoryRecommendationServiceTest`: movements with `DEBIT + INFLOW` category trigger `DIRECTION_MISMATCH` signal with aggregate counts.
- [x] 5.2 Write test: all aligned movements (DEBIT+OUTFLOW, CREDIT+INFLOW) yield no mismatch signal.

### GREEN

- [x] 5.3 In `HistoryRecommendationService`: add `addDirectionMismatchSignal()` method. Query projectable movements; count where `movementDirection` mismatches category direction. Emit `DIRECTION_MISMATCH` INFO signal with `debitInflowCount`, `creditOutflowCount` metrics. No sensitive data exposed.
- [x] 5.4 Wire new signal into `generate()` method — called after existing signals, before healthy-default fallback.

### REFACTOR

- [x] 5.5 Run `./gradlew test --tests "*HistoryRecommendation*"`. Verify green. Check signal ordering: mismatch ranks as INFO (severity rank 1).

**AC**: DIRECTION_MISMATCH signal appears when movements have conflicting directions; absent when aligned.

---

## PR #6 — Smoke + Docs

### Tasks

- [x] 6.1 Write smoke test: ingest negative bank amount → DEBIT persisted with positive amount → history query returns `DEBIT`. Run both DEBIT and CREDIT paths.
- [x] 6.2 Write smoke test: manual import with no direction defaults to CREDIT; explicit DEBIT persists as DEBIT. Re-ingestion with same fingerprint but changed direction returns existing movement.
- [x] 6.3 Remove direction-loss tradeoff documentation from OpenSpec specs (specs already in `cashflow-bank-statement-import/spec.md` REMOVED section).
- [x] 6.4 Update `openspec/specs/` baseline with direction-preservation requirements (if separate baseline exists).
- [x] 6.5 Run `./gradlew test` — full suite green. Verify `ArchitectureTest` enforces hexagonal boundaries.

**AC**: Smoke test proves DEBIT+CREDIT end-to-end. Idempotency preserved across direction changes.

---

### Verification Commands

```bash
# Full suite
./gradlew test

# Per PR
./gradlew test --tests "*TransactionDirection*" --tests "*TransactionTest*"
./gradlew test --tests "*CashflowMovementHistoryJdbcAdapter*" --tests "*Flyway*"
./gradlew test --tests "*SimulatedBankStatement*" --tests "*TransactionFingerprint*" --tests "*CashflowIngestionService*"
./gradlew test --tests "*Controller*"
./gradlew test --tests "*HistoryRecommendation*"

# Architecture gates
./gradlew test --tests "*ArchitectureTest*"
```
