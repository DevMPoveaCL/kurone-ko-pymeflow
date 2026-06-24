# Apply Progress: PymeFlow Cashflow Direction Hardening

## Status

| Field | Value |
|-------|-------|
| Current PR | PR #6 — Smoke + Docs |
| Status | Complete — verification warnings resolved |
| Scope boundary | Clean — PR #6 smoke/docs plus post-verify warning cleanup only |
| Artifact store | OpenSpec |
| Strict TDD | Active |
| Chain strategy | feature-branch-chain |

## Completed Tasks

- [x] 1.1 Write `TransactionDirectionTest`: enum values `DEBIT`, `CREDIT`; not equal to `CashflowDirection`.
- [x] 1.2 Write `TransactionTest` for new canonical constructor: `direction` required; overload defaults to `CREDIT`; null validation.
- [x] 1.3 Write `CashflowMovementDraftTest` for new `direction` field: required, not null; amount must stay positive.
- [x] 1.4 Write `CashflowMovementRecordTest` for new `direction` field: round-tripped from draft; nullable disallowed.
- [x] 1.5 Write `ExternalStatementEntryTest`: accepts signed amounts unchanged (adapter owns mapping).
- [x] 1.6 Create `domain/cashflow/TransactionDirection.java`: `public enum TransactionDirection { DEBIT, CREDIT }`.
- [x] 1.7 Add `TransactionDirection direction` to `Transaction` canonical ctor; overload defaults to `CREDIT`; validate not null.
- [x] 1.8 Add `TransactionDirection direction` to `CashflowMovementDraft` record; validate not null.
- [x] 1.9 Add `TransactionDirection direction` to `CashflowMovementRecord` record; validate not null, propagate from draft in `IngestionOutcome` static factories.
- [x] 1.10 Add `TransactionDirection direction` to `ExternalStatementEntry` (default `null` in old overload; adapter populates).
- [x] 1.11 Add `TransactionDirection direction` to `ProjectionReadyCashflowTransaction`, `ProjectedCashflowTransaction`, `PendingManualReviewMovement` records.
- [x] 1.12 Run `./gradlew test` — all PR #1 tests green. Verify `ArchitectureTest` still passes.
- [x] 2.1 Write migration integration test: Flyway V4 applies cleanly; `movement_direction` column exists with `NOT NULL DEFAULT 'CREDIT'`.
- [x] 2.2 Extend `CashflowMovementHistoryJdbcAdapterTest`: INSERT with `DEBIT` reads back `DEBIT`; old rows without column default to `CREDIT`.
- [x] 2.3 Create `db/migration/V4__add_movement_direction.sql`: additive `movement_direction` column with `NOT NULL DEFAULT 'CREDIT'` and allowed values check.
- [x] 2.4 Add `movement_direction` to `CashflowMovementHistoryJdbcAdapter` INSERT; map from `draft.direction().name()`.
- [x] 2.5 Add `movement_direction` to SELECT column list; map in `mapRow()` via `TransactionDirection.valueOf(...)`.
- [x] 2.6 Run focused persistence/Flyway tests and full suite.
- [x] 3.1 Write `SimulatedBankStatementAdapterTest`: `amount=-15000` → `IngestionItem` has `direction=DEBIT, amount=15000`; `amount=15000` → `CREDIT, amount=15000`.
- [x] 3.2 Write `TransactionFingerprintTest`: direction excluded — same `(profileId,amount,currency,date,desc)` produces identical hash regardless of `DEBIT`/`CREDIT`.
- [x] 3.3 Write `CashflowIngestionServiceTest`: draft carries direction from adapter through to persisted draft.
- [x] 3.4 In `SimulatedBankStatementAdapter`: replace `amount.abs()` with sign→direction mapping: `signum() < 0 → DEBIT`, `≥ 0 → CREDIT`; pass positive `abs()` amount.
- [x] 3.5 In `CashflowIngestionService.IngestionOutcome` factories: pass `transaction.direction()` to draft constructors.
- [x] 3.6 Document in `TransactionFingerprint.java` javadoc: direction excluded from `fp:v1` by design.
- [x] 3.7 Run focused PR #3 tests and full suite; all green.
- [x] 4.1 Extend `CashflowManualImportControllerTest`: optional `movementDirection` on row; omitted defaults `CREDIT`; invalid value rejected with field error.
- [x] 4.2 Extend `CashflowBankStatementSimulatedControllerTest`: response entries include `movementDirection`.
- [x] 4.3 Extend `CashflowHistoryControllerTest`: `PendingManualReviewMovementResponse` and `ProjectionReadyTransactionResponse` include `movementDirection`.
- [x] 4.4 Extend `CashflowProjectionControllerTest`: request DTO accepts optional `movementDirection` and maps it into projection command transactions.
- [x] 4.5 Extend `ManualReviewResolutionControllerTest`: transient and persisted resolution responses expose direction.
- [x] 4.6 Add `String movementDirection` to `CashflowManualImportController.ManualImportRow`; default `null` → `CREDIT`; invalid values return a row-level field error.
- [x] 4.7 Add `movementDirection` to controller DTOs in `CashflowBankStatementSimulatedController`, `CashflowHistoryController`, `CashflowProjectionController` request mapping, and `ManualReviewResolutionController`.
- [x] 4.8 Map `TransactionDirection` to response strings in `.from()` methods; manual import row resolution now constructs `Transaction` with resolved direction.
- [x] 4.9 Run focused PR #4 controller tests, controller/composition suite, and full suite; all green.
- [x] 5.1 Write `HistoryRecommendationServiceTest` coverage for `DEBIT + INFLOW` and `CREDIT + OUTFLOW` mismatches producing `DIRECTION_MISMATCH` with aggregate counts only.
- [x] 5.2 Write aligned movement test proving `DEBIT + OUTFLOW` and `CREDIT + INFLOW` produce no mismatch signal and preserve healthy fallback.
- [x] 5.3 Add `addDirectionMismatchSignal()` in `HistoryRecommendationService`; it queries projectable movements already loaded by `generate()`, compares movement direction against profile category direction, and emits aggregate `debitInflowCount` / `creditOutflowCount` metrics only.
- [x] 5.4 Wire mismatch signal after existing signal builders and before `HEALTHY_HISTORY` fallback.
- [x] 5.5 Run focused recommendation tests and full suite; mismatch signal is `INFO`, so severity rank remains 1 and warning-first ordering is unchanged.
- [x] 6.1 Write `CashflowDirectionSmokeTest`: simulated bank-statement negative amount persists as `DEBIT` with positive amount; positive amount persists as `CREDIT`; history projection-ready query returns both directions.
- [x] 6.2 Write `CashflowDirectionSmokeTest`: manual import omitted direction defaults to `CREDIT`; explicit `DEBIT` persists; replaying the same fingerprint with changed direction returns the existing movement.
- [x] 6.3 Remove direction-loss tradeoff documentation from active OpenSpec baseline specs and stale simulated bank-statement OpenAPI wording.
- [x] 6.4 Update `openspec/specs/` baseline with direction-preservation requirements, including new `cashflow-direction-preservation` baseline spec and related manual import/idempotency/history/bank-statement requirements.
- [x] 6.5 Run focused smoke tests, `ArchitectureTest`, and full `./gradlew.bat test --rerun-tasks`; all green.

