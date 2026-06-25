# Exploration: MVP frontend for PymeFlow

## Current State

The repository is a Java 21 / Spring Boot 3.3.6 backend with hexagonal boundaries already enforced by package structure and tests. There is no frontend toolchain today: no `package.json`, no `pnpm-lock.yaml`, no Vite config, no Playwright config, and no `src/main/resources/static` assets. The only template-like resource is `src/main/resources/templates/pharmacy-recommendations.es.mustache`, used for recommendation copy rather than UI rendering.

Spring Boot can serve static assets from `classpath:/static/`, `classpath:/public/`, `classpath:/resources/`, or `classpath:/META-INF/resources/` by default, so a minimal UI can be added without changing backend architecture or introducing a JS build step.

Existing APIs already cover the MVP flow:
- Manual import: `POST /api/cashflow/imports/manual` with per-row results and `movementDirection`.
- Simulated bank-statement import: `POST /api/cashflow/imports/bank-statement/simulated` preserving signed bank movements as positive `amount` plus `DEBIT`/`CREDIT`.
- History: `GET /api/cashflow/history/manual-review` and `GET /api/cashflow/history/projection-ready` expose safe movement history and direction.
- Provider sync: `POST /api/cashflow/provider-syncs` and `GET /api/cashflow/provider-syncs/{syncId}` expose durable sync status, counts, errors, and durability.
- Profile metadata: `GET /api/profiles/active` and `/active/categories` provide the active Chilean pharmacy profile and category vocabulary.

## Affected Areas

- `src/main/resources/static/` — best location for a zero-build MVP cockpit served by Spring Boot.
- `src/main/java/com/kuroneko/pymeflow/interfaces/web/*Controller.java` — existing REST API remains the contract consumed by the UI; avoid domain/application changes.
- `build.gradle` — may need only test/dev additions if Playwright tasks are wired through Gradle; otherwise keep backend build stable.
- `package.json`, `pnpm-lock.yaml`, `playwright.config.ts`, `tests/` — needed only if Playwright smoke tests are added with pnpm.
- `openspec/specs/*` — future spec should add a frontend-facing MVP requirement without expanding backend domain scope.

## Approaches

1. **Static Spring Boot cockpit** — Add `index.html`, `app.css`, and `app.js` under `src/main/resources/static`, using `fetch` against existing REST APIs.
   - Pros: smallest change, no npm, no bundler, works inside the existing Spring Boot app, easy to smoke test at `/` or `/index.html`, preserves hexagonal backend.
   - Cons: less component structure; discipline required to avoid a messy JS file.
   - Effort: Low.

2. **Server-rendered Spring MVC pages** — Add a template engine and controllers that render pages server-side.
   - Pros: still avoids Node for the application UI and can reuse Spring MVC.
   - Cons: adds backend web page concerns, new dependency, and template-controller coupling that is unnecessary for an API-first MVP.
   - Effort: Medium.

3. **pnpm + Vite frontend app** — Add a small modern frontend with a build copied into Spring Boot static resources.
   - Pros: better UI structure and long-term frontend ergonomics.
   - Cons: larger toolchain, larger review footprint, more moving parts, and likely beyond the MVP correction requested by the user.
   - Effort: Medium/High.

## Recommendation

Use **Approach 1: Static Spring Boot cockpit**. It is the smallest viable frontend integration that gives the product identity, exercises the real backend, and avoids dragging the roadmap into frontend infrastructure. Add pnpm only for Playwright smoke testing if automated browser tests are part of the implementation phase.

The MVP UI should be one cockpit screen with a clear flow:
1. Trigger fixture provider sync or submit a small manual/imported cashflow batch.
2. Show import/sync summary as an operational receipt.
3. Inspect movements split by `DEBIT`/`CREDIT`, with positive CLP amounts and explicit direction labels.
4. Show provider sync status/durability/errors from the durable status API.
5. Surface manual-review backlog and projection-ready history without adding new backend capabilities.

## Identity Direction

Position the product as a **Chilean PyME cashflow cockpit**, not a generic analytics dashboard.

- Domain concepts: caja diaria, cartola bancaria, abonos/cargos, proveedores, farmacia chilena, conciliación, estado de sincronización.
- Visual direction: calm operational cockpit; paper receipt/boleta texture, bank ink blue, CLP green, copper/amber alert accents, dense but readable ledger rows.
- Signature element: a **cashflow receipt rail** showing each import/sync as a traceable voucher with counts, direction chips (`ABONO/CREDIT`, `CARGO/DEBIT`), and durable sync stamp.
- Avoid defaults: no generic KPI-card grid as the primary experience; lead with flow, ledger evidence, and sync traceability.

## Testing Notes

Playwright smoke coverage should be end-to-end and minimal:
- Open the cockpit served by Spring Boot.
- Trigger a fixture provider sync with `santander` or `bancoestado`.
- Verify sync status/counts/durability are visible and no credential value is echoed.
- Submit/import a debit and credit movement.
- Verify history displays positive amounts and explicit `DEBIT`/`CREDIT` direction.

Use `pnpm` for Node tooling if needed. Playwright supports launching a local server with `webServer` and using `baseURL`, so the smoke test can start the Spring Boot app or reuse an existing local server.

## Risks

- Static JS can become unstructured if implementation goes beyond one cockpit screen; keep the MVP small and testable.
- Current APIs do not expose a complete general movement history list; the UI must use existing `manual-review` and `projection-ready` endpoints unless the next spec explicitly adds a read model.
- Playwright will introduce Node tooling even if the app UI itself avoids a build step; use pnpm and keep scripts minimal.
- Provider sync remains fixture-backed; UI copy must not imply real bank connectivity.

## Ready for Proposal

Yes. The proposal should scope a single Spring Boot-served static cockpit plus Playwright smoke tests, explicitly excluding real provider auth, new domain behavior, full dashboard analytics, and a Vite/React app unless implementation discovers static assets cannot satisfy the smoke flow.
