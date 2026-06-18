# Design: PymeFlow CSV/Manual Import MVP

## Technical Approach

Add `CashflowManualImportController` in `interfaces/web/` that accepts flat JSON rows, validates each row programmatically (no `@Valid`), maps valid rows to `CashflowIngestionCommand.IngestionItem`, delegates to the existing `CashflowIngestionService`, and returns a unified response with `importId`, flat summary counts, categorized/manualReview/rejected arrays carrying `row` traceability, and row-level validation errors.

## Architecture Decisions

| Decision | Choice | Alternatives | Rationale |
|----------|--------|--------------|-----------|
| Validation | Programmatic per-row | Spring `@Valid` on request body | `@Valid` blocks the entire batch; per-row tolerance is required for CSV imports |
| Application layer | Reuse `CashflowIngestionService` | New application service | Zero changes to domain/application/infrastructure; hexagonal boundary preserved |
| Batch identity | Response-only `UUID` | Persisted batch table | No schema migration; client correlates via `movementId`/`sourceReference` |
| Response DTOs | Duplicate nested records in controller | Extract shared package | Consistent with existing `CashflowIngestionController` pattern; extract only if duplication grows |
| Row numbering | Echo request `rowNumber` when present; fallback to 1-based submitted position | Always positional or 0-based | Human-friendly CSV/manual-import line correlation, including original spreadsheet line numbers |
| HTTP status | 200 if any row processed; 400 if all invalid | Always 200 | Clear client signal when the entire payload is unusable |

## Data Flow

```
POST /api/cashflow/imports/manual
  ManualImportRequest (profileId, importLabel?, rows[])
    ├── responseRowNumber(row, index) → row.rowNumber || 1-based position
    ├── validateRow(row, responseRowNumber) → RowError?
    │     ├── invalid → collect in errors[]
    │     └── valid   → build IngestionItem(Transaction, externalReference)
    │         currency defaults to CLP if omitted/blank
    ├── CashflowIngestionCommand(profileId, validItems[])
    ├── CashflowIngestionService.ingest(command)
    │     ├── dedup (safe externalReference || fingerprint fallback; sensitive references use fallback)
    │     ├── sensitive-data check
    │     ├── categorization
    │     └── persistence
    └── ManualImportResponse(importId, profileId, flat counts, categorized[row], manualReview[row], rejected[row], errors)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `interfaces/web/CashflowManualImportController.java` | Create | Controller + DTOs (`ManualImportRequest`, `ManualImportRow`, `ManualImportResponse`, `RowErrorResponse`) |
| `interfaces/web/CashflowManualImportControllerTest.java` | Create | `@WebMvcTest` tolerance, mapping, no-echo, dedup delegation |
| `interfaces/web/ApiExceptionHandler.java` | Modify (optional) | Add friendly messages only if new top-level `IllegalArgumentException` cases are introduced |

## Interfaces / Contracts

```java
// Controller DTOs (nested records, same pattern as CashflowIngestionController)
public record ManualImportRequest(String profileId, String importLabel, List<ManualImportRow> rows) {}
public record ManualImportRow(String description, BigDecimal amount, String currency, String date, String externalReference) {}
public record ManualImportResponse(UUID importId, String profileId,
    List<CategorizedTransactionResponse> categorized,  // each item includes row
    List<ManualReviewTransactionResponse> manualReview, // each item includes row
    List<RejectedTransactionResponse> rejected, // each item includes row
    List<RowErrorResponse> errors) {}
public record RowErrorResponse(int row, String field, String message) {}
```

### Validation Rules (Programmatic)

- `description`: non-blank
- `amount`: non-null, `> 0`
- `date`: non-null, parseable as `LocalDate`
- `currency`: defaults to `CLP`; if provided, must equal `CLP`
- `externalReference`: optional, passed as-is; blank normalized to `null` by `IngestionItem`

### Idempotency Reuse

Every valid row becomes an `IngestionItem`. The service:
1. Normalizes `externalReference` (blank → null)
2. If `externalReference` is present and safe → `findBySourceReference`
3. If absent or sensitive → `TransactionFingerprint.compute(profileId, transaction)` → `findBySourceReference`
4. Falls through to categorization + persistence

Sensitive explicit references are never persisted or echoed. They still get deterministic idempotency through the same fingerprint fallback used by omitted references.

### No-Echo Policy

- **Row validation errors**: messages identify the field and constraint (e.g., `El monto debe ser mayor que cero.`) but NEVER echo the submitted value.
- **Service rejections**: reuse existing `RejectedTransactionResponse` pattern — omit `description` and do not echo `externalReference`.

### Result-to-Row Mapping

`errors` and successful service result entries (`categorized`, `manualReview`, `rejected`) carry `row.rowNumber()` as `row` when supplied, otherwise the original 1-based submitted row position. The controller keeps metadata for valid rows, delegates only valid items, and maps returned ingestion results back to the valid-row metadata without using request values as persisted identifiers.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit / Controller | Tolerance (valid rows proceed when others invalid), row error indexing (1-based), CLP default, externalReference passthrough, dedup delegation, no-echo on sensitive rejections, 400 when all rows invalid | `@WebMvcTest` mocking `CashflowIngestionService` (same pattern as `CashflowIngestionControllerTest`) |
| Integration | N/A | No new application service or infrastructure; coverage deferred to existing service tests |
| Docs | OpenAPI schema accuracy | Verify via `springdoc-openapi` after controller is wired |

## Migration / Rollout

No migration required. No schema changes. Rollback: delete controller + test + any exception-handler additions.

## Chained PR Plan

Forecast ~470 lines → force-chained split:

- **PR1** (skeleton + tests, ~250 lines): `CashflowManualImportController` class skeleton, empty DTO records, full `@WebMvcTest`. Compiles; tests fail (red).
- **PR2** (implementation, ~220 lines): Controller validation logic, DTO mapping, `IngestionCommand` assembly, test green-up. Merging PR2 makes the full feature pass.

## Open Questions

- None. All technical decisions resolved.
