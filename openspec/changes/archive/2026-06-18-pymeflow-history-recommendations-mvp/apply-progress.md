# Apply Progress: PymeFlow History Recommendations MVP

## Work Unit

- **Current PR**: Verification fix batch — service threshold corrections and stateless runtime evidence
- **Delivery strategy**: feature-branch-chain
- **Mode**: Strict TDD
- **Scope boundary**: Surgical verification-failure fixes only: recommendation threshold semantics, rejection-rate denominator, focused service tests, and OpenSpec evidence updates. No endpoint contract changes, no persistence snapshots, no AI/ML.

## Completed Tasks

- [x] 1.1 **RED** — Added `findByStatus` adapter tests covering status filtering for `PROJECTABLE`, `MANUAL_REVIEW`, and `REJECTED`; empty unmatched status; profile isolation; movement-date ordering; rejected rows preserving `rejectionReasonCode` with null `safeDescription`.
- [x] 1.2 **GREEN** — Added `findByStatus(ProfileId, CashflowMovementStatus)` to `CashflowMovementHistoryPort`.
- [x] 1.3 **GREEN** — Implemented `CashflowMovementHistoryJdbcAdapter.findByStatus` with `SELECT_COLUMNS + " where profile_id = ? and status = ? order by movement_date, created_at"`; existing manual-review and projection-ready finders delegate to it.
- [x] 2.1 **RED** — Added `HistoryRecommendationServiceTest` before production service code, covering manual-review thresholds, rejection-rate thresholds and aggregate-only metrics, category concentration, insufficient data, recent inactivity, healthy fallback, severity ordering, and profile-not-found behavior.
- [x] 2.2 **GREEN** — Created `HistoryRecommendationService.generate(ProfileId)` loading `MANUAL_REVIEW`, `PROJECTABLE`, and `REJECTED` via `findByStatus`, computing deterministic hardcoded signals, ordering WARNING before INFO, and returning `HEALTHY_HISTORY` only when no other signal applies.
- [x] 2.3 **GREEN** — Added `HistoryRecommendationResponse` and `HistorySignalResponse` records in the service file; rejected metrics expose only `rejectedCount`, `projectableCount`, `rejectionRatePercent`, and `topRejectionReasonCode`.
- [x] 3.1 **RED** — Added `HistoryRecommendationControllerTest` first with `@WebMvcTest` coverage for happy-path JSON shape and deterministic signal order, blank and missing profile validation, profile-not-found neutral Spanish error, and absence of sensitive rejected fields/values.
- [x] 3.2 **GREEN** — Created `HistoryRecommendationController` exposing `GET /api/cashflow/recommendations?profileId={id}` with inner DTO records, OpenAPI annotations, interface validation, and service delegation only.
- [x] 3.3 **GREEN** — Wired `HistoryRecommendationService` in `ApplicationServiceConfiguration` using existing `VerticalProfileService` and `CashflowMovementHistoryPort` beans.
- [x] 4.1 — Ran full suite after fixes: `./gradlew.bat test --rerun-tasks` passed with 142 tests.
- [x] 4.2 — Confirmed ArchUnit boundary rules passed as part of the full suite (`ArchitectureTest`: 4 tests, 0 failures).
- [x] 4.3 — Ran JaCoCo report after fixes: `./gradlew.bat jacocoTestReport` passed.
- [x] 5.1 **RED** — Added focused boundary tests proving the verification failures: exactly 5 manual-review rows must be WARNING, exactly 30% rejected over total history must be WARNING, and exactly 60% category concentration must emit an INFO signal.
- [x] 5.2 **GREEN** — Updated `HistoryRecommendationService` to use `>=` semantics for manual-review, rejection-rate, and category-concentration thresholds; rejection-rate denominator now includes `MANUAL_REVIEW`, `PROJECTABLE`, and `REJECTED` persisted history.
- [x] 5.3 **RED/GREEN** — Added repeated-request runtime service test proving deterministic recomputation and no write-like history-port methods (`saveAll`, `resolveManualReview`) are invoked; read statuses are invoked twice in the same order.
- [x] 5.4 — Completed post-fix targeted, full-suite, and JaCoCo verification.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/CashflowMovementHistoryJdbcAdapterTest.java` | Integration (`@JdbcTest`) | ✅ Existing adapter tests passed: `BUILD SUCCESSFUL` | ✅ Written first; failed compile because `findByStatus(ProfileId, CashflowMovementStatus)` did not exist | ✅ Passed after port/adapter implementation | ✅ Covered non-empty status matches, empty unmatched status, three statuses, profile isolation, ordering, and rejected safe fields | ✅ Test helpers generalized with date/source-reference overload |
| 1.2 | Same | Integration-driven port contract | ✅ Same safety net | ✅ Test referenced missing port/adapter method before production change | ✅ Compile and adapter tests passed | ✅ Multiple status inputs forced generic method rather than status-specific method | ✅ Existing test fakes updated to satisfy new port contract |
| 1.3 | Same | Integration (`@JdbcTest`) | ✅ Same safety net | ✅ Adapter tests failed before implementation | ✅ `./gradlew.bat test --rerun-tasks --tests "*CashflowMovementHistoryJdbcAdapterTest"` passed | ✅ PROJECTABLE, MANUAL_REVIEW, REJECTED, empty status, profile isolation, ordering | ✅ Existing `findPendingManualReviews` and `findProjectionReady` delegate to `findByStatus` |
| 2.1 | `src/test/java/com/kuroneko/pymeflow/application/recommendation/HistoryRecommendationServiceTest.java` | Unit | N/A (new service/test files) | ✅ Written first; `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationServiceTest"` failed compile because `HistoryRecommendationService` did not exist | ✅ Passed after service/model implementation | ✅ 10 behavioral tests cover threshold edges, empty/no-projectable data, safe rejected metrics, ordering, profile lookup, inactivity, and healthy fallback | ✅ Test helpers kept in-memory fake focused on `findByStatus` behavior |
| 2.2 | Same | Unit | N/A (new service file) | ✅ Service tests referenced missing `generate(ProfileId)` behavior first | ✅ Targeted service tests passed | ✅ Different status mixes forced real deterministic rule evaluation instead of hardcoded output | ✅ Rules extracted into small private methods with named threshold constants |
| 2.3 | Same | Unit | N/A (new records in service file) | ✅ Tests referenced response/signal records and aggregate metrics before production records existed | ✅ Targeted service tests passed | ✅ Safe-metrics test verifies rejected descriptions/source references are absent while aggregate metrics are present | ✅ Records defensively copy signal lists and metric maps |
| 3.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/HistoryRecommendationControllerTest.java` | Integration (`@WebMvcTest`) | N/A (new controller/test file) | ✅ Written first; targeted test failed compile because `HistoryRecommendationController` did not exist | ✅ Targeted controller tests passed after controller implementation | ✅ Added separate missing-profile test after blank-profile GREEN; it failed until `@RequestParam(required = false)` allowed existing validation shape | ✅ Assertions verify concrete JSON fields, Spanish messages, metrics, ordering, and sensitive-field absence |
| 3.2 | Same | Integration (`@WebMvcTest`) | N/A (new controller file) | ✅ Controller tests referenced missing endpoint and DTO response before production controller existed | ✅ `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationControllerTest"` passed, 5 tests | ✅ Happy path, validation, service exception, and sensitive-output cases force real mapping and validation behavior | ✅ DTO mapping kept as inner records; controller remains HTTP-only and delegates rules to service |
| 3.3 | Same plus full suite | Integration / application context | ✅ Existing full suite passed after PR2 | ✅ Controller test required injectable service; full suite validates Spring context wiring | ✅ Targeted controller tests and full `./gradlew.bat test --rerun-tasks` passed | ✅ Bean wiring verified with real application-context tests in the full suite | ✅ Bean factory follows existing `ApplicationServiceConfiguration` pattern |
| 5.1 | `src/test/java/com/kuroneko/pymeflow/application/recommendation/HistoryRecommendationServiceTest.java` | Unit | ✅ Baseline service tests passed before modification: `BUILD SUCCESSFUL`, 10 tests | ✅ Boundary tests written first; targeted run failed at exactly 5 manual reviews, exactly 30% rejected over total history, and exactly 60% category concentration | ✅ Passed after service threshold fixes | ✅ Existing below-threshold tests preserved 4 manual-review INFO and 20% rejection absent behavior | ✅ Invalid zero-amount fixture corrected before GREEN so assertions exercise production behavior |
| 5.2 | Same | Unit | ✅ Same service safety net | ✅ Tests specified new `>=` semantics and total-history denominator before production change | ✅ `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationServiceTest"` passed, 14 tests | ✅ Boundary plus below-boundary cases prove non-strict threshold behavior and denominator change | ✅ Production change stayed minimal: comparison operators and denominator only |
| 5.3 | Same | Unit | ✅ Same service safety net | ✅ Runtime repeated-request test added before relying on verification evidence | ✅ Targeted service tests passed; two responses are equal and write-like fake counters remain zero | ✅ Test asserts two full read cycles (`MANUAL_REVIEW`, `PROJECTABLE`, `REJECTED` repeated) and no `saveAll`/`resolveManualReview` calls | ✅ Fake port gained lightweight counters without adding snapshot concepts to production code |