## TDD Cycle Evidence

| Task(s) | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1–1.5 | PR #1 test files | Unit | Historical PR #1 context | Existing cancelled implementation created tests first, but full historical RED output is unavailable after sub-agent cancellation. | Current focused PR #1 tests pass. | PR #3-specific ingestion tests were removed from PR #1 boundary. | Compatibility overloads/defaults retained to keep PR #1 additive. |
| 1.6–1.11 | PR #1 test files | Unit | Historical PR #1 context | Covered by the PR #1 tests above. Full historical RED output is unavailable after sub-agent cancellation. | Domain and application records compile and tests pass. | Direction-aware domain/application records covered; ingestion propagation deferred. | PR #3-specific propagation removed from PR #1 boundary. |
| 1.12 | Full suite | Verification | N/A | N/A | Focused PR #1 tests, `ArchitectureTest`, and full suite pass. | N/A | Docker/Postgres dependency documented by execution evidence. |
| 2.1 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/FlywaySeedIntegrationTest.java` | Integration | ✅ Baseline focused persistence/Flyway tests passed before changes. | ✅ `flywayV4AddsMovementDirectionColumnWithCreditDefault` failed because `movement_direction` did not exist. | ✅ Focused persistence/Flyway tests passed after V4 migration. | ➖ Single schema contract: column exists, non-null, default `CREDIT`. | ✅ Migration kept additive; no destructive schema change. |
| 2.2 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/CashflowMovementHistoryJdbcAdapterTest.java` | Integration | ✅ Baseline focused persistence/Flyway tests passed before changes. | ✅ New tests failed before V4/JDBC mapping: missing migration script/column mapping. | ✅ `DEBIT` draft round-trips and legacy-style insert without `movement_direction` reads as `CREDIT`. | ✅ Two cases: explicit `DEBIT` insert and omitted-column `CREDIT` default. | ✅ Test setup now recreates H2 movement table before applying V2+V4 to avoid duplicate migration state. |
| 2.3–2.5 | Same as 2.1–2.2 | Persistence integration | ✅ Baseline focused persistence/Flyway tests passed before changes. | ✅ Tests demanded V4 and adapter read/write of `movement_direction`. | ✅ Adapter INSERT writes `draft.direction().name()`; SELECT maps `movement_direction` to `TransactionDirection`. | ✅ Explicit `DEBIT` and default `CREDIT` prove non-hardcoded mapping. | ✅ Import added only in infrastructure adapter; domain/application boundaries unchanged. |
| 2.6 | Gradle suite | Verification | N/A | N/A | ✅ `./gradlew.bat test --rerun-tasks --tests "*CashflowMovementHistoryJdbcAdapter*" --tests "*Flyway*"` and `./gradlew.bat test --rerun-tasks` passed. | N/A | ✅ No additional cleanup needed. |
| 3.1 | `src/test/java/com/kuroneko/pymeflow/infrastructure/bank/SimulatedBankStatementAdapterTest.java` | Unit | ✅ Focused PR #3 baseline passed before changes. | ✅ Direction assertions failed for negative amount because adapter still defaulted transactions to `CREDIT`. | ✅ Adapter maps `signum() < 0` to `DEBIT` and keeps positive amount via `abs()`. | ✅ Two cases: `-15000 → DEBIT/15000` and positive amount → `CREDIT`/positive amount. | ✅ Extracted `directionFor(BigDecimal)` helper; focused tests stayed green. |
| 3.2 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/TransactionFingerprintTest.java` | Unit | ✅ Focused PR #3 baseline passed before changes. | ✅ Test written before production doc update; current code already excluded direction, so behavior was already green. | ✅ Fingerprint remains identical for same fields with `DEBIT` vs `CREDIT`. | ✅ Existing fingerprint tests cover deterministic hash, normalization, and material-field differences; new direction case proves exclusion. | ✅ Added javadoc documenting direction exclusion from `fp:v1`; focused tests stayed green. |
| 3.3 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/CashflowIngestionServiceTest.java` | Unit | ✅ Focused PR #3 baseline passed before changes. | ✅ Draft-direction assertion failed because `IngestionOutcome` factories used compatibility constructors defaulting drafts to `CREDIT`. | ✅ Categorized, manual-review, and rejected drafts now receive `transaction.direction()`. | ✅ Three paths covered: projectable `DEBIT`, manual review `CREDIT`, rejected `DEBIT`. | ✅ Existing-record reconstruction now also carries `record.direction()` to avoid direction loss on duplicate responses. |
| 3.4–3.6 | Same PR #3 test files | Unit | ✅ Focused PR #3 baseline passed before changes. | ✅ Tests for adapter direction, fingerprint direction exclusion, and ingestion draft propagation were in place before production changes. | ✅ Focused command passed after production changes. | ✅ Adapter and service tests include multiple directions/statuses; fingerprint test triangulates against existing material-field coverage. | ✅ No PR #4/#5/#6 code touched. |
| 3.7 | Gradle suite | Verification | N/A | N/A | ✅ Focused PR #3 command and full `./gradlew.bat test --rerun-tasks` passed. | N/A | ✅ Docker/Postgres confirmed running before full suite. |
| 4.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowManualImportControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Baseline `./gradlew.bat test --rerun-tasks --tests "*Controller*" --tests "*Composition*"` passed before PR #4 changes. | ✅ Tests failed before production changes: explicit `DEBIT` still delegated as default `CREDIT`; invalid direction reached service path; response lacked `movementDirection`. | ✅ Manual import now accepts optional `movementDirection`, defaults omitted/null to `CREDIT`, validates enum values, and returns row-level field error. | ✅ Three cases: omitted → `CREDIT`, explicit `DEBIT`, invalid → `movementDirection` field error. | ✅ Row matching includes direction to avoid ambiguous mapping for otherwise identical rows. |
| 4.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Controller/composition baseline passed. | ✅ JSON path assertions failed because categorized/manual-review/rejected responses did not expose `movementDirection`. | ✅ `TransactionResponse` and rejected response now map `transaction.direction().name()`. | ✅ Covers `CREDIT` categorized plus all partitions with `CREDIT`/`DEBIT`. | ✅ No adapter or PR #5 logic changed. |
| 4.3 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowHistoryControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Controller/composition baseline passed. | ✅ JSON path assertions failed because history responses lacked `movementDirection`. | ✅ Pending manual review and projection-ready responses now expose `movement.direction().name()` / `transaction.direction().name()`. | ✅ Two response shapes covered: pending manual review `DEBIT`, projection-ready `CREDIT`. | ✅ Sensitive fields remain excluded where previously excluded. |
| 4.4 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowProjectionControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Controller/composition baseline passed. | ✅ Captured command assertion failed because request `movementDirection` was ignored and defaulted to `CREDIT`. | ✅ Projection request DTO now accepts optional `movementDirection` and maps it to `ProjectedCashflowTransaction`. | ✅ Explicit `DEBIT` and omitted `CREDIT` command mapping covered. | ✅ Kept projection response shape unchanged because this controller has no transaction response list. |
| 4.5 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/ManualReviewResolutionControllerTest.java` | Integration (`@WebMvcTest`) | ✅ Controller/composition baseline passed. | ✅ JSON path assertions failed because transient and persisted resolution responses lacked `movementDirection`. | ✅ Projectable and projection-ready resolution DTOs now expose `transaction.direction().name()`. | ✅ Covers transient `DEBIT` and persisted `CREDIT` responses. | ✅ No resolution service behavior changed. |
| 4.6–4.8 | PR #4 controller files | Integration/API contract | ✅ Controller/composition baseline passed. | ✅ PR #4 tests demanded additive request/response direction contract before production edits. | ✅ Focused PR #4 controller tests passed after implementation. | ✅ Manual import request, bank response, history responses, projection request mapping, and resolution responses covered. | ✅ Implementation stayed in interface/web DTO mapping; domain/application/infrastructure behavior unchanged. |
| 4.9 | Gradle suite | Verification | N/A | N/A | ✅ Required controller/composition suite and full `./gradlew.bat test --rerun-tasks` passed. | N/A | ✅ Docker/Postgres confirmed running before verification. |
| 5.1 | `src/test/java/com/kuroneko/pymeflow/application/recommendation/HistoryRecommendationServiceTest.java` | Unit | ✅ `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendation*"` passed before PR #5 changes. | ✅ New aggregate mismatch test failed because `DIRECTION_MISMATCH` did not exist. | ✅ Focused recommendation tests passed after implementation. | ✅ Covers `DEBIT + INFLOW` and `CREDIT + OUTFLOW` counts in one non-trivial signal. | ✅ Assertions verify aggregate metrics only and no sensitive movement/category details in signal output. |
| 5.2 | `src/test/java/com/kuroneko/pymeflow/application/recommendation/HistoryRecommendationServiceTest.java` | Unit | ✅ Same focused baseline passed before PR #5 changes. | ✅ Aligned-direction test failed while healthy fallback was displaced during RED/GREEN iteration. | ✅ `DEBIT + OUTFLOW` and `CREDIT + INFLOW` aligned movements produce no mismatch signal and return `HEALTHY_HISTORY`. | ✅ Companion non-empty mismatch test plus aligned empty-result test satisfy empty collection rule. | ✅ Test profile now includes distinct inflow and outflow categories without changing production fixtures. |
| 5.3–5.4 | `src/main/java/com/kuroneko/pymeflow/application/recommendation/HistoryRecommendationService.java` | Application unit | ✅ Focused recommendation baseline passed before production edits. | ✅ PR #5 tests demanded direction/category comparison and aggregate metrics before production edits. | ✅ `generate()` loads the profile, compares projectable movement direction with category direction, and emits `DIRECTION_MISMATCH` as `INFO`. | ✅ Counts both mismatch forms: debit inflow and credit outflow; aligned movements and unknown/non-mismatch directions do not emit. | ✅ Signal is added before healthy fallback and exposes only counts. |
| 5.5 | Gradle suite | Verification | N/A | N/A | ✅ `docker compose up -d postgres`, `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendation*"`, and `./gradlew.bat test --rerun-tasks` passed. | N/A | ✅ Scope stayed inside PR #5; no PR #6 smoke/docs cleanup touched. |
| 6.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowDirectionSmokeTest.java` | Smoke / Spring Boot integration | ✅ `docker compose up -d postgres`; related controller/history baseline passed before PR #6 changes. | ✅ Smoke test written first for bank DEBIT/CREDIT persistence and history retrieval. Existing implementation already satisfied the new smoke assertion. | ✅ `./gradlew.bat test --rerun-tasks --tests "*CashflowDirectionSmokeTest*"` passed. | ✅ Two bank paths: negative `-15000 → DEBIT/15000` and positive `15000 → CREDIT/15000`, both verified through history. | ✅ No production behavior needed; smoke test uses direct controllers and persisted PostgreSQL state with unique references. |
| 6.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowDirectionSmokeTest.java` | Smoke / Spring Boot integration | ✅ Same related baseline passed before PR #6 changes. | ✅ Smoke test written first for manual default `CREDIT`, explicit `DEBIT`, and fingerprint replay with changed direction. Existing implementation already satisfied the new smoke assertion. | ✅ Focused smoke test passed. | ✅ Three manual cases: omitted direction, explicit `DEBIT`, and replay with same fingerprint but changed direction returning the existing `CREDIT` movement. | ✅ No production behavior needed; assertions verify movement id and returned direction. |
| 6.3 | OpenSpec baseline specs + `CashflowBankStatementSimulatedControllerTest.java` | Docs / contract | ✅ Direction-loss grep in `openspec/specs` found stale baseline requirement before cleanup. | ✅ Full suite failed after OpenAPI wording changed because the old test still expected direction-loss copy. | ✅ Updated controller contract test passed with direction-preservation wording. | ✅ Baseline grep now finds no direction-loss requirement/copy in `openspec/specs`. | ✅ Removed stale Javadoc/OpenAPI direction-loss wording so public docs align with preserved direction behavior. |
| 6.4 | `openspec/specs/**/spec.md` | Docs / baseline | N/A (docs-only baseline merge) | ✅ Baseline docs updated after delta specs defined the accepted requirements. | ✅ Full test suite passed after docs update. | ✅ Updated bank-statement, manual import, idempotency, history recommendations, and added `cashflow-direction-preservation` baseline. | ✅ Kept wording concise and requirement-oriented for archive readiness. |
| 6.5 | Gradle suite | Verification | N/A | N/A | ✅ `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` and full `./gradlew.bat test --rerun-tasks` passed. | N/A | ✅ `ArchitectureTest` continues to enforce domain/application boundaries. |

