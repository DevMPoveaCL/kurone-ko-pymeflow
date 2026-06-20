# Tasks: PymeFlow Simulated Bank Statement Adapter MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated lines | ~900 (3 chained PRs) |
| Budget risk | High |
| Chained PRs | Yes |
| Split plan | PR1 ~95L → PR2 ~340L → PR3 ~450L |
| Delivery | auto-chain |
| Chain | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

## Phase 1: Port Contracts + ArchUnit (PR 1)

- [x] 1.1 Create `ExternalStatementImportPort.java` — `importStatement(ExternalStatementImportCommand): CashflowIngestionResult`
- [x] 1.2 Create `ExternalStatementImportCommand.java` — record: `ProfileId`, `importLabel`, `entries`; null-guard
- [x] 1.3 Create `ExternalStatementEntry.java` — record: `externalReference`, `date`, `description`, `amount`, `currency`; non-null/non-blank/non-zero guards
- [x] 1.4 Bean `SimulatedBankStatementAdapter` in `ApplicationServiceConfiguration.java`
- [x] 1.5 ArchUnit rule: `infrastructure.bank..` must not depend on `domain..` or `application..`

## Phase 2: RED — Adapter Tests (PR 2)

`SimulatedBankStatementAdapterTest.java`, mock `CashflowIngestionService`:

- [x] 2.1 `mapsSignedNegativeToPositive()` — `-15000` → `15000`
- [x] 2.2 `enrichesDescription()` — `"Farmacia"` + `"Pago"` → `"Farmacia | Pago"`
- [x] 2.3 `preservesDescriptionWithoutCounterparty()` — no counterparty → original
- [x] 2.4 `rejectsNonClpCurrency()` — `USD` → exception
- [x] 2.5 `delegatesToIngestionService()` — mock, verify passthrough
- [x] 2.6 `rejectsBlankExternalReference()` — blank → exception
- [x] 2.7 `dropsAccountAlias()` — alias excluded from mapped item

## Phase 3: GREEN — Adapter (PR 2)

- [x] 3.1 Create `SimulatedBankStatementAdapter.java` implementing port
- [x] 3.2 Map: `externalReference=bankTransactionId.trim()`, `amount=signedAmount.abs()`, `date=bookingDate`, CLP enforced, `counterpartyName | description`
- [x] 3.3 Delegate to `ingest()`; drop `accountAlias`; no fingerprint fallback

## Phase 4: RED — Controller Tests (PR 3)

`CashflowBankStatementSimulatedControllerTest.java`, `@WebMvcTest`, `@MockBean ExternalStatementImportPort`:

- [x] 4.1 `acceptsValidStatement()` — 200, `importId`, counts
- [x] 4.2 `rejectsEmptyRows()` — 400
- [x] 4.3 `rejectsBlankBankTransactionId()` — blank → error
- [x] 4.4 `rejectsZeroAmount()` — `0` → error
- [x] 4.5 `rejectsNonClpNoEcho()` — response excludes submitted currency
- [x] 4.6 `rejectsInvalidDateNoEcho()` — response excludes bad value
- [x] 4.7 `rejectsSensitiveIdNoEcho()` — error msg excludes sensitive term
- [x] 4.8 `mixedBatchPartialSuccess()` — valid+invalid → 200, `accepted=1`, `invalid=1`
- [x] 4.9 `allInvalidReturns400()` — `accepted=0`
- [x] 4.10 `reimportReturnsExistingId()` — duplicate `bankTransactionId` → existing ID
- [x] 4.11 `oneBasedRowPositions()` — `row` = 1-based index

## Phase 5: GREEN — Controller (PR 3)

- [x] 5.1 `CashflowBankStatementSimulatedController.java` — `POST /api/cashflow/imports/bank-statement/simulated`
- [x] 5.2 Request DTOs: `SimulatedBankStatementRequest` + `SimulatedBankStatementRow` (7 fields)
- [x] 5.3 Row validation: non-blank ID+desc, non-zero amount, ISO date, CLP-only, `SensitiveDataPolicy` on `bankTransactionId`
- [x] 5.4 No-echo: error messages exclude submitted sensitive values
- [x] 5.5 Partial success: valid→adapter, invalid→errors, all-invalid→400
- [x] 5.6 Response DTO: `importId`, `profileId`, `accepted`, `categorizedCount`, `manualReviewCount`, `rejectedCount`, `invalid`, per-row `row`
- [x] 5.7 `@Tag`, `@Operation`, `@ApiResponses`
- [x] 5.8 Javadoc: "Direction loss — signed amounts → positive; debit/credit lost by design (MVP tradeoff)"

## Phase 6: REFACTOR

- [x] 6.1 Extract `"CLP"` constant
- [x] 6.2 `mvn test` all green + `mvn test -Dtest=ArchitectureTest`
- [x] 6.3 Verify: no bank API calls, no batch persistence, anti-corruption boundary intact

## Phase 7: Verification Failure Fix

- [x] 7.1 Preserve source row traceability across categorized/manualReview/rejected result partitions
- [x] 7.2 Add WebMvc regression test for mixed non-sequential result partitions
- [x] 7.3 Run targeted controller+adapter tests, full Gradle suite, and JaCoCo report

## Phase 8: Residual Duplicate Source-Reference Fix

- [x] 8.1 Add WebMvc RED tests for duplicate nonblank `bankTransactionId` partial success and all-duplicate/all-invalid behavior
- [x] 8.2 Reject every row sharing a duplicate nonblank `bankTransactionId` before adapter delegation with safe no-echo row-level messages
- [x] 8.3 Update OpenSpec spec/design/tasks/apply-progress/verify-report for duplicate `bankTransactionId` validation
- [x] 8.4 Run targeted controller tests, targeted adapter+controller tests, full Gradle suite, and JaCoCo report

## Phase 9: Verification-Hardening Idempotency Slice

- [x] 9.1 Add focused composition test invoking controller + real `SimulatedBankStatementAdapter` + real `CashflowIngestionService` idempotency without HTTP
- [x] 9.2 Prove re-importing the same `bankTransactionId` returns the same movement ID and creates no duplicate insert
- [x] 9.3 Run targeted composition test, full Gradle suite, JaCoCo report, and update OpenSpec verification artifacts
