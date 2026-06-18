# Exploration: PymeFlow CSV/Manual Import MVP

## Current State

The existing `POST /api/cashflow/ingestions` endpoint (`CashflowIngestionController`) already accepts a list of transactions as JSON and processes them through `CashflowIngestionService`. The service handles deduplication (via `externalReference` or deterministic `fp:v1:<sha256>` fingerprint fallback), categorization (via `CashflowCategorizationPort`), sensitive-data rejection, and persistence. Each `IngestionItem` maps to exactly one movement in `cashflow_movement_history`.

However, the current endpoint has three limitations for realistic manual/CSV import use cases:

1. **All-or-nothing validation**: Spring `@Valid` on the request body rejects the entire batch if any single row fails validation. A real CSV import needs per-row error tolerance — valid rows should be processed even when some rows have formatting errors.

2. **Nested JSON format**: Each row is a full `CashflowTransactionRequest` object with all fields explicit. Users importing CSV data typically have flat rows with optional columns. Currency (CLP) could be defaulted instead of required per row.

3. **No import batch identity**: There is no concept of "this batch of 50 rows came from file X at timestamp Y." Movements are independently persisted with no batch correlation metadata. For the roadmap item of "realistic CSV/manual import," a lightweight batch label is useful for client-side audit correlation.

### Architecture Boundaries (Hexagonal)

```
interfaces/web/     <- NEW: CashflowManualImportController (REST)
application/        <- REUSE: CashflowIngestionService (unchanged)
application/port/out/ <- REUSE: CashflowMovementHistoryPort, CashflowCategorizationPort (unchanged)
infrastructure/      <- REUSE: CashflowMovementHistoryJdbcAdapter, ProfileDrivenCashflowCategorizationAdapter (unchanged)
domain/              <- REUSE: Transaction, ProfileId, CashflowCategory, VerticalProfile (unchanged)
```

No new application service, port interface, or infrastructure adapter is needed. The change is purely a new interface (web controller + DTOs) that maps CSV-like rows to the existing `IngestionItem` contract and delegates to `CashflowIngestionService`.

### Existing Dedup Flow (Reused As-Is)

```
IngestionItem normalizes externalReference: blank -> null, trimmed -> value
  +-- externalReference != null && sensitive -> rejected
  +-- externalReference != null -> findBySourceReference(profileId, ref)
  |   +-- found -> return existing (no insert)
  |   +-- not found -> categorize -> insert with source_reference
  +-- externalReference == null -> fingerprint fallback -> findBySourceReference(...)
      +-- found -> return existing
      +-- not found -> categorize -> insert with fp:v1: source_reference
```

This flow is already complete and tested. The CSV import MVP reuses it unchanged. No new dedup logic is needed.

### Transaction Domain Model

```java
public record Transaction(String description, BigDecimal amount, Currency currency, LocalDate bookedAt) {
    // description: required, non-blank
    // amount: required, non-null
    // currency: required, non-null
    // bookedAt: required, non-null
}
```

## Affected Areas

| File | Impact | Description |
|------|--------|-------------|
| `interfaces/web/CashflowManualImportController.java` | **New** | REST controller for `POST /api/cashflow/imports/manual` |
| `interfaces/web/CashflowManualImportController.java` (DTOs) | **New** | `ManualImportRequest`, `ManualImportResponse`, `RowErrorResponse`, per-row record |
| `interfaces/web/ApiExceptionHandler.java` | **Modified** | Add friendly error messages for import-specific `IllegalArgumentException` cases |
| `interfaces/web/CashflowManualImportControllerTest.java` | **New** | `@WebMvcTest` for manual import endpoint |
| `application/cashflow/` | **Unchanged** | `CashflowIngestionService`, `SensitiveDataPolicy`, `TransactionFingerprint` reused as-is |
| `application/port/out/` | **Unchanged** | All port interfaces reused |
| `infrastructure/` | **Unchanged** | All adapters reused |
| `domain/` | **Unchanged** | All domain types reused |
| DB migrations | **None** | No new columns, no new indexes |

## Approaches

### 1. Per-Row-Tolerant JSON Batch Endpoint (RECOMMENDED)

New endpoint `POST /api/cashflow/imports/manual` that accepts flat JSON rows with per-row validation tolerance. Currency defaults to CLP when omitted. Valid rows are converted to `IngestionItem` and fed to the existing `CashflowIngestionService.ingest()`. Invalid rows are reported in the response alongside processed results.

