# Apply Progress: PymeFlow Cockpit Brand UX Refresh MVP

## Status

Strict TDD apply completed for the static brand/UX refresh scope. Tasks 1.1 through 5.4 are complete after verification/remediation; archive task 5.5 remains open for the archive phase.

Post-verify remediation on 2026-07-01 fixed the critical mobile overflow reported in `verify-report.md`: `.review-grid` no longer keeps fixed `320px/260px` minimum columns and now stacks under the `860px` breakpoint with `minmax(0, 1fr)` safety.

User-requested theme remediation on 2026-07-01 corrected dark mode to use only dark/cyan/blue brand accents and added an accessible visible theme switch in the cockpit topbar/status area. The switch respects `prefers-color-scheme` as the default, stores only the visual `pymeflow.theme` preference in `localStorage`, applies `data-theme="light|dark"` on the document root, and does not persist guide/demo/business progress.

Reset UX/summary remediation on 2026-07-02 fixed two reported demo issues: the top cash summary now uses only `projectionReady` movements, while manual-review movements remain visible in the ledger/review queue; the reset success message is now a prominent inline success card that scrolls/focuses into view after reset.

Immediate surgical projection/copy cleanup on 2026-07-02 fixed the demo projection period mismatch before the larger fullviewport refactor: frontend projection now starts from available projectable movement dates when today's selected window would miss all demo data, and empty projection copy distinguishes "projectable but outside period" from "categorize first". Static UI labels/copy were shortened to didactic terms (`Dashboard de caja`, `Caja`, `Entradas`, `Salidas`, `Pendientes`, `Proyección`) and headline/amount scale was reduced.

Bounded strict verification on 2026-07-02 passed after the immediate cleanup: focused `CockpitStaticResourceTest` reported 26 tests with 0 failures, and browser/API smoke confirmed reset summary `$305.000`, one successful categorization, rendered projection results, and no old `Categoriza primero` misleading empty state after categorization.

## Completed Tasks

