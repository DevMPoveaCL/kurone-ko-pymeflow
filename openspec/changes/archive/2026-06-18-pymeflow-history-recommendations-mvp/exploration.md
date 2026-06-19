# Exploration: PymeFlow History-Based Recommendations MVP

## Current State

The `cashflow_movement_history` table stores every ingested movement with complete persistence: each row carries `profile_id`, `amount`, `currency`, `movement_date`, `status` (MANUAL_REVIEW | PROJECTABLE | REJECTED), `category_key`, `safe_description`, `source_reference`, `rejection_reason_code`, and timestamps. The existing `CashflowMovementHistoryPort` exposes five query methods:

| Method | Statuses covered |
|--------|-----------------|
| `findPendingManualReviews(ProfileId)` | MANUAL_REVIEW only |
| `findProjectionReady(ProfileId)` | PROJECTABLE only |
| `findById(UUID)` | any |
| `findBySourceReference(ProfileId, String)` | any |
| `resolveManualReview(…)` | MANUAL_REVIEW → PROJECTABLE transition |

**Critical gap**: REJECTED movements are never exposed through the port. Only MANUAL_REVIEW and PROJECTABLE statuses are queryable. The existing `CashflowMovementHistoryJdbcAdapter` maps all three statuses in its `mapRow` method but has no query for REJECTED.

### Existing Recommendation Service (projection-coupled, NOT reusable for history)

The `RecommendationService.generate(RecommendationRequest)` evaluates `ProfileRule` conditions (`projected_balance_below_threshold`, `projected_balance_above_threshold`, `obligations_due_before_cash_inflow`) against a `MerchantRecommendationState` that requires `projectedBalance`, `nextObligationDueAt`, and `nextExpectedInflowAt`. It renders Mustache templates via `RecommendationTemplatePort`. This service is **projection-dependent** — it answers "what should I do given this projected balance?" — NOT "what does my history tell me?"

For the MVP, history-based recommendations answer different questions:
- "Are you sitting on unreviewed data?"
- "Are you rejecting too many movements?"
- "Is your cashflow concentrated in one category?"
- "Do you have enough data for meaningful projections?"

These are deterministic signals computed from persisted history — no projection run, no AI, no ML required.

### Data Schema (authoritative)

```sql
cashflow_movement_history (
    id UUID PK,
    profile_id VARCHAR(63) FK → vertical_profiles,
    amount NUMERIC(18,2) CHECK (amount > 0),
    currency VARCHAR(3),
    movement_date DATE,
    status VARCHAR(24) CHECK (IN 'MANUAL_REVIEW','PROJECTABLE','REJECTED'),
    category_key VARCHAR(80) FK → vertical_profile_categories,  -- non-null only for PROJECTABLE
    safe_description VARCHAR(160),
    source_reference VARCHAR(80),
    rejection_reason_code VARCHAR(80),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
)
```

Indexes:
- `idx_cashflow_movement_history_profile_status_date` on `(profile_id, status, movement_date)` — **already supports status-filtered queries**
- `idx_cashflow_movement_history_profile_created` on `(profile_id, created_at)`
- Unique partial: `(profile_id, source_reference) WHERE source_reference IS NOT NULL`

The index on `(profile_id, status, movement_date)` means a `findByStatus` query would be index-covered — no new index needed.

### Architecture Boundaries (Hexagonal)

```
interfaces/web/     ← NEW: HistoryRecommendationController (REST GET)
application/        ← NEW: HistoryRecommendationService
application/port/out/ ← MODIFIED: CashflowMovementHistoryPort (+findByStatus)
infrastructure/      ← MODIFIED: CashflowMovementHistoryJdbcAdapter (+findByStatus query)
domain/              ← UNCHANGED
```

The recommendation domain types (`RecommendationContext`, etc.) remain projection-coupled and are NOT reused. The history recommendations use their own simple DTOs.

## Affected Areas

