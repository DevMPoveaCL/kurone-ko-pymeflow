# Proposal: PymeFlow History Recommendations MVP

## Intent

Deliver the roadmap MVP for deterministic recommendations from persisted cashflow history, helping SMB users detect review backlog, rejection quality, data sufficiency, category concentration, and inactivity without projections, bank integration, AI, or ML.

## Scope

### In Scope
- `GET /api/cashflow/recommendations?profileId=pharmacy-cl` returning history-based signals.
- `HistoryRecommendationService` computing signals from persisted `cashflow_movement_history`.
- `CashflowMovementHistoryPort.findByStatus(ProfileId, CashflowMovementStatus)` and JDBC support for `MANUAL_REVIEW`, `PROJECTABLE`, and `REJECTED`.
- Response DTOs with `severity`, neutral Spanish `title`/`description`/`actionHint`, and safe aggregate metrics.
- Chained PR delivery: PR1 history status query support; PR2 service, endpoint, DTOs, tests.

### Out of Scope
- AI/ML recommendations, persisted recommendation snapshots, bank integration.
- Projection execution or reuse of projection-coupled `RecommendationService`.
- Configurable thresholds; MVP uses hardcoded defaults.

## Capabilities

### New Capabilities
- `cashflow-history-recommendations`: Read-only deterministic recommendations generated from cashflow movement history.

### Modified Capabilities
- None.

## Approach

Use a thin controller over a new application service. The service loads movements by status through the port, computes aggregate signals in memory, and returns warnings before info signals. Existing `(profile_id, status, movement_date)` index supports the query; no DB migration.

Endpoint response shape:
```json
{
  "profileId": "pharmacy-cl",
  "generatedAt": "2026-06-18T18:30:00Z",
  "signals": [{
    "type": "MANUAL_REVIEW_BACKLOG",
    "severity": "WARNING",
    "title": "Revisión manual pendiente",
    "actionHint": "Revisa y categoriza estos movimientos.",
    "metrics": { "pendingCount": 12 }
  }]
}
```

Types/defaults: `MANUAL_REVIEW_BACKLOG` >0 pending (`WARNING` if pending count is >=5, else `INFO` for 1-4); `HIGH_REJECTION_RATE` rejected movements >=30% of total persisted history (`MANUAL_REVIEW` + `PROJECTABLE` + `REJECTED`); `CATEGORY_CONCENTRATION` one category >=60% of projectable amount; `INSUFFICIENT_DATA` projectable count <10; `RECENT_INACTIVITY` no movements in 30 days; `HEALTHY_HISTORY` when no other signal applies. `CRITICAL` is reserved.

Safety: rejected rows MUST contribute only aggregate counts/amounts/reasons; no raw descriptions, source references, or sensitive rejected values are exposed.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `application/port/out/CashflowMovementHistoryPort.java` | Modified | Add status query. |
| `infrastructure/persistence/CashflowMovementHistoryJdbcAdapter.java` | Modified | Implement indexed query. |
| `application/recommendation/HistoryRecommendationService.java` | New | Compute deterministic signals. |
| `interfaces/web/HistoryRecommendationController.java` | New | Expose REST endpoint. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Review exceeds 400 lines | High | Force chained PRs: PR1 query support, PR2 endpoint/service/tests. |
| Large histories load in memory | Low | Accept MVP tradeoff; add aggregate queries later. |
| Sensitive rejected data exposure | Low | Return aggregate metrics only. |

## Rollback Plan

Revert PR2 to remove endpoint/service, then PR1 if unused. No schema changes or data migrations require rollback.

## Dependencies

- Existing `cashflow_movement_history` table and profile validation.
- Existing Spring MVC/JDBC test patterns.

## Success Criteria

- [ ] Endpoint returns deterministic signals for a valid profile.
- [ ] REJECTED history is queryable by status without exposing sensitive values.
- [ ] Tests cover service thresholds, JDBC status query, and controller contract.
- [ ] Chained PRs remain under the 400-line review budget.
