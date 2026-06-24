# Design: Simulated Bank Import OpenAPI Examples

## Technical Approach

This is a documentation-only controller annotation change. Add a concrete OpenAPI request-body example for `POST /api/cashflow/imports/bank-statement/simulated` and protect the critical contract with the existing reflection-style documentation test. No DTO, validation, service, adapter, or response behavior changes are required.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|----------|--------|-------------------------|-----------|
| Example location | Attach one full request example to the controller endpoint request body. | Add only per-field `@Schema` examples. | The consumer mistake is about full payload shape and field nesting; a full example reduces recall burden. |
| Test strategy | Extend `CashflowBankStatementSimulatedControllerTest` docs assertions via reflection. | Fetch `/v3/api-docs` in a larger WebMvc OpenAPI test. | Existing tests already verify docs annotations; reflection keeps this small and stable. |
| Behavior boundary | Change annotations/tests only. | Rename DTO fields or relax validation. | Baseline behavior already requires `bookingDate` and row-level `accountAlias`; the problem is discoverability. |

## Data Flow

No runtime data flow changes.

    Controller annotation ──→ springdoc OpenAPI generation ──→ API consumer docs
             │
             └── reflection docs test guards example shape

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedController.java` | Modify | Add OpenAPI request-body example showing `profileId`, optional `importLabel`, and `rows[]` with `bookingDate` and row-level `accountAlias`. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Modify | Update docs test to assert the example includes the correct fields and rejects misleading `bookedAt`/root-level `accountAlias`. |
| `openspec/changes/pymeflow-simulated-bank-import-openapi-examples/specs/cashflow-bank-statement-import/spec.md` | Create | Delta spec for the OpenAPI example contract. |

## Interfaces / Contracts

No API contract changes. The documented example must reflect the current request shape:

```json
{
  "profileId": "pharmacy-cl",
  "importLabel": "Cartola junio 2026",
  "rows": [
    {
      "bankTransactionId": "BT-100",
      "bookingDate": "2026-06-15",
      "description": "Venta POS",
      "amount": 125000,
      "currency": "CLP",
      "accountAlias": "Cuenta corriente",
      "counterpartyName": "Cliente mostrador"
    }
  ]
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Documentation unit | OpenAPI annotation exposes a request example with `bookingDate`, `rows`, and row-level `accountAlias`. | Reflection assertions in `CashflowBankStatementSimulatedControllerTest`. |
| Regression | Example excludes `bookedAt` and root-level `accountAlias`. | Parse or inspect the example string in the same docs test. |
| Integration | Existing endpoint behavior remains unchanged. | Existing WebMvc tests continue covering request handling. |

## Migration / Rollout

No migration required. Deploy as a docs-only API annotation update.

## Open Questions

- None
