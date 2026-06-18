# Exploration: PymeFlow Fingerprint Fallback for Ingestion Idempotency

## Current State

The existing idempotency MVP relies entirely on the optional `externalReference` field mapped to `source_reference` in persistence. The flow in `CashflowIngestionService.ingest()` (lines 46-57) only performs a duplicate lookup when `externalReference` is non-null. When omitted or blank, the item bypasses the lookup entirely and always produces a new insert with `source_reference = null`. The V3 unique partial index (`WHERE source_reference IS NOT NULL`) does not constrain null values, so repeated ingestion of the same transaction without `externalReference` creates a new row every time - corrupting cashflow history, projections, and trust metrics.

### Architecture Boundaries

- `domain/` - `Transaction`, `ProfileId`, `VerticalProfile`, `CashflowCategory` (pure POJOs, zero framework deps)
- `application/` - `CashflowIngestionService`, `SensitiveDataPolicy`, `CashflowMovementDraft`, `CashflowMovementRecord` (uses port abstractions only)
- `application/port/out/` - `CashflowMovementHistoryPort` with `findBySourceReference(ProfileId, String)`
- `infrastructure/persistence/` - `CashflowMovementHistoryJdbcAdapter` (JDBC adapter, `@Repository`)
- `interfaces/web/` - `CashflowIngestionController` (REST, `@RestController`)
- Enforced by `ArchitectureTest`: domain has zero framework imports; application has zero infrastructure/interfaces imports; no bank/provider/vertical literals in domain/application

### Current Deduplication Flow

```
IngestionItem normalizes externalReference: blank -> null, trimmed -> value
  +-- externalReference != null && sensitive -> rejected (source_reference not persisted)
  +-- externalReference != null -> findBySourceReference(profileId, ref)
  |   +-- found -> return existing movement (no insert)
  |   +-- not found -> categorize -> insert with source_reference
  +-- externalReference == null -> categorize -> insert with source_reference = null  <-- GAP
```

### DB Constraint Analysis

- V2 creates `source_reference VARCHAR(80)` nullable column
- V3 creates `UNIQUE INDEX ... ON (profile_id, source_reference) WHERE source_reference IS NOT NULL`
- `CashflowMovementHistoryJdbcAdapter.save()` catches `DuplicateKeyException` only when `draft.sourceReference() != null` (line 148)
- Null `source_reference` inserts never trigger the unique constraint; H2 test fixture uses full unique index (no WHERE clause) but production PostgreSQL uses partial index

## Affected Areas

- `CashflowIngestionService.java` — duplicate detection logic (lines 42-70), `IngestionOutcome` factories (lines 177-246)
- `CashflowIngestionService.CashflowIngestionCommand.IngestionItem` — externalReference normalization (lines 108-117)
- `CashflowMovementDraft.java` — sourceReference field, normalization (line 39)
- `CashflowMovementHistoryJdbcAdapter.java` — `save()` DuplicateKeyException handling (lines 121-155), `findBySourceReference` (lines 62-75)
- `V2__create_cashflow_movement_history.sql` — `source_reference VARCHAR(80)` column
- `V3__unique_partial_index_source_reference.sql` — unique partial index
- `CashflowIngestionServiceTest.java` — duplicate tests, blank externalReference (lines 164-276)
- `CashflowMovementHistoryJdbcAdapterTest.java` — DB-level dedup (lines 163-172)
- `CashflowIngestionControllerTest.java` — integration coverage

## Approaches

### 1. Persisted Fallback Source Reference Using Deterministic Fingerprint (RECOMMENDED)

Generate a deterministic SHA-256 fingerprint from transaction identity fields when `externalReference` is absent. Store the fingerprint as the `source_reference` value, reusing the existing unique partial index for automatic deduplication.

- **Fingerprint formula**: `SHA-256("pymeflow|{profileId}|{amount}|{currency}|{date}|{safeDescription}")` -> 64 hex chars
- **Namespace prefix** (`pymeflow`) prevents collision with real externalReference values and makes system-generated values identifiable
- **No database migration needed** - reuses existing `source_reference VARCHAR(80)` column and V3 unique partial index
- **Service layer change**: generate fingerprint when externalReference is null, use it for the `findBySourceReference` lookup and as the persisted `source_reference`
- **DB adapter**: no changes needed - `save()` already handles `DuplicateKeyException` for non-null source_reference, and `findBySourceReference` already returns empty for null ref
- **Test scope**: verify fingerprint dedup of identical transactions without externalReference; verify non-duplicate behavior across different profiles/amounts/descriptions

**Pros**:
- Zero schema migration - reuses existing column and unique partial index
- Minimal code change - one fingerprint method in service layer + flow adjustment in `ingest()`. No port changes, no adapter changes
- Deterministic - same transaction always produces same fingerprint; safe for retries
- Automatic dedup via existing DB constraint at insert time
- Backward compatible - externalReference present behavior unchanged
- Pure application-layer computation; zero infrastructure/domain changes

**Cons**:
- Semantic ambiguity - `source_reference` mixes client-provided and system-generated values (mitigated by `pymeflow|` namespace prefix)
- Overblocking risk - two genuine transactions with identical (profile, amount, currency, date, description) would be treated as duplicates (edge case, acceptable for MVP; see Risk analysis below)
- Fingerprint values are opaque hex strings in DB (not human-friendly for debugging)

**Effort**: Low

---

### 2. Separate Fingerprint Column and Index

Add a new `fingerprint VARCHAR(64)` column with its own unique partial index, independent from `source_reference`. The fingerprint is always generated (regardless of externalReference presence), and deduplication checks both fields independently.