| File | Impact | Description |
|------|--------|-------------|
| `application/port/out/CashflowMovementHistoryPort.java` | **Modified** | Add `findByStatus(ProfileId, CashflowMovementStatus)` |
| `infrastructure/persistence/CashflowMovementHistoryJdbcAdapter.java` | **Modified** | Implement `findByStatus` query (index-covered) |
| `application/recommendation/HistoryRecommendationService.java` | **New** | Computes recommendation signals from history |
| `interfaces/web/HistoryRecommendationController.java` | **New** | REST endpoint `GET /api/recommendations/history` |
| `infrastructure/config/ApplicationServiceConfiguration.java` | **Modified** | Wire new service bean |
| Test: `HistoryRecommendationServiceTest.java` | **New** | Unit test with mock port |
| Test: `CashflowMovementHistoryJdbcAdapterTest.java` | **Modified** | Add `findByStatus` test cases |
| Test: `HistoryRecommendationControllerTest.java` | **New** | `@WebMvcTest` |

## Approaches

### 1. Read-only application-layer endpoint using existing port (and one new method)

Add only `findRejected(ProfileId)` to the port (minimal surface area). The new controller calls existing `findPendingManualReviews`, `findProjectionReady`, and the new `findRejected`, loads all records into memory, and computes signals in the controller itself — no separate application service.

**Pros:**
- Minimal code — controller does everything (~80 lines controller + ~15 lines port adapter)
- No new application service to test
- Fastest to implement

**Cons:**
- Violates hexagonal separation: controller contains business logic (computing recommendations is application-layer responsibility)
- Harder to unit-test business logic independently of Spring MVC
- `findRejected` is a one-off — if we later need `findByStatus` for other use cases, we'd add another method
- Not reusable — if another interface needs the same signals, logic is duplicated

**Effort:** Low (but dirty)

---

### 2. New recommendation application service + generic port query method (RECOMMENDED)

Add a generic `findByStatus(ProfileId, CashflowMovementStatus)` to `CashflowMovementHistoryPort` — one method that serves all three statuses. The JDBC adapter implements it as `WHERE profile_id = ? AND status = ?`, reusing the existing composite index. Create `HistoryRecommendationService` in the application layer to compute signals, and a thin REST controller that delegates to it.

**Pros:**
- Clean hexagonal separation: business logic in application layer, controller is pure delegation
- One generic port method replaces the need for future status-specific methods
- Existing index `(profile_id, status, movement_date)` covers the query — zero DB migration
- Unit-testable without Spring (mock port)
- Service is reusable if another interface (e.g., admin dashboard, scheduled job) needs the same signals
- Deterministic: counts, sums, percentages from real data — no heuristics, no AI
- Existing port methods (`findPendingManualReviews`, `findProjectionReady`) remain unchanged (backward compatible; could delegate to `findByStatus` later)

**Cons:**
- Adds a port method that overlaps with existing named methods (minor duplication; acceptable for MVP)
- Loads all movements into memory per request (acceptable for SMB data; aggregate queries deferred to future iteration)
- `CashflowMovementStatus.REJECTED` was previously not queryable through the port — adding it is a behavioral expansion, not a breaking change

**Effort:** Low-Medium

---

### 3. Persisted recommendation snapshots

Pre-compute recommendations into a new `history_recommendations` table via a scheduled task or on-write trigger. The API reads snapshots instead of computing on-the-fly.

**Pros:**
- Constant-time reads regardless of history volume
- Could support historical trend analysis over time

**Cons:**
- New DB table + Flyway migration
- Snapshot staleness — recommendations may be outdated between computations
- Scheduling/trigger infrastructure not present in the codebase today
- Overengineered for MVP — SMB history volume is small (hundreds to low thousands of rows)
- Adds persistence complexity and testing surface
- Premature optimization before proving recommendation value

**Effort:** High

**Verdict:** Rejected for MVP. Revisit when history volume reaches tens of thousands of rows or when real-time guarantees weaken.

---

## Recommendation

**Approach 2** is the right choice for MVP:

1. **One port method, one service, one controller** — minimal change surface that respects hexagonal boundaries
2. **Deterministic and practical** — counts, sums, and ratios from real persisted data. No projection run needed. No AI/ML.
3. **Index-covered query** — the existing `(profile_id, status, movement_date)` index supports the new query with zero migration
4. **Response in neutral Spanish** — user-facing severity labels and action hints match the existing API convention
5. **Testable at every layer** — mock port for unit tests, `@JdbcTest` for adapter, `@WebMvcTest` for controller
6. **Review size well under 400 lines** — fits in a single PR

