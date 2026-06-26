# Design: Cockpit Period Cash Projection MVP

## Technical Approach

Add a read-only `GET` endpoint that sources persisted `PROJECTABLE` movements via `CashflowMovementHistoryService.projectionReady()`, maps them to `ProjectedCashflowTransaction`, builds a `CashflowProjectionCommand`, and delegates to the existing `CashflowProjectionService.project()`. The static cockpit UI adds period controls (7 d/30 d/custom), an opening-balance input, and renders daily balances, totals, obligations, alerts, and safe empty/error states in neutral Chilean Spanish.

## Architecture Decisions

| Decision | Options | Tradeoffs | Choice |
|---|---|---|---|
| Controller location | A) Reuse `CashflowProjectionController` B) New `CockpitProjectionController` | A keeps projection semantics together; B isolates cockpit churn. | **A**, per proposal, to keep related routes discoverable. |
| Orchestration layer | A) Controller calls history + projection service directly B) Thin `CockpitProjectionService` use case | A violates hexagonal boundary (adapter orchestrates); B keeps controller thin and testable. | **B** |
| UI period model | A) Vanilla JS state object B) New JS module | A matches current static app; B adds file overhead with no build pipeline. | **A** |
| Response reuse | A) Reuse `CashflowProjectionResponse` B) New cockpit-specific DTO | A avoids duplication; B decouples evolution. | **A**, because the shape is identical. |
| Currency for GET | A) Add `currency` query param B) Default to CLP in use case | A is explicit; B reduces UI friction in CLP-only MVP. | **B** with an open question for future multi-currency support. |

## Data Flow

```
User input (balance, horizon)
       │
       ▼
Static JS ──GET──► CashflowProjectionController
                       │
                       ▼
         CockpitProjectionService (application)
           ├─► CashflowMovementHistoryService
           │       └─► CashflowMovementHistoryPort
           │               └─► DB (PROJECTABLE rows)
           │
           └─► CashflowProjectionService.project()
                       │
                       ▼
         CashflowProjectionResult ──► JSON response ──► JS render
```

## File Changes

| File | Action | Description |
|---|---|---|
| `interfaces/web/CashflowProjectionController.java` | Modify | Add `GET /api/cashflow/cockpit/projection` with query param mapping and validation. |
| `application/cashflow/CockpitProjectionService.java` | Create | Orchestrates history fetch → command build → projection delegate. Defaults currency to CLP. |
| `application/cashflow/CockpitProjectionServiceTest.java` | Create | Unit tests for orchestration, empty-history path, and command construction. |
| `interfaces/web/CashflowProjectionControllerTest.java` | Modify | Add `@WebMvcTest` cases for validation, happy path, empty state, and neutral Spanish errors. |
| `resources/static/index.html` | Modify | Projection controls (balance input, 7 d/30 d/custom selectors) and results markup section. |
| `resources/static/app.js` | Modify | Period state, `fetch` call to new endpoint, render functions for table/list, totals, alerts, obligations, and safe empty/error states. |
| `resources/static/styles.css` | Modify | Responsive projection panel, table/list layout, and alert/obligation chips following existing token system. |

## Interfaces / Contracts

```java
// Existing reused types:
// CashflowProjectionCommand, CashflowProjectionResult,
// DailyProjectedBalance, AppliedObligation, ProjectionAlert

// New application orchestrator
@Service
public final class CockpitProjectionService {
    public CashflowProjectionResult projectFromHistory(
            ProfileId profileId,
            BigDecimal openingBalance,
            LocalDate startDate,
            int horizonDays
    ) { ... }
}

// Controller addition
@GetMapping("/cockpit/projection")
public ResponseEntity<CashflowProjectionResponse> cockpitProjection(
        @RequestParam @NotBlank String profileId,
        @RequestParam @NotNull @PositiveOrZero BigDecimal openingBalance,
        @RequestParam @NotNull LocalDate startDate,
        @RequestParam @Positive int horizonDays
) { ... }
```

**Endpoint**: `GET /api/cashflow/cockpit/projection?profileId={}&openingBalance={}&startDate={}&horizonDays={}`

**Response shape**: Reuses `CashflowProjectionResponse` (list of `DailyProjectedBalanceResponse`, `closingProjectedBalance`, `appliedObligations`, `alerts`).

**Error states**: 400 with neutral Spanish messages for invalid profile, negative balance, non-positive horizon, or missing dates; 200 with empty `dailyBalances` when no `PROJECTABLE` movements exist (UI shows safe empty state).

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | `CockpitProjectionService` maps history to command and handles empty history | JUnit 5 + Mockito, AssertJ |
| Integration | `GET` endpoint validation, response shape, neutral Spanish error messages | `@WebMvcTest(CashflowProjectionController.class)` + MockMvc |
| ArchUnit | No domain/framework coupling, no application→infrastructure imports | Existing ArchUnit suite |
| Smoke | Cockpit projection section renders with controls and states | Manual Playwright MCP script (documented, not automated in CI) |

## Migration / Rollout

No migration required. Feature is additive.

## Open Questions

- [ ] Should the maximum `horizonDays` be capped at the backend (e.g., 90 days) to prevent abuse?
- [ ] Do we expose a custom date-range picker in this MVP, or only 7 d/30 d presets?
- [ ] Should `currency` become an optional query param now, or wait until multi-currency is a real requirement?
