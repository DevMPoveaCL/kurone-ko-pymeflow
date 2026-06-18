# Apply Progress: PymeFlow Fingerprint Fallback

## Status

Strict TDD apply completed for all planned tasks. Verification remediation added for the missing history scenario, and the full suite passed with `./gradlew.bat test --rerun-tasks`. Post-verification artifact cleanup corrected the invalid one-character profile fixture to `profileId="p1"` and removed stale H2 partial-index wording from the design artifact.

## Completed Tasks

- [x] 1.1 `TransactionFingerprintTest` determinism test added.
- [x] 1.2 Fingerprint normalization and differentiation tests added.
- [x] 2.1 No-reference dedup service test added.
- [x] 2.2 Materially different no-reference fields service test added.
- [x] 2.3 Cross-profile no-reference scoping service test added.
- [x] 2.4 Identical cash MVP limitation service test added.
- [x] 2.5 Blank `externalReference` test updated to expect fallback lookup and `fp:v1:` source reference.
- [x] 3.1 `TransactionFingerprint` application-layer helper implemented.
- [x] 3.2 `CashflowIngestionService` now resolves effective source references before lookup and persistence.
- [x] 4.1 H2 index alignment verified; PostgreSQL-style partial index syntax is unsupported by H2 in this project, so the existing unfiltered H2 unique index was retained because it allows multiple NULLs and enforces duplicate non-null references.
- [x] 4.2 Fingerprint duplicate persistence test added for the `DuplicateKeyException` catch path.
- [x] 5.1 `CashflowIngestionController` OpenAPI operation description updated in Spanish.
- [x] 5.2 Full Gradle test suite executed successfully.
- [x] 6.1 Verification remediation: fallback-generated `fp:v1:` source reference is now covered through ingestion plus history query runtime test.
- [x] 6.2 Design arithmetic corrected: `fp:v1:` + SHA-256 hex is 70 characters, and the H2 index deviation is documented in design.
- [x] 7.1 Artifact cleanup: spec/task examples now use valid `ProfileId("p1")`, and design file-change/testing notes match the retained H2-compatible index behavior.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/TransactionFingerprintTest.java` | Unit | ✅ Baseline relevant tests passed before edits | ✅ Compile failed because `TransactionFingerprint` did not exist | ✅ Application tests passed | ✅ Same-input determinism plus exact hash assertion | ✅ Helper kept stateless and package-private |
| 1.2 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/TransactionFingerprintTest.java` | Unit | ✅ Baseline relevant tests passed before edits | ✅ Compile failed before helper existed | ✅ Application tests passed | ✅ Whitespace/null description, BigDecimal scale, profile/date/description differences | ✅ Pure normalization isolated in helper |
| 2.1 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/CashflowIngestionServiceTest.java` | Unit | ✅ `CashflowIngestionServiceTest` baseline passed | ✅ Test expected fallback lookup and no second save before service supported it | ✅ Application tests passed | ✅ Re-ingestion returns existing movement ID and no draft save | ✅ Source reference resolution centralized before lookup |
| 2.2 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/CashflowIngestionServiceTest.java` | Unit | ✅ `CashflowIngestionServiceTest` baseline passed | ✅ Different amount expected distinct generated references before implementation | ✅ Application tests passed | ✅ Two drafts with distinct `fp:v1:` values | ✅ Existing recording port reused |
| 2.3 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/CashflowIngestionServiceTest.java` | Unit | ✅ `CashflowIngestionServiceTest` baseline passed | ✅ Cross-profile no-reference behavior expected distinct references before implementation | ✅ Application tests passed | ✅ Same transaction under two profiles yields two drafts | ✅ No infrastructure changes needed |
| 2.4 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/CashflowIngestionServiceTest.java` | Unit | ✅ `CashflowIngestionServiceTest` baseline passed | ✅ Identical no-reference retry expected dedup before implementation | ✅ Application tests passed | ✅ Existing-list replay simulates persisted first movement | ✅ Added `recordFromDraft` test helper |
| 2.5 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/CashflowIngestionServiceTest.java` | Unit | ✅ Existing blank-reference test passed with old behavior | ✅ Updated assertion failed against old null/no-lookup behavior | ✅ Application tests passed | ✅ Lookup count and `fp:v1:` draft assertion | ✅ Test name updated to new behavior |
| 3.1 | `src/main/java/com/kuroneko/pymeflow/application/cashflow/TransactionFingerprint.java` | Unit-driven production | N/A (new file) | ✅ Fingerprint tests written first | ✅ Application tests passed | ✅ Multiple normalization/differentiation cases | ✅ Pure stateless helper; no external dependencies |
| 3.2 | `src/main/java/com/kuroneko/pymeflow/application/cashflow/CashflowIngestionService.java` | Unit-driven production | ✅ Existing service tests baseline passed | ✅ Service tests written/updated first | ✅ Application tests passed | ✅ Explicit, omitted, blank, different amount, cross-profile, retry paths covered | ✅ Effective reference variable reduces branching |
| 4.1 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/CashflowMovementHistoryJdbcAdapterTest.java` | Integration | ✅ Adapter baseline passed | ✅ Attempted partial-index alignment failed setup with `BadSqlGrammarException` | ✅ Adapter tests passed after retaining H2-compatible unique index | ✅ Existing nullable-source tests plus duplicate non-null tests cover behavior | ➖ Retained H2-compatible setup and documented divergence |
| 4.2 | `src/test/java/com/kuroneko/pymeflow/infrastructure/persistence/CashflowMovementHistoryJdbcAdapterTest.java` | Integration | ✅ Adapter baseline passed | ✅ Fingerprint duplicate catch-path test added | ✅ Adapter tests passed | ✅ Existing explicit duplicate test plus new `fp:v1:` duplicate test | ✅ No production adapter change needed |
| 5.1 | `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowIngestionController.java` | Documentation/API metadata | ✅ Full changed-target test set passed before final suite | ➖ Structural doc-only change | ✅ Full suite passed | ➖ Single OpenAPI description update | ✅ Spanish text kept neutral and Chile-market suitable |
| 5.2 | Full suite | Verification | ✅ Targeted suites passed | N/A | ✅ `./gradlew.bat test --rerun-tasks` passed | N/A | N/A |
| 6.1 | `src/test/java/com/kuroneko/pymeflow/application/cashflow/CashflowMovementHistoryServiceTest.java` | Application integration-style unit | ✅ 6/6 history service tests passed before edits | ✅ Verification RED gap identified: no runtime history test existed for fallback-created movement | ✅ Updated history service test passed | ✅ Test creates no-reference movement via ingestion service, then queries history and asserts exact `fp:v1:` source reference | ✅ Reused in-memory port and kept assertions behavior-visible |
| 6.2 | `openspec/changes/pymeflow-fingerprint-fallback/design.md` | Documentation correction | ✅ Verification warning identified off-by-one design note | ➖ Doc-only correction | ✅ Full suite passed after doc correction | ➖ Single arithmetic/index-deviation correction | ✅ Corrected without changing implementation |
| 7.1 | OpenSpec artifacts | Documentation/artifact cleanup | ✅ Latest verify passed with non-blocking artifact warnings | ➖ Doc-only cleanup requested after verification | ➖ No tests run; no code changed | ✅ Invalid fixture and stale design wording corrected in artifacts | ✅ Kept cleanup limited to OpenSpec documentation |

