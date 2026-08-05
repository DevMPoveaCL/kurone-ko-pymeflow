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

## PR2 Focused UI Polish Slice

- [x] Product identity made more prominent with a dedicated brand modifier and larger desktop size contract.
- [x] Removed the visible `Modo demo seguro` / `Datos demo/manuales. Sin conexión bancaria.` topbar card while keeping the compact theme switch visible.
- [x] Removed non-functional decorative KPI orb CSS from `Entradas` / `Salidas` cash cards.

### PR2 Polish TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| PR2 polish | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration | ✅ 40 focused static tests passing before edits | ✅ 5 expected failures for brand prominence, removed visible demo-mode copy/card, and KPI orb CSS removal | ✅ 41 focused static tests passed; cockpit-pattern suite passed | ✅ brand class/size, exact removed strings, absent `status-note` topbar card, compact theme still present, and no `.cash-card::after` orb | ✅ HTML/CSS-only change; APIs, selectors, reset/review/projection/tabs unchanged |

### PR2 Polish Commands

- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — safety net ✅ 40 focused static tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — RED ✅ 5 expected static failures.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — GREEN ✅ 41 focused static tests passed.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after PR2 polish.

### PR2 Polish Review Budget

- Slice diff before this apply-progress note: 3 files changed, 44 insertions(+), 9 deletions(-).

## PR2 Centered Composition Polish Slice

- [x] Added centered-composition static contract for the topbar/brand/heading block, centered visible copy blocks, centered KPI card content, and left/default-aligned form controls.
- [x] Reworked the dashboard topbar into a centered brand/heading composition with the compact theme switch positioned as a secondary action.
- [x] Centered KPI card content and visible section copy where appropriate while preserving input/select usability and all existing selectors/APIs/tabs/reset/review/projection behavior.

### PR2 Composition TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| PR2 centered composition | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration + browser smoke | ✅ 41 focused static tests passing before edits | ✅ 1 expected failure in `servesDashboardWithCenteredVisualCompositionAndUsableControls` for missing centered topbar/KPI contracts | ✅ 42 focused static tests passed; cockpit-pattern suite passed | ✅ header structure/order, centered topbar/heading/lead/KPIs/section copy, and no centered number input/select text | ✅ HTML/CSS-only change; no JS/backend/API selector changes |

### PR2 Composition Commands

- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — safety net ✅ passed before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — RED ✅ 1 expected static failure, 42 tests run.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — GREEN ✅ passed after HTML/CSS polish.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after composition polish.

### PR2 Composition Browser Smoke

- Static server smoke at 1366x768 ✅ `scrollWidth=1366`, `clientWidth=1366`, `scrollHeight=768`, centered topbar delta `0`, `h1` text-align `center`, KPI text-align `center`, one visible panel.
- Static server smoke at 390x844 ✅ `scrollWidth=390`, `clientWidth=390`, `scrollHeight=844`, centered topbar delta `-4`, `h1` text-align `center`, KPI text-align `center`, input text-align `start`, one visible panel.

### PR2 Composition Review Budget

- Current cumulative working diff before this apply-progress note: 4 files changed, 130 insertions(+), 15 deletions(-). This includes preserved prior PR2 polish changes already present on the branch.

## PR2 Topbar Order Correction Slice

- [x] Added RED-first static contract for a 3-column topbar: brand/logo first, centered title block second, compact theme switch third.
- [x] Reworked `index.html` so the prominent PymeFlow brand is horizontally integrated on the left, `PYMEFLOW · MVP` / `Dashboard de caja` / lead copy live in a centered middle block, and the theme switch stays on the right.
- [x] Updated `styles.css` so KPI cards follow directly after the topbar with a 6-8px gap in smoke checks, while preserving centered KPI text, no demo-safe card copy, no KPI orbs, form control usability, and no overflow.

