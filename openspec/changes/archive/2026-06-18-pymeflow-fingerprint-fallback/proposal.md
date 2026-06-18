# Proposal: PymeFlow Fingerprint Fallback

## Intent

Close the duplicate-ingestion gap when `externalReference` is omitted. Today those items persist with `source_reference = null`, bypass duplicate lookup, and can corrupt cashflow history on retries.

## Scope

### In Scope
- Generate deterministic `source_reference` fallback as `fp:v1:<sha256-hex>` when `externalReference` is absent.
- Reuse existing `findBySourceReference` flow and unique partial index; repeated no-reference transactions return the existing movement.
- Normalize fingerprint inputs and cover MVP edge cases in tests.
- Keep sensitive explicit `externalReference` rejection and avoid echoing sensitive values.

### Out of Scope
- New fingerprint column, new port methods, or DB migration.
- Request-level `Idempotency-Key` support.
- Perfect disambiguation of legitimate identical repeated cash transactions; accepted MVP edge case to document/test.

## Capabilities

### New Capabilities
- `cashflow-ingestion-idempotency`: deterministic fallback idempotency for cashflow ingestion when client references are omitted.

### Modified Capabilities
- None.

## Approach

In `CashflowIngestionService`, compute an effective reference before lookup: explicit trimmed `externalReference` wins; otherwise generate `fp:v1:<hash>`. Hash input: `pymeflow|v1|{profileId}|{amount}|{currency}|{date}|{safeDescription}`. Normalize amount to canonical decimal text, currency uppercase ISO code, date ISO-8601, and description as trimmed text with collapsed whitespace; null description becomes empty. Hash with SHA-256 and lowercase hex. Store only the generated reference, not raw fingerprint input.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `application/CashflowIngestionService.java` | Modified | Effective reference generation, lookup, duplicate behavior. |
| `application/.../IngestionItem` | Modified | Reuse existing external reference normalization. |
| `CashflowIngestionServiceTest.java` | Modified | No-reference dedup, normalization, identical-cash edge case. |
| `CashflowMovementHistoryJdbcAdapterTest.java` | Modified | Align H2 partial unique index with production behavior. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Legitimate identical no-reference cash transactions deduped | Med | Document/test as MVP limitation; clients needing precision send `externalReference`. |
| Hash collision | Low | SHA-256 makes practical collision risk negligible. |
| Sensitive data exposure | Low | Do not echo raw inputs; keep sensitive explicit references rejected. |

## Rollback Plan

Revert service/test changes. Generated `fp:v1:` rows remain readable as `source_reference`; future migration can distinguish them by prefix if needed.

## Dependencies

- Java `MessageDigest`; no new external dependency.
- Review forecast: expected under 400 changed lines. Forced chained strategy is active, but not needed by size; one focused PR is sufficient if policy allows.

## Success Criteria

- [ ] Re-ingesting the same no-reference transaction returns the existing movement.
- [ ] Explicit `externalReference` behavior and sensitive-reference rejection remain unchanged.
- [ ] Generated references use `fp:v1:` and fit `VARCHAR(80)`.