## Test Summary

- **PR3 RED evidence**: `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationControllerTest"` — failed compile with missing `HistoryRecommendationController`; later missing-profile triangulation failed until request parameter handling was generalized.
- **PR3 targeted GREEN**: `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationControllerTest"` — passed, 5 tests.
- **Verification-fix safety net**: `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationServiceTest"` — passed before modifications, 10 tests.
- **Verification-fix RED evidence**: `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationServiceTest"` — failed with 3 expected boundary failures before production fixes.
- **Verification-fix targeted GREEN**: `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationServiceTest"` — passed, 14 tests.
- **Post-fix full verification run**: `./gradlew.bat test --rerun-tasks` — passed, 142 tests.
- **Post-fix coverage run**: `./gradlew.bat jacocoTestReport` — passed.
- **Total tests written in PR3**: 5 controller integration tests.
- **Total tests written in verification fix batch**: 4 service unit tests.
- **Layers used in fix batch**: Unit.
- **Approval tests**: None — no behavior-preserving refactor task.
- **Pure functions created**: 0 public pure functions; existing private rule helpers were adjusted surgically.

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `src/main/java/com/kuroneko/pymeflow/application/port/out/CashflowMovementHistoryPort.java` | Modified in PR1 | Added generic `findByStatus(ProfileId, CashflowMovementStatus)` port method. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/persistence/CashflowMovementHistoryJdbcAdapter.java` | Modified in PR1 | Added indexed status query and delegated existing status-specific finders to it. |
| `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/CashflowMovementHistoryJdbcAdapterTest.java` | Modified in PR1 | Added status query coverage and helper overload for ordered projectable fixtures. |
| `src/test/java/com/kuroneko/pymeflow/application/cashflow/CashflowIngestionServiceTest.java` | Modified in PR1 | Updated fake port to satisfy the new port contract. |
| `src/test/java/com/kuroneko/pymeflow/application/cashflow/CashflowMovementHistoryServiceTest.java` | Modified in PR1 | Updated fake port and reused `findByStatus` for existing fake status-specific methods. |
| `src/main/java/com/kuroneko/pymeflow/application/recommendation/HistoryRecommendationService.java` | Created in PR2; modified in fix batch | Added deterministic recommendation service; fix batch changed thresholds to `>=` and rejection-rate denominator to total persisted history across manual-review, projectable, and rejected rows. |
| `src/test/java/com/kuroneko/pymeflow/application/recommendation/HistoryRecommendationServiceTest.java` | Created in PR2; modified in fix batch | Added focused unit tests for deterministic rules, exact-boundary thresholds, total-history rejection-rate denominator, and repeated-request stateless recomputation. |
| `src/main/java/com/kuroneko/pymeflow/interfaces/web/HistoryRecommendationController.java` | Created in PR3 | Added recommendations REST endpoint, inner DTO mapping records, OpenAPI annotations, and profile validation. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/HistoryRecommendationControllerTest.java` | Created in PR3 | Added `@WebMvcTest` coverage for response shape, validation, profile-not-found mapping, deterministic signal order, and sensitive-output safety. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfiguration.java` | Modified in PR3 | Registered `HistoryRecommendationService` bean. |
| `openspec/changes/pymeflow-history-recommendations-mvp/tasks.md` | Modified | Marked all verification tasks complete and added checked verification-fix tasks. |
| `openspec/changes/pymeflow-history-recommendations-mvp/apply-progress.md` | Modified | Captured cumulative PR1 + PR2 + PR3 apply progress plus verification-fix TDD evidence. |
| `openspec/changes/pymeflow-history-recommendations-mvp/verify-report.md` | Modified | Updated verdict and evidence after surgical fixes. |

## Deviations from Design

- `HistoryRecommendationService` includes a `Clock` constructor overload to make `generatedAt` and recent-inactivity rules deterministic in unit tests; the public two-argument constructor still uses `Clock.systemUTC()` and preserves runtime behavior.
- PR3 keeps all rule/copy ownership in `HistoryRecommendationService`; the controller maps service output only, preserving the hexagonal boundary.
- Proposal, design, and task wording now document the same threshold semantics used by the spec and implementation: manual-review warning at `>=5`, rejection rate at `>=30%` of total persisted history, and category concentration at `>=60%` of projectable amount.

## Issues Found

- Missing `profileId` must be handled with `@RequestParam(required = false)` so the existing neutral Spanish `VALIDATION_ERROR` shape is returned instead of Spring's default missing-parameter error.
- `RECENT_INACTIVITY` follows the design decision to consider any loaded status, not only projectable movements.
- Verification found the spec is authoritative over conflicting proposal/design threshold wording; manual-review, rejection-rate, and category-concentration boundaries now use `>=`.

## Remaining Tasks

- None — all implementation, verification-fix, full-suite, ArchUnit, and JaCoCo tasks are complete.
