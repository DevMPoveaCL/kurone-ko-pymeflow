# Design: Cockpit Guided Demo MVP

## Technical Approach

Add a static, compact demo guide to the existing Spring Boot-served cockpit. The guide sits above the current evidence areas and points to existing reset, review, categorization, and projection controls. `app.js` keeps only in-memory browser-session progress derived from successful existing actions; no backend endpoint, storage, build tool, or `localStorage` is introduced.

## Architecture Decisions

| Option | Tradeoff | Decision |
|---|---|---|
| Static rail/stepper over the cockpit | Adds minimal UI while preserving evidence visibility | Use a `Guía de demo` rail after quick navigation and before `#caja`/actions; avoid modal or strict wizard gating. |
| Browser-session state only | Progress resets on reload, but avoids false durability | Add a small `state.guide` object in `app.js`; completed steps are hints, not workflow truth. |
| Hook existing success paths | Depends on current action functions, but avoids duplicate API logic | Advance guide from `runDemoReset`, `renderManualReview`, `resolveManualReviewMovement`, and `renderProjection` only after success. |
| Static Java tests + Playwright smoke evidence | No committed E2E harness, but matches no-Node constraint | Extend `CockpitStaticResourceTest`; use Playwright MCP/manual smoke during verify without adding `package.json`. |

## Data Flow

```text
User clicks guide step ──→ anchor/focus existing section ──→ existing action/API
        │                                                   │
        └──────── guide status / next hint ←── success path ┘
```

Guide updates must never call new APIs. Existing same-origin endpoints remain: demo reset, manual review history, manual review resolution, projection-ready history, cockpit projection, preferences, recommendations.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/resources/static/index.html` | Modify | Add `Guía de demo` section with four ordered anchors, concise neutral Spanish copy, `aria-label`, status region, and target IDs/data attributes. |
| `src/main/resources/static/app.js` | Modify | Add in-memory guide state, render/update helpers, anchor/focus handler, and success hooks from existing functions. |
| `src/main/resources/static/styles.css` | Modify | Add compact rail/stepper styles using existing receipt paper, bank ink, CLP green, copper alert, soft borders, and responsive wrapping. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Modify | Assert guide order, Spanish demo-safe copy, accessibility markers, session-only JS semantics, and forbidden live-bank/provider claims. |

## Interfaces / Contracts

No public backend contract changes.

Internal JS state forecast:

```js
state.guide = {
  currentStep: "reset",
  completed: new Set(),
  lastMessage: "Demo lista para reiniciar datos fixture."
};
```

Step keys: `reset`, `review`, `categorize`, `project`. UI copy should use: `Reiniciar demo`, `Revisar pendientes`, `Categorizar`, `Proyectar caja`. Forbidden claims include live bank/provider wording such as `conectividad bancaria real habilitada`, `proveedor real conectado`, `bank-live`, `live bank`, `token`, `cursor`, `stack`, and `trace` in static user-facing paths.

## Accessibility Semantics

- Guide is a named `<section aria-label="Guía de demo">` with an ordered list of steps.
- The active/complete state is conveyed with visible text, `aria-current="step"` for the current step, and a polite `role="status"` next-step hint.
- Step links are normal anchors; activation scrolls/focuses the existing section/control without blocking direct cockpit usage.
- Existing visible focus and skip-link behavior remain unchanged.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Static contract | Markup order, safe copy, accessibility attributes, no backend/build/storage drift | Extend `CockpitStaticResourceTest` for `index.html`, `app.js`, and `styles.css`. |
| Integration | Existing API behavior | No new Java service/controller tests; existing test suite covers endpoints. |
| E2E smoke | Guided path remains usable | Playwright smoke: load cockpit, click `Reiniciar demo`, follow guide to review, categorize first available movement if present, enter manual balance, calculate projection, verify guide hints. |

## Migration / Rollout

No migration required. Rollout is a static-resource change only and can be reverted by removing guide markup, CSS, JS state wiring, and static assertions.

## Risks

- Visual density can increase; mitigate with one-line step copy and compact wrapping.
- Progress can look durable; label as session demo hints and reset on reload.
- Static tests can overfit Spanish copy; assert stable guide phrases and forbidden claims, not every sentence.

## Open Questions

- [ ] None blocking.
