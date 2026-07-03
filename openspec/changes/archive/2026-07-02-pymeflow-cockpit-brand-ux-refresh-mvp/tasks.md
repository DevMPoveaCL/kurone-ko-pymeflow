# Tasks: PymeFlow Cockpit Brand UX Refresh MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 280-380 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR; monitor CSS churn |
| Delivery strategy | auto-chain |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Static contracts and asset wiring | PR 1 | RED tests, copy assets, HTML metadata |
| 2 | Token/layout refresh and smoke | PR 1 | CSS implementation, browser smoke, verify/archive |

## Phase 1: Static RED Contracts

- [x] 1.1 Extend `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` with failing assertions for `/favicon.png`, `/branding.png`, favicon link, brand image, and `color-scheme` metadata.
- [x] 1.2 Add failing static assertions for `@media (prefers-color-scheme: dark)`, Farmacia Uniacc token markers, no Node tooling, and unchanged `app.js` API targets/selectors.
- [x] 1.3 Run `./gradlew.bat test --rerun-tasks` and confirm the new static contracts fail before implementation.

## Phase 2: Assets and HTML Wiring

- [x] 2.1 Copy root `favicon.png` to `src/main/resources/static/favicon.png` and root `branding.png` to `src/main/resources/static/branding.png`.
- [x] 2.2 Update `src/main/resources/static/index.html` with favicon metadata, `<meta name="color-scheme" content="light dark">`, and constrained `.brand-lockup` image markup.
- [x] 2.3 Preserve existing Spanish demo copy, button labels, `data-api-target`, `data-guide-*`, and section landmarks in `index.html`.

## Phase 3: CSS Tokens and Layout

- [x] 3.1 Replace paper/bank tokens in `src/main/resources/static/styles.css` with `--flow-*` light tokens matching the design token map.
- [x] 3.2 Add dark-mode `@media (prefers-color-scheme: dark)` tokens, semantic cashflow contrast adjustments, and visible focus states.
- [x] 3.3 Refine `styles.css` spacing, typography, card radii, brand containment, responsive grids, and no-horizontal-scroll behavior at `860px` and `520px`.

## Phase 4: Verification and Smoke

- [x] 4.1 Run `./gradlew.bat test --rerun-tasks` and make `CockpitStaticResourceTest` pass without changing `src/main/resources/static/app.js` behavior.
- [x] 4.2 Start the Spring Boot app and use Playwright smoke to capture desktop/mobile light-dark evidence for assets, contrast, focus, and layout.
- [x] 4.3 Smoke reset, import/sync, review, and projection flows; verify receipts, DEBIT/CREDIT, positive CLP amounts, and demo-safe copy remain unchanged.

## Phase 5: SDD Closeout

- [x] 5.1 Create `openspec/changes/pymeflow-cockpit-brand-ux-refresh-mvp/verify-report.md` with test commands, screenshots/evidence, and residual risks.
- [x] 5.2 Apply and verify user-requested theme correction: remove magenta from dark mode, add accessible header theme switch, cover CSS/JS/markup contracts RED-first, rerun focused Gradle static tests, and complete browser smoke for theme toggle/runtime dark tokens.
- [x] 5.3 Apply reset UX/summary semantics bugfix: cover RED-first static contracts, calculate top cash summary from `projectionReady` only, keep manual-review movements visible in ledger/review, and make reset success status prominent/focused.
- [x] 5.4 Apply immediate surgical projection/copy cleanup: align projection start date with available projectable movement dates when the selected window would otherwise miss demo data, replace the misleading projection empty state with precise period/categorization messages, shorten didactic labels/copy, reduce typography scale, and verify with focused static/cockpit tests plus bounded browser/API smoke (`BUILD SUCCESSFUL in 38s`; reset summary `$305.000`; categorization + projection no longer shows old misleading copy).
- [ ] 5.5 Archive the change after verification by merging the delta into `openspec/specs/pymeflow-mvp-cockpit/spec.md` and moving the change folder under `openspec/changes/archive/`.

## Verification Notes

- 2026-07-02 strict bounded verify for task `5.4` passed: `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*CockpitStaticResourceTest"` reported 26 tests, 0 failures.
- Browser/API smoke passed against the running app: reset kept the summary at `$305.000`, categorizing one pending movement reduced manual-review cards from `2` to `1`, and projection rendered a result instead of the old `Categoriza primero` empty state.
