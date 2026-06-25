# Proposal: PymeFlow MVP Frontend Review

## Intent

Return PymeFlow to user-visible MVP functionality by adding a minimal Chilean PyME cashflow cockpit that exercises existing backend flows and gives the product a concrete identity.

## Scope

### In Scope
- Spring Boot-served static cockpit for caja, abonos, cargos, sync status, and movement evidence.
- UI consumption of existing import, provider-sync, history, profile, and recommendation APIs.
- Minimal pnpm/Playwright smoke only if needed for runtime verification.
- Neutral Spanish user-facing copy for the Chilean market; avoid “mostrador”.

### Out of Scope
- Real bank/provider connectivity, credential flows, auth, or multi-user behavior.
- Vite/React/large SPA toolchain.
- Unrelated backend domain expansion or dashboard analytics.

## Capabilities

### New Capabilities
- `pymeflow-mvp-cockpit`: Spring Boot static frontend cockpit for manual/provider cashflow flows, receipt-style sync traceability, and Spanish user-facing copy.

### Modified Capabilities
- None. Existing cashflow APIs remain the contract; add the smallest read endpoint only if implementation proves the cockpit cannot verify MVP flow with current APIs.

## Approach

Implement `index.html`, `app.css`, and `app.js` under `src/main/resources/static`, using `fetch` against existing REST APIs. Lead with a cashflow receipt rail and ledger evidence, not generic KPI cards. Keep JS small and explicit. If E2E is added, use pnpm + Playwright with one smoke path against the running Spring Boot app.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/resources/static/` | New | Cockpit assets served by Spring Boot. |
| `src/main/java/com/kuroneko/pymeflow/interfaces/web/` | Modified-if-needed | Only a minimal read endpoint/read model if blocked. |
| `package.json`, `pnpm-lock.yaml`, `playwright.config.ts`, `tests/` | New-if-needed | Minimal E2E smoke tooling. |

## Review Workload Forecast

Estimated change: 350–600 lines. 400-line budget risk: Medium. Chained PRs recommended: Yes if Playwright/tooling pushes diff above 400 lines; split UI assets first, E2E smoke second.

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Static JS becomes unstructured | Med | One screen, small functions, no framework creep. |
| UI implies real bank connectivity | Med | Copy states fixture/simulated sync clearly. |
| E2E adds Node review weight | Med | Add pnpm/Playwright only for smoke verification. |

## Rollback Plan

Remove static assets and optional Playwright files. Revert any minimal read endpoint separately; existing backend APIs remain unchanged.

## Dependencies

- Spring Boot static resource serving.
- Optional pnpm/Playwright only for browser smoke.

## Success Criteria

- [ ] Cockpit is available from the Spring Boot app without a frontend build step.
- [ ] User can trigger/import MVP cashflow data and see caja, abonos, cargos, status, and safe errors.
- [ ] Smoke verification proves DEBIT/CREDIT direction and positive CLP amounts are visible.