- [x] 1.1 Static asset and metadata RED assertions added.
- [x] 1.2 Palette, dark mode, no-tooling, and selector/API drift RED assertions added.
- [x] 1.3 RED run confirmed new static contracts failed before implementation.
- [x] 2.1 Root favicon and branding assets copied into Spring Boot static resources.
- [x] 2.2 Favicon metadata, `color-scheme`, and constrained brand lockup wired in `index.html`.
- [x] 2.3 Existing Spanish demo copy, API targets, guide selectors, and landmarks preserved.
- [x] 3.1 CSS tokens migrated to Farmacia Uniacc-derived `--flow-*` light palette.
- [x] 3.2 `prefers-color-scheme: dark`, semantic contrast tokens, and visible focus states added.
- [x] 3.3 Layout, spacing, typography, card radii, brand containment, responsive grids, and horizontal overflow behavior refined.
- [x] 4.1 Focused/static, cockpit-focused, and full Gradle test runs passed without changing `app.js`.
- [x] Remediation: mobile overflow regression fixed after failed verify by adding static responsive contracts and removing fixed review-grid minimum columns.
- [x] Remediation: dark palette/theme switch fixed with RED-first static contracts for accessible switch markup, JS theme wiring, light/dark override tokens, and no magenta in dark mode.
- [x] Remediation: reset UX and summary semantics fixed with RED-first static contracts for projection-ready-only cash totals and prominent reset success feedback.
- [x] 5.4 Immediate surgical fix: projection date semantics, precise empty-state copy, short didactic labels, and reduced typography scale.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Integration/static resource | ✅ `*CockpitStaticResourceTest` passed before edits | ✅ Added favicon/branding/static metadata assertions first | ✅ Passed after asset copy + HTML wiring | ✅ Asset URLs plus HTML metadata/markup cases | ✅ Assertions grouped by static contract |
| 1.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Integration/static resource | ✅ `*CockpitStaticResourceTest` passed before edits | ✅ Added token/dark-mode/no-tooling/API-selector assertions first | ✅ Passed after CSS token refresh | ✅ Light tokens, dark tokens, responsive markers, no-tooling, unchanged API/selector markers | ✅ Kept no-`app.js` behavior drift as a separate contract |
| 1.3 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Integration/static resource | ✅ `*CockpitStaticResourceTest` passed before edits | ✅ RED command failed on new brand asset and CSS palette contracts | ✅ Later focused run passed | ➖ Execution gate task; triangulation represented by 1.1/1.2 cases | ➖ None needed |
| 2.1 | `CockpitStaticResourceTest` | Integration/static resource | ✅ Baseline captured before edits | ✅ `/favicon.png` and `/branding.png` expected before files existed | ✅ Static resources served as `image/png` | ✅ Favicon and brand lockup resource cases | ➖ Binary copy only |
| 2.2 | `CockpitStaticResourceTest` | Integration/static resource | ✅ Baseline captured before edits | ✅ Metadata and `.brand-lockup` assertions added before HTML edit | ✅ HTML served with favicon, color-scheme, and constrained image | ✅ Head metadata plus image dimensions/alt/src cases | ✅ Header identity remains compact |
| 2.3 | `CockpitStaticResourceTest` | Integration/static resource | ✅ Baseline captured before edits | ✅ No-tooling/API-selector drift contract added before implementation | ✅ Existing copy and selector tests still pass | ✅ HTML selector and app.js API marker cases | ✅ `app.js` untouched |
| 3.1 | `CockpitStaticResourceTest` | Integration/static resource | ✅ Baseline captured before edits | ✅ `--flow-*` light token assertions added before CSS replacement | ✅ Stylesheet serves expected light tokens | ✅ Canvas/surface/elevated/inset/violet/cyan/magenta token cases | ✅ Removed old paper/bank token dependency |
| 3.2 | `CockpitStaticResourceTest` | Integration/static resource | ✅ Baseline captured before edits | ✅ Dark media and dark token assertions added before CSS implementation | ✅ Stylesheet serves dark token block and focus/color-scheme contract | ✅ Dark canvas/surface/elevated/raised/inset/cyan cases | ✅ Semantic success/warning/error tokens centralized |
| 3.3 | `CockpitStaticResourceTest` | Integration/static resource | ✅ Baseline captured before edits | ✅ Brand containment, responsive breakpoints, and overflow assertions added before CSS implementation | ✅ Stylesheet serves layout contract markers | ✅ Desktop, 860px, 520px, brand object-fit/max-inline-size, overflow cases | ✅ CSS refactored into tokenized surface/layout system |
| 4.1 | `CockpitStaticResourceTest`, `*Cockpit*`, full test suite | Integration/regression | ✅ Focused baseline passed before edits | ✅ Static contracts failed first | ✅ Focused, cockpit-focused, and full suite passed | ✅ Focused static + cockpit-focused + full suite commands | ✅ `app.js` behavior/API unchanged |
| Remediation | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Integration/static resource | ✅ `*CockpitStaticResourceTest` passed before remediation edits (`BUILD SUCCESSFUL in 47s`) | ✅ Added mobile-safe `.review-grid` contract before CSS fix; failed on fixed `minmax(320px...)` rule | ✅ Focused static test passed after CSS fix (`BUILD SUCCESSFUL in 40s`) | ✅ Covers desktop flexible columns, `860px` stacked grid, `520px` min-width safety, and button wrapping | ✅ Kept desktop two-column intent with fractional `minmax(0, ...)` columns |
| Theme remediation | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Integration/static resource | ✅ `*CockpitStaticResourceTest` passed before edits (`BUILD SUCCESSFUL in 1m 31s`) | ✅ Added failing contracts first for switch markup, theme JS, `data-theme` overrides, and no dark magenta (`6 failed`) | ✅ Focused static test passed after HTML/CSS/JS changes (`BUILD SUCCESSFUL in 43s`) | ✅ Markup + JS + light/dark token override + dark-no-magenta cases | ✅ Theme storage scoped to `pymeflow.theme`; API/demo-guide persistence remains unchanged |
| Reset UX/summary remediation | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Integration/static resource | ✅ `*CockpitStaticResourceTest` passed before edits (`BUILD SUCCESSFUL in 1m 17s`) | ✅ Added failing contracts first for `projectionReady`-only totals and prominent reset status (`2 failed`) | ✅ Focused static test passed after HTML/CSS/JS changes (`BUILD SUCCESSFUL in 55s`) | ✅ Summary source + ledger/review visibility + reset status markup/JS/CSS cases | ✅ Manual-review remains visible outside projected totals; status focus helper extracted |
| 5.4 Immediate projection/copy cleanup | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Integration/static resource | ✅ `*CockpitStaticResourceTest` passed before edits (`BUILD SUCCESSFUL in 42s`) | ✅ Added failing contracts first for projectable-date projection start, precise empty-state semantics, short didactic labels, and typography scale (`2 failed`) | ✅ Focused static test passed after JS/HTML/CSS/test updates (`BUILD SUCCESSFUL in 44s`) | ✅ Projection start selection + fallback empty state + didactic copy/scale cases; cockpit-focused run passed (`BUILD SUCCESSFUL in 53s`) | ✅ Kept backend/API contracts unchanged; projection refresh now waits for movement evidence dates before calculating |

