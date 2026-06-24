# Design: PymeFlow Cashflow Direction Hardening

## Technical Approach

Introduce `TransactionDirection { DEBIT, CREDIT }` in `domain.cashflow`, propagate through `Transaction`, drafts, records, persistence, and API responses. Keep `CashflowDirection` category-only. Map signed bank amounts to `direction + positive amount` at the adapter boundary. Exclude direction from `fp:v1` to preserve idempotency.

## Architecture Decisions

| Decision | Choice | Alternatives | Rationale |
|---|---|---|---|
| Direction enum | `TransactionDirection` in `domain.cashflow` | Reuse `CashflowDirection` | Category and movement direction are orthogonal concepts. `TRANSFER` is meaningless for bank movements. |
| Transaction compat | Add required `direction` to canonical record constructor; provide overload defaulting to `CREDIT` | Break all call sites immediately | Overload lets PR #1 compile without touching every controller in the same commit. |
| DB migration | `V4__add_movement_direction.sql`: additive `NOT NULL DEFAULT 'CREDIT'` with `CHECK (IN ('DEBIT','CREDIT'))` | Backfill from source | Default is safe for existing rows; no source material exists for backfill. |
| Amount constraint | Keep `amount > 0` everywhere | Allow signed amounts | Signed amounts conflate magnitude and direction, breaking projection math and existing checks. |
| Fingerprint | Exclude `direction` from `fp:v1` | Include it | Including direction would break idempotency for re-imported transactions that now map to DEBIT. |
| Adapter mapping | `SimulatedBankStatementAdapter` maps `signum() < 0 → DEBIT`, `≥ 0 → CREDIT` | Controller maps sign | Adapter owns provider normalization; controller stays thin. |
| API defaults | Optional `direction` on manual/import requests, default `CREDIT`; additive `direction` on all responses | Required direction | Avoids breaking existing API consumers. |
| Projection math | Continue using `CashflowDirection` for sums; add mismatch visibility only | Use movement direction for sums | Category direction drives cashflow semantics; movement direction is audit/trace. |

## Data Flow

```
Bank Row (signed amount)
    ↓
SimulatedBankStatementAdapter → sign→direction + abs(amount)
    ↓
ExternalStatementEntry(direction, positive amount)
    ↓
CashflowIngestionService → Transaction(direction, positive amount)
    ↓
CashflowMovementDraft(direction, positive amount)
    ↓
CashflowMovementHistoryJdbcAdapter → DB (movement_direction, amount>0)
    ↓
History/Projection/Recommendation responses include direction additively
```

## File Changes

