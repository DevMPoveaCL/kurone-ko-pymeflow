# Tasks: PymeFlow CSV/Manual Import MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~470 (tests ~270, controller ~200) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1: skeleton + RED tests (~270) → PR2: implementation GREEN (~200) |
| Delivery strategy | force-chained |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Skeleton controller + full `@WebMvcTest` suite (RED) | PR 1 | Base: `import/csv-manual-mvp`; DTO records + stub POST; all tests fail |
| 2 | Controller validation, mapping, delegation logic (GREEN) | PR 2 | Base: PR 1 branch; all tests pass |

## Phase 1: PR1 — Contract Skeleton + RED Tests

- [x] 1.1 Create `CashflowManualImportController.java` skeleton: `@RestController`, `@RequestMapping("/api/cashflow/imports/manual")`, nested `record` DTOs (`ManualImportRequest`, `ManualImportRow`, `ManualImportResponse`, `RowErrorResponse`) with no logic; stub endpoint returns empty `ManualImportResponse`. Inject `CashflowIngestionService`.
- [x] 1.2 Create `CashflowManualImportControllerTest.java` with `@WebMvcTest(CashflowManualImportController.class)`, `@MockBean CashflowIngestionService`.
- [x] 1.3 RED: Write tolerance tests — valid+mixed rows → 200; all invalid rows → 400; row counts in response.
- [x] 1.4 RED: Write field validation tests — blank `description`, `amount` <= 0, non-ISO `date`, non-CLP `currency` → row error with field + message (no value echo).
- [x] 1.5 RED: Write delegation test — `ArgumentCaptor<CashflowIngestionCommand>` verifies `profileId`, `IngestionItem` mapping, currency default to CLP, `externalReference` passthrough.
- [x] 1.6 RED: Write idempotency test — mock service returns existing movement; verify response contains existing `movementId`.
- [x] 1.7 RED: Write no-echo test — sensitive `description`/`externalReference` rejected by service; verify response lacks sensitive text.
- [x] 1.8 RED: Write response shape test — `importId`, `profileId`, `categorized[]`, `manualReview[]`, `rejected[]`, `errors[]` with 1-based `row` numbering.
- [x] 1.9 RED checkpoint: targeted controller test command → all new tests FAIL (controller returns empty stub).

## Phase 2: PR2 — Implementation GREEN + REFACTOR

- [x] 2.1 Implement `validateRow(ManualImportRow, int rowNumber)` → `Optional<RowErrorResponse>`: description non-blank, amount positive, date ISO-parseable, currency CLP-only. Error messages must NOT echo submitted values.
- [x] 2.2 Implement row-to-`IngestionItem` mapping: build `Transaction(description, amount, Currency.getInstance("CLP"), LocalDate.parse(date))`, pass `externalReference` to `IngestionItem` constructor (null/blank → service normalizes).
- [x] 2.3 Implement endpoint logic: separate valid/invalid rows, build `CashflowIngestionCommand` from valid rows, call `cashflowIngestionService.ingest(command)`.
- [x] 2.4 Implement response assembly: generate `UUID` `importId`, merge `CategorizedTransaction`/`ManualReviewTransaction`/`RejectedTransaction` from result, attach row `errors[]` with 1-based indices.
- [x] 2.5 Implement 400 status when all rows invalid (valid `profileId` present but zero valid rows → 400 with `errors[]`).
- [x] 2.6 GREEN checkpoint: `./gradlew.bat test --rerun-tasks` → ALL tests PASS.
- [x] 2.7 REFACTOR: extract `MANUAL_REVIEW_REASON` and `SENSITIVE_REJECTION_REASON` constants (match existing controller pattern); clean up mapping helpers. Re-run tests to confirm green.

## Phase 3: Verification Warning Fix

- [x] 3.1 Correct OpenSpec all-invalid scenario to HTTP 400 so spec, design, tasks, implementation, and tests agree.
- [x] 3.2 Add focused controller triangulation for exact summary counts (`2 accepted + 1 rejected + 1 invalid`) and positive manual-review response mapping.
- [x] 3.3 Verification warning GREEN checkpoint: targeted controller tests and full `./gradlew.bat test --rerun-tasks` pass.

## Phase 4: Post-Verify Smoke Fix

- [x] 4.1 RED: Add controller WebMvc assertions proving `categorized`, `manualReview`, and `rejected` response entries include 1-based `row` traceability mapped to source row positions.
- [x] 4.2 GREEN/REFACTOR: Preserve valid-row metadata in `CashflowManualImportController`, map ingestion results back to original valid rows, and keep flat summary counts unchanged.
- [x] 4.3 RED: Add service coverage proving a sensitive explicit `externalReference` uses safe fingerprint fallback, is not persisted/echoed, and replay returns the existing rejected movement without duplicate insert.
- [x] 4.4 GREEN/REFACTOR: Update `CashflowIngestionService` so sensitive explicit references compute the effective `sourceReference` from `TransactionFingerprint` before lookup/persistence.
- [x] 4.5 Update archived/promoted specs, archived design, apply progress, and verify report with the smoke-discovered row-traceability and sensitive-reference idempotency fix.
- [x] 4.6 GREEN checkpoint: targeted controller/service tests, full `./gradlew.bat test --rerun-tasks`, and `./gradlew.bat jacocoTestReport` pass.

## Phase 5: Surgical Post-Smoke RowNumber/OpenAPI Fix

- [x] 5.1 Safety net: targeted `CashflowManualImportControllerTest` passed before edits.
- [x] 5.2 RED: Update WebMvc assertions so provided non-sequential `rowNumber` values are echoed for categorized/manualReview/rejected/errors.
- [x] 5.3 RED: Add fallback coverage proving missing `rowNumber` still uses submitted 1-based position.
- [x] 5.4 RED: Add OpenAPI annotation coverage for documented HTTP 200 and HTTP 400 responses.
- [x] 5.5 GREEN/REFACTOR: Update `CashflowManualImportController` row-number resolution and add OpenAPI annotations.
- [x] 5.6 Update promoted/archived specs and archived SDD reports with rowNumber echo semantics and 400 documentation.

### Verification Commands

```bash
./gradlew.bat test --rerun-tasks
./gradlew.bat build -x test
./gradlew.bat jacocoTestReport
```

### Acceptance Criteria (per task completion)

- Row tolerance: valid rows persist when others are invalid.
- Field validation: each constraint returns a `RowErrorResponse` without echoing the submitted value.
- Currency default: omitted/blank `currency` becomes CLP.
- Dedup: omitted/blank `externalReference` delegates to service fingerprint fallback.
- Idempotency: re-import returns existing `movementId`.
- No-echo: rejected sensitive rows do not expose sensitive text in response.
- Response shape: `importId`, `profileId`, `categorized`, `manualReview`, `rejected`, `errors`.
- Row traceability: `categorized`, `manualReview`, `rejected`, and `errors` entries echo supplied `rowNumber`, falling back to submitted 1-based position only when omitted.
- Sensitive reference idempotency: sensitive explicit references are not persisted, use fingerprint fallback for deduplication, and replay returns the existing rejected movement.
- All-rows-invalid → HTTP 400.
- OpenAPI documents both HTTP 200 and HTTP 400 responses for the manual import endpoint.