## Test Summary

- **Total tests written**: 11 new integration/static contract tests across the brand refresh, mobile overflow remediation, theme switch/dark-palette remediation, reset UX/summary semantics remediation, and immediate projection/copy cleanup.
- **Total tests passing**: Focused `CockpitStaticResourceTest` and cockpit-focused Gradle tests passed after immediate projection/copy cleanup.
- **Layers used**: Integration/static resource (5 new tests across remediations; 2 new tests in this batch).
- **Approval tests**: Existing `CockpitStaticResourceTest` safety net preserved; no pure function refactor task.
- **Pure functions created**: 0.

## Commands Run

1. Safety net: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — passed before edits.
2. RED: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — failed on new brand asset and CSS palette contracts before implementation.
3. GREEN focused: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — passed.
4. Cockpit-focused: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*Cockpit*"` — passed.
5. Full suite: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks` — passed.
6. Remediation safety net: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — passed before remediation edits.
7. Remediation RED: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — failed on the new mobile-safe review-grid contract before CSS changes.
8. Remediation GREEN: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — passed after CSS changes.
9. Theme remediation safety net: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — passed before edits.
10. Theme remediation RED: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — failed on switch markup, JS wiring, dark override, and no-dark-magenta contracts before implementation.
11. Theme remediation GREEN focused: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — passed after implementation.
12. Theme remediation cockpit-focused: `./gradlew.bat --% --no-daemon --max-workers=1 test --rerun-tasks --tests "*Cockpit*"` — passed after implementation. Plain PowerShell wildcard invocation expanded `*Cockpit*` against screenshot filenames, so `--%` was required.
13. Reset UX/summary safety net: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — passed before edits.
14. Reset UX/summary RED: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — failed on the new projection-ready-only summary and prominent reset status contracts before implementation.
15. Reset UX/summary GREEN focused: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — passed after implementation.
16. Reset UX/summary cockpit-focused: `./gradlew.bat --% --no-daemon --max-workers=1 test --rerun-tasks --tests "*Cockpit*"` — passed after implementation.
17. Reset UX/summary verify focused: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — passed during final verification (`BUILD SUCCESSFUL in 55s`; 24 static-resource tests).
18. Reset UX/summary verify full suite: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks` — passed during final verification (`BUILD SUCCESSFUL in 1m 8s`; 356 tests across 62 suites).
19. Immediate projection/copy safety net: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — passed before edits (`BUILD SUCCESSFUL in 42s`).
20. Immediate projection/copy RED: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — failed on the new projection-date and copy/scale contracts before implementation (`2 failed`).
21. Immediate projection/copy GREEN focused: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — passed after implementation (`BUILD SUCCESSFUL in 44s`; 26 static-resource tests).
22. Immediate projection/copy cockpit-focused: `./gradlew.bat --% --no-daemon --max-workers=1 test --rerun-tasks --tests "*Cockpit*"` — passed after implementation (`BUILD SUCCESSFUL in 53s`).
23. Immediate projection/copy verify focused: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — passed during bounded verification (`BUILD SUCCESSFUL in 38s`; 26 static-resource tests, 0 failures/errors/skipped).
24. Immediate projection/copy browser/API smoke: running app health `UP`; headless Chrome reset showed `$305.000` summary (`$425.000` entradas, `$120.000` salidas), categorized one manual-review movement (`2 -> 1` cards), calculated projection with a rendered `.projection-summary`, and did not show old `Categoriza primero`/`Categoriza movimientos primero...` copy.
25. Demo fixture restore: `POST /api/cockpit/demo/reset-and-seed?profileId=pharmacy-cl` returned `DEMO_RESET_SEEDED`; projection-ready API confirmed 3 movements, credits `425000`, debits `120000`, net `305000`.