### New Port Method Contract

```java
// CashflowMovementHistoryPort.java (addition)
List<CashflowMovementRecord> findByStatus(ProfileId profileId, CashflowMovementStatus status);
```

The JDBC adapter implements it as:
```sql
SELECT ... FROM cashflow_movement_history
WHERE profile_id = ? AND status = ?
ORDER BY movement_date, created_at
```

This query is covered by `idx_cashflow_movement_history_profile_status_date(profile_id, status, movement_date)`.

### Recommendation Signals (MVP scope)

Six deterministic signals computed from the three status sets:

| Signal | Trigger | Severity | Data source |
|--------|---------|----------|-------------|
| `MANUAL_REVIEW_BACKLOG` | Pending count > 0 | WARNING if >=5, INFO for 1-4 | `findByStatus(MANUAL_REVIEW)` |
| `HIGH_REJECTION_RATE` | Rejected count > 0 AND rejected/(manual-review + projectable + rejected) >= 0.3 | WARNING | `findByStatus(MANUAL_REVIEW)` + `findByStatus(REJECTED)` + `findByStatus(PROJECTABLE)` |
| `CATEGORY_CONCENTRATION` | Any single category >= 60% of PROJECTABLE total amount | INFO | `findByStatus(PROJECTABLE)` grouped by categoryKey |
| `INSUFFICIENT_DATA` | PROJECTABLE count < 10 | INFO | `findByStatus(PROJECTABLE)` |
| `RECENT_INACTIVITY` | No movements (any status) in last 30 days | WARNING | `findByStatus(*)` filtered by date |
| `HEALTHY_HISTORY` | None of the above triggers | INFO | All statuses |

**Default thresholds** (configurable via `application.yml` later, hardcoded for MVP):
- Manual review backlog warning: >= 5
- Rejection rate warning: >= 30% of total persisted history
- Category concentration: >= 60%
- Insufficient data: < 10 PROJECTABLE movements
- Recent inactivity: no movements in 30 days

### Response Contract (neutral Spanish, user-facing)

```json
GET /api/recommendations/history?profileId=pharmacy-cl

{
  "profileId": "pharmacy-cl",
  "generatedAt": "2026-06-18T18:30:00Z",
  "signals": [
    {
      "type": "MANUAL_REVIEW_BACKLOG",
      "severity": "WARNING",
      "title": "Revisión manual pendiente",
      "description": "Tienes 12 movimientos pendientes de revisión por un total de $1.450.000.",
      "actionHint": "Revisa y categoriza estos movimientos para mantener tus proyecciones al día.",
      "metrics": {
        "pendingCount": 12,
        "pendingTotalAmount": "1450000"
      }
    },
    {
      "type": "HEALTHY_HISTORY",
      "severity": "INFO",
      "title": "Historial en buen estado",
      "description": "Tus datos históricos están en buen estado. 45 movimientos proyectables disponibles.",
      "actionHint": "Continúa importando movimientos para mantener tus proyecciones actualizadas.",
      "metrics": {
        "projectableCount": 45
      }
    }
  ]
}
```

**Severity values**: `INFO` (neutral/positive), `WARNING` (attention recommended), `CRITICAL` (reserved for future use — blocked projection, data loss risk, etc.)

**Copy rules**:
- Neutral Spanish, consistent with existing API error messages (`ApiExceptionHandler`)
- Chilean-market neutral (no regional slang)
- Amounts formatted as currency strings without decimals (`$1.450.000`)
- Counts as integers, percentages as integers (`60%`)

### Implementation Sketch

```
GET /api/recommendations/history?profileId={id}

Controller (HistoryRecommendationController):
  1. Validate profileId (non-blank)
  2. Call historyRecommendationService.generate(profileId)
  3. Map domain signals to response DTOs
  4. Return 200 OK

Service (HistoryRecommendationService):
  1. Load profile via VerticalProfileService (validates profile exists)
  2. Load MANUAL_REVIEW movements → compute backlog signal
  3. Load REJECTED movements → compute rejection rate signal
  4. Load PROJECTABLE movements → compute category concentration, insufficient data signals
  5. Combine all movements → compute inactivity signal
  6. If no warning signals triggered → add HEALTHY_HISTORY
  7. Return ordered list (warnings first, then infos)
```