**Input contract:**
```json
{
  "profileId": "pharmacy-cl",
  "importLabel": "Ventas junio 2026",
  "rows": [
    {"description": "Venta Caja 1", "amount": 125000, "date": "2026-06-15"},
    {"description": "Pago proveedor", "amount": 450000, "date": "2026-06-10", "externalReference": "prov-001"}
  ]
}
```

**Fields per row:**
| Field | Required | Default | Validation |
|-------|----------|---------|------------|
| `description` | Yes | — | Non-blank string |
| `amount` | Yes | — | Positive `BigDecimal` |
| `date` | Yes | — | Valid `LocalDate` |
| `currency` | No | `"CLP"` | Must be `"CLP"` if provided |
| `externalReference` | No | `null` | String, triggers fingerprint fallback if omitted/blank |

**Per-row validation strategy:** The controller does NOT use `@Valid` on the request body. Instead, it accepts a raw DTO and programmatically validates each row, collecting errors into a list. Rows that pass validation (all required fields present, types correct, constraints met) are converted to `IngestionItem`. Rows that fail are collected into `RowErrorResponse` objects with row index, field name, and message.

The controller then calls `CashflowIngestionService.ingest()` with only the valid items. The service handles sensitive-data checks, dedup lookup, categorization, and persistence — exactly as it does today.

**Output contract:**
```json
{
  "importId": "a1b2c3d4-...",
  "profileId": "pharmacy-cl",
  "categorized": [...],
  "manualReview": [...],
  "rejected": [...],
  "errors": [
    {"row": 3, "field": "amount", "message": "El monto debe ser mayor que cero."},
    {"row": 7, "field": "date", "message": "La fecha es obligatoria."}
  ]
}
```

The `categorized`, `manualReview`, and `rejected` fields mirror the existing `CashflowIngestionResponse` structure — same `CategorizedTransactionResponse`, `ManualReviewTransactionResponse`, `RejectedTransactionResponse` records. The `errors` list contains only row-level input validation failures (missing fields, bad types, constraint violations). Service-level rejections (sensitive data, categorization failures) appear in `rejected`/`manualReview` as they do today.

**Import batch identity:** The controller generates a `UUID` import ID returned in the response. This ID is NOT persisted — it is response-only metadata for the client to correlate the batch. No DB migration is needed. The `source_reference` column (per-row fingerprint or explicit reference) already provides per-movement idempotency; batch identity is purely a client-side correlation aid.

**Controller-to-service mapping:**
```
ManualImportRequest
  -> validateRow(row0) -> valid? -> IngestionItem(Transaction, externalReference)
  -> validateRow(row1) -> invalid -> RowErrorResponse(row=1, field="amount", ...)
  -> validateRow(rowN) -> valid? -> IngestionItem(...)
  -> CashflowIngestionCommand(profileId, List<IngestionItem>)
  -> CashflowIngestionService.ingest(command)
  -> CashflowIngestionResult
  -> ManualImportResponse(importId, categorized, manualReview, rejected, errors)
```

**Pros:**
- Zero changes to domain, application, or infrastructure layers — pure interface addition
- No DB migration, no new port methods, no new adapters
- Reuses 100% of existing idempotency (fingerprint and explicit reference), sensitive-data policy, and categorization
- Per-row error tolerance — bad rows don't block good rows (critical for real CSV imports)
- Flat row format with defaulted currency reduces boilerplate for end users
- Response combines processed results with validation errors in one call — practical for client UIs
- Testable with `@WebMvcTest` (existing pattern from `CashflowIngestionControllerTest`)
- Review size estimated at 450–550 lines (controller ~150, DTOs ~80, controller test ~250) — borderline for chained PR

**Cons:**
- Not a true CSV file upload — user must convert CSV to JSON first (addressed in Approach 2 as deferred enhancement)
- Batch identity not persisted — client must correlate movements to import batches on their own
- Currency validation is hardcoded to CLP for now (matches current MVP constraint; vertical-agnostic path: make it a profile-level default later)
- Controller does its own validation instead of using Spring's declarative `@Valid` — slightly more code, but necessary for per-row tolerance

**Effort:** Low-Medium

---

### 2. Multipart CSV Upload Endpoint (Deferred Enhancement)

New endpoint `POST /api/cashflow/imports/csv` that accepts `multipart/form-data` with a CSV file. The server parses the CSV (with configurable column headers and delimiter), maps rows to `IngestionItem`, and delegates to `CashflowIngestionService`.

**Pros:**
- True CSV file upload — users can upload Excel/CSV exports directly
- No manual JSON conversion needed
- Can auto-detect columns from headers