### PR2 Topbar Order TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| PR2 topbar order/layout correction | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration + browser smoke | ✅ 42 focused static tests passing before edits | ✅ 1 expected failure in `servesDashboardWithThreeColumnTopbarOrderAndUsableControls` for missing 3-column topbar/title-block contract | ✅ 42 focused static tests passed; cockpit-pattern suite passed | ✅ desktop/mobile topbar order, KPI-after-topbar ordering, centered title/KPI text, no absolute theme switch, and mobile overflow constraints | ✅ HTML/CSS-only correction; no JS/backend/API selector changes |

### PR2 Topbar Order Commands

- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — safety net ✅ 42 focused static tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — RED ✅ 1 expected static failure, 42 tests run.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — GREEN ✅ 42 focused static tests passed after topbar correction.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after topbar correction.

### PR2 Topbar Order Browser Smoke

- Static server smoke at 1366x768 ✅ `scrollWidth=1366`, `clientWidth=1366`, `scrollHeight=768`, brand left of title, title left of theme action, KPI gap after topbar `8px`, `h1`/KPI text centered, one visible panel.
- Static server smoke at 390x844 ✅ `scrollWidth=390`, `clientWidth=390`, `scrollHeight=844`, brand/title/action visible in horizontal topbar, KPI gap after topbar `6px`, `h1`/KPI text centered, input text-align `start`, one visible panel.

### PR2 Topbar Order Review Budget

- Current cumulative working diff before this apply-progress note: 4 files changed, 175 insertions(+), 20 deletions(-). This includes preserved prior PR2 polish and composition changes already present on the branch.

## PR2 Action Highlight and Centered Module Polish Slice

- [x] Added RED-first static contract for centered module tabs, review/recommendation headings and help text, centered action rows, and progressive demo action highlighting.
- [x] Removed fixed primary emphasis from `Revisar abonos y cargos`; `Reiniciar demo` is the initial highlighted action via `data-demo-highlight="current"`.
- [x] Added simple frontend-only demo highlight state in `app.js`: initial reset, review after demo reset / pending review, and projection after pending review is cleared or a categorization completes.
- [x] Preserved existing APIs, selectors, tabs, reset/review/projection flow, viewport/no-overflow shell, topbar order, 300% brand, no demo-safe topbar copy, and no KPI orbs.

### PR2 Action Highlight TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| PR2 centered modules + progressive highlight | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration + browser smoke | ✅ 42 focused static tests passing before edits | ✅ 1 expected failure for missing centered module/content/action contracts and state-driven demo highlight; follow-up RED for initial-empty reset guard | ✅ 43 focused static tests passed; cockpit-pattern suite passed | ✅ reset initial highlight, review action highlight after reset/pending review, projection highlight after clear/categorize, centered tabs/review/recommendations/actions, and non-primary review button | ✅ Frontend-only state helper; no backend/API changes |

### PR2 Action Highlight Commands

- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — safety net ✅ 42 focused static tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — RED ✅ expected failure in `servesDashboardWithCenteredModuleContentAndProgressiveDemoHighlight`.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — GREEN ✅ 43 focused static tests passed after HTML/CSS/JS polish.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after action highlight polish.

### PR2 Action Highlight Browser Smoke

- Static server smoke at 1366x768 ✅ `scrollWidth=1366`, `clientWidth=1366`, `scrollHeight=768`, tabs/review/action row centered, initial highlighted action `Reiniciar demo`, manual review button transparent.
- Static server smoke at 390x844 ✅ `scrollWidth=390`, `clientWidth=390`, `scrollHeight=844`, tabs/review/action row centered, initial highlighted action `Reiniciar demo`, no horizontal overflow.

### PR2 Action Highlight Review Budget

- Current cumulative working diff before this apply-progress note: 5 files changed, 284 insertions(+), 24 deletions(-). This includes preserved prior PR2 polish/topbar changes already present on the branch.

## PR2 Focused Layout Cleanup Slice

