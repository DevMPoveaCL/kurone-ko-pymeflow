## Verification Report

**Change**: `pymeflow-fingerprint-fallback`
**Version**: N/A
**Mode**: Strict TDD
**Artifact store**: OpenSpec
**Verification pass**: Final post-cleanup verification

### Final Verdict

**PASS**

The previous CRITICAL history-consistency gap remains covered by a passing runtime test. The prior non-blocking artifact warnings were resolved by post-verification OpenSpec cleanup, and the full Gradle test suite plus JaCoCo report pass after that cleanup.

### Completeness

| Metric | Value |
|--------|-------|
| Proposal read | ✅ `openspec/changes/pymeflow-fingerprint-fallback/proposal.md` |
| Spec read | ✅ `openspec/changes/pymeflow-fingerprint-fallback/specs/cashflow-ingestion-idempotency/spec.md` |
| Design read | ✅ `openspec/changes/pymeflow-fingerprint-fallback/design.md` |
| Tasks read | ✅ `openspec/changes/pymeflow-fingerprint-fallback/tasks.md` |
| Apply progress read | ✅ `openspec/changes/pymeflow-fingerprint-fallback/apply-progress.md` |
| Prior verify report read | ✅ `openspec/changes/pymeflow-fingerprint-fallback/verify-report.md` |
| Tasks total | 15 |
| Tasks complete | 15 |
| Tasks incomplete | 0 |
| Source inspected | ✅ implementation and relevant tests inspected |

### Build & Tests Execution

| Command | Result | Evidence |
|---------|--------|----------|
| `./gradlew.bat test --rerun-tasks` | ✅ PASS | `BUILD SUCCESSFUL in 34s`; JUnit XML: 105 tests, 0 failures, 0 errors, 0 skipped |
| `./gradlew.bat jacocoTestReport` | ✅ PASS | `BUILD SUCCESSFUL in 4s`; XML/HTML JaCoCo report generated |

Relevant passing test result files:

| Test class | Tests | Failures | Errors | Skipped |
|------------|-------|----------|--------|---------|
| `TransactionFingerprintTest` | 3 | 0 | 0 | 0 |
| `CashflowIngestionServiceTest` | 13 | 0 | 0 | 0 |
| `CashflowMovementHistoryServiceTest` | 7 | 0 | 0 | 0 |
| `CashflowMovementHistoryJdbcAdapterTest` | 8 | 0 | 0 | 0 |
| `CashflowIngestionControllerTest` | 6 | 0 | 0 | 0 |
| `CashflowHistoryControllerTest` | 4 | 0 | 0 | 0 |

### Spec Compliance Matrix

| Requirement | Scenario | Passing runtime test evidence | Result |
|-------------|----------|-------------------------------|--------|
| Explicit Reference Behavior Preserved | Explicit reference stores normally | `CashflowIngestionServiceTest.duplicateExternalReferenceReturnsExistingMovementWithoutInsert`; `sameExternalReferenceForDifferentProfileInsertsNewMovement` | ✅ COMPLIANT |
| Explicit Reference Behavior Preserved | Sensitive reference rejected without echo | `CashflowIngestionServiceTest.sensitiveExternalReferenceIsRejectedBeforeLookupAndPersistence`; `CashflowIngestionControllerTest.returnsSensitiveExternalReferenceRejectionWithoutEchoingSensitiveText` | ✅ COMPLIANT |
| Blank Reference Normalization | Whitespace triggers fallback | `CashflowIngestionServiceTest.blankExternalReferenceIsTreatedAsOmittedWithFingerprintLookup` | ✅ COMPLIANT |
| Deterministic Fingerprint Fallback | Hash from normalized fields | `TransactionFingerprintTest.computesDeterministicSha256FingerprintFromNormalizedFields`; `normalizesWhitespaceAndNullDescriptionsToStableFingerprintInput`; `differentiatesMaterialFingerprintFieldsWithoutCollapsingBigDecimalScale` | ✅ COMPLIANT |
| No-Reference Idempotency | Re-ingestion returns existing row | `CashflowIngestionServiceTest.repeatedNoReferenceTransactionReturnsExistingMovementWithoutInsert` | ✅ COMPLIANT |
| No-Reference Idempotency | Materially different fields create new row | `CashflowIngestionServiceTest.materiallyDifferentNoReferenceFieldsCreateNewMovements` | ✅ COMPLIANT |
| No-Reference Idempotency | Identical cash transactions dedupe (MVP accepted) | `CashflowIngestionServiceTest.identicalNoReferenceCashTransactionsDeduplicateAsMvpLimitation` | ✅ COMPLIANT |
| Profile-Scoped Dedup | Cross-profile same fields are distinct | `CashflowIngestionServiceTest.sameNoReferenceFingerprintIsScopedByProfile` | ✅ COMPLIANT |
| History Consistency | Fallback reference visible in history | `CashflowMovementHistoryServiceTest.returnsFallbackGeneratedSourceReferenceWhenHistoryIsQueried` | ✅ COMPLIANT |

**Compliance summary**: 9/9 scenarios compliant.

### Prior Issue Re-check

| Prior issue | Severity | Current Status | Evidence |
|-------------|----------|----------------|----------|
| Fallback-generated `fp:v1:` reference visible in history was untested | CRITICAL | ✅ RESOLVED | `CashflowMovementHistoryServiceTest.returnsFallbackGeneratedSourceReferenceWhenHistoryIsQueried` ingests through `CashflowIngestionService`, queries `CashflowMovementHistoryService.pendingManualReviews`, and asserts the exact generated `fp:v1:` reference. Passed in full suite. |
| Spec fixture used invalid one-character profile ID | WARNING | ✅ RESOLVED | Spec/task artifacts now use valid `profileId="p1"` / `ProfileId("p1")` examples. |
| Design had stale H2 partial-index wording in test fixture notes | WARNING | ✅ RESOLVED | `design.md` consistently documents retaining the H2-compatible unfiltered unique index for tests while production keeps the V3 partial unique index. |

