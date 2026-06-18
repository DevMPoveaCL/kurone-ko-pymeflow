# Apply Progress: PymeFlow CSV/Manual Import MVP

## Change

`pymeflow-csv-manual-import-mvp`

## Mode

Strict TDD — PR1 RED checkpoint merged with PR2 GREEN + REFACTOR implementation, plus surgical post-smoke rowNumber/OpenAPI fix.

## Workload / PR Boundary

- Mode: chained PR slice (`feature-branch-chain`)
- Current work unit: PR2 — controller validation, mapping, delegation, response assembly, and GREEN verification
- Boundary: starts from PR1 RED branch state; ends with manual import endpoint behavior implemented and controller/full test suite green
- Review budget impact: PR2 changes production controller implementation and OpenSpec progress/task marks only; PR1 test contract remains unchanged

## Completed Tasks

- [x] 1.1 Controller skeleton with `@RestController`, manual import route, nested DTO records, and injected `CashflowIngestionService`.
- [x] 1.2 `@WebMvcTest(CashflowManualImportController.class)` with mocked ingestion service.
- [x] 1.3 RED tolerance tests for mixed and all-invalid batches.
- [x] 1.4 RED field validation tests for blank description, non-positive amount, invalid date, and non-CLP currency with no value echo.
- [x] 1.5 RED delegation test using `ArgumentCaptor<CashflowIngestionCommand>`.
- [x] 1.6 RED idempotency response test for existing movement IDs returned by ingestion service.
- [x] 1.7 RED no-echo test for sensitive service rejection.
- [x] 1.8 RED response shape test for `importId`, `profileId`, summary counts, result arrays, and 1-based row errors.
- [x] 1.9 RED checkpoint captured with targeted Gradle test execution.
- [x] 2.1 Programmatic row validation for description, amount, ISO date, and CLP-only currency with safe no-echo messages.
- [x] 2.2 Row-to-`IngestionItem` mapping using `Transaction(description, amount, CLP, LocalDate.parse(date))` and existing external-reference normalization.
- [x] 2.3 Endpoint logic separates invalid/valid rows and delegates valid rows through `CashflowIngestionService.ingest(...)`.
- [x] 2.4 Response assembly maps categorized/manual-review/rejected service results, generates response-only `importId`, and attaches row validation errors.
- [x] 2.5 All-invalid/non-ingestable requests return HTTP 400 with zero accepted rows and row-level errors.
- [x] 2.6 GREEN checkpoint passed with targeted controller tests and full Gradle test suite.
- [x] 2.7 REFACTOR checkpoint completed: shared constants and mapping helpers extracted; tests remained green.
- [x] 3.1 Corrected OpenSpec all-invalid scenario from HTTP 200 to HTTP 400 to align with design/tasks/tests/user requirement.
- [x] 3.2 Added focused response-summary triangulation for exact `2 accepted + 1 rejected + 1 invalid` counts.
- [x] 3.3 Added positive manual-review response mapping coverage for movement ID, transaction fields, count, and reason.
- [x] 3.4 Verification warning GREEN checkpoint passed with targeted controller tests and full Gradle suite.
- [x] 4.1 Post-verify smoke RED: added controller WebMvc assertions for `row` traceability on categorized/manual-review/rejected response entries.
- [x] 4.2 Post-verify smoke GREEN: mapped successful service results back to original valid source rows while preserving flat response counts.
- [x] 4.3 Post-verify smoke RED: added service tests for sensitive explicit `externalReference` fingerprint fallback and replay idempotency.
- [x] 4.4 Post-verify smoke GREEN: sensitive explicit references now use safe transaction fingerprint as effective `sourceReference` and are never persisted/echoed.
- [x] 4.5 Updated archived/promoted specs, archived design/tasks/apply-progress/verify-report with the smoke fix note.
- [x] 4.6 Post-verify smoke GREEN checkpoint passed with targeted controller/service tests, full Gradle suite, and JaCoCo report generation.
- [x] 5.1 Safety net passed: targeted `CashflowManualImportControllerTest` was green before rowNumber/OpenAPI edits.
- [x] 5.2 RED checkpoint: WebMvc assertions updated so provided non-sequential `rowNumber` values must be echoed for categorized/manualReview/rejected/errors; existing positional implementation failed.
- [x] 5.3 RED checkpoint: fallback WebMvc test added for missing `rowNumber` → submitted 1-based position.
- [x] 5.4 RED checkpoint: OpenAPI annotation test added for documented HTTP 200 and HTTP 400 responses; endpoint initially lacked `@ApiResponses`.
- [x] 5.5 GREEN/REFACTOR: controller resolves response row identifiers with `row.rowNumber()` when present, otherwise `index + 1`, while preserving valid-result mapping.
- [x] 5.6 Updated promoted and archived OpenSpec specs plus archived design/tasks/apply-progress/verify/archive notes for rowNumber echo semantics and documented HTTP 400.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | N/A (new) | ✅ Controller contract tests exist against skeleton endpoint | ❌ Not attempted by design; PR1 stayed RED | ✅ Contract covers mixed/all-invalid, validation, delegation, idempotency, no-echo, response shape | ➖ None; skeleton only |
| 1.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | N/A (new) | ✅ `@WebMvcTest` suite compiles with mocked service | ❌ Not attempted by design; PR1 stayed RED | ✅ Multiple endpoint scenarios exercise the same controller slice | ➖ None; test setup follows existing pattern |
| 1.3 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | N/A (new) | ✅ Mixed-batch and all-invalid tests written | ✅ Passed in PR2 targeted run | ✅ Mixed and all-invalid tolerance paths covered | ✅ Endpoint separates valid/invalid rows cleanly |
| 1.4 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | N/A (new) | ✅ Four validation tests written | ✅ Passed in PR2 targeted run | ✅ Distinct field/error branches covered | ✅ Validation helper extracted |
| 1.5 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | N/A (new) | ✅ Delegation/captor test written | ✅ Passed in PR2 targeted run | ✅ Verifies profile, transaction fields, CLP default, external reference trim | ✅ Mapping helper extracted |
| 1.6 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | N/A (new) | ✅ Existing movement response test written | ✅ Passed in PR2 targeted run | ➖ Single idempotency response scenario through existing service contract | ✅ Response mapper reused service result DTO helpers |
| 1.7 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | N/A (new) | ✅ Sensitive rejection no-echo test written | ✅ Passed in PR2 targeted run | ✅ Covers sensitive description and external reference no-echo | ✅ Sensitive rejection reason constant extracted |
| 1.8 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | N/A (new) | ✅ Response shape test written | ✅ Passed in PR2 targeted run | ✅ Shape covers summary counts and result arrays | ✅ Response assembly helper extracted |
| 1.9 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | N/A (new) | ✅ Targeted RED checkpoint executed | ✅ Superseded by PR2 targeted GREEN run | ✅ Failures span all PR1 contract areas | ➖ None |
| 2.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | ✅ Baseline RED confirmed: 10/10 tests failed as expected before PR2 code | ✅ PR1 validation tests already written first | ✅ Targeted run passed 10/10 after validation implementation | ✅ Blank description, non-positive amount, invalid date, non-CLP currency, and mixed batch covered | ✅ `validateRow` helper extracted |
| 2.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | ✅ Baseline RED confirmed | ✅ PR1 delegation test already written first | ✅ Targeted run passed 10/10 after mapping implementation | ✅ CLP default, `Transaction` fields, and external-reference normalization covered | ✅ `toIngestionItem` and `currencyOrDefault` helpers extracted |
| 2.3 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | ✅ Baseline RED confirmed | ✅ PR1 delegation/tolerance tests already written first | ✅ Targeted run passed 10/10 after service delegation implementation | ✅ Valid-only and mixed valid/invalid paths covered | ✅ Endpoint keeps orchestration minimal |
| 2.4 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | ✅ Baseline RED confirmed | ✅ PR1 response/idempotency/no-echo tests already written first | ✅ Targeted run passed 10/10 after response assembly implementation | ✅ Categorized, rejected, empty-result, and row-error responses covered | ✅ `fromResult` and nested DTO `from(...)` helpers extracted |
| 2.5 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | ✅ Baseline RED confirmed | ✅ PR1 all-invalid and field validation tests already written first | ✅ Targeted run passed 10/10 with HTTP 400 for zero valid rows | ✅ Single-row invalid and multi-row all-invalid paths covered | ✅ Status selection kept explicit in endpoint |
| 2.6 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice + full suite | ✅ Baseline RED confirmed | ✅ PR1 RED suite existed before implementation | ✅ `./gradlew.bat test --rerun-tasks` passed | ✅ Full suite protects cross-feature behavior | ➖ No production refactor beyond helpers |
| 2.7 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice + full suite | ✅ Targeted GREEN achieved before refactor | ✅ Existing tests protected refactor | ✅ Targeted and full suite stayed green | ✅ Constants/helpers exercised by response and validation scenarios | ✅ `MANUAL_REVIEW_REASON`, `SENSITIVE_REJECTION_REASON`, `CLP`, and mapping helpers extracted |
| 3.1 | `openspec/changes/pymeflow-csv-manual-import-mvp/specs/cashflow-manual-import/spec.md` | Spec artifact | ✅ Read previous verify warning and existing all-invalid controller test | ➖ Documentation-only correction; no production code | ✅ Existing all-invalid test already passes with HTTP 400 | ➖ Triangulation not applicable to spec text | ➖ None needed |
| 3.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | ✅ Targeted controller safety net passed before edits | ✅ Focused summary triangulation test written before any production change | ✅ Targeted controller run passed 12/12; no production change required | ✅ Exact `2 accepted + 1 rejected + 1 invalid` response counts covered in one scenario | ➖ None needed |
| 3.3 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | ✅ Targeted controller safety net passed before edits | ✅ Positive manual-review mapping test written before any production change | ✅ Targeted controller run passed 12/12; no production change required | ✅ Manual-review movement ID, transaction fields, count, and reason covered | ➖ None needed |
| 4.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | ✅ Targeted controller/service safety net passed before edits | ✅ Row-traceability assertions written first | ✅ Targeted controller/service run passed after implementation | ✅ Categorized, manual-review, rejected, and invalid row positions covered | ✅ Result row mapper extracted in controller |
| 4.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice | ✅ Safety net passed before edits | ✅ RED row assertions failed with `PathNotFoundException` before implementation | ✅ Targeted controller/service run passed after implementation | ✅ Mixed valid/invalid source row mapping plus existing count tests cover flat summary behavior | ✅ `ValidRow` metadata and `ResultRowMapper` keep response assembly focused |
| 4.3 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/CashflowIngestionServiceTest.java` | Unit/application service | ✅ Targeted controller/service safety net passed before edits | ✅ Sensitive reference fallback/replay tests written first | ✅ Targeted controller/service run passed after implementation | ✅ First import persists safe fingerprint; replay returns existing rejected movement without duplicate insert | ✅ Effective source-reference logic kept local to ingestion loop |
| 4.4 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/CashflowIngestionServiceTest.java` | Unit/application service | ✅ Safety net passed before edits | ✅ RED service assertions failed before implementation | ✅ Targeted controller/service run passed after implementation | ✅ Sensitive explicit reference and replay paths covered | ✅ Reused `TransactionFingerprint` fallback; no new persistence strategy introduced |
| 5.1-5.5 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Web MVC integration slice + annotation contract | ✅ Targeted controller safety net passed before edits (13 existing tests) | ✅ RowNumber echo and OpenAPI 400 documentation tests written before production changes; targeted run failed 3 tests | ✅ Targeted controller run passed after implementation (15 tests) | ✅ Non-sequential provided row numbers and missing-rowNumber positional fallback both covered | ✅ `responseRowNumber` helper extracted; result-to-valid-row mapper preserved |