- [x] Added RED-first static contract for compact vertical rhythm, grouped review header/actions, associated review content columns, stable internal scroll gutters, and no horizontal overflow.
- [x] Reworked review markup so the heading/actions live in a `review-command-group` above a paired `review-content-grid` with explicit movements/recommendations columns.
- [x] Tightened shell gaps and review panel spacing; added bounded column/list scrolling with `scrollbar-gutter: stable`, internal padding, and stretch-aligned paired blocks.
- [x] Preserved APIs, selectors, tabs, reset/review/projection behavior, progressive `Reiniciar demo` highlight, 300% brand/topbar order, no demo-safe topbar copy, and no KPI decorative circles.

### PR2 Layout Cleanup TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| PR2 focused layout cleanup | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration + browser smoke | ✅ 43 focused static tests passing before edits | ✅ 1 expected failure in `servesDashboardWithCompactGroupedReviewLayoutAndUnclippedInternalScroll` for missing grouped review/layout/scroll contracts | ✅ 44 focused static tests passed; cockpit-pattern suite passed | ✅ compact shell gaps, grouped command block before content grid, paired movement/recommendation columns, stable scroll gutters, internal padding, no horizontal overflow | ✅ HTML/CSS-only cleanup; no backend/API/JS changes |

### PR2 Layout Cleanup Commands

- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — safety net ✅ 43 focused static tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — RED ✅ expected failure in `servesDashboardWithCompactGroupedReviewLayoutAndUnclippedInternalScroll`.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — GREEN ✅ 44 focused static tests passed after HTML/CSS cleanup.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after layout cleanup.

### PR2 Layout Cleanup Browser Smoke

- Spring Boot smoke at 1366x768 ✅ `scrollWidth=1366`, `clientWidth=1366`, `scrollHeight=768`, shell gaps `8px`, command group before content grid, paired columns `718px / 431px`, movement/recommendation targets `overflow=auto` + `scrollbar-gutter=stable`, highlighted action `Reiniciar demo`.
- Spring Boot smoke at 390x844 ✅ `scrollWidth=390`, `clientWidth=390`, `scrollHeight=844`, review panel `top=271` / `bottom=844`, one-column content grid, movement/recommendation targets `overflow=auto` + `scrollbar-gutter=stable`, highlighted action `Reiniciar demo`.

### PR2 Layout Cleanup Review Budget

- Current cumulative working diff after this slice: 5 files changed, 370 insertions(+), 42 deletions(-). This includes preserved prior PR2 polish/topbar/action-highlight changes already present on the branch.

## PR2 Layout Tightening Follow-up Slice

- [x] Added RED-first static contract for zero topbar top padding, zero KPI margin, compact horizontal `review-command-group`, and taller bounded review content/list scroll space.
- [x] Updated review markup with `review-title-stack` and `review-actions-panel` so `REVISIÓN`/`Pendientes` and the action buttons share one compact command row.
- [x] Tightened shell rhythm and review spacing in CSS, restored same-row desktop actions, and increased manual-review/recommendation target heights with stable both-edge scroll gutters.
- [x] Preserved existing APIs, selectors, tabs, reset/review/projection behavior, progressive `Reiniciar demo` highlight, 300% brand/topbar order, no demo-safe visible topbar copy, no KPI decorative circles, and usable form text alignment.

### PR2 Layout Tightening TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| PR2 layout tightening follow-up | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration + browser smoke | ✅ 44 focused static tests passing before edits | ✅ 1 expected failure in `servesDashboardWithTightTopRhythmCompactReviewCommandRowAndUsefulReviewScrollSpace` for missing title/action split, zero topbar padding, compact command row, and taller bounded scroll contracts | ✅ 45 focused static tests passed; cockpit-pattern suite passed | ✅ topbar top padding `0`, KPI margin `0`, compact row before content grid, content grid min-height, target min-height, stable both-edge gutters, and no tall centered one-column command block | ✅ HTML/CSS-only fix; no JS/backend/API selector changes |

### PR2 Layout Tightening Commands

- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — safety net ✅ 44 focused static tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — RED ✅ expected failure in `servesDashboardWithTightTopRhythmCompactReviewCommandRowAndUsefulReviewScrollSpace`.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — GREEN ✅ 45 focused static tests passed after HTML/CSS tightening.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after tightening.

