# Design: PymeFlow MVP Frontend Review

## Technical Approach

Add a zero-build Spring Boot static cockpit at `/` using `src/main/resources/static/index.html`, `styles.css`, and `app.js`. The UI remains a client of REST APIs only: active profile/categories, provider sync trigger/status, manual import, manual-review history, projection-ready history, and recommendations. Backend hexagonal boundaries stay intact. A consolidated movement read endpoint is designed only as the smallest fallback if current history APIs cannot provide enough smokeable movement evidence.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Frontend delivery | Spring Boot static resources, no app build | Vite/React, server-rendered templates | Smallest MVP surface; avoids npm for runtime; keeps UI separate from domain/application code. |
| UI data contract | `fetch` existing REST endpoints first | New dashboard aggregate API | Existing controllers already expose the flows; adding aggregation early would hide backend behavior the MVP must prove. |
| Backend gap handling | Create `GET /api/cashflow/history/movements` only if blocked | Expanding provider sync response or projection APIs | A read-only history projection is the least invasive contract and maps to existing persisted `CashflowMovementRecord`. |
| Test tooling | Existing JUnit/WebMvc plus optional pnpm Playwright smoke | Full E2E suite or Gradle-wired Node tasks | One browser smoke proves user-visible MVP without large toolchain/review weight. |

## Data Flow

```text
Browser cockpit
  ├─ GET /api/profiles/active(+categories) ──→ profile header/filter labels
  ├─ POST /api/cashflow/provider-syncs ─────→ receipt rail sync voucher
  ├─ POST /api/cashflow/imports/manual ─────→ receipt rail import voucher
  ├─ GET /api/cashflow/history/manual-review ┐
  ├─ GET /api/cashflow/history/projection-ready ├─→ ledger evidence + caja totals
  └─ GET /api/cashflow/recommendations ──────┘
```

Errors render as safe Spanish messages from API responses. `credentialRef` is sent only as fixture reference and never displayed.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/resources/static/index.html` | Create | Semantic one-screen cockpit shell with accessible regions/forms. |
| `src/main/resources/static/styles.css` | Create | Identity tokens, receipt rail, ledger, states, responsive layout. |
| `src/main/resources/static/app.js` | Create | Small explicit fetch/render functions; no framework/global complexity. |
| `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowHistoryController.java` | Modify-if-needed | Add read-only movements endpoint for consolidated safe evidence. |
| `src/main/java/com/kuroneko/pymeflow/application/cashflow/CashflowMovementHistoryService.java` | Modify-if-needed | Expose recent safe movements while preserving profile validation. |
| `src/main/java/com/kuroneko/pymeflow/application/port/out/CashflowMovementHistoryPort.java` | Modify-if-needed | Add query for recent movements by profile/date/limit. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/persistence/CashflowMovementHistoryJdbcAdapter.java` | Modify-if-needed | Implement read-only SQL projection ordered by date/created time. |
| `src/test/java/.../CashflowHistoryControllerTest.java` | Modify-if-needed | Verify safe fields, direction, positive CLP, filters. |
| `package.json`, `pnpm-lock.yaml`, `playwright.config.ts`, `tests/cockpit/*` | Create-if-needed | Minimal smoke only if automated browser verification is required. |

## Interfaces / Contracts

Primary UI calls existing contracts. Fallback read model, if needed:

```http
GET /api/cashflow/history/movements?profileId=pharmacy-cl&startDate=2026-06-01&endDate=2026-06-30&limit=25
```

```json
[{"movementId":"uuid","amount":125000,"currency":"CLP","date":"2026-06-15","movementDirection":"CREDIT","status":"PROJECTABLE","categoryKey":"sales","description":"Venta POS","sourceReference":"BT-100"}]
```

## Identity Direction

- **Domain**: caja diaria, cartola bancaria, abonos/cargos, proveedores, farmacia chilena, conciliación, sincronización durable.
- **Color world**: papel boleta, tinta bancaria azul, verde CLP para abonos, cobre/ámbar de alerta, grafito de timbre, celulosa suave.
- **Signature**: cashflow receipt rail; each import/sync appears as a voucher with stamp, counts, `ABONO/CREDIT`, `CARGO/DEBIT`, and durability.
- **Defaults rejected**: KPI-card grid → receipt rail + ledger evidence; generic sidebar dashboard → single operational cockpit; vague “income/expense” copy → caja/abonos/cargos for Chile.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | JS formatters/classifiers for CLP and direction | Keep pure functions small; test manually unless tooling added. |
| Integration | Optional endpoint + existing controllers | JUnit/WebMvc tests following current controller style. |
| E2E | Cockpit loads, triggers sync/import, shows positive CLP with DEBIT/CREDIT | Optional pnpm Playwright smoke with role/label selectors. |

## Migration / Rollout

No migration required. Static assets are removable. Optional read endpoint is read-only and rollback-safe.

## Open Questions

- [ ] None blocking. Decide during implementation whether existing history endpoints are enough or fallback movement read model is required.
