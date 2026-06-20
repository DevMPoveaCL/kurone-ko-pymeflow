# Proposal: PymeFlow Simulated Bank Statement Adapter MVP

## Intent

Support the roadmap bank-integration step with a simulated bank-statement import that proves the anti-corruption boundary before real providers. The MVP must ingest bank-like rows without leaking provider/bank concepts into domain or application core.

## Scope

### In Scope
- `POST /api/cashflow/imports/bank-statement/simulated` endpoint for JSON rows: `profileId`, optional `importLabel`, `rows[{bankTransactionId, bookingDate, description, amount, currency, accountAlias, counterpartyName}]`.
- Bank-agnostic application port/model: `ExternalStatementImportPort`, `ExternalStatementImportCommand`, `ExternalStatementEntry`.
- Infrastructure adapter mapping simulated bank rows to existing ingestion items and delegating to `CashflowIngestionService.ingest(...)`.
- Response contract mirroring manual import: `importId`, `profileId`, counts, `categorized`, `manualReview`, `rejected`, `errors`, with row traceability.

### Out of Scope
- Real bank APIs, OAuth, balances, account sync, persisted bank batches, direction model overhaul.
- Multi-currency support beyond CLP.

## Capabilities

### New Capabilities
- `cashflow-bank-statement-import`: Simulated bank-statement-shaped cashflow import through an infrastructure anti-corruption adapter.

### Modified Capabilities
- None.

## Approach

Use the recommended Port + Infrastructure Anti-Corruption Adapter, with one correction: keep the application port name bank-agnostic (`ExternalStatementImportPort`) while confining bank naming to `interfaces/web` and `infrastructure/bank`.

Mapping rules: `bankTransactionId` → `externalReference`; signed `amount.abs()` → positive transaction amount; `counterpartyName` prepends/enriches description when present; `accountAlias` is dropped for MVP; only `CLP` accepted. `bankTransactionId` is mandatory for idempotency; no fingerprint fallback for this endpoint. Sensitive-data handling remains delegated to existing ingestion policy and must not echo sensitive values.

Accepted tradeoff: debit/credit direction is lost when signed amounts become positive. Cashflow direction hardening is required in a future domain iteration.

Implementation will use chained PRs because the forecast exceeds 400 review lines: contracts/tests, adapter, then controller/tests.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `application/port/out/ExternalStatementImportPort.java` | New | Bank-agnostic import contract |
| `infrastructure/bank/SimulatedBankStatementAdapter.java` | New | Anti-corruption mapping and ingestion delegation |
| `interfaces/web/CashflowBankStatementSimulatedController.java` | New | Simulated bank statement endpoint and row validation |
| Tests | New | Port, adapter, WebMvc, ArchUnit smoke coverage |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Direction/sign loss | High | Document MVP tradeoff; later add transaction/draft direction |
| Description enrichment affects categorization | Med | Preserve original description as substring |
| Review size exceeds budget | Med | Force chained PRs under ~400 lines |

## Rollback Plan

Remove the new endpoint, port, adapter, tests, and `cashflow-bank-statement-import` spec delta. No DB rollback is required because the MVP adds no migrations or persisted batch state.

## Dependencies

- Existing `CashflowIngestionService`, idempotency, sensitive-data policy, categorization, and history ports.

## Success Criteria

- [ ] Mixed valid/invalid simulated statement batches return row-level results.
- [ ] Re-importing the same `bankTransactionId` is idempotent.
- [ ] Non-CLP, blank IDs, zero amounts, and sensitive values are handled safely.
- [ ] Domain/application remain provider-agnostic; existing architecture tests pass.