### PR2 Layout Tightening Browser Smoke

- Spring Boot smoke at 1366x768 ✅ `scrollWidth=1366`, `clientWidth=1366`, `scrollHeight=768`, topbar top `0px`, KPI gap after topbar `8px`, review command/content ratio `0.25`, review grid height `296px`, manual/recommendation targets `218px`, `overflow=auto`, `scrollbar-gutter=stable both-edges`, highlighted action `Reiniciar demo`.

### PR2 Layout Tightening Review Budget

- Current cumulative working diff after this slice: 5 files changed, 456 insertions(+), 45 deletions(-). This includes preserved uncommitted PR2 polish/topbar/action-highlight/layout changes already present on the branch.

## PR2 Microcopy Removal and Logo-to-Caja Gap Fix Slice

- [x] Added RED-first static contracts that remove persistent microcopy under `Pendientes`, `Reiniciar demo`, movements, and recommendations from the primary shell.
- [x] Kept the optional `demo-guide` affordance collapsed by default so guidance remains available without occupying the operating UI.
- [x] Tightened the topbar-to-metrics rhythm so `Caja` intentionally overlaps/abuts the PymeFlow logo instead of leaving dead space.
- [x] Preserved APIs, selectors, tabs, reset/review/projection behavior, progressive demo highlight, 300% brand, topbar order, centered content, no demo-safe visible topbar copy, no KPI decorative circles, compact review layout, and viewport/no-overflow behavior.

### PR2 Microcopy/Gap TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| PR2 focused UI cleanup and gap fix | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration + browser smoke | ✅ 45 focused static tests passing before edits | ✅ 5 expected failures for visible microcopy and missing zero/negative topbar-metrics gap contracts | ✅ 46 focused static tests passed; cockpit-pattern suite passed | ✅ covers no `Clasifica pendientes`, no `Solo reinicia datos demo`, no `Elige categoría y proyecta`, no `Señales de caja`, collapsed guide, and CSS logo/Caja abutment contracts | ✅ HTML/CSS-only production change; no backend/API/JS selector changes |

### PR2 Microcopy/Gap Commands

- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — safety net ✅ 45 focused static tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — RED ✅ 5 expected static failures.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — GREEN ✅ 46 focused static tests passed after HTML/CSS/test updates.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after cleanup/gap fix.

### PR2 Microcopy/Gap Browser Smoke

- Spring Boot smoke at 1366x768 ✅ `scrollWidth=1366`, `clientWidth=1366`, `scrollHeight=768`, `overflow-x=hidden`, `logoToCajaGap=-13.59px`, `topbarToMetricsGap=-13.59px`, one visible panel, no visible `Clasifica pendientes para proyectar.`, `Solo reinicia datos demo.`, `Elige categoría y proyecta.`, or `Señales de caja.`.

### PR2 Microcopy/Gap Review Budget

- Current cumulative working diff after this slice: 5 files changed, 528 insertions(+), 57 deletions(-). Increment since the previous recorded slice is approximately +72 insertions / +12 deletions; cumulative PR2 remains over the 400-line review budget because preserved uncommitted polish slices are still present on the branch.

## PR2 Root UI Refactor: Onboarding, Bounded Category Picker, Structural Logo/Caja Fix

- [x] Added RED-first static contracts for an initial optional step-by-step onboarding dialog, bounded custom category combobox/listbox, structural brand/Caja composition, and browser smoke hooks for gap/picker measurement.
- [x] Replaced the visible manual-review native `<select>` with a custom combobox/listbox backed by a hidden `data-review-category` input, preserving the existing manual review resolution payload (`chosenCategoryKey`) and API contract.
- [x] Added first-load onboarding overlay with `Omitir`/close controls and `pymeflow.onboardingGuide.dismissed` local-storage dismissal, while keeping persistent tutorial microcopy out of the primary shell.
- [x] Reworked the top layout so the PymeFlow brand and `Caja` card live in the same `brand-caja-stack` inside a `top-composition`; the measured logo-to-Caja gap is now structural `0px`, not dependent on negative margins.
- [x] Preserved reset/review/projection APIs, tabs, manual review behavior, progressive demo highlight, no KPI decorative circles, compact review layout, and fullviewport/no-horizontal-overflow behavior.