### Correctness (Static Evidence)

| Requirement | Status | Notes |
|-------------|--------|-------|
| Generated reference format | ✅ Implemented | `TransactionFingerprint` returns `fp:v1:` + lowercase SHA-256 hex; length is 70. |
| Explicit reference precedence | ✅ Implemented | `CashflowIngestionService` uses non-blank explicit reference before fallback. |
| Null/blank external reference handling | ✅ Implemented | `IngestionItem` normalizes blank references to `null`; service then computes fallback. |
| Profile-scoped deduplication | ✅ Implemented | Profile ID is included in hash input and lookup remains `findBySourceReference(profileId, sourceReference)`. |
| Duplicate persistence race protection | ✅ Implemented | Existing unique-index catch path returns existing rows; fingerprint duplicate path has JDBC coverage. |
| Sensitive explicit reference behavior | ✅ Preserved | Sensitive explicit references are rejected before lookup/persistence and not echoed by web response. |

### Coherence (Design)

| Decision | Followed? | Notes |
|----------|-----------|-------|
| Application-layer helper | ✅ Yes | `TransactionFingerprint` is package-private, stateless, pure application code. |
| Reuse `source_reference` | ✅ Yes | No schema change; effective reference flows through existing lookup and draft persistence. |
| SHA-256 lowercase hex | ✅ Yes | Uses `MessageDigest` and `HexFormat`. |
| Normalization contract | ✅ Yes | Uses profile ID, `BigDecimal.toPlainString()`, uppercase ISO currency, ISO date, and trimmed/collapsed description. |
| H2 test index divergence documented | ✅ Yes | Design decision table, file-change table, testing strategy, tasks, and apply progress document retaining the H2-compatible unfiltered unique index. |
| OpenAPI limitation note | ✅ Yes | Controller documents fallback and identical no-reference transaction MVP limitation. |

### TDD Compliance

| Check | Result | Details |
|-------|--------|---------|
| TDD Evidence reported | ✅ | `apply-progress.md` includes a TDD Cycle Evidence table for all 15 tasks. |
| All tasks have tests / verification evidence | ✅ | 15/15 tasks have test, command, or doc-only verification evidence. |
| RED confirmed | ✅ | Referenced test files exist; historical RED failures are recorded in apply progress and cannot be re-executed without reverting implementation. |
| GREEN confirmed | ✅ | Referenced test classes pass under `./gradlew.bat test --rerun-tasks`. |
| Triangulation adequate | ✅ | Fingerprint, fallback dedup, field differentiation, cross-profile scoping, duplicate persistence, and history visibility are covered by varied assertions. |
| Safety Net for modified files | ✅ | Apply progress reports baseline target suites before edits; current full suite passes. |

**TDD Compliance**: 6/6 checks passed for verifiable current-state evidence.

### Test Layer Distribution

| Layer | Tests | Files | Tools |
|-------|-------|-------|-------|
| Unit / application service | 23 | 3 | JUnit 5 + AssertJ |
| JDBC integration | 8 | 1 | Spring `@JdbcTest`, H2 PostgreSQL mode |
| Web MVC | 10 | 2 | Spring `@WebMvcTest`, MockMvc |
| E2E | 0 | 0 | Not present |
| **Total relevant** | **41** | **6** | |

### Changed File Coverage

| File | Line % | Branch % | Uncovered Lines | Rating |
|------|--------|----------|-----------------|--------|
| `src/main/java/com/kuroneko/pymeflow/application/cashflow/CashflowIngestionService.java` | 88.0% | 75.8% | 101, 107, 113, 136, 137, 141, 144, 151, 152, 156, 159, 166, 167, 171, 174 | ⚠️ Acceptable |
| `src/main/java/com/kuroneko/pymeflow/application/cashflow/TransactionFingerprint.java` | 89.5% | 100.0% | 57, 58 | ⚠️ Acceptable |
| `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowIngestionController.java` | 100.0% | n/a | — | ✅ Excellent |

**Average changed production file line coverage**: 92.5%.

### Assertion Quality

**Assertion quality**: ✅ All inspected assertions verify real behavior.

Inspected files:
- `TransactionFingerprintTest.java`
- `CashflowIngestionServiceTest.java`
- `CashflowMovementHistoryServiceTest.java`
- `CashflowMovementHistoryJdbcAdapterTest.java`
- `CashflowIngestionControllerTest.java`
- `CashflowHistoryControllerTest.java`

No tautologies, ghost loops, orphan smoke tests, or type-only assertions used as sole proof were found in the change-relevant tests.

### Quality Metrics

| Check | Result | Notes |
|-------|--------|-------|
| Java compile / type check | ✅ | Executed as part of `test` and `jacocoTestReport`. |
| Linter | ➖ | No separate linter configured in `build.gradle`. |
| Architecture boundaries | ✅ | Helper remains in application layer; no domain, port, or schema changes. |

### Issues Found

**CRITICAL**: None.

**WARNING**: None.

**SUGGESTION**: None.

### Verdict

PASS — all spec scenarios are covered by passing runtime tests, prior critical and warning items are resolved, and final post-cleanup verification passed.
