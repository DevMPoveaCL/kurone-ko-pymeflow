## Exploration: pymeflow-cockpit-guided-demo-mvp

### Current State
The cockpit is a static Spring Boot page served from `src/main/resources/static/` with plain HTML, CSS, and JavaScript. It already supports the full demo ingredients: `Reiniciar demo`, manual review loading and categorization, projection controls, preferences, receipts, recommendations, and safe demo-only copy. Existing APIs cover the narrative steps: reset fixture data, read pending review, resolve one movement category, refresh projection-ready evidence, and request cockpit projection. The smallest valuable next change is therefore not new backend capability, but a guided narrative layer over the existing flow.

Design context:
- Domain: caja diaria, cartola, boleta/comprobante, revisión pendiente, categoría, proyección, saldo manual, fixture/demo.
- Color world already present: receipt paper, bank ink blue, CLP green, copper alert, cellulose background.
- Signature to preserve: receipt/ledger cockpit with visible evidence and demo-safe stamps.
- Defaults to avoid: generic onboarding modal, wizard that hides the cockpit, progress UI that claims live bank connectivity.

### Affected Areas
- `src/main/resources/static/index.html` — add a lightweight guided-demo strip/stepper and anchors around the existing reset, review, categorize, and projection sections.
- `src/main/resources/static/app.js` — add static client-side guide state only: current step, next-step hints, section focus/scroll, and optional completion markers derived from existing actions.
- `src/main/resources/static/styles.css` — add compact guide styling using the existing receipt/ledger visual language without changing the design system.
- `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` — extend static smoke assertions for guide copy, step order, safe demo-only claims, and no forbidden real-connectivity copy.
- `openspec/specs/pymeflow-mvp-cockpit/spec.md` — likely delta target for guided flow requirements if this moves to spec.

### Approaches
1. **Static narrative rail over existing flow** — Add a visible 4-step guide: “1 Reiniciar demo → 2 Revisar pendientes → 3 Categorizar → 4 Proyectar caja”. Each step links to an existing section, includes one sentence of instruction, and updates lightweight completion hints after existing actions run.
   - Pros: smallest scope, no backend changes, keeps the cockpit transparent for demos, preserves existing fixture-only honesty, fits 400-line budget.
   - Cons: completion state is session-local and approximate; it guides the presenter rather than enforcing a strict workflow.
   - Effort: Low

2. **Strict frontend wizard** — Gate the cockpit behind a guided wizard where each step must complete before the next appears.
   - Pros: very controlled demo path; reduces presenter decisions.
   - Cons: hides existing cockpit evidence, increases JS state and edge cases, risks making reset/review/projection harder to inspect, more smoke burden.
   - Effort: Medium

3. **Backend-backed demo session state** — Persist guide progress server-side and expose a demo progress endpoint.
   - Pros: durable progress and exact state across reloads.
   - Cons: overengineered for a fixture demo, introduces new hexagonal backend work, storage/tests/spec load, and no clear user value for MVP.
   - Effort: High

### Recommendation
Use **Approach 1: Static narrative rail over existing flow**.

Recommended MVP scope:
- Add a compact `Guía de demo` section near the top, before the existing action panel.
- Use exactly four steps:
  1. `Reiniciar demo` — “Deja la demo con datos fixture conocidos.”
  2. `Revisar pendientes` — “Mira qué movimientos requieren clasificación manual.”
  3. `Categorizar` — “Elige una categoría activa sin cambiar DEBIT/CREDIT.”
  4. `Proyectar caja` — “Calcula caja con saldo inicial manual, no bancario.”
- Use anchor links and one primary “Siguiente paso” affordance; avoid modal overlays.
- Mark steps as “listo” only from existing successful actions where cheap: reset success, review list loaded/non-empty or empty-safe, categorization success, projection success.
- Keep all progress in static browser state; no `localStorage` required for MVP unless the proposal explicitly wants reload persistence.
- Do not add backend endpoints.

Copy pattern to reduce cognitive load:
- One task per step, verb first, no paragraphs.
- Keep evidence visible: each guide step points to the existing receipt/review/projection area instead of duplicating data.
- Neutral Chilean-market Spanish: use `caja`, `movimientos pendientes`, `categorizar`, `saldo inicial manual`, `datos fixture/demo`.
- Avoid claims like `banco conectado`, `saldo bancario`, `sincronización real`, or provider-specific promises.

Test/smoke implications:
- Extend `CockpitStaticResourceTest` to assert guide labels and order are present in `index.html`.
- Assert `app.js` contains guide state/update wiring and still avoids `token`, `cursor`, `stack`, `bank-live`, or real connectivity claims.
- Browser smoke can remain optional evidence: open cockpit, click “Reiniciar demo”, follow guide anchors, categorize one movement if available, enter manual balance, calculate projection.
- No Java domain/application/infrastructure tests needed if no backend support is added.

### Risks
- The current page is already dense; adding a guide can increase visual load unless it is a compact rail with progressive hints.
- Completion markers can become misleading if they imply durable workflow state; label them as demo-session hints, not persisted status.
- A strict wizard would reduce inspectability during demos and increase failure modes.
- Copy must keep demo/fixture boundaries explicit to avoid real-bank/provider connectivity claims.
- Static tests can overfit copy strings; use stable guide phrases but avoid excessive brittle assertions.

### Ready for Proposal
Yes — propose a small frontend-only guided demo MVP. Tell the user this should be a static narrative layer on top of the existing cockpit, not a backend workflow engine. The proposal should explicitly keep backend changes out of scope unless implementation discovers a hard blocker, and should target a small PR under the 400-line review budget.
