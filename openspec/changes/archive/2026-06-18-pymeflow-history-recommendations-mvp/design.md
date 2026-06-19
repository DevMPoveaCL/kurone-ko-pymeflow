# Design: PymeFlow History Recommendations MVP

## Technical Approach

Add a generic `findByStatus` query to the existing `CashflowMovementHistoryPort`, then build a thin read-only application service that loads movements by status, computes deterministic aggregate signals in memory, and returns them ordered by severity (WARNING before INFO). A dedicated controller exposes `GET /api/cashflow/recommendations`.

No new tables, no migrations, no AI/ML. Rejected rows contribute only aggregate counts and reason-code summaries; no raw descriptions or source references are emitted.

## Architecture Decisions

| Decision | Options | Trade-offs | Choice |
|----------|---------|------------|--------|
| Port query method | `findByStatus(ProfileId, Status)` vs specific finders | Specific finders are explicit; generic avoids port bloat as the domain grows | `findByStatus` — aligns with proposal and future status queries |
| Data loading | 3 status queries vs single `findByProfileId` | Single query loads everything at once but needs a new port method; 3 queries reuse the new `findByStatus` and leverage the existing `(profile_id, status, movement_date)` index | 3 calls to `findByStatus` — acceptable MVP tradeoff, no extra index needed |
| Signal severity & ordering | Hardcode in service vs configurable rules | Configurable rules require DB/UI changes; MVP needs speed | Hardcoded thresholds in `HistoryRecommendationService` |
| Response DTO location | Controller inner records vs standalone package | Inner records keep mapping logic co-located with the HTTP contract; existing controllers follow this pattern | Inner records in `HistoryRecommendationController` |
| Rejected data safety | Aggregate metrics only vs detailed lists | Lists risk leaking sensitive descriptions | Aggregate metrics only (`rejectedCount`, `rejectionRatePercent`, `topRejectionReasonCode`) |

## Data Flow

```
Client
  │ GET /api/cashflow/recommendations?profileId=pharmacy-cl
  ▼
HistoryRecommendationController
  │ validate profileId
  ▼
HistoryRecommendationService
  │ loadProfile(VerticalProfileService)
  │ findByStatus(MANUAL_REVIEW)
  │ findByStatus(PROJECTABLE)
  │ findByStatus(REJECTED)
  ▼
CashflowMovementHistoryJdbcAdapter
  │ indexed queries on cashflow_movement_history
  ▼
PostgreSQL / H2
  │
  ▼
HistoryRecommendationService (in-memory aggregates)
  │ compute signals + severity + ordering
  ▼
HistoryRecommendationController
  │ map to response DTOs
  ▼
Client ← JSON response
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `application/port/out/CashflowMovementHistoryPort.java` | Modify | Add `List<CashflowMovementRecord> findByStatus(ProfileId profileId, CashflowMovementStatus status)` |
| `infrastructure/persistence/CashflowMovementHistoryJdbcAdapter.java` | Modify | Implement `findByStatus` using `SELECT_COLUMNS + " where profile_id = ? and status = ? order by movement_date, created_at"` |
| `application/recommendation/HistoryRecommendationService.java` | Create | Compute signals with hardcoded thresholds; return `HistoryRecommendationResponse` record |
| `interfaces/web/HistoryRecommendationController.java` | Create | `GET /api/cashflow/recommendations`; request validation; response mapping |
| `infrastructure/persistence/CashflowMovementHistoryJdbcAdapterTest.java` | Modify | Cover `findByStatus` for `MANUAL_REVIEW`, `PROJECTABLE`, `REJECTED` |
| `application/recommendation/HistoryRecommendationServiceTest.java` | Create | Threshold edge cases, severity logic, signal ordering, healthy fallback |
| `interfaces/web/HistoryRecommendationControllerTest.java` | Create | `@WebMvcTest` for contract, validation, and JSON shape |

## Interfaces / Contracts

### Port Addition

```java
List<CashflowMovementRecord> findByStatus(ProfileId profileId, CashflowMovementStatus status);
```

### Service & Response Records

```java
public final class HistoryRecommendationService {
    public HistoryRecommendationResponse generate(ProfileId profileId) { ... }
}

