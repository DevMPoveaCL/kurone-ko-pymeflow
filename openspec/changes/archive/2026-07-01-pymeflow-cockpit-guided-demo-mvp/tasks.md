# Tasks: Cockpit Guided Demo MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 240-340 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR with TDD work-unit commits |
| Delivery strategy | auto-chain |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Static contracts define the guide behavior | PR 1 | RED tests before markup/JS/CSS |
| 2 | Guided rail UI and session hooks | PR 1 | Keep frontend-only and non-blocking |
| 3 | Smoke, verify, archive | PR 1 | Evidence and spec lifecycle |

## Phase 1: RED Static Contracts

- [x] 1.1 Extend `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` to fail until the four `Guía de demo` steps render in order.
- [x] 1.2 Add failing static assertions for neutral Spanish demo/fixture wording and forbidden live provider/bank claims in `index.html` and `app.js`.
- [x] 1.3 Add failing assertions that `app.js` uses session-only in-memory guide state: no `localStorage`, durable storage, new API URLs, token/cursor/trace copy, or build-tool drift.
- [x] 1.4 Add failing accessibility assertions for named guide section, ordered list, `aria-current`, `role="status"`, focusable anchors, and non-blocking cockpit controls.

## Phase 2: HTML/CSS Guide Rail

- [x] 2.1 Modify `src/main/resources/static/index.html` with compact `<section aria-label="Guía de demo">` after quick navigation and before cockpit evidence.
- [x] 2.2 Add four anchor steps targeting existing reset, review, categorization, and projection sections with safe Chilean-market Spanish demo copy.
- [x] 2.3 Modify `src/main/resources/static/styles.css` with responsive rail/stepper styles using existing receipt paper, bank ink, CLP green, copper alert, soft borders, and visible focus.

## Phase 3: JS Session Hooks

- [x] 3.1 Modify `src/main/resources/static/app.js` with `state.guide`, step metadata, render/update helpers, and a polite next-step status message.
- [x] 3.2 Add guide anchor click/focus behavior that scrolls to existing cockpit sections without gating direct reset, review, categorize, or projection actions.
- [x] 3.3 Hook successful `runDemoReset`, `renderManualReview`, `resolveManualReviewMovement`, and `renderProjection` paths to mark session hints complete only after success.
- [x] 3.4 Keep all guide logic frontend-only; do not add endpoints, persistence, auth, provider integration, Node tooling, or backend/domain changes.

## Phase 4: GREEN/Refactor Verification

- [x] 4.1 Run `./gradlew.bat test --rerun-tasks` and keep `CockpitStaticResourceTest` passing with the RED assertions now GREEN.
- [x] 4.2 Refactor duplicated guide selectors/copy in `index.html`, `app.js`, and `styles.css` only if it preserves static contract readability.
- [x] 4.3 Perform Playwright smoke against the running cockpit: reset demo, review pending, categorize first available movement, enter manual balance, project cashflow, and confirm guide hints.

## Phase 5: SDD Verify / Archive

- [x] 5.1 Create `openspec/changes/pymeflow-cockpit-guided-demo-mvp/verify-report.md` with test command output and Playwright smoke evidence.
- [x] 5.2 After verification passes, run archive flow to merge `openspec/changes/pymeflow-cockpit-guided-demo-mvp/specs/pymeflow-mvp-cockpit/spec.md` into `openspec/specs/pymeflow-mvp-cockpit/spec.md`.
