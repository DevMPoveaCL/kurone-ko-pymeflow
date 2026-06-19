# Tasks: PymeFlow History Recommendations MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~568 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1 (66) → PR2 (~312) → PR3 (~190) |
| Delivery strategy | force-chained |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Port + adapter + adapter tests | PR 1 | Base: `recommendations/history-mvp` |
| 2 | Service + service tests | PR 2 | Base: PR 1 branch; depends on PR 1 |
| 3 | Controller + controller tests + config | PR 3 | Base: PR 2 branch; depends on PR 2 |

## Phase 1: PR1 — History Status Query Support (~66 lines)

- [x] 1.1 **RED** — Add `findByStatus` tests to `CashflowMovementHistoryJdbcAdapterTest`: returns filtered by status, returns empty for unmatched, REJECTED returns `rejectionReasonCode` with null `safeDescription`.
  - Verify: `./gradlew.bat test --rerun-tasks --tests "*CashflowMovementHistoryJdbcAdapterTest"` (RED — method missing).
- [x] 1.2 **GREEN** — Add `findByStatus(ProfileId, CashflowMovementStatus)` to `CashflowMovementHistoryPort`.
- [x] 1.3 **GREEN** — Implement `findByStatus` in `CashflowMovementHistoryJdbcAdapter` reusing `SELECT_COLUMNS + " where profile_id = ? and status = ? order by movement_date, created_at"`.
  - Acceptance: tests pass; covered by `idx_cashflow_movement_history_profile_status_date`; deterministic ordering.
  - Verify: `./gradlew.bat test --rerun-tasks --tests "*CashflowMovementHistoryJdbcAdapterTest"` (GREEN).

## Phase 2: PR2 — Recommendation Service (~312 lines)

- [x] 2.1 **RED** — Write `HistoryRecommendationServiceTest` covering: `MANUAL_REVIEW_BACKLOG` WARNING (>=5) / INFO (1-4) / absent (0); `HIGH_REJECTION_RATE` WARNING (>=30% of total persisted history) / absent (<30%); `CATEGORY_CONCENTRATION` INFO (>=60% amount); `INSUFFICIENT_DATA` INFO (<10 projectable); `RECENT_INACTIVITY` WARNING (no movements ≤30d); `HEALTHY_HISTORY` fallback; WARNING-before-INFO ordering; REJECTED metrics aggregate-only (no `safeDescription`/`sourceReference` exposed); profile-not-found exception.
  - Verify: `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationServiceTest"` (RED — class missing).
- [x] 2.2 **GREEN** — Create `HistoryRecommendationService` with `generate(ProfileId)` loading 3 statuses, computing 6 hardcoded-threshold signals, deterministic ordering, `HEALTHY_HISTORY` fallback.
- [x] 2.3 **GREEN** — Define `HistoryRecommendationResponse` and `HistorySignalResponse` records in same file. REJECTED metrics: `rejectedCount`, `projectableCount`, `rejectionRatePercent`, `topRejectionReasonCode` only.
  - Acceptance: all tests pass; no rejected descriptions/source-refs in response; `generatedAt` uses `Instant.now()`.
  - Verify: `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationServiceTest"` (GREEN).

## Phase 3: PR3 — REST Endpoint (~190 lines)

- [x] 3.1 **RED** — Write `HistoryRecommendationControllerTest` (`@WebMvcTest`, `@MockBean` service): valid profile returns 200 with JSON `profileId`/`generatedAt`/`signals[].type`/`severity`/`title`; blank profileId returns 400 + `VALIDATION_ERROR` + Spanish message; profile-not-found returns 400 + `"El perfil solicitado no está configurado."`; response contains NO `safeDescription`/`sourceReference` in any signal field.
  - Verify: `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationControllerTest"` (RED — endpoint missing).
- [x] 3.2 **GREEN** — Create `HistoryRecommendationController` at `GET /api/cashflow/recommendations?profileId={id}` with inner DTO records, non-blank validation, service delegation, neutral Spanish `title`/`description`/`actionHint`.
- [x] 3.3 **GREEN** — Wire `HistoryRecommendationService` bean in `ApplicationServiceConfiguration`.
  - Acceptance: controller tests pass; JSON shape matches contract; deterministic ordering; no rejected-sensitive data.
  - Verify: `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationControllerTest"` (GREEN).

## Phase 4: Full Verification

- [x] 4.1 Run full suite: `./gradlew.bat test --rerun-tasks`.
- [x] 4.2 Confirm ArchUnit hexagonal-boundary rules pass.
- [x] 4.3 Confirm JaCoCo coverage does not regress: `./gradlew.bat jacocoTestReport`.

## Phase 5: Verification Failure Fixes

- [x] 5.1 **RED** — Add focused service boundary tests for exactly 5 manual-review rows, exactly 30% rejected rows using total persisted history, and exactly 60% category concentration.
  - Verify: `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationServiceTest"` (RED — 3 expected failures before threshold fixes).
- [x] 5.2 **GREEN** — Update `HistoryRecommendationService` threshold comparisons to `>=` semantics and compute rejection rate over `MANUAL_REVIEW + PROJECTABLE + REJECTED` history.
  - Verify: `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationServiceTest"` (GREEN — 14 tests passed).
- [x] 5.3 **RED/GREEN** — Add runtime stateless repeated-request test proving deterministic recomputation and no write-like history-port methods invoked.
  - Verify: `./gradlew.bat test --rerun-tasks --tests "*HistoryRecommendationServiceTest"` (GREEN — same response, read statuses invoked twice, `saveAll`/`resolveManualReview` not invoked).
- [x] 5.4 Run post-fix full verification and coverage.
  - Verify: `./gradlew.bat test --rerun-tasks` (GREEN — 142 tests passed); `./gradlew.bat jacocoTestReport` (GREEN).
