# Design: Cockpit Operational Preferences MVP

## Technical Approach

Add durable per-profile cockpit preferences (opening balance, horizon days) via a small hexagonal slice: Flyway table, pure domain record, application port, JDBC adapter, REST controller, and static-JS debounced auto-save. The JS loads preferences on startup, pre-fills controls, and calls `PUT /api/cashflow/cockpit/preferences` on changes. Projection copy remains explicitly manual/non-bank.

## Architecture Decisions

| Decision | Options | Tradeoffs | Choice |
|----------|---------|-----------|--------|
| Upsert strategy | `INSERT … ON CONFLICT` vs read-then-write | ON CONFLICT is atomic and idempotent; read-then-write risks lost updates | `ON CONFLICT UPDATE` in JDBC adapter |
| Horizon validation | DB `CHECK` vs application-only | DB CHECK is durable; app validation gives better errors | Both: DB `CHECK (horizon_days IN (7,30))` + app pre-validation |
| Preference granularity | Single row per profile vs key-value | Single row is simpler for exactly two fields; key-value is over-engineered for MVP | Single row per `profile_id` |
| JS debounce | 500 ms vs 1000 ms | 500 ms feels responsive; 1000 ms reduces save noise | 500 ms debounce on balance/period change |
| UI state model | Dedicated preference state slice vs reuse `state.projection` | Reuse keeps changes minimal; dedicated slice is cleaner | Extend `state.projection` with `openingBalance` and add `preferencesLoaded` flag |

## Data Flow

```
  Static JS
     │ fetch GET /api/cashflow/cockpit/preferences
     │ pre-fill #opening-balance + horizonDays radio
     │ on change ──debounce(500ms)──► PUT preferences
     │                                │
     ▼                                ▼
REST Controller ──► Application Service ──► CockpitPreferencesPort
                                                  │
                                                  ▼
                                       JdbcCockpitPreferencesAdapter
                                                  │
                                                  ▼
                                        cockpit_preferences (Flyway V6)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/resources/db/migration/V6__create_cockpit_preferences.sql` | Create | `cockpit_preferences(profile_id PK/FK, opening_balance NUMERIC(18,2), preferred_horizon_days INTEGER CHECK IN (7,30), created_at, updated_at)` |
| `src/main/java/com/kuroneko/pymeflow/domain/cockpit/CockpitPreferences.java` | Create | Pure domain record with validation in compact constructor |
| `src/main/java/com/kuroneko/pymeflow/application/port/out/CockpitPreferencesPort.java` | Create | Output port: `Optional<CockpitPreferences> findByProfile(ProfileId)`, `save(ProfileId, CockpitPreferences)` |
| `src/main/java/com/kuroneko/pymeflow/application/cockpit/CockpitPreferencesService.java` | Create | Application service with `@Transactional` boundary, delegates to port |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/persistence/JdbcCockpitPreferencesAdapter.java` | Create | JDBC adapter with idempotent upsert (`INSERT … ON CONFLICT UPDATE`) and load |
| `src/main/java/com/kuroneko/pymeflow/interfaces/web/CockpitPreferencesController.java` | Create | `GET`/`PUT /api/cashflow/cockpit/preferences` with DTO records and validation |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfiguration.java` | Modify | Add beans: `CockpitPreferencesService`, `CockpitPreferencesPort` adapter |
| `src/main/resources/static/app.js` | Modify | Startup load, pre-fill controls, debounced auto-save, keep manual-balance copy |
| `src/main/resources/static/index.html` | Modify | Minor copy-only if needed (already states manual) |
| `src/test/java/…/infrastructure/persistence/JdbcCockpitPreferencesAdapterTest.java` | Create | H2-backed JDBC adapter contract tests |
| `src/test/java/…/interfaces/web/CockpitPreferencesControllerTest.java` | Create | `@WebMvcTest` for GET/PUT validation and Spanish error messages |
| `src/test/java/…/application/cockpit/CockpitPreferencesServiceTest.java` | Create | Unit tests for service boundary validation |
| `src/test/java/…/interfaces/web/CockpitPreferencesSmokeTest.java` | Create | `@SpringBootTest` end-to-end: save → reload → assert controls |

## Interfaces / Contracts

### REST API

- `GET /api/cashflow/cockpit/preferences?profileId={id}`
  - 200: `{ "openingBalance": 350000, "preferredHorizonDays": 7 }`
  - 404: profile not found (or no preferences yet — return defaults with 200)
- `PUT /api/cashflow/cockpit/preferences`
  - Body: `{ "profileId": "pharmacy-cl", "openingBalance": 350000, "preferredHorizonDays": 7 }`
  - 200: updated preferences
  - 400: validation errors in Spanish

### Application Port

```java
public interface CockpitPreferencesPort {
    Optional<CockpitPreferences> findByProfile(ProfileId profileId);
    void save(ProfileId profileId, CockpitPreferences preferences);
}
```

### Domain Record

```java
public record CockpitPreferences(BigDecimal openingBalance, int preferredHorizonDays) {
    public CockpitPreferences {
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo.");
        }
        if (preferredHorizonDays != 7 && preferredHorizonDays != 30) {
            throw new IllegalArgumentException("El horizonte debe ser 7 o 30 días.");
        }
    }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Domain validation, service boundary rules | Plain JUnit + AssertJ |
| Integration | JDBC adapter upsert/load, SQL contract | H2 in-memory with Flyway V6 migration populator |
| Integration | Controller validation, Spanish errors, shape | `@WebMvcTest` with mocked service |
| E2E / Smoke | Save preferences → GET → static page reload → controls pre-filled | `@SpringBootTest` hitting real beans + in-memory DB |

## Migration / Rollout

1. Flyway V6 runs automatically on startup (existing Flyway baseline).
2. No data migration needed — table starts empty; UI falls back to defaults (empty balance, 7 days).
3. Rollback: revert code changes and remove `V6__create_cockpit_preferences.sql` before release. If already released, add `V7__drop_cockpit_preferences.sql`; projection still works with manual input.

## Review-Budget Split Recommendation

Forecast exceeds 400 changed lines.

- **PR 1 (backend)**: Flyway V6 + domain + port + service + JDBC adapter + controller + config wiring + backend tests. ~250 lines.
- **PR 2 (frontend + smoke)**: `app.js` load/debounce/save, `index.html` copy if needed, smoke test. ~150 lines.

Decision needed before apply: Yes  
Chained PRs recommended: Yes  
400-line budget risk: Medium

## Open Questions

- None
