# Tasks: PymeFlow Fingerprint Fallback

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~190 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | force-chained (not needed — under budget) |
| Chain strategy | n/a |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: n/a
400-line budget risk: Low

## Phase 1: RED — Fingerprint Unit Tests

- [x] 1.1 Create `src/test/.../application/cashflow/TransactionFingerprintTest.java` with determinism test: `compute(ProfileId("p1"), 1000.00, CLP, 2024-06-18, "Pago")` produces stable SHA-256 hex; verify same inputs → same hash.
- [x] 1.2 Add normalization tests: whitespace-only description collapses to `""`; `null` description → `""`; currency lowercase `"clp"` → `"CLP"`; `BigDecimal` `1.0` and `1.00` different; different fields produce different hashes.

## Phase 2: RED — Service No-Reference Dedup Tests (CashflowIngestionServiceTest)

- [x] 2.1 No-reference dedup: re-ingestion with omitted `externalReference` calls `findBySourceReference` with `fp:v1:` prefix; second identical item returns existing without new save.
- [x] 2.2 Materially different fields: changed amount produces different fingerprint → new movement created.
- [x] 2.3 Cross-profile scoping: same no-reference fields in different profiles create two distinct movements.
- [x] 2.4 Identical-cash edge case: two legitimate identical no-reference cash transactions → only one persisted (MVP limitation).
- [x] 2.5 Fix `blankExternalReferenceIsTreatedAsOmittedWithoutDeduplicationLookup`: assert `sourceReference` is `fp:v1:...` not `null`; assert `lookupCalls > 0`.

## Phase 3: GREEN — Implementation

- [x] 3.1 Create `TransactionFingerprint.java` in `application/cashflow/`: `static String compute(ProfileId, Transaction)` — normalizes fields, SHA-256, returns `fp:v1:<hex>`. Verify all RED tests pass.
- [x] 3.2 Modify `CashflowIngestionService.ingest()`: after sensitive-check, resolve `effectiveReference = externalReference != null ? externalReference : TransactionFingerprint.compute(...)`, use for `findBySourceReference` and draft `sourceReference`.

## Phase 4: GREEN — H2 Partial Index Alignment

- [x] 4.1 Verify `CashflowMovementHistoryJdbcAdapterTest.createSourceReferenceUniqueIndexForH2()` H2 alignment. Note: H2 rejected PostgreSQL-style `WHERE source_reference IS NOT NULL`, so the unfiltered unique index was retained because H2 allows multiple NULL values while still enforcing duplicate non-null references.
- [x] 4.2 Add test: insert same fingerprint reference twice triggers DuplicateKeyException → returns existing row via `save()` catch path.

## Phase 5: Polish

- [x] 5.1 Update `CashflowIngestionController` `@Operation` description: document fingerprint fallback for omitted `externalReference` and identical-cash MVP limitation in Spanish.
- [x] 5.2 Run `./gradlew.bat test --rerun-tasks`, verify all tests pass, confirm existing explicit-reference tests unchanged.

## Phase 6: Verification Remediation

- [x] 6.1 Add runtime coverage that creates a no-reference movement through fingerprint fallback, queries history, and asserts the returned `sourceReference` equals the generated `fp:v1:` reference.
- [x] 6.2 Correct design notes for generated reference length (`70`) and H2 partial-index test divergence.

## Phase 7: Post-Verification Artifact Cleanup

- [x] 7.1 Correct invalid/stale OpenSpec artifact details: replace the invalid one-character profile fixture with valid `ProfileId("p1")` examples, and align `design.md` H2 index notes with the retained H2-compatible index behavior.
