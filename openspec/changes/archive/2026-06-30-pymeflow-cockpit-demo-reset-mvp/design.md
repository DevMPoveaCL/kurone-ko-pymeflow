# Design: Cockpit Demo Reset MVP

## Technical Approach

Add a hexagonal demo-reset capability scoped to the active profile (`pharmacy-cl`). A single transactional service orchestrates cleanup of profile-scoped transactional tables, then seeds deterministic fixture data via existing persistence ports. One controller exposes `POST /api/cockpit/demo/reset-and-seed`. The cockpit static UI adds a “Reiniciar demo” button that triggers the endpoint and refreshes evidence.

## Architecture Decisions

| Decision | Choice | Alternatives | Rationale |
|---|---|---|---|
| Endpoint | Single `POST /api/cockpit/demo/reset-and-seed` | Separate reset + seed endpoints | Simpler UX, atomic operation, matches proposal intent |
| Transaction boundary | `@Transactional` service method | Adapter-level TX, manual TX | Spring declarative TX is codebase standard; full rollback on any seed failure |
| Cleanup scope | Profile-scoped `DELETE` on 3 tables | `TRUNCATE`, broader `DELETE` | Safer; reference tables untouched; respects FKs |
| Seed mechanism | Direct port calls with deterministic `CashflowMovementDraft`s | Re-use full ingestion service | Deterministic outcome; avoids categorization variability between runs |
| Adapter implementation | `JdbcTemplate` parameterized `DELETE` | JPA repository | Matches existing JDBC adapter pattern; project has no JPA |
| Review delivery | Chained PRs recommended | Single PR | Forecast ~400 lines; split (1) backend API + tests, (2) cockpit UI + smoke |

## Data Flow

```
Cockpit JS ──POST──→ DemoDataController
                          │
                          ▼
              CockpitDemoResetService (@Transactional)
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
     DemoDataPort   CashflowMovement   SyncSessionPort /
     (reset)        HistoryPort         CockpitPreferencesPort
          │        (saveAll)            (seed)
          ▼
     JdbcDemoDataAdapter
          │
          ▼
     DELETE FROM cashflow_movement_history
     DELETE FROM provider_sync_sessions
     DELETE FROM cockpit_preferences
     WHERE profile_id = ?
```

## File Changes

| File | Action | Description |
|---|---|---|
| `application/port/out/DemoDataPort.java` | Create | Profile-scoped reset output port |
| `application/cockpit/CockpitDemoResetService.java` | Create | Transactional orchestration + deterministic seed logic |
| `infrastructure/demo/JdbcDemoDataAdapter.java` | Create | JDBC `DELETE` implementation; wired in config |
| `interfaces/web/DemoDataController.java` | Create | `POST /api/cockpit/demo/reset-and-seed` with validation |
| `resources/static/index.html` | Modify | Add “Reiniciar demo” button in actions panel |
| `resources/static/app.js` | Modify | Wire button, safe status states, call `refreshCockpitEvidence()` on success |
| `infrastructure/config/ApplicationServiceConfiguration.java` | Modify | Wire `JdbcDemoDataAdapter` and `CockpitDemoResetService` beans |
| `test/.../web/DemoDataControllerTest.java` | Create | `@WebMvcTest` for endpoint, validation, and safe response fields |
| `test/.../demo/JdbcDemoDataAdapterTest.java` | Create | `@JdbcTest` proving DELETE is profile-scoped |
| `test/.../cockpit/CockpitDemoResetServiceTest.java` | Create | Unit test for orchestration order and seed determinism |
| `test/.../web/CockpitStaticResourceTest.java` | Modify | Assert reset button presence, JS wiring, demo-only copy |

## Interfaces / Contracts

```java
package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

public interface DemoDataPort {
    void reset(ProfileId profileId);
}
```

```java
package com.kuroneko.pymeflow.interfaces.web;

public record DemoResetResponse(
    String status,
    int movementsSeeded,
    String syncSessionId
) {}
```

Controller contract:
```java
@PostMapping("/api/cockpit/demo/reset-and-seed")
public ResponseEntity<DemoResetResponse> resetAndSeed(
    @RequestParam @NotBlank String profileId
)
```

Seed dataset (deterministic per profile):
- 3 `PROJECTABLE` movements (categorized: sales, acquirer-settlements, suppliers)
- 2 `MANUAL_REVIEW` movements (uncategorized: rent, utilities)
- 1 `COMPLETED` sync session snapshot with fixture counts
- Preferences: `openingBalance = 350000`, `preferredHorizonDays = 7`

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Service orchestration order, seed determinism, profile validation | JUnit 5 + Mockito; assert port interactions |
| Integration | Adapter DELETE is scoped to profile only, leaves other profiles intact | `@JdbcTest` with H2 schema setup |
| Integration | Controller returns safe fields, rejects missing/unknown profile | `@WebMvcTest` |
| Static smoke | Button copy, JS endpoint wiring, safe status messages | `@SpringBootTest` + `MockMvc` content assertions |

## Migration / Rollout

No database migration required. Reset is runtime `DELETE` + `INSERT` within the existing schema. Rollback is code-only revert.

## Open Questions

- [ ] Should the endpoint reject non-demo profiles explicitly, or is active-profile validation sufficient?
- [ ] Should seed data include a `REJECTED` movement to exercise that state, or keep the cockpit focused on review + projection?