## Test Summary

- Command: `./gradlew.bat test --tests "com.kuroneko.pymeflow.interfaces.web.CashflowManualImportControllerTest" --rerun-tasks`
- Baseline result before PR2 implementation: RED — 10 tests executed, 10 failed as expected.
- First PR2 GREEN attempt: 10 tests executed, 1 failed (`accepted` count for valid row with empty mocked service result).
- Final targeted result: GREEN — build successful, 10 controller tests passing.
- Command: `./gradlew.bat test --rerun-tasks`
- Full suite result: GREEN — build successful.
- Command: `./gradlew.bat build -x test`
- Build result: GREEN — build successful.
- Total tests written in PR2: 0 (PR1 had already written the RED contract tests first)
- Verification warning fix safety net: `./gradlew.bat test --tests "com.kuroneko.pymeflow.interfaces.web.CashflowManualImportControllerTest" --rerun-tasks` passed before edits (10/10).
- Verification warning fix targeted result: `./gradlew.bat test --tests "com.kuroneko.pymeflow.interfaces.web.CashflowManualImportControllerTest" --rerun-tasks` passed after edits (12/12).
- Verification warning fix full suite result: `./gradlew.bat test --rerun-tasks` passed after edits (full suite green).
- Post-verify smoke safety net: `./gradlew.bat test --tests "com.kuroneko.pymeflow.interfaces.web.CashflowManualImportControllerTest" --tests "com.kuroneko.pymeflow.application.cashflow.CashflowIngestionServiceTest" --rerun-tasks` passed before edits.
- Post-verify smoke RED: same targeted command failed with 6 failures (missing `row` JSON paths and sensitive-reference fallback/idempotency assertions).
- Post-verify smoke targeted result: same targeted command passed after implementation (27 related tests green).
- Post-verify smoke full suite result: `./gradlew.bat test --rerun-tasks` passed with 119 tests, 0 failures, 0 errors, 0 skipped.
- Post-verify smoke coverage result: `./gradlew.bat jacocoTestReport` passed.
- Surgical rowNumber/OpenAPI safety net: `./gradlew.bat test --rerun-tasks --tests "com.kuroneko.pymeflow.interfaces.web.CashflowManualImportControllerTest"` passed before edits.
- Surgical rowNumber/OpenAPI RED: same targeted command failed 3 tests (`categorized[0].row` expected provided rowNumber, non-sequential row mapping expected provided rowNumbers, and missing OpenAPI `@ApiResponses`).
- Surgical rowNumber/OpenAPI targeted result: same targeted command passed after implementation (15 controller tests green).
- Surgical rowNumber/OpenAPI full suite result: `./gradlew.bat test --rerun-tasks` passed.
- Surgical rowNumber/OpenAPI coverage result: `./gradlew.bat jacocoTestReport` passed.
- Total tests written in verification warning fix: 2
- Total tests written in post-verify smoke fix: 2 new tests plus focused assertions added to existing controller/service tests
- Total tests passing: 15 targeted controller tests; targeted controller/service tests green; full suite green
- Layers used: Web MVC integration slice, application service unit tests, full Gradle suite
- Approval tests: None — no existing behavior refactor outside the new controller
- Pure functions created: 0 (private pure-ish validation/mapping helpers inside controller)