**Cons:**
- Adds multipart handling (file size limits, encoding detection, Spring `MultipartFile`)
- Requires CSV parsing logic (manual or library like OpenCSV/Commons CSV — new dependency or hand-rolled parser)
- Error reporting is harder: line numbers in the file don't map directly to row indices, and encoding issues can corrupt data silently
- More test variants: empty file, wrong encoding, BOM handling, different delimiters, quoted fields
- New dependency or custom parser adds maintenance surface
- Overkill for MVP where CSV data is typically < 100 rows and the user can paste into JSON

**Effort:** Medium-High

**Recommendation:** Defer to a follow-up iteration. The JSON batch endpoint (Approach 1) provides the same underlying value (per-row tolerance, convenient defaults) without the file upload complexity. When true CSV upload is needed, it can be added as a parallel endpoint that reuses the same row validation and service delegation.

---

### 3. Two-Step Import Preview/Confirm Flow (Over-Engineering for MVP)

First endpoint uploads/parses rows and returns a preview (what would be categorized, what would go to manual review, what would be rejected). A temporary "import session" holds the parsed data server-side. Second endpoint confirms and persists. The preview shows the user exactly what will happen before committing.

**Pros:**
- User can review categorization results before persisting
- Good UX for large or sensitive imports
- Prevents "oops" moments on mis-categorized rows

**Cons:**
- Requires server-side state management (import session storage, expiry, cleanup)
- Two API calls instead of one — more complex client integration
- Stateful server is harder to scale and test
- The existing idempotency already provides a safety net: re-importing the same CSV produces the same results (no duplicates), so the user can fix the CSV and re-import without fear
- Significant complexity for MVP — the benefit does not justify the cost at this stage
- For the pharmacy vertical, CSV imports are likely small (< 50 rows) and reviewed on the client side before submission

**Effort:** High

**Recommendation:** Do not implement for MVP. The idempotency guarantee (fingerprint + explicit reference) already protects against accidental duplicate imports. If preview becomes a real user need, it can be added later as a parallel flow.

---

## Recommendation

**Approach 1 — Per-Row-Tolerant JSON Batch Endpoint** — is the right choice for MVP:

1. **Minimal change surface**: A new controller + DTOs in `interfaces/web/`. Zero changes to application, domain, or infrastructure layers. The existing `CashflowIngestionService` is reused as-is, including all idempotency, categorization, and sensitive-data logic.

2. **Addresses the real gap**: The current `POST /api/cashflow/ingestions` endpoint is all-or-nothing. A CSV import with 50 rows where row 23 has a typo should not block the other 49 rows. The per-row-tolerant approach solves this without changing the ingestion service.

3. **Practical defaults for manual entry**: Currency defaults to CLP — users don't need to specify it on every row. Import label provides client-side audit correlation. These are small UX improvements that make the difference between "API for machines" and "tool for humans."

4. **Architectural fit**: The new controller lives in `interfaces/web/`, depends only on `CashflowIngestionService` (application layer), and maps DTOs to domain types (`Transaction`, `ProfileId`). Hexagonal boundaries are fully respected. ArchUnit tests pass without changes.

5. **Idempotency is already solved**: The `externalReference`/fingerprint fallback from `f3de9b9` provides per-row dedup. The CSV import doesn't need to add anything new — just send `externalReference` on rows that should be explicitly deduped, or let the fingerprint handle the rest.

6. **Testable with existing patterns**: `@WebMvcTest` for the controller (mocking `CashflowIngestionService`), following the exact pattern in `CashflowIngestionControllerTest`. No Testcontainers or DB setup needed for controller tests.

### Implementation Sketch

```
POST /api/cashflow/imports/manual

Controller (CashflowManualImportController):
  1. Receive ManualImportRequest (profileId, importLabel?, rows[])
  2. Generate importId (UUID)
  3. For each row:
     a. Validate required fields (description, amount, date)
     b. Validate types (amount is positive BigDecimal, date is valid LocalDate)
     c. Validate constraints (currency is CLP if provided)
     d. If valid: build IngestionItem(transaction, externalReference)
     e. If invalid: collect RowErrorResponse(row index, field, message)
  4. Build CashflowIngestionCommand(profileId, validItems)
  5. Call cashflowIngestionService.ingest(command) -> CashflowIngestionResult
  6. Build ManualImportResponse(importId, profileId, categorized, manualReview, rejected, errors)
  7. Return 200 OK

DTOs:
  - ManualImportRequest (record: profileId, importLabel, rows)
  - ManualImportRow (record: description, amount, currency, date, externalReference)
  - ManualImportResponse (record: importId, profileId, categorized, manualReview, rejected, errors)
  - RowErrorResponse (record: row, field, message)
  - Reuse: CategorizedTransactionResponse, ManualReviewTransactionResponse, RejectedTransactionResponse
            from CashflowIngestionController (extract to shared DTOs or duplicate)

Validation per row (programmatic, NOT @Valid):
  - description: non-blank string
  - amount: non-null, positive (> 0)
  - date: non-null, parseable as LocalDate
  - currency: if provided, must be "CLP"; if omitted, defaults to CLP
  - externalReference: optional, any non-blank string
```