## Scope Cleanup

The PR #1 audit found a scope leak in `CashflowIngestionService` and `CashflowIngestionServiceTest`: direction propagation through ingestion belongs to PR #3 (`Adapter sign→direction + ingestion propagation`). That leak was removed before PR #2.

PR #2 stayed inside the persistence boundary: Flyway V4 adds storage, and `CashflowMovementHistoryJdbcAdapter` reads/writes `movement_direction`. Adapter sign mapping, ingestion propagation, API DTOs, recommendation signals, and smoke/docs cleanup remained pending before PR #3.

PR #3 stayed inside the ingestion/fingerprint boundary: simulated bank adapter sign normalization, ingestion draft direction propagation, duplicate-result direction preservation, and `fp:v1` direction-exclusion documentation/tests. API DTO exposure, recommendation signals, smoke tests, and docs cleanup remain pending.

PR #4 stayed inside the API contract boundary: controller request/response DTOs now expose or accept additive `movementDirection` where required. Recommendation signals, smoke tests, OpenSpec baseline/docs cleanup, and direction-loss wording outside touched API contracts remain pending for PR #5/PR #6.

PR #5 stayed inside the mismatch recommendation boundary: `HistoryRecommendationService` now emits an informational aggregate-only `DIRECTION_MISMATCH` signal for projectable movement/category direction conflicts. Smoke tests, OpenSpec baseline/docs cleanup, and direction-loss wording remain pending for PR #6.