## Test Summary

- **Total tests written/updated**: 10 new tests, 1 updated behavior test.
- **Total tests passing**: Full Gradle suite passed, including `CashflowMovementHistoryServiceTest` with the new fallback-history scenario.
- **Layers used**: Unit and JDBC integration.
- **Approval tests**: Existing explicit-reference and sensitive-reference tests preserved.
- **Pure functions created**: 1 (`TransactionFingerprint.compute`).

## Deviations / Issues

- The spec example now uses `ProfileId("p1")` because this codebase rejects one-character profile IDs such as `"1"`.
- Java `Currency` canonicalizes ISO currency codes; lowercase request currency is rejected at the HTTP layer by existing validation. Fingerprint tests assert the application helper emits canonical uppercase `CLP` via `Currency`.
- H2 rejected `CREATE UNIQUE INDEX ... WHERE source_reference IS NOT NULL`; retained the unfiltered H2 unique index because it still allows multiple NULL source references and enforces duplicate non-null references, matching the behavior the adapter tests need.
- Verification found that no passing runtime test covered fallback-generated references being returned from history. Added `returnsFallbackGeneratedSourceReferenceWhenHistoryIsQueried`, which ingests a no-reference movement, queries manual-review history, and asserts the exact generated `fp:v1:` reference.
- Design previously stated generated reference length as 69. Corrected to 70 because `fp:v1:` is 6 characters plus 64 SHA-256 hex characters.

## Verification Commands

- `./gradlew.bat test --tests "com.kuroneko.pymeflow.application.cashflow.CashflowIngestionServiceTest" --tests "com.kuroneko.pymeflow.infrastructure.persistence.CashflowMovementHistoryJdbcAdapterTest" --rerun-tasks` — baseline passed before edits.
- `./gradlew.bat test --tests "com.kuroneko.pymeflow.application.cashflow.TransactionFingerprintTest" --tests "com.kuroneko.pymeflow.application.cashflow.CashflowIngestionServiceTest" --rerun-tasks` — RED compile failure before `TransactionFingerprint` existed, then GREEN passed after implementation.
- `./gradlew.bat test --tests "com.kuroneko.pymeflow.infrastructure.persistence.CashflowMovementHistoryJdbcAdapterTest" --rerun-tasks` — failed with H2 partial-index syntax, then passed after retaining H2-compatible index.
- `./gradlew.bat test --rerun-tasks` — passed.
- `./gradlew.bat test --tests "com.kuroneko.pymeflow.application.cashflow.CashflowMovementHistoryServiceTest" --rerun-tasks` — baseline passed before remediation edits; passed again after adding fallback-history coverage.
- `./gradlew.bat test --rerun-tasks` — passed after verification remediation.
- No tests were run for the post-verification artifact cleanup because only OpenSpec documentation artifacts were modified.

## Workload / PR Boundary

- Mode: single focused PR under 400-line budget, despite force-chained preflight policy.
- Boundary: fingerprint helper, service effective-reference flow, focused unit/integration tests, and OpenAPI description.
- Estimated review budget impact: small/medium; implementation remains under the configured review budget.