### Edge Cases

| Edge case | Behavior |
|-----------|----------|
| Profile has zero movements | Single `INSUFFICIENT_DATA` signal returned |
| All movements are MANUAL_REVIEW | Backlog warning + insufficient data for projection |
| All movements are REJECTED | High rejection rate + insufficient data |
| Single category has 100% of PROJECTABLE | Category concentration signal triggered |
| `findByStatus` for REJECTED finds zero rows | No rejection signal emitted (not an error) |
| Profile not found | 400 with existing neutral Spanish message pattern |

## Risks

1. **Memory load for large histories (low probability, low impact)**: Loading all movements into memory for computing signals works for SMB volumes (hundreds to low thousands of rows). If a profile accumulates 100K+ movements, this becomes inefficient. Mitigation: add aggregate port methods (`countByStatus`, `sumAmountByStatus`) in a follow-up. Document as known MVP limitation.

2. **Generic `findByStatus` overlaps with existing named methods (low probability, low impact)**: The new `findByStatus` covers the same territory as `findPendingManualReviews` and `findProjectionReady`. Duplication is intentional for MVP — refactoring existing callers to use the generic method is scope creep. The existing methods remain as-is.

3. **Recommendation threshold tuning (medium probability, low impact)**: Hardcoded thresholds (5 pending, 30% rejection, 60% concentration) may not fit all verticals. Mitigation: thresholds are documented as configurable via `application.yml` in a follow-up. The MVP uses sensible defaults for the Chilean pharmacy vertical.

4. **Service name collision with existing `RecommendationService` (low probability, low impact)**: The existing `RecommendationService` is projection-coupled. The new `HistoryRecommendationService` is history-coupled. Both coexist in `application/recommendation/` with clear naming. No architectural conflict.

5. **REJECTED movements returning potentially sensitive `safe_description` (low probability, high impact)**: REJECTED movements have `safe_description = NULL` by design (the ingestion service sets it to null for rejected items — see `IngestionOutcome.rejected()` in `CashflowIngestionService`). The service naturally avoids leaking sensitive data. The response DTOs only expose counts and amounts for rejected movements, never descriptions.

6. **`findByStatus` with `REJECTED` is a behavioral expansion (low probability, low impact)**: Previously, REJECTED movements were only queryable by ID or source reference. Making them queryable by status is a net-new capability but does not break existing behavior. It does mean rejected volume is now visible in recommendations — this is the intended feature.

## Review Size Forecast

| Component | Estimated lines |
|-----------|----------------|
| `CashflowMovementHistoryPort` (+ `findByStatus`) | ~5 |
| `CashflowMovementHistoryJdbcAdapter` (+ method) | ~18 |
| `HistoryRecommendationService` | ~90 |
| `HistoryRecommendationController` (+ DTOs) | ~100 |
| `ApplicationServiceConfiguration` (+ bean) | ~5 |
| `HistoryRecommendationServiceTest` (unit) | ~120 |
| `CashflowMovementHistoryJdbcAdapterTest` (+ new tests) | ~40 |
| `HistoryRecommendationControllerTest` (`@WebMvcTest`) | ~80 |
| **Total** | **~458** |

At ~458 lines, the change is **slightly above the 400-line review budget**. Chained PR split is recommended:

- **PR1**: Port interface + JDBC adapter + adapter tests (~63 lines, infrastructure foundation)
- **PR2**: Application service + controller + DTOs + config + service test + controller test (~395 lines, feature delivery)

PR1 can be reviewed independently as a pure infrastructure addition. PR2 depends on PR1 but contains all business logic and interface code. Both PRs stay under 400 lines.

## Ready for Proposal

**Yes** — The exploration confirms:
1. The data exists in `cashflow_movement_history` and is sufficient for deterministic recommendations
2. The only gap is a missing `findByStatus` port method (or at minimum `findRejected`)
3. Approach 2 (application service + generic port method) is the clean hexagonal fit
4. Six concrete recommendation signals are defined with trigger conditions, severity levels, and neutral Spanish copy
5. Zero DB migration needed — the existing composite index covers the new query
6. Review size is manageable with a 2-PR chained split
7. No AI/ML, no projection run, no snapshotting — pure deterministic computation from persisted data