- **Requires**: V4 migration adding `fingerprint` column + unique partial index
- **Service layer**: generate fingerprint for every transaction, use both `findBySourceReference` and `findByFingerprint` for dedup
- **Port interface**: new `findByFingerprint(ProfileId, String)` method on `CashflowMovementHistoryPort`
- **DB adapter**: new query method, new column in `mapRow()`, new `DuplicateKeyException` handling for fingerprint index

**Pros**:
- Clean separation of concerns - client reference and system fingerprint are distinct columns
- Fingerprint can be generated for ALL transactions (including those with externalReference) for future use
- No semantic confusion in `source_reference`

**Cons**:
- DB migration required (new column, new index, Flyway migration V4)
- More code changes - new port method, adapter method, service logic change
- Two indexes to maintain (storage and write overhead per insert)
- Over-engineering for MVP - the benefit (clean semantics) does not justify the complexity

**Effort**: Medium

---

### 3. Request-Level Idempotency Key (Header/API Key)

Require clients to send an `Idempotency-Key` HTTP header or a request-level key. The server stores the key and returns the original response for repeat requests.

- **Standard HTTP pattern**: `Idempotency-Key: {uuid}` header
- **Storage**: new table or cache (e.g., `idempotency_keys`) mapping key -> response digest
- **Behavior**: first request processes normally and stores response; repeat requests return stored response without re-processing

**Pros**:
- Well-understood HTTP pattern (Stripe, PayPal use it)
- No fingerprint complexity
- Works for whole-request dedup regardless of individual transaction fields

**Cons**:
- Burden on client - requires client to generate and manage idempotency keys per request
- Does not solve intra-batch dedup - if a single item within a batch is repeated across different requests, it would not be detected (different idempotency keys)
- Requires additional storage/cache infrastructure
- Pushes more responsibility onto API consumers
- Client retry semantics differ from data-level dedup (different layers of concern)

**Effort**: Medium-High

---

## Recommendation

**Approach 1 - deterministic fingerprint as fallback source_reference** - is the right choice for MVP:

1. **Minimizes change surface**: no DB migration, no new port methods, no new adapter methods. The only changes are in `CashflowIngestionService` (fingerprint generation + always performing the duplicate lookup) and tests.

2. **Overblocking risk is controlled and documented**: a genuine pharmacy could have two identical sales ("Venta Caja 1", CLP 125,000, same date) that would be treated as duplicates. This is an unlikely edge case in practice because:
   - Most points of sale have sequential or timestamped identifiers in their descriptions
   - The `safeDescription` stored reflects the actual transaction description, which typically varies
   - Clients who need precise dedup can ALREADY provide `externalReference` - this is the primary mechanism
   - The fingerprint is a FALLBACK, not a replacement for explicit dedup

3. **Architectural fit**: the fingerprint computation lives entirely in the application layer (a pure computation), with no infrastructure or domain changes. The unique partial index in the DB continues to be the enforcement layer. This keeps the hexagonal boundaries intact.

4. **Reversible**: if fingerprint collisions become a real problem, we can migrate to Approach 2 later. The namespace prefix (`pymeflow|...`) makes system-generated values distinguishable from client-provided ones in the DB, enabling clean migration paths.

### Implementation Sketch (Approach 1)

```
ingest() per item loop:
  effectiveRef = externalReference ?? fingerprint(profileId, transaction)
  if sensitive(effectiveRef) -> reject  [fingerprint always passes this; hash is never sensitive]
  existing = findBySourceReference(profileId, effectiveRef)
  if found -> return existing (no insert)
  categorize -> draft with sourceReference = effectiveRef -> insert with dedup protection
```

Fingerprint method (pure computation in application layer, uses `java.security.MessageDigest`):
- Input: `"pymeflow|{profileId.value()}|{amount}|{currency.code}|{date}|{safeDescription}"`
- Output: `SHA-256` hex digest (64 chars, fits in VARCHAR(80))
- Null-safe: safeDescription can be null (rejected transactions); use empty placeholder

### H2 Test Fixture Gap

The `CashflowMovementHistoryJdbcAdapterTest` creates the V3 index without the `WHERE source_reference IS NOT NULL` clause (line 63-67 of test). This means the H2 test fixture enforces uniqueness even for null source_reference rows, while production PostgreSQL only enforces for non-null. To test the fingerprint flow correctly, the V3 index in test must match production: `CREATE UNIQUE INDEX ... WHERE source_reference IS NOT NULL`. This is a pre-existing test fixture gap to fix alongside the implementation.

## Risks

1. **Fingerprint collision (low probability, high impact)**: Two different transactions produce the same SHA-256 hash. Cryptographic collision probability is negligible (~1/2^128 for 256-bit hash via birthday bound); practical risk is near zero.

2. **Overblocking legitimate repeats (medium probability, low impact)**: A pharmacy selling "Venta Caja 1" twice in one day for the same amount would be deduplicated. Mitigation: document as known MVP limitation; clients needing exact dedup must use `externalReference`. Impact is one "lost" transaction visible only in cashflow history.

3. **H2 test fixture divergence (medium probability, medium impact)**: The current H2 test creates a full unique index (no WHERE clause), so fingerprint tests would enforce stricter uniqueness than production. Must fix the test fixture to match production partial-index semantics.

4. **externalReference arriving as sensitive text with blank/null fallback (existing behavior)**: Already handled - sensitive externalReference is rejected before fingerprint generation. If absent, fingerprint is always non-sensitive (hash).

## Ready for Proposal

**Yes** - The exploration confirms the gap, identifies a low-effort/low-risk approach, and provides enough detail for the `sdd-propose` phase. The orchestrator should launch `sdd-propose` for `pymeflow-fingerprint-fallback`.

Key decisions needed from user before proposal:
- Confirm Approach 1 (fingerprint fallback as source_reference) is acceptable for MVP
- Confirm acceptable tradeoff on overblocking risk (documented limitation)
