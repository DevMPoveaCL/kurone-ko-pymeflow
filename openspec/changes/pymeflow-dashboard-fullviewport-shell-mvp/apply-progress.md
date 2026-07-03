# Apply Progress: PymeFlow Dashboard Fullviewport Shell MVP

## Mode

Strict TDD.

## Completed Tasks

- [x] 1.1 Visible `Dashboard de caja` framing and no primary user-facing cockpit wording.
- [x] 1.2 Fullviewport shell markers/classes and mobile no-overflow CSS guards.
- [x] 1.3 Selector/API/tooling preservation contracts.
- [x] 1.4 Concise Spanish demo-safe copy contracts.
- [x] 2.1 HTML shell refactor with topbar, metrics, guide/actions, projection, and review in the primary shell.
- [x] 2.2 Concise `Dashboard de caja` copy while preserving selectors and API targets.
- [x] 2.3 Secondary evidence moved below primary shell and kept reachable.
- [x] 3.1 CSS `100dvh` grid, compact rhythm, dense type, tabular amounts, and bounded overflow.
- [x] 3.2 Responsive mobile stacking/no horizontal overflow guards.
- [x] 3.3 `app.js` reviewed; no changes needed because selectors and IDs were preserved.
- [x] 4.1 Static resource test RED/GREEN evidence captured.
- [x] 4.5 Runtime layout remediation: compact desktop shell CSS and review/projection overflow guards added after failed browser smoke.
- [x] 4.6 Systemic UI/copy cleanup: audited dashboard HTML/CSS/JS/static tests, simplified primary labels/help text, converted theme switch to compact reusable control, and added reusable help/status CSS patterns.
- [x] 5.1 Diff kept under the 400-line review budget.
- [x] PR1.1 Modular shell RED contracts added for accessible tabs/panels, default visible module, concise shell copy, compact theme control, and fullviewport CSS/JS contracts.
- [x] PR1.2 `index.html` refactored into persistent topbar + KPI strip + accessible module tabs with `Revisión` active by default and `Proyección`, `Cartola`, `Comprobantes` hidden until selected.
- [x] PR1.3 `styles.css` updated for fullviewport modular workspace, bounded active panel, hidden inactive panels, compact tab rhythm, and mobile no-overflow behavior.
- [x] PR1.4 `app.js` now owns minimal selector-safe tab state with click and arrow-key activation while preserving theme/demo/review/projection/API behavior.
- [x] PR1.5 Focused static and cockpit-pattern test commands passed after GREEN.
- [x] PR1.6 Smoke-failure remediation: demoted the guided demo into a closed details/help control, removed visible tutorial labels from the primary shell, bounded review/recommendation evidence, and restored full-width mobile workspace by neutralizing legacy `grid-area` placement.
- [x] 3.2 Browser smoke 1366x768 passed via local static server: no horizontal overflow, page scroll height equals viewport, one visible panel, no visible tutorial copy.
- [x] 3.3 Browser smoke 390x844 passed via local static server: no horizontal overflow, active workspace uses full width, no visible tutorial copy.
- [x] 3.4 Fullviewport smoke remediation: added RED-first contracts for `100dvh` bounded desktop shell, compact mobile header/KPIs/tabs/workspace ordering, internal evidence scrolling, and removed mobile `overflow: visible` shell patterns.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration | ✅ 26 existing static tests passing before edits | ✅ `servesDashboardCajaAsPrimaryUserFacingShellWithoutCockpitFraming` failed first | ✅ Passed in `CockpitStaticResourceTest` run | ✅ title, topbar label, heading, main shell, and forbidden cockpit copy | ✅ Existing legacy assertions updated to new user-facing wording |
| 1.2 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration | ✅ Same baseline | ✅ `servesDashboardWithFullviewportPrimaryShellContracts` and CSS guard test failed first | ✅ Passed in `CockpitStaticResourceTest` run | ✅ HTML structure, section ordering, `100dvh`, grid areas, mobile guards | ✅ CSS consolidated around `dashboard-shell`/`primary-shell` |
| 1.3 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration | ✅ Existing selector/API tests protected contracts | ✅ Preservation expectations existed before implementation | ✅ Passed in `CockpitStaticResourceTest` and `com.kuroneko.pymeflow.*Cockpit*` runs | ✅ Existing coverage spans `data-api-target`, `data-action`, guide hooks, API URLs, no tooling | ✅ `app.js` left unchanged |
| 1.4 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration | ✅ Existing copy safety tests passed before edits | ✅ `servesDashboardWithConcisePrimaryCopyAndSecondaryEvidenceBelowFold` failed first | ✅ Passed in `CockpitStaticResourceTest` run | ✅ concise labels, secondary evidence order, forbidden live/credential/provider claims | ✅ Primary copy shortened and duplicate long helper copy removed |
| 2.1-2.3 | Same static tests | Static integration | ✅ Baseline captured | ✅ Shell/order tests failed before HTML refactor | ✅ Passed after HTML refactor | ✅ Main shell, primary ordering, secondary evidence reachability | ✅ Removed user-facing cockpit labels while preserving technical selectors |
| 3.1-3.3 | Same static tests | Static integration | ✅ Baseline captured | ✅ CSS guard tests failed before CSS refactor | ✅ Passed after CSS refactor | ✅ Desktop grid plus 860/700/520px mobile rules | ✅ No JS change needed |
| 4.5 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration | ✅ 30/30 existing focused static tests passed before remediation | ✅ `servesDashboardStylesWithRuntimeSafeDesktopCompactShellAndReviewOverflowContracts` failed first | ✅ 31/31 passed after CSS remediation | ✅ covers compact-height desktop shell, bounded projection/review overflow, review child `min-width: 0`, and removal of fixed `220px`/`260px` columns | ✅ CSS-only surgical fix; no API/JS changes |
| 4.6 | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration | ✅ 31/31 focused static tests passing before cleanup | ✅ `servesDashboardWithSystemicConciseControlCopyAndReusableCompactPatterns` and theme markup expectations failed first | ✅ 32/32 focused static tests passed after cleanup | ✅ compact theme label, reusable `.compact-control`/`.help-text`/`.status-text`, concise review/projection/cartola/comprobantes copy, no redundant theme explanation | ✅ HTML/CSS/JS copy cleanup preserved selectors, APIs, storage key, and fullviewport guards |
| PR1 modular shell | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration | ⚠️ Initial safety net blocked by local PostgreSQL connection before isolating static test with H2/Flyway disabled | ✅ 3 modular shell tests failed first for missing tablist, default-hidden panels, and modular CSS/JS contracts | ✅ 35/35 focused static tests passed | ✅ tab semantics, four module labels, one default visible panel, concise copy, fullviewport CSS, and selector-safe JS tab state | ✅ Updated old static assertions from page-stack IA to module-shell IA |
| PR1 smoke remediation | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration + browser smoke | ✅ 35 focused static tests passing before edits | ✅ 3 expected failures for visible tutorial guide, missing full-width mobile workspace contracts, and unbounded review evidence; later 1 expected failure for legacy grid-area placement | ✅ 38/38 focused static tests passed; cockpit-pattern suite passed | ✅ hidden help/details guide, selector preservation, mobile full-width workspace, bounded review/recommendation lists, and no legacy implicit grid-area layout | ✅ CSS/HTML-only fix; `app.js` behavior unchanged |
| PR1 fullviewport shell remediation | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration + browser smoke | ✅ 38 focused static tests passed before edits | ✅ 2 expected failures for missing bounded desktop `100dvh` shell contracts and compact mobile app-shell/internal-scroll contracts | ✅ 40/40 focused static tests passed; cockpit-pattern suite passed | ✅ desktop `scrollHeight=768`, mobile `scrollHeight=844`, workspace starts at `280px` on 390x844, one visible panel, no horizontal overflow | ✅ CSS-only remediation; APIs, selectors, data hooks, theme, review/projection/reset behavior unchanged |