### PR2 Root Refactor TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| PR2 root UI refactor | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration + browser smoke | ✅ 46 focused static tests passing before edits | ✅ 3 expected failures for missing onboarding dialog, custom bounded category picker, and structural brand/Caja smoke hooks | ✅ 49 focused static tests passed; cockpit-pattern suite passed | ✅ onboarding visible + dismissible, reset/review/categorize/project steps, hidden input + custom combobox/listbox, bounded picker CSS/JS, direct brand/Caja composition, smoke hooks | ✅ Removed negative-margin gap dependency; added cache-busted static assets for smoke freshness; picker positioning accounts for containing blocks created by panel effects |

### PR2 Root Refactor Commands

- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — safety net ✅ 46 focused static tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — RED ✅ 3 expected static failures.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — GREEN ✅ 49 focused static tests passed after root refactor.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after root refactor.

### PR2 Root Refactor Browser Smoke

- Spring Boot at `http://localhost:8080/` was running but served the previous static asset version during smoke; latest-file smoke used a local static server with API routes mocked for layout measurement.
- Latest-file smoke at 1366x768 ✅ guide visible on fresh session (`guideVisible=true`), `Omitir` persisted dismissal (`stored="true"`), `logoToCajaGap=0`, `topbarToMetricsGap=0`, no horizontal overflow (`scrollWidth=clientWidth=1366`), and no page vertical overflow (`scrollHeight=viewportHeight=768`).
- Category picker smoke ✅ custom listbox opened with `position=fixed`, measured `top=457.31`, `bottom=586.19`, active panel `top=448.31`, `bottom=756`, `withinViewport=true`, `withinPanel=true`.
- Snapshot artifact saved outside the repo commit plan: `pymeflow-pr2-guide-picker-gap-smoke.png`.

### PR2 Root Refactor Review Budget

- Current cumulative working diff after this slice: 5 files changed, 937 insertions(+), 81 deletions(-). The PR2 branch already contained many uncommitted polish slices; this remains over the 400-line review budget and should be handled as an explicit review exception or split before PR.

## PR2 Corrective Root Fix: Cropped Brand Box and Normal KPI Row

- [x] Added RED-first static contracts proving `Caja` stays inside `.shell-metrics` with `Entradas` and `Salidas`, no `brand-caja-stack` exists, and the PymeFlow brand uses a cropped overflow-hidden wrapper instead of moving a KPI into the topbar.
- [x] Restored the `Caja` KPI card to the normal metrics row before `Entradas` and `Salidas`.
- [x] Added `.brand-crop-box` around `branding.png` so the transparent source canvas is clipped by layout; the image is translated/scaled inside the cropped visual box.
- [x] Preserved the left brand, centered title, right theme topbar order; onboarding, bounded custom category picker, APIs/selectors/tabs/reset/review/projection behavior, progressive highlight, no visible demo-safe topbar copy, no KPI decorative circles, and no horizontal overflow.
- [x] Browser smoke confirmed the brand wrapper visual box is smaller than the raw rendered image box and that `Caja`, `Entradas`, and `Salidas` share the KPI row at 1366x768.

### PR2 Corrective Root Fix TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| PR2 corrective brand/KPI root fix | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration + browser smoke | ✅ 49 focused static tests passing before edits | ✅ 3 expected failures for the obsolete `brand-caja-stack`, missing cropped brand wrapper, and `Caja` outside `.shell-metrics`; follow-up RED caught compact desktop KPI row wrapping to two columns | ✅ 49 focused static tests passed; cockpit-pattern suite passed | ✅ no `brand-caja-stack`, brand crop wrapper + translated/scaled image, `Caja`/`Entradas`/`Salidas` DOM order, compact desktop 3-column KPI row, viewport/no-overflow smoke | ✅ HTML/CSS-only correction; no JS/backend/API selector changes |

