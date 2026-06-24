# Proposal: PymeFlow Cashflow Direction Hardening

## Intent

Prepare cashflow ingestion for real bank integrations by preserving bank movement direction end-to-end. Today signed statement amounts are converted to positive amounts and the debit/credit signal is lost, while category direction (`INFLOW`/`OUTFLOW`) is used for cashflow semantics. This change separates movement direction (`DEBIT`/`CREDIT`) from category direction and keeps amounts as positive magnitudes.

## Scope

### In Scope
- Add domain `TransactionDirection` and propagate through transactions, drafts, records, persistence, history, projection, and recommendation paths.
- Add migration/defaults: `movement_direction NOT NULL DEFAULT 'CREDIT'`; keep `amount > 0`.
- Map signed bank-adapter amounts to `direction + positive amount`; expose additive request/response fields.
- Update import adapters, history/projection/recommendation responses, tests, and neutral Spanish user-facing docs/examples.
- Preserve idempotency: exclude direction from the existing fingerprint unless a later spec explicitly accepts a breaking hash version.
- Deliver as ~6 chained PRs: domain, persistence, external ingestion, API contracts, mismatch/recommendations, specs/docs.

### Out of Scope
- Real bank APIs, OAuth, balances, or provider batch persistence.
- Full accounting model or signed/negative internal amounts.
- Changing category semantics (`CashflowDirection` remains category-only).

## Capabilities

### New Capabilities
- `cashflow-direction-preservation`: Movement direction is modeled, stored, and returned separately from category direction.

### Modified Capabilities
- `cashflow-bank-statement-import`: Replace direction-loss tradeoff with signed amount → `DEBIT`/`CREDIT` mapping.
- `cashflow-manual-import`: Accept additive optional direction input while preserving positive amounts.
- `cashflow-ingestion-idempotency`: Document direction exclusion from `fp:v1` for backward compatibility.
- `cashflow-history-recommendations`: Add direction mismatch visibility/recommendation without exposing sensitive data.

## Approach

Use full domain propagation: introduce `TransactionDirection { DEBIT, CREDIT }`, keep `CashflowDirection { INFLOW, OUTFLOW, TRANSFER }` for categories, and preserve positive amounts. Existing rows default to `CREDIT`. Projection math continues to use category direction; movement/category mismatches are surfaced, not silently corrected.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/.../domain/cashflow` | New/Modified | Direction enum and transaction records |
| `src/main/java/.../application/cashflow` | Modified | Drafts, records, ingestion, projection, recommendations |
| `src/main/java/.../infrastructure` | Modified | JDBC, Flyway V4, simulated bank adapter |
| `src/main/java/.../interfaces` | Modified | Additive DTO request/response fields |
| `src/test/java`, `openspec/specs` | Modified | Tests and spec deltas |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Review size blowout | High | Force chained PRs under 400-line budget |
| Fingerprint churn | Med | Keep `fp:v1` excluding direction |
| Default mislabels old debits | Med | Additive `CREDIT` default documented; no data loss |
| Direction/category confusion | Med | Specs name both concepts explicitly |

## Rollback Plan

Revert chained PRs in reverse order. If persistence is deployed, keep the additive column harmless or run a rollback migration to drop `movement_direction` after code rollback.

## Dependencies

- Existing cashflow import/history/projection code and Flyway migration path.

## Success Criteria

- [ ] Bank-like negative amounts become `DEBIT` with positive amount; positive amounts become `CREDIT`.
- [ ] Existing rows read as `CREDIT` and current idempotency hashes remain stable.
- [ ] History/projection/recommendation responses expose movement direction additively.
