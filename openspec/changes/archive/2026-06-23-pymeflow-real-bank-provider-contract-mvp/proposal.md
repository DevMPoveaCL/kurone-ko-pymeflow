# Proposal: PymeFlow Real Bank Provider Contract MVP

## Intent

Prepare PymeFlow for future real bank integrations by defining a provider-agnostic PULL contract above the existing `ExternalStatementImportPort`, without storing real credentials or calling production bank APIs.

## Scope

### In Scope
- Add provider fetch contracts for date windows, pagination cursors, auth descriptors, rate-limit hints, and normalized errors.
- Add a sync use case that orchestrates provider fetches into the existing external statement import boundary.
- Add fixture/fake provider tests proving the contract, mapping, pagination, error taxonomy, and session traceability.

### Out of Scope
- Real credential storage, OAuth/token rotation, encryption, or secret persistence.
- UI, scheduled sync, webhooks, or production bank/sandbox API dependency.
- Domain model changes or replacement of the existing simulated bank-statement import.

## Capabilities

### New Capabilities
- `cashflow-provider-sync`: Provider-agnostic statement sync contract and orchestration for fixture-backed PULL imports.

### Modified Capabilities
- None.

## Approach

Use the exploration recommendation: **Provider Port + Sync Use Case + Error Taxonomy**. Define Java 21 records/sealed types in `application/port/out`, keep provider literals in `infrastructure/provider`, and wire Spring beans through configuration. Validate with fixture contract tests; split implementation into chained PRs under the 400-line review budget.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/.../application/port/out` | New | Provider port, query/page/auth/error contracts. |
| `src/main/java/.../application/cashflow` | New | Sync use case and report/session orchestration. |
| `src/main/java/.../infrastructure/provider` | New | Fixture-backed fake provider adapter only. |
| `src/main/java/.../infrastructure/config` | Modified | Bean/configuration properties wiring. |
| `src/test/...` and `src/test/resources/fixtures/provider` | New | Contract, adapter, use-case, and fixture tests. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Contract may differ from future bank APIs | High | Keep contract minimal and cursor/date-window based. |
| Fixtures miss real API edge cases | High | Treat as boundary proof; add sandbox tests later. |
| In-memory sync state loses cursors | Med | Hide behind session port for future JDBC swap. |

## Rollback Plan

Remove the new provider contracts, fake adapter, sync use case, wiring, fixtures, and delta spec. Existing manual/simulated import paths remain unchanged because `ExternalStatementImportPort`, `SimulatedBankStatementAdapter`, and `domain/` are not modified.

## Dependencies

- Existing `ExternalStatementImportPort` and cashflow ingestion behavior.
- Existing Java 21, Spring Boot 3, JUnit/Mockito/ArchUnit test stack.

## Success Criteria

- [ ] Provider contracts model auth descriptor, pagination, normalized errors, rate-limit hints, and sync traceability without real secrets.
- [ ] Fixture adapter contract tests validate mapped `ExternalStatementEntry` pages and safe provider errors.
- [ ] Sync use case imports fetched pages through the existing anti-corruption boundary and preserves provider-agnostic layering.