## Deviations from Design

None. PR2 keeps the implementation in the web controller, delegates valid rows to `CashflowIngestionService`, avoids new infrastructure logic, and keeps service rejections free of sensitive descriptions/references. The post-verify smoke fix updates controller response mapping and ingestion-service effective reference selection to match the row-traceability and no-persist-without-dedup requirements.

## Issues Found

- The existing PR1 response-shape test expects `accepted` to count valid submitted rows even when the mocked ingestion result contains no categorized/manual-review/rejected items. PR2 implements `accepted` from valid row count, not from service result list sizes.
- Verify found an artifact inconsistency where the OpenSpec all-invalid scenario still said HTTP 200; corrected to HTTP 400.
- Verify found partial triangulation for response summary/manual-review mapping; added focused controller coverage without production changes.
- Smoke found successful result entries lacked row traceability; fixed by returning `row` on categorized/manual-review/rejected entries and documenting flat count fields.
- Follow-up smoke found explicit `rowNumber` values were not echoed; fixed by using `row.rowNumber()` for categorized/manual-review/rejected/errors and falling back to `index + 1` only when absent.
- Swagger UI showed all-invalid HTTP 400 as undocumented; fixed by adding OpenAPI response annotations for 200 and 400.
- Smoke found sensitive explicit external references were rejected before fallback `sourceReference` assignment; fixed by using `TransactionFingerprint` as the safe effective dedup reference without persisting/echoing the sensitive value.

## Remaining Tasks

None for this change. Ready for SDD verify re-run.