## Files Changed

| File | Action | Notes |
|------|--------|-------|
| `src/main/resources/static/favicon.png` | Created | Copied root asset into Spring Boot static resources. |
| `src/main/resources/static/branding.png` | Created | Copied root asset into Spring Boot static resources. |
| `src/main/resources/static/index.html` | Modified | Added color-scheme metadata, favicon link, and constrained brand lockup image. |
| `src/main/resources/static/styles.css` | Modified | Replaced visual system with Farmacia Uniacc-derived tokens, dark mode, responsive layout polish, and dark cyan/blue-only accent overrides. |
| `src/main/resources/static/app.js` | Modified | Added visual theme preference wiring only: system default, `localStorage` override, `data-theme`, and accessible switch state updates. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Modified | Added RED-first contracts for assets, metadata, palette, dark mode, no-tooling, API/selector drift, switch markup, JS theme wiring, and dark no-magenta behavior. |
| `src/main/resources/static/index.html` | Modified | Added projection-ready copy to summary cards and made reset status focusable with a prominent status class. |
| `src/main/resources/static/styles.css` | Modified | Added prominent success-card styling for reset feedback. |
| `src/main/resources/static/app.js` | Modified | Updated top cash summary to use `projectionReady` only; kept ledger/review combined visibility; added reset status focus/scroll after success. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Modified | Added RED-first contracts for projection-ready-only summary semantics and prominent reset feedback. |
| `src/main/resources/static/index.html` | Modified | Shortened static cockpit labels/copy to didactic terms: dashboard, caja, entradas, salidas, pendientes, and proyección. |
| `src/main/resources/static/styles.css` | Modified | Reduced headline and amount typography scale while preserving the existing static layout. |
| `src/main/resources/static/app.js` | Modified | Projection now chooses a useful start date from available projectable movement dates when today's selected window would miss demo data, and renders a precise out-of-period empty state. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Modified | Added RED-first contracts for projection-date semantics, precise empty-state copy, didactic labels, and typography scale. |
| `openspec/changes/pymeflow-cockpit-brand-ux-refresh-mvp/tasks.md` | Modified | Marked completed apply tasks through 5.4; archive remains 5.5. |
| `openspec/changes/pymeflow-cockpit-brand-ux-refresh-mvp/proposal.md` | Modified | Reflected user-requested visible theme switch and visual-only preference storage. |
| `openspec/changes/pymeflow-cockpit-brand-ux-refresh-mvp/design.md` | Modified | Updated theme decision, token map, file changes, and contracts for `data-theme` and dark no-magenta behavior. |
| `openspec/changes/pymeflow-cockpit-brand-ux-refresh-mvp/specs/pymeflow-mvp-cockpit/spec.md` | Modified | Added visible theme switch scenario and dark cyan/blue-only palette expectation. |
| `openspec/changes/pymeflow-cockpit-brand-ux-refresh-mvp/apply-progress.md` | Modified | Added post-verify mobile overflow and theme switch/dark-palette remediation evidence. |
| `openspec/changes/pymeflow-cockpit-brand-ux-refresh-mvp/verify-report.md` | Modified | Updated final verification evidence for reset UX/summary semantics, browser/API smoke, and Strict TDD compliance. |

## Deviations

Implementation now narrowly modifies `app.js` for the user-requested visual theme switch. This intentionally supersedes the original no-JS-toggle design note while preserving backend endpoints, API selectors, guide state, and business/demo behavior.

## Remaining

- [x] 4.2 Re-run browser smoke for desktop/mobile and light/dark visual evidence after remediation.
- [x] 4.3 Browser smoke for reset, import/sync, review, and projection flows.
- [x] 5.1 SDD verify report, updated after reset UX/summary remediation verification.
- [x] Reset UX/summary remediation.
- [x] 5.4 Immediate projection/copy cleanup.
- [ ] 5.5 Archive after verification.

## Workload / PR Boundary

- Mode: single PR.
- Boundary: one static brand/UX refresh work unit, from RED static contracts through Gradle regression pass.
- Review budget note: CSS replacement is the largest diff; no chained PR required by forecast.