PR #6 stayed inside the smoke/docs boundary: added end-to-end direction smoke coverage, removed stale direction-loss documentation from active baseline specs/OpenAPI wording, and updated baseline OpenSpec requirements for archive readiness. No new domain/application/persistence behavior was introduced in PR #6.

## Verification Evidence

| Command | Result |
|---------|--------|
| `docker compose up -d postgres` | PASS — container `pymeflow-postgres` running |
| `./gradlew.bat test --rerun-tasks --tests "*CashflowMovementHistoryJdbcAdapter*" --tests "*Flyway*"` before PR #2 changes | PASS baseline |
| `./gradlew.bat test --rerun-tasks --tests "*CashflowMovementHistoryJdbcAdapter*" --tests "*Flyway*"` after PR #2 RED tests | FAIL as expected: missing `V4__add_movement_direction.sql` / missing `movement_direction` column |
| `./gradlew.bat test --rerun-tasks --tests "*CashflowMovementHistoryJdbcAdapter*" --tests "*Flyway*"` after PR #2 implementation | PASS |
| `./gradlew.bat test --rerun-tasks` after PR #2 | PASS |
| `./gradlew.bat test --rerun-tasks --tests "*SimulatedBankStatement*" --tests "*TransactionFingerprint*" --tests "*CashflowIngestionService*"` before PR #3 changes | PASS baseline |
| `./gradlew.bat test --rerun-tasks --tests "*SimulatedBankStatement*" --tests "*TransactionFingerprint*" --tests "*CashflowIngestionService*"` after PR #3 RED tests | FAIL as expected: adapter negative amount still `CREDIT`; ingestion drafts still defaulted to `CREDIT` |
| `./gradlew.bat test --rerun-tasks --tests "*SimulatedBankStatement*" --tests "*TransactionFingerprint*" --tests "*CashflowIngestionService*"` after PR #3 implementation/refactor | PASS |
| `docker compose up -d postgres` before full PR #3 suite | PASS — container `pymeflow-postgres` running |
| `./gradlew.bat test --rerun-tasks` after PR #3 | PASS |
| `./gradlew.bat test --rerun-tasks --tests "*Controller*" --tests "*Composition*"` before PR #4 changes | PASS baseline |
| `./gradlew.bat test --rerun-tasks --tests "*CashflowManualImportControllerTest" --tests "*CashflowBankStatementSimulatedControllerTest" --tests "*CashflowHistoryControllerTest" --tests "*CashflowProjectionControllerTest" --tests "*ManualReviewResolutionControllerTest"` after PR #4 RED tests | FAIL as expected: missing `movementDirection` API fields/defaulting/mapping |
| `./gradlew.bat test --rerun-tasks --tests "*CashflowManualImportControllerTest" --tests "*CashflowBankStatementSimulatedControllerTest" --tests "*CashflowHistoryControllerTest" --tests "*CashflowProjectionControllerTest" --tests "*ManualReviewResolutionControllerTest"` after PR #4 implementation | PASS |
| `docker compose up -d postgres` before PR #4 verification | PASS — container `pymeflow-postgres` running |
| `./gradlew.bat test --rerun-tasks --tests "*Controller*" --tests "*Composition*"` after PR #4 | PASS |
| `./gradlew.bat test --rerun-tasks` after PR #4 | PASS |
| `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendation*"` before PR #5 changes | PASS baseline |
| `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendation*"` after PR #5 RED tests | FAIL as expected: `DIRECTION_MISMATCH` signal was absent and aligned healthy fallback was not yet implemented for the new behavior |
| `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendation*"` after PR #5 implementation | PASS |
| `docker compose up -d postgres` before PR #5 full verification | PASS — container `pymeflow-postgres` running |
| `./gradlew.bat test --rerun-tasks` after PR #5 | PASS |
| `docker compose up -d postgres` before PR #6 smoke tests | PASS — container `pymeflow-postgres` running |
| `./gradlew.bat test --rerun-tasks --tests "*CashflowDirectionSmokeTest*"` after PR #6 smoke tests | PASS |
| `./gradlew.bat test --rerun-tasks --tests "*CashflowBankStatementSimulatedControllerTest*" --tests "*CashflowDirectionSmokeTest*"` after OpenAPI docs cleanup | PASS |
| `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` after PR #6 | PASS |
| `./gradlew.bat test --rerun-tasks` after PR #6 | PASS |