### PR2 Corrective Root Fix Commands

- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — safety net ✅ 49 focused static tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — RED ✅ 3 expected static failures for wrong structural composition.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — GREEN ✅ 49 focused static tests passed after restoring KPI row and adding brand crop wrapper.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — triangulation RED ✅ 1 expected failure for compact desktop KPI row wrapping to two columns.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — triangulation GREEN ✅ 49 focused static tests passed after restoring three KPI columns in compact desktop CSS.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after corrective fix.

### PR2 Corrective Root Fix Browser Smoke

- Spring Boot smoke at `http://localhost:8080/?v=pr2-root-crop-fix` with 1366x768 ✅ `scrollWidth=1366`, `clientWidth=1366`, `scrollHeight=768`, `viewportHeight=768`, `cajaInMetrics=true`, `kpiSameRow=true`, `rowOrder=true`, `brandBoxSmallerThanImage=true`, `topbarToMetricsGap=0`, `visibleLogoToMetricsGap=0`, `overflowX=hidden`.

### PR2 Corrective Root Fix Review Budget

- This is a focused corrective slice over an already-large PR2 branch. It changes only static HTML/CSS/tests plus this apply-progress note; no assets, screenshots, backend, API, or JS behavior were changed.

## PR2 Category Modal, Nav Order, Onboarding Guide, and Reset Status Slice

- [x] Added RED-first static contracts for replacing the manual-review category dropdown/listbox with a modal dialog selector, theme-safe category trigger styling, stable reset status placement, guide-first/reset-last nav order, and a real step-by-step onboarding guide.
- [x] Reordered the nav row so `Guía` is the left action, module tabs remain centered, and `Reiniciar demo` is the right action in the same nav block.
- [x] Moved `demo-reset-status` into a compact nav status slot so reset success/error messages are readable and do not overlap review headers or content.
- [x] Replaced the in-card category combobox/listbox with a global `role="dialog"` category selector. Cards now expose a theme-safe `Seleccionar categoría`/selected-category button plus hidden `data-review-category` input, and `Categorizar movimiento` submits the existing `chosenCategoryKey` payload unchanged.
- [x] Redesigned onboarding into an optional step-by-step dialog explaining where Guía, Reiniciar demo, Revisión, category selection, and Proyección live, with previous/next/start/finish/omit/close controls and Escape dismissal.
- [x] Preserved Caja/Entradas/Salidas reset totals, `Caja` in the KPI row, brand crop wrapper, no visible `Revisar abonos y cargos` / `Sync demo`, no horizontal overflow, existing APIs, selectors, tabs, projection, and manual-review resolution behavior.

### PR2 Category Modal/Guide TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| PR2 category modal + guide/nav/status correction | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration + browser smoke | ✅ 49 focused static tests passing before edits | ✅ 4 expected static failures for nav order, real onboarding controls/copy, modal category selector, and theme-safe selector styling/status placement | ✅ 52 focused static tests passed; cockpit-pattern suite passed | ✅ desktop/mobile smoke covered reset totals non-zero, readable status, guide visible/controls, category dialog bounds/options/selection, hidden value preservation, and no horizontal overflow | ✅ Replaced dropdown positioning logic with modal focus-return flow; removed in-card listbox/combobox roles and kept hidden input/API payload semantics |

### PR2 Category Modal/Guide Commands

- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — safety net ✅ 49 focused static tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — RED ✅ 4 expected failures after new contracts.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — GREEN ✅ 52 focused static tests passed after HTML/CSS/JS/test updates.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after category modal/guide/nav/status correction.

### PR2 Category Modal/Guide Browser Smoke

