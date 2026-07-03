# Proposal: PymeFlow Cockpit Brand UX Refresh MVP

## Intent

Refresh the static cockpit so it feels like a cohesive PymeFlow demo surface while preserving current Spring Boot static delivery, API behavior, and demo-safe cashflow flows.

## Scope

### In Scope
- Copy `favicon.png` and `branding.png` into `src/main/resources/static/` and reference them from `index.html`.
- Replace current paper/bank CSS tokens with `E:\farmaciauniacc`-derived light/dark tokens in `styles.css`.
- Add `prefers-color-scheme` dark mode, `color-scheme` metadata, and a visible session/local browser theme switch.
- Refine existing cockpit layout, typography, spacing, contrast, symmetry, and brand coherence.
- Extend static tests and capture Playwright visual/flow smoke evidence against the running app.
- Keep plain HTML/CSS/JS; no Node tooling.

### Out of Scope
- Backend endpoint, API contract, or business-flow changes.
- React/Vite/npm, frontend build steps, remote font fetching, or business/demo progress persistence. A tiny `localStorage` visual theme preference is allowed.
- Full information-architecture redesign or new cockpit features.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `pymeflow-mvp-cockpit`: add explicit static brand assets, system dark mode, tokenized visual identity, and visual/flow smoke expectations while preserving current cockpit behavior.

## Approach

Use the exploration-recommended MVP brand shell refresh: HTML asset wiring plus CSS token/layout work, with minimal JS only for the visible theme switch. Keep business API behavior unchanged. Implement a restrained pharmacy-cash cockpit signature: contained brand lockup, demo-safety badge, tabular CLP rows, and light-mode magenta/cyan accent rails; dark mode maps accents to the cyan/blue family only.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/resources/static/favicon.png` | New | Serve favicon through Spring Boot static resources. |
| `src/main/resources/static/branding.png` | New | Serve contained brand lockup. |
| `src/main/resources/static/index.html` | Modified | Add icon/brand image and `color-scheme` metadata. |
| `src/main/resources/static/styles.css` | Modified | Palette tokens, dark mode, spacing, contrast, responsive polish. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Modified | Assert assets, dark-mode markers, static-only/no Node contract. |
| `openspec/specs/pymeflow-mvp-cockpit/spec.md` | Modified | Delta spec for brand/dark-mode/static smoke requirements. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Brand image appears cropped or stretched | Med | Constrain dimensions and use `object-fit: contain`. |
| Dark palette weakens contrast | Med | Test light/dark screenshots and focus states. |
| Scope exceeds review budget | Low | Limit changes to assets, HTML, CSS, and static tests. |

## Rollback Plan

Remove the two copied static assets, revert `index.html`, `styles.css`, and static-test assertions, then rerun `./gradlew.bat test --rerun-tasks`.

## Dependencies

- Existing root `favicon.png` and `branding.png` assets.
- Existing Spring Boot static cockpit and MockMvc static-resource tests.
- Playwright MCP/browser smoke; no project Node dependency.

## Success Criteria

- [ ] `/favicon.png`, `/branding.png`, `/index.html`, `/styles.css`, and `/app.js` are served by Spring Boot.
- [ ] Light and dark themes render with readable contrast, visible focus, and no horizontal scroll at mobile width.
- [ ] Existing import, sync, reset, review, and projection flows remain behaviorally unchanged.
- [ ] Static tests pass and Playwright smoke captures desktop/mobile light-dark visual and flow evidence.
