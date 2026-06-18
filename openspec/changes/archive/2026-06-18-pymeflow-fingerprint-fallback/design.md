# Design: PymeFlow Fingerprint Fallback

## Technical Approach

When `externalReference` is absent, compute a deterministic SHA-256 fingerprint and use it as the effective `source_reference`. This reuses the existing `findBySourceReference` dedup path and the partial unique index `WHERE source_reference IS NOT NULL` without any schema change.

The fingerprint generator lives in the application layer as a stateless helper. `CashflowIngestionService` resolves an **effective reference** per item (explicit wins, fallback = `fp:v1:<hash>`) and passes it through the existing lookup and persistence flow.

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Fingerprint location | Application-layer helper class (`TransactionFingerprint`) | Pure function, no I/O, easy to unit-test; keeps domain free of hashing concerns |
| Column reuse | Reuse `source_reference` with `fp:v1:` prefix | Zero migration; partial unique index already guards non-null values |
| Hash algorithm | SHA-256, lowercase hex | Negligible collision risk; fits `VARCHAR(80)` (`fp:v1:` prefix + 64 chars = 70 chars) |
| Input normalization | `pymeflow\|v1\|{profileId}\|{amount}\|{currency}\|{date}\|{safeDescription}` | Pipe-delimited prevents field-boundary ambiguity; canonical formats remove formatting variance |
| Amount normalization | `BigDecimal.toPlainString()` | Removes scientific notation and locale variance |
| Description normalization | Trim + collapse whitespace (`\\s+` → ` `) | Identical descriptions with extra spaces produce the same hash |
| Null description handling | Empty string in hash, `null` in draft | Fingerprint input must be stable; draft stores `null` as before |
| H2 test index | Retain H2-compatible unfiltered unique index | H2 rejected PostgreSQL-style partial index syntax in this project; its unique index still allows multiple NULLs and enforces duplicate non-null references for tests |

## Data Flow

```
IngestionItem
    │
    ▼
externalReference present & not blank?
    ├─ Yes ──→ sensitive? ──→ reject or lookup by explicit reference
    └─ No  ──→ TransactionFingerprint.compute(profileId, transaction)
                     │
                     ▼
              fp:v1:<sha256>
                     │
                     ▼
         findBySourceReference(profileId, fp:v1:...)
                     │
            ┌────────┴────────┐
            ▼                 ▼
       existing found    not found
            │                 │
            ▼                 ▼
       return existing   categorize & save
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `application/cashflow/TransactionFingerprint.java` | Create | Stateless helper: normalizes inputs, computes SHA-256, returns `fp:v1:<hex>` |
| `application/cashflow/CashflowIngestionService.java` | Modify | Resolve effective reference before lookup; pass it through existing flow |
| `application/cashflow/CashflowIngestionServiceTest.java` | Modify | Tests: no-reference dedup, normalization stability, identical-cash edge case, blank-as-omitted now generates fp |
| `infrastructure/persistence/CashflowMovementHistoryJdbcAdapterTest.java` | Modify | Retain H2-compatible unique index setup and add duplicate `fp:v1:` reference coverage for the save catch path |

## Interfaces / Contracts

```java
// application/cashflow/TransactionFingerprint.java
final class TransactionFingerprint {
    static String compute(ProfileId profileId, Transaction transaction) { ... }
}
```

Normalization contract:
- `profileId.value()` as-is
- `amount.toPlainString()` — no exponent, no locale grouping
- `currency.getCurrencyCode().toUpperCase()`
- `date.toString()` — ISO-8601 (`YYYY-MM-DD`)
- `description` — `null` becomes `""`; otherwise `trim().replaceAll("\\s+", " ")`

Hash input template:
```
pymeflow|v1|{profileId}|{amountPlain}|{currencyUpper}|{dateIso}|{normalizedDescription}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `TransactionFingerprint.compute` normalization and determinism | Direct invocation with varied inputs; assert same/different hashes |
| Unit | `CashflowIngestionService` no-reference dedup | Mock `CashflowMovementHistoryPort`; verify `findBySourceReference` called with `fp:v1:` prefix; second identical item returns existing without save |
| Unit | `CashflowIngestionService` explicit reference unchanged | Existing tests continue to pass; sensitive rejection untouched |
| Integration | H2 duplicate handling for fingerprint references | `CashflowMovementHistoryJdbcAdapterTest`: insert same fingerprint twice, assert second returns same ID |
| Integration | H2 duplicate reference behavior | Retain the H2-compatible unfiltered unique index because it allows multiple NULL values in this project and still enforces duplicate non-null references, including `fp:v1:` references |

## Migration / Rollout

No migration required. Existing rows with `source_reference = null` remain as-is. Future re-ingestion of previously unreferenced transactions will generate new fingerprints and insert new rows; this is acceptable because those historical rows were already non-deterministic duplicates.

Rollback: revert service and test changes. Rows already persisted with `fp:v1:` remain valid `source_reference` values and do not break any query.

## Open Questions

- [x] **H2 partial index syntax verification**: H2 rejected `CREATE UNIQUE INDEX ... WHERE ...` in this project, so the divergence is documented and the H2-compatible unique index is retained.
- [ ] **Identical-cash edge case documentation location**: Add note to API docs (`CashflowIngestionController` OpenAPI description) or internal ADR? **Decision**: OpenAPI description is user-facing and appropriate for Chilean SMB clients.

## Estimate

- New code: ~50 lines (fingerprint helper + service integration)
- Test additions: ~100 lines
- Test modifications: ~20 lines
- **Total changed lines: ~170** — well under 400-line budget. **Single focused PR is sufficient**; forced chained strategy not required by size.
