# Proposal: Simulated Bank Import OpenAPI Examples

## Intent

Make the simulated bank-statement import OpenAPI docs show the correct request shape so API consumers use `bookingDate` and put `accountAlias` inside each `rows[]` item.

## Scope

### In Scope
- Add a focused request-body example for `POST /api/cashflow/imports/bank-statement/simulated`.
- Keep the example aligned with the existing DTO contract: row-level `bookingDate` and `accountAlias`.
- Add/update a docs-focused test that fails if the OpenAPI example drifts to `bookedAt` or root-level `accountAlias`.

### Out of Scope
- DTO, validation, mapping, or endpoint behavior changes.
- New endpoints, UI changes, real bank integrations, or schema-wide documentation cleanup.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `cashflow-bank-statement-import`: documents the OpenAPI request example contract for the simulated bank import endpoint.

## Approach

Add an OpenAPI request-body `ExampleObject` at the controller method parameter or operation level, using the existing Spring Boot/springdoc annotation pattern. Extend the current reflection-based controller documentation test to assert that the example includes `rows`, `bookingDate`, and row-level `accountAlias`, and excludes `bookedAt`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedController.java` | Modified | Add explicit OpenAPI request example only. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` | Modified | Guard the request example shape. |
| `openspec/changes/pymeflow-simulated-bank-import-openapi-examples/specs/cashflow-bank-statement-import/spec.md` | New | Delta spec for documentation contract. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Swagger `@RequestBody` import conflicts with Spring `@RequestBody` | Medium | Use fully qualified OpenAPI annotation or clear import aliases. |
| String example drifts from DTO fields | Medium | Reflection test asserts critical field names and nesting. |
| Accidental behavior change | Low | Limit production change to annotations and run focused controller tests. |

## Rollback Plan

Revert the controller annotation/test changes and remove this change folder before archive. Runtime behavior is unaffected.

## Dependencies

- Existing `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0` dependency.

## Success Criteria

- [ ] OpenAPI docs show a complete request example with `bookingDate` in each row.
- [ ] `accountAlias` appears inside each row, not at the request root.
- [ ] Focused docs test prevents `bookedAt` or wrong nesting from returning.
