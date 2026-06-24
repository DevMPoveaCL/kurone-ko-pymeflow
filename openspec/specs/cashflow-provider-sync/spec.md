# Cashflow Provider Sync

## Purpose

Provider-agnostic PULL contract for fetching bank statements and orchestrating their ingestion through the existing anti-corruption boundary.

## Requirements

### Requirement: Provider Pull Contract

The system MUST define `BankProviderPort` in `application/port/out` accepting `ProviderSyncQuery` and `ProviderAuth`, returning `ProviderSyncPage`.

| Type | Fields | Constraints |
|------|--------|-------------|
| `ProviderSyncQuery` | `profileId`, `dateFrom`, `dateTo`, `cursor`, `pageSize` | `dateFrom` ≤ `dateTo`; `pageSize` > 0; non-null profile/dates |
| `ProviderAuth` | `providerType`, `credentialRef` | Config key only; no secrets |
| `ProviderSyncPage` | `entries`, `nextCursor`, `totalPagesEstimate`, `rateLimitResetsAt` | `entries` immutable list; `nextCursor` empty when last page |

#### Scenario: Fetch within date window returns page

- GIVEN a valid `ProviderSyncQuery` with a date window and page size
- WHEN `fetchStatements` is invoked
- THEN a `ProviderSyncPage` with entries and optional `nextCursor` is returned

#### Scenario: Invalid date window rejected

- GIVEN `dateFrom` is after `dateTo`
- WHEN the query is constructed
- THEN `IllegalArgumentException` is thrown

#### Scenario: Missing required fields rejected

- GIVEN null `profileId`, `dateFrom`, or `dateTo`
- WHEN the query is constructed
- THEN `IllegalArgumentException` is thrown

### Requirement: Provider Error Taxonomy

The system MUST model provider failures as a sealed `ProviderError` hierarchy permitting exhaustive `switch`: `AuthError`, `RateLimitError`, `UnavailableError`, `DataError`.

#### Scenario: Auth failure aborts sync

- GIVEN the provider returns `AuthError`
- WHEN the sync use case processes the page
- THEN sync aborts and the error is recorded in the sync report

#### Scenario: Rate limit surfaces retry hint

- GIVEN the provider returns `RateLimitError` with `retryAfterSeconds`
- WHEN the error is normalized
- THEN the report contains the retry hint

#### Scenario: Unavailable error collected with partial continue

- GIVEN the provider returns `UnavailableError`
- WHEN the sync use case processes
- THEN the error is collected and sync continues if partial data is acceptable

#### Scenario: Data error maps field and detail

- GIVEN the provider returns `DataError` for a field
- WHEN normalized
- THEN the report contains the field name and safe detail

### Requirement: Sync Use Case Orchestration

The system MUST provide `ProviderSyncUseCase` that orchestrates fetch → import → report across paginated results.

#### Scenario: Multi-page sync imports all entries

- GIVEN the provider returns two pages with a `nextCursor`
- WHEN sync runs
- THEN both pages are fetched and imported via `ExternalStatementImportPort`

#### Scenario: Single-page sync completes

- GIVEN the provider returns one page with empty `nextCursor`
- WHEN sync runs
- THEN entries are imported and `hasMorePages` is false

#### Scenario: Max page guard prevents infinite loop

- GIVEN the provider returns pages beyond the configured max limit
- WHEN sync reaches the limit
- THEN sync stops and the report indicates truncated pages

### Requirement: Sync Session Traceability

The system MUST track sync sessions behind `SyncSessionPort` with `syncId`, cursor state, `lastSyncAt`, and entry counts.

#### Scenario: Session updated after each page

- GIVEN a successful page fetch
- WHEN the page is imported
- THEN the sync session stores the next cursor and increments counts

#### Scenario: Session available for resume

- GIVEN a previous sync stored a cursor
- WHEN a new sync starts for the same profile and provider
- THEN the previous cursor is used to resume pagination

### Requirement: Fixture-Backed Adapter Validation

The system MUST include `FakeBankProviderAdapter` that loads JSON fixtures and maps them to `ExternalStatementEntry`.

#### Scenario: Fixture mapped to entries with direction

- GIVEN a fixture JSON with signed amount
- WHEN the adapter loads and maps it
- THEN `ExternalStatementEntry` has correct `direction`, positive `amount`, and `externalReference`

#### Scenario: Missing fixture returns empty page

- GIVEN a fixture file is missing
- WHEN the adapter loads
- THEN an empty `ProviderSyncPage` is returned

#### Scenario: CLP-only enforcement in adapter

- GIVEN a fixture with currency other than CLP
- WHEN the adapter maps
- THEN a `DataError` is returned for that entry

### Requirement: Integration with Existing Import Boundary

The system MUST delegate mapped entries to `ExternalStatementImportPort` without modifying the existing anti-corruption boundary.

#### Scenario: Entries flow to ingestion idempotency

- GIVEN fetched entries with `externalReference`
- WHEN imported through the existing port
- THEN idempotency and categorization behave as for manual import

## Out of Scope

- Real credential storage, OAuth, token rotation, encryption.
- UI, scheduled sync, webhooks, production bank API dependency.
- Domain model changes or replacement of simulated bank-statement import.
