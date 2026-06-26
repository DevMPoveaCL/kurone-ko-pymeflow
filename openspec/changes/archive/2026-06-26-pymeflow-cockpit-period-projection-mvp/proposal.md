# Proposal: Cockpit Period Cash Projection MVP

## Intent

Continue the MVP roadmap with a cockpit answer to “¿cómo queda mi caja esta semana/mes?” by reusing persisted `PROJECTABLE` movements and the existing projection engine instead of the current simplified `CREDIT - DEBIT` calculation.

## Scope

### In Scope
- Add a minimal read-only cockpit projection endpoint using persisted projection-ready history plus `CashflowProjectionService`.
- Add cockpit period controls: 7 days, 30 days, and custom only if small enough for the static UI.
- Show opening balance input, daily balances, abonos/cargos/obligations totals, closing balance, alerts, and safe empty/error states.

### Out of Scope
- Broad dashboard analytics, charts beyond a simple timeline/table, auth, multi-user, Node tooling, real bank/provider claims.
- New domain model, database changes, persisted balance/account state, or provider balance lookup unless existing types cannot satisfy the MVP.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `pymeflow-mvp-cockpit`: add period projection behavior and allow one read-only cockpit projection endpoint for this MVP.

## Approach

Add `GET /api/cashflow/cockpit/projection?profileId&startDate&horizonDays&openingBalance`. The interface adapter validates request values, calls an application use case that reads `CashflowMovementHistoryService.projectionReady()`, converts rows via `toProjectionTransaction()`, builds `CashflowProjectionCommand`, and delegates to `CashflowProjectionService.project()`. Static HTML/CSS/JS consumes the response and renders neutral Chilean Spanish UI copy with no real-bank claims.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `interfaces/web/CashflowProjectionController.java` | Modified | Add read-only cockpit projection route/DTO mapping. |
| `application/cashflow/*` | Modified | Add thin use case only if controller orchestration would violate hexagonal boundaries. |
| `src/main/resources/static/app.js` | Modified | Period state, API call, safe states, projection rendering. |
| `src/main/resources/static/index.html` | Modified | Projection controls and evidence sections. |
| `src/main/resources/static/styles.css` | Modified | Responsive cockpit projection UI. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Opening balance is not persisted | High | Require manual input; default clearly labeled, never imply bank balance. |
| No `PROJECTABLE` movements | Medium | Empty state tells user to categorize movements first. |
| Review budget exceeded | Medium | Split backend endpoint/tests and static UI/tests if changed lines exceed 400. |

## Rollback Plan

Revert the endpoint/use-case and static resource changes. The existing cockpit remains usable with current history/review panels because the feature is additive.

## Dependencies

- Existing projection engine, projection-ready history endpoint/service, `pharmacy-cl` vertical profile obligations and alert rules.

## Review Workload Forecast

- 400-line budget risk: Medium.
- Chained PRs recommended: Yes if implementation forecast exceeds 400 changed lines.
- Split guidance: PR 1 backend endpoint/use case/tests; PR 2 static cockpit UI and smoke/static-resource tests.

## Success Criteria

- [ ] Cockpit can project 7-day and 30-day cash from persisted `PROJECTABLE` movements and user opening balance.
- [ ] UI shows daily balances, abonos, cargos, obligations, closing balance, alerts, and safe empty/error states in neutral Chilean Spanish.
- [ ] No Node tooling, real-bank claims, auth, multi-user, or hexagonal boundary violations are introduced.