## Remaining Tasks

None — PR #1 through PR #6 are complete. Ready for verify/archive.

## Post-Verify Warning Resolution

| Warning | Resolution | Evidence |
|---------|------------|----------|
| PR #1 Strict TDD historical RED logs unavailable after cancelled sub-agent context | Documented as an explicit limitation instead of inventing chronology. This is now a known historical-evidence limitation, not a hidden implementation issue. | `apply-progress.md` TDD evidence and this warning-resolution table state that full PR #1 RED logs cannot be retroactively recovered. |
| Several simple record/value-object files below 80% per-file coverage | Added focused value-object validation and compatibility-constructor tests where useful for direction-hardening records. Did not add production code or noise-only tests. | `TransactionTest`, `CashflowMovementDraftTest`, `CashflowMovementRecordTest`, and new `ProjectionMovementValueObjectsTest`; JaCoCo now reports 100% line coverage for the six warned files. |
| `CashflowIngestionController` appeared as a direction DTO target in `design.md`, but specs/tasks intentionally excluded it | Corrected the design to align with specs/tasks. The legacy `/api/cashflow/ingestions` endpoint remains compatibility-only and defaults omitted direction to `CREDIT`; no new API behavior was freelanced. | `design.md` removes `CashflowIngestionController` from the direction DTO target list and adds a verification clarification. |

