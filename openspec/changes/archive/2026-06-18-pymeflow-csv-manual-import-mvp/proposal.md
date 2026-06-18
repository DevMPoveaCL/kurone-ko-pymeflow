# Proposal: PymeFlow CSV/Manual Import MVP

## Intent

Deliver the roadmap’s realistic CSV/manual import MVP without file-upload complexity: let clients submit CSV-like rows as JSON, tolerate row errors, and reuse the existing ingestion/dedup pipeline.

## Scope

### In Scope
- `POST /api/cashflow/imports/manual` JSON batch endpoint.
- Flat row contract: `description`, `amount`, `date`, optional `currency` defaulting to `CLP`, optional `externalReference`.
- Row-level validation and row-level import results; invalid rows do not block valid rows.
- Dedup guarantee: every persisted row delegates to `CashflowIngestionService` and receives an effective source reference via explicit `externalReference` or fingerprint fallback.
- Safe no-echo behavior for validation/service errors involving sensitive data.
- Force-chained PR plan if implementation forecast remains over 400 changed lines.

### Out of Scope
- Multipart CSV upload.
- Persistent import batches/import history.
- Async processing or preview/confirm workflow.
- Bank integration or provider-specific mapping.

## Capabilities

### New Capabilities
- `cashflow-manual-import`: Manual JSON batch import with per-row tolerance and ingestion-service-backed persistence.

### Modified Capabilities
- None. Existing `cashflow-ingestion-idempotency` behavior is reused, not changed.

## Approach

Add a web-only controller in `interfaces/web/`. Programmatically validate rows, map valid rows to `IngestionItem`, call `CashflowIngestionService.ingest(...)`, and merge service results with validation errors. Response includes response-only `importId`, `profileId`, `categorized`, `manualReview`, `rejected`, and `errors`. Request shape:

```json
{"profileId":"pharmacy-cl","importLabel":"Ventas junio 2026","rows":[{"description":"Venta Caja 1","amount":125000,"date":"2026-06-15","externalReference":"optional"}]}
```

Row errors use `{ "row": 3, "field": "amount", "message": "El monto debe ser mayor que cero." }`. Messages may identify fields/reasons but MUST NOT echo sensitive values.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/.../interfaces/web/CashflowManualImportController.java` | New | Endpoint, DTOs, row validation, service mapping |
| `src/test/java/.../interfaces/web/CashflowManualImportControllerTest.java` | New | `@WebMvcTest` coverage for tolerance, response, dedup delegation |
| `src/main/java/.../interfaces/web/ApiExceptionHandler.java` | Modified | Friendly import-level errors if needed |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Duplicate DTOs | Med | Extract shared DTOs only if duplication grows |
| CLP-only MVP | Med | Keep consistent with current ingestion; profile currency later |
| >400-line review | Med | Chain PR1 tests, PR2 implementation |

## Rollback Plan

Remove the new controller/tests and any exception-handler addition. No database rollback is required because no schema or persisted batch state is introduced.

## Dependencies

- Existing `CashflowIngestionService` idempotency, sensitive-data policy, categorization, and movement persistence.

## Success Criteria

- [ ] Valid rows persist even when other rows are invalid.
- [ ] Invalid rows return row-level errors without sensitive-value echo.
- [ ] Every persisted row uses explicit reference or fingerprint fallback through `CashflowIngestionService`.
- [ ] Manual endpoint remains bank/provider-agnostic and commerce-vertical-agnostic.