- Spring Boot smoke at 1366x768 ✅ `scrollWidth=1366`, `clientWidth=1366`, `scrollHeight=768`, guide visible on fresh load, next controls advanced `reset → review → categorize → project`, finish closed guide, nav order `Guía` left / tabs middle / `Reiniciar demo` right, reset status readable and non-overlapping, reset totals `$305.000 / $425.000 / $120.000`, category dialog within viewport with 9 options, choosing category set hidden value `sales` and closed dialog.
- Spring Boot smoke at 390x844 ✅ `scrollWidth=390`, `clientWidth=390`, `scrollHeight=844`, guide visible on fresh load, reset status readable, reset totals non-zero, category dialog within viewport, 9 options, and no horizontal overflow.
- Snapshot artifacts saved outside commit plan: `pymeflow-pr2-category-modal-desktop.png`, `pymeflow-pr2-category-modal-mobile.png`.

### PR2 Category Modal/Guide Review Budget

- Current cumulative working diff after this slice: 5 tracked files changed, 1262 insertions(+), 118 deletions(-). This includes all prior uncommitted PR2 polish/root-fix slices already present on the branch; this corrective slice should be reviewed as part of the existing PR2 exception/split decision.

## PR2 Modal Bounds, Symmetric Shell, and Guide Wizard Correction Slice

- [x] Added RED-first static contracts for a viewport-bounded category modal with fixed header/footer and internally scrollable options, shared shell width/gutter tokens, Guía → tabs → Reiniciar demo order inside a single nav block, and a five-step onboarding wizard.
- [x] Replaced the category modal's loose panel/list structure with `header/body/options/footer`, `max-height: calc(100dvh - var(--modal-safe-margin))`, `grid-template-rows: auto minmax(0, 1fr) auto`, `overflow: hidden`, and internal options scrolling.
- [x] Introduced shared `--shell-max-width` / `--shell-gutter` container tokens so topbar, KPI row, nav block, and content share one horizontal system instead of nested arbitrary widths.
- [x] Promoted the nav row into the visible pill/block containing `Guía` at left, module tabs in the middle, and `Reiniciar demo` at right.
- [x] Converted the guide copy to a concise five-step MVP onboarding wizard covering scope, reset totals, review/categorization, projection, and next-after-MVP work; previous/next/start/finish/skip controls now operate through the same stepper state.
- [x] Preserved localStorage dismissal (`pymeflow.onboardingGuide.dismissed`), backend/API contracts, hidden category payload semantics, tabs, reset/review/projection behavior, and removed visible useless action buttons.

### PR2 Modal/Shell/Guide TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| PR2 modal bounds + symmetric shell + guide wizard correction | `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Static integration | ✅ 52 focused static tests passing before edits | ✅ 3 expected failures for bounded modal/body/footer contracts, shared shell container tokens, and five-step MVP guide copy/controls | ✅ 56 focused static tests passed; cockpit-pattern suite passed | ✅ category modal desktop/mobile CSS contracts, no escaping option structure, shared topbar/KPI/nav/content width, nav order in one block, guide scope/reset/review/projection/next copy, JS next/back handlers | ✅ HTML/CSS/JS static correction; no backend/API/selector payload changes |

### PR2 Modal/Shell/Guide Commands

- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — safety net ✅ 52 focused static tests passing before edits.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — RED ✅ expected failures in new modal/shell/guide contracts.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` — GREEN ✅ 56 focused static tests passed after HTML/CSS/JS/test updates.
- `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "com.kuroneko.pymeflow.*Cockpit*"` — ✅ passed after correction.

### PR2 Modal/Shell/Guide Browser Smoke

- Spring Boot responded at `http://localhost:8080/` with HTTP 200.
- Playwright smoke was not executed: Node could not resolve `playwright`, and `npx playwright --version` exceeded 30s without usable output. No screenshots were created or modified.

### PR2 Modal/Shell/Guide Review Budget

- This is a corrective UI hardening slice on top of the already-large PR2 worktree. It touches only tracked static resources, static contracts, OpenSpec tasks, and this apply-progress note; untracked screenshots/assets were not touched.