| File | Action | Description |
|---|---|---|
| `domain/cashflow/TransactionDirection.java` | Create | `DEBIT, CREDIT` enum |
| `domain/cashflow/Transaction.java` | Modify | Add `direction` parameter; overload defaulting to `CREDIT` |
| `application/cashflow/CashflowMovementDraft.java` | Modify | Add `TransactionDirection direction` field |
| `application/cashflow/CashflowMovementRecord.java` | Modify | Add `TransactionDirection direction` field |
| `application/cashflow/ProjectionReadyCashflowTransaction.java` | Modify | Add direction field |
| `application/cashflow/PendingManualReviewMovement.java` | Modify | Add direction field |
| `application/cashflow/ProjectedCashflowTransaction.java` | Modify | Add direction field (optional for mismatch detection) |
| `application/port/out/ExternalStatementEntry.java` | Modify | Add `TransactionDirection direction` field |
| `application/cashflow/TransactionFingerprint.java` | Modify | Document direction exclusion; no code change |
| `infrastructure/bank/SimulatedBankStatementAdapter.java` | Modify | Replace `abs()` with sign→direction mapping |
| `infrastructure/persistence/CashflowMovementHistoryJdbcAdapter.java` | Modify | SELECT/INSERT `movement_direction` column |
| `db/migration/V4__add_movement_direction.sql` | Create | Additive column with default `CREDIT` |
| `interfaces/web/CashflowBankStatementSimulatedController.java` | Modify | Response DTOs include `direction` |
| `interfaces/web/CashflowManualImportController.java` | Modify | Optional `direction` in `ManualImportRow`; response includes it |
| `interfaces/web/CashflowHistoryController.java` | Modify | Response DTOs include `direction` |
| `interfaces/web/CashflowProjectionController.java` | Modify | Optional `direction` in `ProjectedTransactionRequest` |
| `interfaces/web/ManualReviewResolutionController.java` | Modify | Response DTOs include `direction` |
| `application/cashflow/CashflowIngestionService.java` | Modify | Propagate direction through inner records and drafts |
| `application/cashflow/CashflowMovementHistoryService.java` | Modify | Pass direction through resolution path |
| `application/recommendation/HistoryRecommendationService.java` | Modify | Add `DIRECTION_MISMATCH` signal (PR #5) |

## Interfaces / Contracts

```java
package com.kuroneko.pymeflow.domain.cashflow;

public enum TransactionDirection {
    DEBIT,
    CREDIT
}
```

`Transaction` canonical constructor:
```java
public record Transaction(String description, BigDecimal amount, Currency currency, LocalDate bookedAt, TransactionDirection direction) {
    public Transaction(String description, BigDecimal amount, Currency currency, LocalDate bookedAt) {
        this(description, amount, currency, bookedAt, TransactionDirection.CREDIT);
    }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | `TransactionDirection` enum, `Transaction` record validation | JUnit 5 + AssertJ |
| Unit | `SimulatedBankStatementAdapter` sign→direction mapping | Mockito isolation test |
| Unit | `TransactionFingerprint` stability (direction excluded) | AssertJ hash equality |
| Integration | Flyway V4 migration applies cleanly | `@JdbcTest` or Testcontainers |
| Integration | Controller DTO serialization with new direction field | `@WebMvcTest` |
| Smoke | Ingest negative amount → DB row has `DEBIT` and positive `amount` | End-to-end via `@SpringBootTest` |

## Migration / Rollout

1. Deploy PR #1–#2 (domain + persistence). Existing rows default to `CREDIT`. No downtime.
2. Deploy PR #3–#4 (adapter + API). Consumers receive additive `direction` field.
3. Deploy PR #5 (mismatch signals). Non-breaking additive behavior.
4. Rollback: revert PRs in reverse order. If persistence rolled back, `movement_direction` column remains harmless or can be dropped via rollback migration after code revert.

## Open Questions

- [ ] Should `DIRECTION_MISMATCH` in PR #5 block projection or remain informational? (Defer to spec review.)
- [ ] Do real bank adapters (future) need `TransactionDirection` values beyond DEBIT/CREDIT (e.g., `UNKNOWN`)? (Defer; current enum is sufficient for MVP.)

## Verification Clarification

`CashflowIngestionController` is intentionally not a direction DTO target for this change. The archived specs and tasks scope direction-bearing API contracts to manual import, simulated bank-statement import, history/projection-ready responses, projection request mapping, and manual-review resolution responses. The legacy `/api/cashflow/ingestions` endpoint remains compatibility-only and continues to use the `Transaction` compatibility constructor defaulting omitted movement direction to `CREDIT`.

## 6-PR Chained Implementation

| PR | Scope | Files ~ | Budget |
|---|---|---|---|
| #1 Domain | `TransactionDirection`, `Transaction`, `Draft`, `Record`, `ProjectedCashflowTransaction`, `ProjectionReadyCashflowTransaction`, `PendingManualReviewMovement`, `ExternalStatementEntry` | 8 | ~120 lines |
| #2 Persistence | Flyway V4, JDBC adapter column read/write | 3 | ~60 lines |
| #3 Ingestion | `SimulatedBankStatementAdapter`, `CashflowIngestionService`, fingerprint docs/tests | 5 | ~130 lines |
| #4 API Contracts | All controller request/response DTOs | 6 | ~150 lines |
| #5 Mismatch + Recommendations | Projection alert for mismatch, `HistoryRecommendationService` new signal | 4 | ~80 lines |
| #6 Specs/Docs | OpenSpec delta specs, remove direction-loss tradeoff, neutral Spanish user docs | 5 | ~100 lines |