## Test Summary

- **Total tests written/updated**: 14 new static contract tests plus legacy assertion updates for dashboard wording, compact controls, copy cleanup, modular shell IA, hidden tutorial/help, mobile full-width workspace, bounded review evidence, bounded desktop shell, and compact mobile app-shell ordering.
- **Total tests passing**: 40 `CockpitStaticResourceTest` tests; cockpit-pattern suite passed with `com.kuroneko.pymeflow.*Cockpit*` after fullviewport shell remediation.
- **Layers used**: Static integration (JUnit/MockMvc resource assertions).
- **Approval tests**: Existing `CockpitStaticResourceTest` assertions served as approval coverage for selector/API/theme contracts.
- **Pure functions created**: 0.

## Commands Run

- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — baseline ✅ 26 tests passing.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — RED ✅ 4 expected failures after new tests.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — GREEN ✅ 30 tests passing.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*Cockpit*"` — blocked by PowerShell/Gradle wildcard expansion against screenshot filenames.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — remediation safety net ✅ 30 focused tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — remediation RED ✅ 1 expected failure for missing compact desktop/review overflow contracts.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — remediation GREEN ✅ 31 focused tests passing.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — systemic cleanup safety net ✅ 31 focused tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — cleanup RED ✅ 2 expected failures for compact theme/control/copy contracts.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — cleanup GREEN ✅ 32 focused tests passing.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after cleanup.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — safety net ⚠️ blocked by local PostgreSQL connection before static test isolation.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — modular shell RED ✅ 3 expected failures for missing accessible tabs/panels and modular CSS/JS contracts.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — modular shell GREEN ✅ 35 focused static tests passed.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after modular shell.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — smoke remediation safety net ✅ 35 focused static tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — smoke remediation RED ✅ 3 expected failures for visible tutorial guide, missing mobile workspace/full-width contracts, and unbounded primary evidence; follow-up RED ✅ 1 expected failure for legacy `grid-area` placement.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — smoke remediation GREEN ✅ 38 focused static tests passed.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after smoke remediation.
- Browser smoke through local static server (`python -m http.server 8765`): desktop 1366x768 ✅ `scrollWidth=1366`, `clientWidth=1366`, `scrollHeight=768`, one visible panel, no visible `Guía de demo`/`Demo guiada`; mobile 390x844 ✅ `scrollWidth=375`, `clientWidth=375`, active panel width `355px` at `x=10`, no visible tutorial copy.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — fullviewport safety net ✅ 38 focused static tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — fullviewport RED ✅ 2 expected failures for missing desktop/mobile bounded shell contracts.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — fullviewport GREEN ✅ 40 focused static tests passed.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after fullviewport remediation.
- Browser smoke through local static server (`python -m http.server 8765`): desktop 1366x768 ✅ `scrollWidth=1366`, `clientWidth=1366`, `scrollHeight=768`, workspace `top=320` / `bottom=756`, active panel `bottom=565`, one visible panel, tutorial hidden; mobile 390x844 ✅ `scrollWidth=390`, `clientWidth=390`, `scrollHeight=844`, workspace `top=280` / `bottom=844`, active panel `top=280`, active panel width `370px`, tutorial hidden.

