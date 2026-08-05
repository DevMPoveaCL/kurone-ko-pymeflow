# Tasks: PymeFlow Dashboard Modular Fullviewport Shell MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | PR1 300-390; PR2 120-220 if needed |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1 tabs/module shell + static contracts; PR2 drawer/modal/detail polish |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Accessible tab/module shell with preserved contracts | PR1 | Base main; include RED/GREEN static tests and browser smoke. |
| 2 | Drawer/modal/detail polish and secondary evidence | PR2 | Base PR1; only if PR1 nears 400 lines or detail UX remains rough. |

## Phase 1: RED Contracts (PR1)

- [x] 1.1 Add failing assertions in `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` for `role="tablist"`, four tabs, `aria-controls`, `aria-selected`, and `role="tabpanel"` on `#revision`, `#proyeccion`, `#cartola`, `#comprobantes`.
- [x] 1.2 Add failing static tests in `CockpitStaticResourceTest.java` proving concise Spanish copy: no tutorial paragraphs, no real-bank/credential/provider/live-connectivity claims.
- [x] 1.3 Add failing tests that only one primary module is visible by default, theme switch remains a compact reusable control, and desktop/mobile CSS includes `100dvh` plus no horizontal-overflow guards.
- [x] 1.4 Add preservation tests for `data-api-target`, `data-action`, `data-guide-*`, `#demo-reset-btn`, `#opening-balance`, `[name="horizonDays"]`, API URLs, and `pymeflow.theme`.

## Phase 2: GREEN Modular Shell (PR1)

- [x] 2.1 Refactor `src/main/resources/static/index.html` into topbar, KPI strip, accessible module tabs, one active workspace panel, and compact `Dashboard de caja` framing.
- [x] 2.2 Update `src/main/resources/static/styles.css` with fullviewport grid, compact KPI/tabs rhythm, internal panel scrolling, and 390px mobile no-overflow behavior.
- [x] 2.3 Add minimal selector-safe tab state in `src/main/resources/static/app.js`; do not change APIs, endpoint URLs, storage semantics, or existing action hooks.

## Phase 3: PR1 Verification

- [x] 3.1 Run focused RED/GREEN evidence: `./gradlew.bat test --rerun-tasks --tests "*CockpitStaticResourceTest"`.
- [x] 3.2 Browser smoke 1366x768: topbar, KPIs, tabs, active module, review action area visible; page-level shell fits viewport.
- [x] 3.3 Browser smoke 390x844: tabs reachable, controls stack, keyboard order logical, and `scrollWidth <= clientWidth`.
- [x] 3.4 Remediate viewport smoke regressions with RED-first static contracts for bounded desktop shell, compact mobile ordering, internal evidence scrolling, and no horizontal overflow patterns.

## Phase 4: PR2 Fullviewport Hardening

- [x] 4.1 Add RED tests for hidden/demoted tutorial treatment, bounded desktop shell, compact mobile shell ordering, internal evidence scrolling, and no horizontal-overflow patterns.
- [x] 4.2 Implement final viewport hardening in `styles.css` without touching backend/API contracts.
- [x] 4.3 Run focused static verification and preserve desktop/mobile smoke evidence before verify handoff.

## Phase 5: PR2 Corrective Modal/Shell/Guide Polish

- [x] 5.1 Add RED-first static contracts for bounded category modal panels, shared shell width/gutters, in-block nav ordering, five-step guide copy, and no visible useless action buttons.
- [x] 5.2 Update `index.html`, `styles.css`, and `app.js` so category options scroll inside the panel, topbar/KPI/nav/content share one container system, Guía/tabs/Reiniciar demo live in one nav block, and the guide works as a five-step onboarding wizard.
- [x] 5.3 Run focused `CockpitStaticResourceTest` and cockpit-pattern verification commands.