public record HistoryRecommendationResponse(
    String profileId,
    Instant generatedAt,
    List<HistorySignalResponse> signals
) {}

public record HistorySignalResponse(
    String type,
    String severity,
    String title,
    String description,
    String actionHint,
    Map<String, Object> metrics
) {}
```

### Signal Rules (hardcoded)

| Signal Type | Condition | Severity |
|-------------|-----------|----------|
| `MANUAL_REVIEW_BACKLOG` | `pendingCount >= 5` | WARNING |
| `MANUAL_REVIEW_BACKLOG` | `0 < pendingCount < 5` | INFO |
| `HIGH_REJECTION_RATE` | `rejected / (manualReview + projectable + rejected) >= 0.30` | WARNING |
| `CATEGORY_CONCENTRATION` | one category amount >= 60 % of total projectable amount | INFO |
| `INSUFFICIENT_DATA` | `projectableCount < 10` | INFO |
| `RECENT_INACTIVITY` | no movements (any status) in last 30 days | WARNING |
| `HEALTHY_HISTORY` | none of the above apply | INFO |

Signals are ordered: WARNING first, then INFO. `HEALTHY_HISTORY` is emitted only when no other signal is present.

### Neutral Spanish Copy (examples)

- `MANUAL_REVIEW_BACKLOG` title: *Revisión manual pendiente*; actionHint: *Revisa y categoriza estos movimientos.*
- `HIGH_REJECTION_RATE` title: *Alta tasa de rechazo*; actionHint: *Revisa los motivos de rechazo para corregir la fuente de datos.*
- `CATEGORY_CONCENTRATION` title: *Concentración por categoría*; actionHint: *Revisa si la distribución de categorías refleja tu operación real.*
- `INSUFFICIENT_DATA` title: *Datos insuficientes*; actionHint: *Agrega más movimientos para obtener mejores recomendaciones.*
- `RECENT_INACTIVITY` title: *Inactividad reciente*; actionHint: *Registra los movimientos más recientes para mantener el historial actualizado.*
- `HEALTHY_HISTORY` title: *Historial saludable*; actionHint: *Sigue registrando movimientos para mantener esta tendencia.*

**Safety**: metrics for `HIGH_REJECTION_RATE` include `rejectedCount`, `projectableCount`, `rejectionRatePercent`, and `topRejectionReasonCode` (aggregate, no lists). No `safeDescription`, `sourceReference`, or raw rejected values are exposed.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `HistoryRecommendationService` thresholds, severity logic, signal ordering, `HEALTHY_HISTORY` fallback | JUnit 5 + AssertJ, mock `CashflowMovementHistoryPort` and `VerticalProfileService` |
| Integration | `CashflowMovementHistoryJdbcAdapter.findByStatus` for each status | `@JdbcTest` with H2 in PostgreSQL mode; insert rows per status; assert filtering and ordering |
| Integration | `HistoryRecommendationController` contract, validation, JSON shape, no rejected descriptions | `@WebMvcTest` with `@MockBean` on `HistoryRecommendationService` |

## Migration / Rollout

No migration required. The existing `(profile_id, status, movement_date)` index supports the new query without schema changes.

## Open Questions

- [ ] Should `RECENT_INACTIVITY` consider only `PROJECTABLE` movements or any status? Decision: any status (use all movements loaded), because inactivity means no ingestion at all.
- [ ] Should `CATEGORY_CONCENTRATION` use amount or count? Decision: amount (sum of `amount`) as it reflects cash-flow impact.

## Work-Unit Split / Chained PR Plan

**PR1 — history status query support** (≤ ~70 lines)
- `CashflowMovementHistoryPort`: add `findByStatus`
- `CashflowMovementHistoryJdbcAdapter`: implement query
- `CashflowMovementHistoryJdbcAdapterTest`: status coverage

**PR2 — service, endpoint, DTOs, tests** (≤ ~320 lines)
- `HistoryRecommendationService` + `HistoryRecommendationServiceTest`
- `HistoryRecommendationController` + `HistoryRecommendationControllerTest`

Total forecast well under the 400-line review budget.
