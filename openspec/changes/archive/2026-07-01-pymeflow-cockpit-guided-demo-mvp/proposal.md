# Proposal: Cockpit Guided Demo MVP

## Intent

Make the existing cockpit easier to demo by adding a compact, honest guide over the current fixture flow: reset demo data, review pending movements, categorize one movement, and project cashflow.

## Scope

### In Scope
- Add a visible 4-step `Guía de demo`: “Reiniciar demo → Revisar pendientes → Categorizar → Proyectar caja”.
- Link each step to existing cockpit sections and keep evidence visible.
- Add minimal browser-only JS state for current/completed steps based on existing successful actions.
- Use neutral Spanish copy for the Chilean market and explicit fixture/demo semantics.
- Extend static contract tests and capture Playwright smoke evidence against the running cockpit.

### Out of Scope
- Backend endpoints, domain/application/infrastructure changes, persistence, `localStorage`, auth, or provider integrations.
- Node tooling, frontend build steps, modal onboarding, strict wizard gating, or real bank/provider claims.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `pymeflow-mvp-cockpit`: add guided demo navigation, demo-session progress hints, safe Spanish copy, and smokeability requirements.

## Approach

Use the exploration recommendation: a static narrative rail over the existing flow. Add guide markup near the top of `index.html`, style it with the current receipt/ledger language, and wire `app.js` to mark steps “listo” only after existing successful reset, review load, categorization, and projection actions. The guide must inform, not enforce.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/resources/static/index.html` | Modified | Guide rail, anchors, concise Spanish task copy. |
| `src/main/resources/static/app.js` | Modified | Session-local guide state and next-step hints. |
| `src/main/resources/static/styles.css` | Modified | Compact guide styling using existing visual language. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Modified | Static assertions for order, safe copy, and no live-bank claims. |
| `openspec/changes/pymeflow-cockpit-guided-demo-mvp/specs/pymeflow-mvp-cockpit/spec.md` | New | Delta requirements for guided demo behavior. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Added visual density | Med | Keep rail compact, anchor-based, and evidence-first. |
| Misleading progress state | Med | Label as demo-session hints; do not persist. |
| Copy implies live connectivity | Low | Assert forbidden phrases in static tests. |
| Review budget creep | Low | Frontend-only scope; split tests only if needed. |

## Rollback Plan

Revert the static guide markup, CSS, JS guide wiring, static test additions, and delta spec. No data, backend, or build-tool changes are expected.

## Dependencies

- Existing cockpit reset, review, categorization, preferences, and projection actions.
- Existing Spring Boot static resource delivery; no Node dependency.

## Review Workload Forecast

- Expected size: under 400 changed lines.
- Decision needed before apply: No
- Chained PRs recommended: No
- 400-line budget risk: Low

## Success Criteria

- [ ] The cockpit shows the four guide steps in order with neutral Spanish demo copy.
- [ ] Guide completion reflects only successful existing actions and never claims persisted workflow state.
- [ ] Static contract tests and Playwright smoke prove the flow remains demo-only and frontend-only.