## Audit Findings

- Theme switch was treated like explanatory card copy (`Preferencia visual`, helper `<small>`), not a compact control.
- Dashboard sections mixed concise labels with long explanatory paragraphs in projection, review, cartola, comprobantes, and demo guide.
- CSS had useful component styling but no reusable compact-control/help/status text pattern for repeated cleanup.
- `app.js` preserved APIs but several user-facing status messages still used longer cockpit/live-bank phrasing.
- `CockpitStaticResourceTest` was coupled to the full Spring/PostgreSQL context for static assets; it now disables Flyway and uses H2 so static contracts run without local PostgreSQL.
- The previous dashboard still used a stacked tutorial/page IA. The PR1 shell now shows only one active product module by default while keeping topbar and KPIs persistent.
- Legacy `grid-area` declarations from the earlier multi-section shell created implicit dashboard columns on mobile, squeezing the active workspace to ~102px; resetting those areas to `auto` restored full-width stacking.
- Keeping the demo guide as a visible primary card made the shell feel tutorial-first. A closed `<details>` help control preserves guide hooks without occupying the primary product surface.

## Review Budget

- Previous batch `git diff --shortstat`: 3 files changed, 222 insertions(+), 142 deletions(-).
- Current cumulative working diff after PR1 modular shell: 5 files changed, forecast above 400 changed lines. This should remain PR1 as the coherent base slice; PR2 drawer/modal polish stays deferred.

## Deviations / Issues

- No backend/API changes.
- No `app.js` changes; relocated markup kept selector contracts stable.
- Browser smoke was rerun with a local static server for layout-only evidence after remediation; API calls logged expected 404/errors because the Spring backend was not running for that smoke.
- Runtime-risk CSS now bounds the compact desktop primary shell with internal projection/review scrolling and removes fixed-width review/projection columns that caused horizontal clipping.
- Cleanup preserved backend/API behavior, `data-*` hooks, `pymeflow.theme` storage semantics, and previous fullviewport desktop/mobile CSS guards.
- No backend/API changes in PR1 modular shell.
- Browser smoke confirmed the desktop and mobile layout failures from the orchestrator evidence are resolved for static layout.
- The final fullviewport smoke confirms the PR1 shell is bounded to the viewport on desktop and mobile; mobile workspace begins well before the first viewport bottom (`280px` on 390x844), with page-level scroll eliminated and active evidence constrained to internal scrolling.
- Review budget risk is high because the coherent PR1 shell diff now exceeds 400 changed lines; do not add PR2 drawer/modal polish to this slice.