### Post-Verify TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| Warning 2 coverage hardening | `TransactionTest`, `CashflowMovementDraftTest`, `CashflowMovementRecordTest`, `ProjectionMovementValueObjectsTest` | Unit | ✅ Focused baseline record/value-object tests passed before test additions. | ➖ Existing validation behavior already existed; tests were written before any production change and no production change was needed. | ✅ Focused tests, architecture tests, full suite, and JaCoCo passed. | ✅ Covers explicit/default direction, required fields, optional-field normalization, status/category invariants, projection conversion, record-to-pending mapping. | ✅ Test-only cleanup; production value objects left unchanged. |
| Warning 3 design alignment | `design.md` | Documentation | ✅ Specs/tasks/design/verify report reviewed before edit. | ➖ Docs-only scope correction; no runtime behavior required by specs. | ✅ Full suite and JaCoCo passed after design clarification. | ✅ Clarification compares design table against specs/tasks scope and preserves legacy endpoint behavior. | ✅ Removed misleading target instead of adding out-of-scope API behavior. |

### Post-Verify Coverage Evidence

| File | Line coverage | Branch coverage | Result |
|------|---------------|-----------------|--------|
| `Transaction.java` | 100% | 100% | Warning removed |
| `CashflowMovementDraft.java` | 100% | 92.9% | Warning removed |
| `CashflowMovementRecord.java` | 100% | 90.0% | Warning removed |
| `ProjectionReadyCashflowTransaction.java` | 100% | 88.9% | Warning removed |
| `ProjectedCashflowTransaction.java` | 100% | 85.7% | Warning removed |
| `PendingManualReviewMovement.java` | 100% | 92.9% | Warning removed |

