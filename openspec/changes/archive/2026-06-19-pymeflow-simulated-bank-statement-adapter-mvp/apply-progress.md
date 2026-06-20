# Apply Progress: PymeFlow Simulated Bank Statement Adapter MVP

## Workload / PR Boundary

- Mode: chained PR slice
- Chain strategy: feature-branch-chain
- Current work unit: Verification-hardening idempotency test slice
- Boundary: one focused non-HTTP composition test covering controller + real simulated adapter + real ingestion idempotency; OpenSpec artifact updates only; no production behavior changes
- Estimated review budget impact: minimal hardening slice; adds one test class and documentation updates

## Completed Tasks

- [x] 1.1 Create `ExternalStatementImportPort.java` — `importStatement(ExternalStatementImportCommand): CashflowIngestionResult`
- [x] 1.2 Create `ExternalStatementImportCommand.java` — record: `ProfileId`, `importLabel`, `entries`; null-guard
- [x] 1.3 Create `ExternalStatementEntry.java` — record: `externalReference`, `date`, `description`, `amount`, `currency`; non-null/non-blank/non-zero guards
- [x] 1.4 Bean `SimulatedBankStatementAdapter` in `ApplicationServiceConfiguration.java`
- [x] 1.5 ArchUnit boundary guard for bank-specific infrastructure not leaking into domain/application core
- [x] 2.1 `mapsSignedNegativeToPositive()` — `-15000` → `15000`
- [x] 2.2 `enrichesDescription()` — `"Farmacia"` + `"Pago"` → `"Farmacia | Pago"`
- [x] 2.3 `preservesDescriptionWithoutCounterparty()` — no counterparty → original
- [x] 2.4 `rejectsNonClpCurrency()` — `USD` → exception without echoing submitted currency
- [x] 2.5 `delegatesToIngestionService()` — mock, verify passthrough
- [x] 2.6 `rejectsBlankExternalReference()` — blank → exception without echoing submitted value
- [x] 2.7 `dropsAccountAlias()` — alias excluded from mapped item
- [x] 3.1 Create `SimulatedBankStatementAdapter.java` implementing port
- [x] 3.2 Map: `externalReference=bankTransactionId.trim()`, `amount=signedAmount.abs()`, `date=bookingDate`, CLP enforced, `counterpartyName | description`
- [x] 3.3 Delegate to `ingest()`; drop `accountAlias`; no fingerprint fallback
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
- [x] 5.1 `CashflowBankStatementSimulatedController.java` — `POST /api/cashflow/imports/bank-statement/simulated`
- [x] 5.2 Request DTOs: `SimulatedBankStatementRequest` + `SimulatedBankStatementRow` (7 fields)
- [x] 5.3 Row validation: non-blank ID+desc, non-zero amount, ISO date, CLP-only, `SensitiveDataPolicy` on `bankTransactionId`
- [x] 5.4 No-echo: error messages exclude submitted sensitive values
- [x] 5.5 Partial success: valid→adapter, invalid→errors, all-invalid→400
- [x] 5.6 Response DTO: `importId`, `profileId`, `accepted`, `categorizedCount`, `manualReviewCount`, `rejectedCount`, `invalid`, per-row `row`
- [x] 5.7 `@Tag`, `@Operation`, `@ApiResponses`
- [x] 5.8 Javadoc: "Direction loss — signed amounts → positive; debit/credit lost by design (MVP tradeoff)"
- [x] 6.1 Extract `"CLP"` constant
- [x] 6.2 Full Gradle test suite green (`./gradlew.bat test --rerun-tasks`)
- [x] 6.3 Verify: no bank API calls, no batch persistence, anti-corruption boundary intact
- [x] 7.1 Preserve source row traceability across categorized/manualReview/rejected result partitions
- [x] 7.2 Add WebMvc regression test for mixed non-sequential result partitions
- [x] 7.3 Run targeted controller+adapter tests, full Gradle suite, and JaCoCo report
- [x] 8.1 Add WebMvc RED tests for duplicate nonblank `bankTransactionId` partial success and all-duplicate/all-invalid behavior
- [x] 8.2 Reject every row sharing a duplicate nonblank `bankTransactionId` before adapter delegation with safe no-echo row-level messages
- [x] 8.3 Update OpenSpec spec/design/tasks/apply-progress/verify-report for duplicate `bankTransactionId` validation
- [x] 8.4 Run targeted controller tests, targeted adapter+controller tests, full Gradle suite, and JaCoCo report
- [x] 9.1 Add focused composition test invoking controller + real `SimulatedBankStatementAdapter` + real `CashflowIngestionService` idempotency without HTTP
- [x] 9.2 Prove re-importing the same `bankTransactionId` returns the same movement ID and creates no duplicate insert
- [x] 9.3 Run targeted composition test, full Gradle suite, JaCoCo report, and update OpenSpec verification artifacts

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/kuroneko/pymeflow/application/port/out/ExternalStatementImportContractsTest.java` | Unit | N/A (new) | ✅ Compile failed on missing `ExternalStatementImportPort` | ✅ Targeted tests passed | ➖ Single structural port contract | ➖ None needed |
| 1.2 | `src/test/java/com/kuroneko/pymeflow/application/port/out/ExternalStatementImportContractsTest.java` | Unit | N/A (new) | ✅ Compile failed on missing `ExternalStatementImportCommand` | ✅ Targeted tests passed | ✅ Valid command + null profile + defensive copy cases | ➖ None needed |
| 1.3 | `src/test/java/com/kuroneko/pymeflow/application/port/out/ExternalStatementImportContractsTest.java` | Unit | N/A (new) | ✅ Compile failed on missing `ExternalStatementEntry` | ✅ Targeted tests passed | ✅ Valid entry + blank/null/zero guard cases | ➖ None needed |
| 1.4 | `src/test/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfigurationTest.java` | Unit | N/A (new bean method; existing config had no direct test) | ✅ Compile failed on missing bean method and adapter class | ✅ Targeted tests passed | ➖ Structural bean wiring only | ➖ None needed |
| 1.5 | `src/test/java/com/kuroneko/pymeflow/architecture/ArchitectureTest.java` | Architecture | ✅ Existing ArchitectureTest baseline passed before modification | ✅ Boundary rule written before production adapter stub | ✅ Targeted architecture tests passed | ➖ Structural architecture guard | ➖ None needed |
| 2.1 | `src/test/java/com/kuroneko/pymeflow/infrastructure/bank/SimulatedBankStatementAdapterTest.java` | Unit | ✅ PR1 contract/config tests passed before modifying existing files | ✅ Adapter test compile failed before new 7-field entry support / mapping | ✅ Targeted adapter tests passed | ✅ Positive amount/date case prevents hardcoded abs mapping | ➖ None needed |
| 2.2 | `src/test/java/com/kuroneko/pymeflow/infrastructure/bank/SimulatedBankStatementAdapterTest.java` | Unit | ✅ PR1 contract/config tests passed | ✅ Test written before adapter mapping existed | ✅ Targeted adapter tests passed | ✅ Missing-counterparty task covers alternate branch | ➖ None needed |
| 2.3 | `src/test/java/com/kuroneko/pymeflow/infrastructure/bank/SimulatedBankStatementAdapterTest.java` | Unit | ✅ PR1 contract/config tests passed | ✅ Test written before adapter mapping existed | ✅ Targeted adapter tests passed | ✅ Counterparty-present task covers alternate branch | ➖ None needed |
| 2.4 | `src/test/java/com/kuroneko/pymeflow/infrastructure/bank/SimulatedBankStatementAdapterTest.java` | Unit | ✅ PR1 contract/config tests passed | ✅ Test written before CLP guard existed | ✅ Targeted adapter tests passed | ✅ CLP happy-path tests cover allowed branch | ➖ None needed |
| 2.5 | `src/test/java/com/kuroneko/pymeflow/infrastructure/bank/SimulatedBankStatementAdapterTest.java` | Unit | ✅ PR1 contract/config tests passed | ✅ Test written before delegation implementation existed | ✅ Targeted adapter tests passed | ✅ Captures mapped command and verifies passthrough result | ➖ None needed |
| 2.6 | `src/test/java/com/kuroneko/pymeflow/infrastructure/bank/SimulatedBankStatementAdapterTest.java` | Unit | ✅ PR1 contract/config tests passed | ✅ Test exercised existing contract guard before adapter GREEN | ✅ Targeted adapter tests passed | ✅ Valid references in delegation tests cover accepted branch | ➖ None needed |
| 2.7 | `src/test/java/com/kuroneko/pymeflow/infrastructure/bank/SimulatedBankStatementAdapterTest.java` | Unit | ✅ PR1 contract/config tests passed | ✅ Test written before account alias existed on entry | ✅ Targeted adapter tests passed | ✅ Description/reference assertions prove alias is not mapped into visible ingestion fields | ➖ None needed |
| 3.1 | `src/test/java/com/kuroneko/pymeflow/infrastructure/bank/SimulatedBankStatementAdapterTest.java` | Unit | ✅ PR1 contract/config tests passed | ✅ Adapter tests failed against stub implementation | ✅ Targeted adapter tests passed | ✅ Multiple mapping tests forced real implementation | ➖ None needed |
| 3.2 | `src/test/java/com/kuroneko/pymeflow/infrastructure/bank/SimulatedBankStatementAdapterTest.java` | Unit | ✅ PR1 contract/config tests passed | ✅ Mapping tests written first | ✅ Targeted adapter tests passed | ✅ Negative/positive amount, counterparty/no-counterparty, CLP/USD cases | ➖ None needed |
| 3.3 | `src/test/java/com/kuroneko/pymeflow/infrastructure/bank/SimulatedBankStatementAdapterTest.java` | Unit | ✅ PR1 contract/config tests passed | ✅ Delegation/passthrough test written first | ✅ Targeted adapter tests passed | ✅ Captured command verifies trimmed external reference and mapped transaction | ➖ None needed |
| 4.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Manual import controller safety net passed before PR3 changes | ✅ Compile failed on missing `CashflowBankStatementSimulatedController` | ✅ Targeted controller tests passed | ✅ Summary/list assertions prevent empty/trivial response | ➖ None needed |
| 4.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Manual import controller safety net passed | ✅ Empty rows test written before endpoint existed | ✅ Targeted controller tests passed | ✅ All-invalid task covers non-empty invalid rows branch | ➖ None needed |
| 4.3 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Manual import controller safety net passed | ✅ Blank ID test written before validation existed | ✅ Targeted controller tests passed | ✅ Valid ID and sensitive ID tests cover alternate branches | ➖ None needed |
| 4.4 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Manual import controller safety net passed | ✅ Zero amount test written before validation existed | ✅ Targeted controller tests passed | ✅ Valid positive and mixed signed amount acceptance paths cover alternate branch | ➖ None needed |
| 4.5 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Manual import controller safety net passed | ✅ Non-CLP no-echo test written before validation existed | ✅ Targeted controller tests passed | ✅ CLP valid tests cover allowed branch | ➖ None needed |
| 4.6 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Manual import controller safety net passed | ✅ Invalid date no-echo test written before validation existed | ✅ Targeted controller tests passed | ✅ Valid ISO date tests cover allowed branch | ➖ None needed |
| 4.7 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Manual import controller safety net passed | ✅ Sensitive ID no-echo test written before controller used `SensitiveDataPolicy` | ✅ Targeted controller tests passed | ✅ Blank/valid ID branches cover alternatives | ➖ None needed |
| 4.8 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Manual import controller safety net passed | ✅ Mixed batch test written before partial success logic existed | ✅ Targeted controller tests passed | ✅ All-invalid and all-valid tasks cover status branches | ➖ None needed |
| 4.9 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Manual import controller safety net passed | ✅ All-invalid test written before 400 branch existed | ✅ Targeted controller tests passed | ✅ Mixed batch task covers 200 partial branch | ➖ None needed |
| 4.10 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Manual import controller safety net passed | ✅ Reimport test written before endpoint delegated to port | ✅ Targeted controller tests passed | ✅ Response maps existing movement ID returned by mocked port | ➖ None needed |
| 4.11 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Manual import controller safety net passed | ✅ Row position test written before response row mapping existed | ✅ Targeted controller tests passed | ✅ Valid row command capture + invalid row response cover both traces | ➖ None needed |
| 5.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Manual import controller safety net passed | ✅ Endpoint tests failed before controller existed | ✅ Targeted controller tests passed | ✅ Valid, partial, and all-invalid requests cover endpoint branches | ➖ None needed |
| 5.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | N/A (new DTOs) | ✅ Tests posted all 7 request row fields before DTOs existed | ✅ Targeted controller tests passed | ✅ Optional counterparty omitted/null while account alias present | ➖ None needed |
| 5.3 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | N/A (new validation path) | ✅ Validation tests written first for ID, amount, date, currency, sensitive ID | ✅ Targeted controller tests passed | ✅ Valid rows and multiple invalid fields force real validation branches | ➖ None needed |
| 5.4 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | N/A (new validation path) | ✅ No-echo assertions written first for invalid currency/date/sensitive ID | ✅ Targeted controller tests passed | ✅ Sensitive and non-sensitive invalid fields cover message construction | ➖ None needed |
| 5.5 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | N/A (new endpoint path) | ✅ Partial/all-invalid tests written before controller branching existed | ✅ Targeted controller tests passed | ✅ 200 mixed and 400 all-invalid force real branching | ➖ None needed |
| 5.6 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | N/A (new response DTOs) | ✅ Response shape assertions written before DTOs existed | ✅ Targeted controller tests passed | ✅ Counts, lists, movement IDs, and row positions cover non-trivial mapping | ➖ None needed |
| 5.7 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration/reflection | N/A (new annotations) | ✅ Reflection test written before annotation metadata existed | ✅ Targeted controller tests passed | ✅ Checks both response codes and operation description | ➖ None needed |
| 5.8 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration/reflection | N/A (new Javadoc/OpenAPI metadata) | ✅ Direction-loss documentation assertion written before OpenAPI metadata existed | ✅ Targeted controller tests passed | ✅ Spanish direction-loss and debit/credit-loss clauses both asserted | ➖ None needed |
| 6.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Targeted controller tests passed before refactor | ✅ Existing CLP tests protected behavior before constant extraction | ✅ Targeted controller tests passed after refactor | ✅ CLP and USD tests cover both branches | ✅ Extracted `CLP` constant |
| 6.2 | Full suite | Integration/unit/architecture | ✅ Targeted adapter + controller tests passed | ✅ N/A — verification task | ✅ `./gradlew.bat test --rerun-tasks` passed | ➖ Verification only | ➖ None needed |
| 6.3 | Full suite + code inspection | Architecture/integration | ✅ Targeted adapter + controller tests passed | ✅ N/A — verification task | ✅ `./gradlew.bat test --rerun-tasks` passed | ➖ Verification only | ✅ Controller depends on port only; no bank API/batch persistence added |
| 7.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ 12/12 controller tests passed before fix | ✅ `preservesOriginalRowsAcrossMixedResultPartitions()` failed at compile because result records had no source reference metadata | ✅ Controller test passed after preserving source references in ingestion results and mapping rows by `bankTransactionId` | ✅ Mixed categorized/manualReview/rejected plus invalid rows force non-sequential row mapping | ✅ Backward-compatible constructors retained for existing tests |
| 7.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Existing controller suite passed before adding regression | ✅ Regression test written before production fix | ✅ Targeted controller suite passed | ✅ Exact row assertions: rejected row 1, categorized row 2, invalid row 3, manual review row 4 | ➖ None needed |
| 7.3 | Full suite + JaCoCo | Integration/unit/architecture | ✅ Targeted controller test established RED/GREEN | ✅ N/A — verification task | ✅ Targeted adapter+controller, full suite, and JaCoCo passed | ➖ Verification only | ➖ None needed |
| 8.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Existing controller suite passed before duplicate-ID changes | ✅ Duplicate partial-success and all-duplicate tests failed before production validation existed | ✅ Targeted controller suite passed after duplicate validation | ✅ Mixed unique+duplicate and all-duplicate batches force both 200 partial and 400 all-invalid branches | ➖ None needed |
| 8.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Duplicate tests established RED first | ✅ Tests asserted safe no-echo duplicate messages and no adapter delegation for all-duplicate batch | ✅ Targeted controller suite passed | ✅ Trimmed duplicate ID variant plus unique row proves real duplicate normalization | ✅ Extracted duplicate ID scan into focused helper |
| 8.3 | OpenSpec artifacts | Documentation | ✅ Existing OpenSpec artifacts read before update | ✅ N/A — artifact update | ✅ Spec/design/tasks/apply-progress/verify-report updated | ➖ Documentation-only | ➖ None needed |
| 8.4 | Full suite + JaCoCo | Integration/unit/architecture | ✅ Targeted controller GREEN established | ✅ N/A — verification task | ✅ Targeted controller, targeted adapter+controller, full suite, and JaCoCo passed | ➖ Verification only | ➖ None needed |
| 9.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedCompositionTest.java` | Focused composition integration (no HTTP) | ✅ Related controller + adapter + ingestion suites passed before adding hardening test | ✅ Test written before any production/documentation change; it closed the missing executable controller→adapter→ingestion idempotency coverage gap | ✅ Targeted composition test passed | ➖ Single warning-specific scenario | ➖ None needed |
| 9.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedCompositionTest.java` | Focused composition integration (no HTTP) | ✅ Composition test added after safety net | ✅ Assertions require same returned movement ID and exactly one inserted record for repeated `BT-COMPOSITION-001` | ✅ Targeted composition test and full suite passed | ✅ Same ID + no duplicate insert + mapped description/absolute amount prove real path behavior | ➖ None needed |
| 9.3 | Full suite + JaCoCo + OpenSpec artifacts | Verification/documentation | ✅ Targeted composition GREEN established | ✅ N/A — verification/artifact update | ✅ Targeted composition test, full Gradle suite, and JaCoCo passed | ➖ Verification only | ➖ None needed |

## Test Summary

- Total tests written: 31 test methods cumulative (7 PR1 methods + 8 PR2 adapter methods + 15 PR3/controller regression methods + 1 focused composition integration method)
- Total tests passing: full `./gradlew.bat test --rerun-tasks` suite passed (32 suites, 174 tests, 0 failures, 0 errors, 0 skipped)
- Layers used: Unit (14), Integration/WebMvc (14), Focused composition integration (1), Architecture (1)
- Approval tests (refactoring): None — no behavioral refactoring tasks beyond constant extraction guarded by controller tests
- Pure functions created: 1 (`descriptionFor`)

## Verification

- `./gradlew.bat test --tests com.kuroneko.pymeflow.interfaces.web.CashflowManualImportControllerTest --rerun-tasks` — passed as PR3 safety net before adding the new controller
- `./gradlew.bat test --tests com.kuroneko.pymeflow.interfaces.web.CashflowBankStatementSimulatedControllerTest --rerun-tasks` — RED failed at compile first because `CashflowBankStatementSimulatedController` did not exist; GREEN passed after endpoint implementation
- `./gradlew.bat test --tests com.kuroneko.pymeflow.infrastructure.bank.SimulatedBankStatementAdapterTest --tests com.kuroneko.pymeflow.interfaces.web.CashflowBankStatementSimulatedControllerTest --rerun-tasks` — passed
- `./gradlew.bat test --rerun-tasks` — passed
- `./gradlew.bat test --tests com.kuroneko.pymeflow.interfaces.web.CashflowBankStatementSimulatedControllerTest --rerun-tasks` — verification-fix safety net passed before production changes; RED failed after adding `preservesOriginalRowsAcrossMixedResultPartitions()` because result records lacked source reference metadata; GREEN passed after fix
- `./gradlew.bat test --tests com.kuroneko.pymeflow.infrastructure.bank.SimulatedBankStatementAdapterTest --tests com.kuroneko.pymeflow.interfaces.web.CashflowBankStatementSimulatedControllerTest --rerun-tasks` — passed after row traceability fix
- `./gradlew.bat test --rerun-tasks` — passed after row traceability fix
- `./gradlew.bat jacocoTestReport` — passed after row traceability fix
- `./gradlew.bat test --tests com.kuroneko.pymeflow.interfaces.web.CashflowBankStatementSimulatedControllerTest --rerun-tasks` — duplicate-ID safety net passed before production changes; RED failed after adding duplicate partial-success and all-duplicate tests; GREEN passed after duplicate row validation; refactor pass also passed
- `./gradlew.bat test --tests com.kuroneko.pymeflow.infrastructure.bank.SimulatedBankStatementAdapterTest --tests com.kuroneko.pymeflow.interfaces.web.CashflowBankStatementSimulatedControllerTest --rerun-tasks` — passed after duplicate-ID fix
- `./gradlew.bat test --rerun-tasks` — passed after duplicate-ID fix
- `./gradlew.bat jacocoTestReport` — passed after duplicate-ID fix
- `./gradlew.bat test --tests com.kuroneko.pymeflow.interfaces.web.CashflowBankStatementSimulatedControllerTest --tests com.kuroneko.pymeflow.infrastructure.bank.SimulatedBankStatementAdapterTest --tests com.kuroneko.pymeflow.application.cashflow.CashflowIngestionServiceTest --rerun-tasks` — verification-hardening safety net passed before adding the composition test
- `./gradlew.bat test --tests com.kuroneko.pymeflow.interfaces.web.CashflowBankStatementSimulatedCompositionTest --rerun-tasks` — passed after final cleanup; focused composition test proves repeated `bankTransactionId` returns the same movement ID and only one inserted history record through real controller/adapter/ingestion objects
- `./gradlew.bat test --rerun-tasks` — passed after hardening test (32 suites, 174 tests, 0 failures, 0 errors, 0 skipped)
- `./gradlew.bat jacocoTestReport` — passed after hardening test

## Deviations / Notes

- The PR1 note still applies: the literal task text said `infrastructure.bank..` must not depend on `domain..` or `application..`, but the design requires this adapter to implement the application port and delegate to `CashflowIngestionService`. The implemented architecture keeps domain/application from depending on bank infrastructure.
- `ExternalStatementEntry` gained optional `counterpartyName` and `accountAlias` fields with a backward-compatible 5-argument constructor. This is needed for the adapter to own counterparty enrichment and account alias dropping as required by the spec/design.
- PR3 keeps mapping/delegation at the boundary: the controller validates HTTP/JSON rows, builds `ExternalStatementEntry` commands, and delegates reimport/idempotency plus bank-to-ingestion mapping to `ExternalStatementImportPort`.
- OpenAPI direction-loss documentation is neutral Spanish for the Chilean market, while code identifiers and technical artifacts remain English.
- Task 6.2 names Maven commands, but this Gradle project uses the configured test command `./gradlew.bat test --rerun-tasks`; verification used Gradle.
- Verification-fix note: `CashflowIngestionService` result records now carry optional `sourceReference` metadata with backward-compatible constructors. The simulated controller maps result partitions back to original rows by `bankTransactionId`/source reference and falls back to sequential mapping only when legacy results omit metadata.
- Duplicate-ID fix note: the simulated controller now pre-scans nonblank trimmed `bankTransactionId` values and rejects every row sharing a duplicate value with the safe message `El identificador bancario está duplicado en la solicitud.`; unique rows in the same request still delegate to the adapter.
- Verification-hardening note: the warning-specific test intentionally uses a focused composition layer instead of full Spring/HTTP context to keep the slice small while exercising the real `CashflowBankStatementSimulatedController`, real `SimulatedBankStatementAdapter`, and real `CashflowIngestionService` idempotency behavior with in-memory ports.

## Remaining Tasks

- None.

## Status

47/47 tasks complete. Controller→adapter→ingestion idempotency warning resolved by focused composition coverage; change is ready for re-verify/archive.