### Row-Level Validation vs Service-Level Processing

| Stage | What fails | Where | Result |
|-------|-----------|-------|--------|
| Row validation | Missing amount, bad date, wrong currency | Controller | `errors[]` in response |
| Sensitive data | Description contains blocked terms | `CashflowIngestionService` | `rejected[]` in response |
| Sensitive externalReference | Reference contains blocked terms | `CashflowIngestionService` | `rejected[]` in response |
| Category match | Description matched a category | `CashflowCategorizationPort` | `categorized[]` in response |
| No category match | Description unmatched | `CashflowCategorizationPort` | `manualReview[]` in response |
| Duplicate | Same externalReference or fingerprint | `CashflowMovementHistoryPort` | Returns existing (categorized/manualReview/rejected) |

The controller only handles **row validation** (structural/type errors). All business logic (sensitive data, categorization, dedup) stays in the service. This keeps the separation clean.

## Risks

1. **Duplicate DTO explosion (low probability, low impact)**: The new controller needs response DTOs (`CategorizedTransactionResponse`, etc.) that duplicate the existing controller's DTOs. Mitigation: extract shared response DTOs to a common package or accept the duplication for now (6 records, ~80 lines). Duplication is acceptable for MVP; extract if both controllers stabilize.

2. **CLP-only currency hardcoding (medium probability, low impact)**: The current system only supports CLP (`@Pattern(regexp = "CLP")`). The new controller defaults to CLP and rejects other currencies. This is consistent with the existing behavior but limits vertical-agnostic aspirations. Mitigation: when multi-currency is needed, the default currency can come from the profile configuration. Document as known MVP limitation.

3. **Batch identity not persisted (low probability, low impact)**: The import ID is response-only. If the client loses the response, they cannot query "all movements from import X." Mitigation: the `source_reference` column already provides per-movement tracing. If batch-level queries become necessary, a V4 migration adding `import_batch_id` can be done later without breaking existing data.

4. **Controller doing its own validation (low probability, low impact)**: Avoiding `@Valid` means we lose automatic Spring error messages. Mitigation: the controller handles validation programmatically with explicit error messages. This is more code but gives us the per-row tolerance we need.

5. **No idempotency gap**: The existing fingerprint fallback already covers per-row dedup. The CSV import doesn't introduce any new idempotency gaps. No risk of duplicate movements from re-imports.

## Review Size Forecast

| Component | Estimated lines |
|-----------|----------------|
| `CashflowManualImportController` (controller + DTOs) | ~200 |
| `CashflowManualImportControllerTest` (`@WebMvcTest`) | ~250 |
| `ApiExceptionHandler` (new error messages) | ~20 |
| **Total** | **~470** |

At ~470 lines, the change is **borderline** for the 400-line review budget. If exactly measured, it could be:
- **Slightly over 400** → forced chained PR should be **honored**: split into 2 PRs (controller tests first, then controller implementation) per strict TDD
- **Right at 400** → a single PR could be argued as acceptable

**Recommendation**: Honor the chained PR strategy regardless. Split into:
- **PR1**: `CashflowManualImportControllerTest` + test-only DTOs/helpers (~250 lines, pure tests, fails until PR2)
- **PR2**: `CashflowManualImportController` + DTOs + `ApiExceptionHandler` changes (~220 lines, makes PR1 pass)

This keeps each PR well under 400 lines, follows strict TDD, and respects the review budget.

## Ready for Proposal

**Yes** — The exploration confirms the gap (all-or-nothing validation, no import batch identity, verbose row format), identifies a low-effort/low-risk approach (per-row-tolerant JSON batch endpoint reusing existing service), and provides enough detail for the `sdd-propose` phase.

Key decisions needed before proposal:
- Confirm Approach 1 (JSON batch with per-row tolerance) is acceptable for MVP
- Confirm Approach 2 (multipart CSV upload) is deferred
- Confirm CLP-only currency default is acceptable for MVP (consistent with existing behavior)
- Confirm batch identity as response-only metadata (not persisted) is acceptable
- Confirm chained PR split into 2 (tests first, then implementation)
