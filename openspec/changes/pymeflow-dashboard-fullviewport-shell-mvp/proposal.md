# Proposal: PymeFlow Dashboard Fullviewport Shell MVP

## Intent

Make the cash dashboard usable as a first-screen operating surface, not a long cockpit page. The change should emphasize daily cash work, use visible `Dashboard de caja` wording, and preserve all existing static Spring Boot APIs, selectors, and demo-safe behavior.

## Scope

### In Scope
- Rename visible user-facing cockpit framing to `Dashboard de caja` where users see it.
- Recompose the static page into a `100dvh`-target shell: compact topbar, key metrics, guide/actions, projection, and a focused review area above the fold.
- Keep cartola and comprobantes available but visually secondary; drawer/modal-ready placement is acceptable for PR1.
- Reduce copy, typography scale, spacing, and card density using simple didactic Spanish labels.
- Update static resource tests and add/run desktop/mobile smoke evidence for no horizontal overflow and above-fold structure without Node tooling.

### Out of Scope
- Full drawer/modal focus implementation if it exceeds the PR1 review budget.
- Backend endpoint, API contract, persistence, or domain changes.
- Adding Node/Vite/React/frontend build tooling.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `pymeflow-mvp-cockpit`: add fullviewport dashboard shell requirements, visible dashboard wording, secondary evidence treatment, compact copy, and smokeable desktop/mobile layout acceptance.

## Approach

Use the exploration’s **Fullviewport shell MVP**. Change mostly `index.html` and `styles.css`: preserve `data-api-target`, `data-action`, guide hooks, and API URLs; restructure layout into a bounded shell with internal panel overflow only where needed. Keep `app.js` changes minimal and only for required shell affordances.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/resources/static/index.html` | Modified | Reorder IA, visible wording, compact shell landmarks. |
| `src/main/resources/static/styles.css` | Modified | `100dvh` shell grid, denser typography/spacing, overflow boundaries. |
| `src/main/resources/static/app.js` | Modified | Preserve behavior; minimal affordance wiring only if needed. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Modified | Assert selectors, copy, no tooling, shell markers, accessibility hooks. |
| `openspec/changes/pymeflow-dashboard-fullviewport-shell-mvp/specs/pymeflow-mvp-cockpit/spec.md` | New | Delta spec for shell behavior. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| PR1 exceeds 400 changed lines | Med | Keep drawers/modal polish in PR2; commit by reviewable shell/test work units. |
| Above-fold target is ambiguous | Med | Define smoke viewport targets for desktop and mobile in spec/tasks. |
| Copy reduction weakens demo-safe semantics | Low | Keep persistent demo/simulated badge and static forbidden-claim tests. |

## Rollback Plan

Revert the static resource, static test, and delta spec changes for this change. No data or backend rollback is required because APIs and persistence remain unchanged.

## Dependencies

- Existing static Spring Boot frontend and `pymeflow-mvp-cockpit` capability.
- Existing Gradle static tests; Playwright smoke may run externally/MCP without adding project tooling.

## Success Criteria

- [ ] Visible user-facing shell says `Dashboard de caja` and no longer presents the primary UI as a cockpit.
- [ ] Desktop target renders topbar, metrics, guide/actions, projection, and compact review above fold with no horizontal overflow.
- [ ] Mobile smoke preserves primary task flow without horizontal overflow.
- [ ] Static tests prove preserved selectors, APIs, demo-safe copy, and no frontend build tooling.