### Post-Verify Commands

| Command | Result |
|---------|--------|
| `./gradlew.bat test --rerun-tasks --tests "*TransactionTest" --tests "*CashflowMovementDraftTest" --tests "*CashflowMovementRecordTest" --tests "*CashflowMovementHistoryServiceTest" --tests "*CashflowProjectionServiceTest"` | PASS — baseline before warning cleanup |
| `./gradlew.bat test --rerun-tasks --tests "*TransactionTest" --tests "*CashflowMovementDraftTest" --tests "*CashflowMovementRecordTest" --tests "*ProjectionMovementValueObjectsTest"` | PASS |
| `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` | PASS |
| `./gradlew.bat test --rerun-tasks` | PASS |
| `./gradlew.bat jacocoTestReport` | PASS |

## Risks

- Historical Strict TDD RED logs for the cancelled PR #1 sub-agent work are unavailable. Current runtime verification is green, and the limitation is explicitly documented; chronology cannot be fully proven for pre-existing PR #1 work without inventing evidence.
- PR #2 migration is additive and safe; rollback would leave a harmless column unless a follow-up rollback migration is chosen.
- PR #3 intentionally preserves `fp:v1` idempotency by excluding direction; re-importing the same fingerprint with a different direction returns the existing movement rather than updating stored direction.
- PR #6 smoke tests use the local PostgreSQL dependency and unique references/descriptions to avoid collisions with existing persisted test data.
