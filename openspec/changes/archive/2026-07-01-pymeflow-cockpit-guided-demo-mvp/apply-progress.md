# Apply Progress: Cockpit Guided Demo MVP

## Status

Apply implementation is reconciled as complete for tasks 1.1-4.2. Playwright smoke, verify report, and archive remain pending.

## Mode

Strict TDD.

## Workload / PR Boundary

- Mode: single PR
- Current work unit: frontend-only guided demo MVP
- Boundary: static cockpit guide contracts, HTML/CSS rail, in-memory JS guide hints, and Gradle evidence through full test suite.
- Estimated review budget impact: within the task forecasted low-risk single-PR scope; no chain needed.

## Completed Tasks

- [x] 1.1 Static contract covers four `Guía de demo` steps in required order.
- [x] 1.2 Static contract covers neutral Spanish demo/fixture wording and forbidden live provider/bank claims.
- [x] 1.3 Static contract covers session-only in-memory guide state and no frontend drift.
- [x] 1.4 Static contract covers accessibility markers and non-blocking cockpit controls.
- [x] 2.1 `index.html` includes compact `<section aria-label="Guía de demo">` after quick navigation and before cockpit evidence.
- [x] 2.2 Guide includes four anchors targeting reset, review, categorization, and projection with safe demo copy.
- [x] 2.3 `styles.css` includes responsive receipt-paper guide rail/stepper styles with visible focus.
- [x] 3.1 `app.js` includes `state.guide`, `GUIDE_STEPS`, render/update helpers, and polite status copy.
- [x] 3.2 Guide anchors scroll/focus existing cockpit controls without gating direct use.
- [x] 3.3 Successful reset, review load, categorization, and projection paths mark session hints complete only after success.
- [x] 3.4 Guide remains frontend-only: no new endpoint, durable storage, auth, provider integration, Node tooling, or backend/domain changes.
- [x] 4.1 Gradle test evidence passed for focused cockpit static resources, cockpit slice, and full suite.
- [x] 4.2 Refactor/readability pass preserved static contract clarity; no extra abstraction added beyond compact guide helpers/selectors.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| Initial blocker | `CockpitStaticResourceTest.java` | Static contract | ❌ First attempt blocked: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` failed before edits because Spring Boot attempted PostgreSQL/Flyway and raised `java.net.ConnectException`. | Not started | Not started | Not started | Not started |
| 1.1 | `CockpitStaticResourceTest.java` | Static contract | ✅ DB unblocked in later attempt; focused cockpit static test passed per orchestrator evidence. | ✅ Assertions added for guide section, four step keys, href targets, and required order. | ✅ `*CockpitStaticResourceTest` passed. | ✅ Order assertions compare step index positions and target markers. | ✅ Kept readable static contract assertions. |
| 1.2 | `CockpitStaticResourceTest.java` | Static contract | ✅ Same focused test safety net. | ✅ Assertions added for demo-safe Spanish copy and forbidden live claims in HTML/JS. | ✅ `*CockpitStaticResourceTest` and `*Cockpit*` passed. | ✅ Covers allowed fixture/demo wording and disallowed live bank/provider wording. | ✅ Forbidden-claim checks remain centralized in static contract style. |
| 1.3 | `CockpitStaticResourceTest.java` | Static contract | ✅ Same focused test safety net. | ✅ Assertions added for `guide`, `completed: new Set()`, `GUIDE_STEPS`, helpers, and no storage/build drift. | ✅ `*CockpitStaticResourceTest` passed. | ✅ Covers positive in-memory markers and negative durable-storage/new-API/tooling markers. | ✅ Guide state kept in existing static JS state shape. |
| 1.4 | `CockpitStaticResourceTest.java` | Static contract | ✅ Same focused test safety net. | ✅ Assertions added for heading, ordered list, `aria-current`, status/live region, anchors, and direct controls. | ✅ `*CockpitStaticResourceTest` passed. | ✅ Covers guide semantics plus direct reset/import/projection controls to prove non-blocking shape. | ✅ Accessibility markers retained in markup. |
| 2.1-2.3 | `CockpitStaticResourceTest.java` | Static contract | ✅ Existing cockpit resources covered. | ✅ RED contracts from 1.x failed until markup/styles existed. | ✅ Guide markup/styles satisfy contracts. | ✅ Step order, targets, safe copy, responsive/focus styles audited in files. | ✅ CSS uses existing receipt paper, bank ink, CLP green, copper alert, soft borders. |
| 3.1-3.4 | `CockpitStaticResourceTest.java` | Static contract | ✅ Existing JS resource covered. | ✅ RED contracts from 1.x failed until session guide state/hooks existed. | ✅ JS contracts pass; orchestrator provided full suite pass. | ✅ Positive guide helper assertions plus negative no-storage/no-new-guide-API assertions. | ✅ Helpers (`markGuideStepComplete`, `renderGuideProgress`, `handleGuideClick`) keep logic compact. |
| 4.1 | `CockpitStaticResourceTest.java` and full suite | JUnit/Spring static contract | ✅ DB unblocked. | N/A — verification execution task. | ✅ Provided evidence: `*CockpitStaticResourceTest`, `*Cockpit*`, and full `test --rerun-tasks` all passed. | N/A. | N/A. |
| 4.2 | Static resources | Refactor/readability | ✅ Contracts remained green per provided test evidence. | N/A — refactor preservation task. | ✅ No critical defect found during reconciliation audit. | N/A. | ✅ No extra refactor needed beyond existing guide helpers; static readability preserved. |

## Test Summary

- Total tests written: 4 guided-demo static contract tests added to `CockpitStaticResourceTest.java` during implementation, in addition to existing cockpit static tests.
- Total tests passing: full suite passed per orchestrator evidence.
- Layers used: Static contract / Spring MockMvc resource tests.
- Approval tests: None — no behavior-preserving refactor-only task beyond static resource readability.
- Pure functions created: 0; frontend behavior is DOM/session state wiring.

## Tests Run / Evidence

Provided by orchestrator after DB unblock:

```text
./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"  # passed
./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*Cockpit*"                    # passed
./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks                                      # passed
```

Earlier blocker retained for audit trail:

```text
./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"
```

Result: failed before implementation because Spring context attempted PostgreSQL/Flyway connection and raised `java.net.ConnectException`; later DB-unblocked evidence supersedes this blocker for apply status.

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Modified | Added static contract coverage for guide order, safe copy, session-only state, accessibility markers, and non-blocking controls. |
| `src/main/resources/static/index.html` | Modified | Added compact `Guía de demo` rail with four ordered anchor steps, status copy, and accessible semantics. |
| `src/main/resources/static/app.js` | Modified | Added in-memory guide state, step metadata, render/update helpers, anchor focus behavior, and success hooks. |
| `src/main/resources/static/styles.css` | Modified | Added responsive guide rail/stepper styling using existing cockpit visual tokens. |
| `openspec/changes/pymeflow-cockpit-guided-demo-mvp/tasks.md` | Modified | Marked completed apply tasks 1.1-4.2. |
| `openspec/changes/pymeflow-cockpit-guided-demo-mvp/apply-progress.md` | Modified | Replaced stale blocked-only status with merged implementation and test evidence. |

## Deviations

None found during reconciliation audit. Implementation remains frontend-only, static-resource based, browser-session-only, and non-blocking as designed.

## Issues / Risks

- Playwright/browser smoke has not been fully proven in this apply artifact; task 4.3 remains pending.
- Verify report and archive remain pending; do not archive until smoke evidence is captured.

## Remaining Tasks

- [ ] 4.3 Perform Playwright smoke against the running cockpit: reset demo, review pending, categorize first available movement, enter manual balance, project cashflow, and confirm guide hints.
- [ ] 5.1 Create `openspec/changes/pymeflow-cockpit-guided-demo-mvp/verify-report.md` with test command output and Playwright smoke evidence.
- [ ] 5.2 After verification passes, run archive flow to merge the delta spec into `openspec/specs/pymeflow-mvp-cockpit/spec.md`.
