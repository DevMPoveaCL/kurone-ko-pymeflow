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

The system MUST track sync sessions behind `SyncSessionPort` with `syncId`, cursor state, `lastSyncAt`, entry counts, provider errors, and a safe durable status snapshot retrievable by `syncId` across application restarts.

#### Scenario: Session updated after each page

- GIVEN a successful page fetch
- WHEN the page is imported
- THEN the sync session stores the next cursor and increments counts durably

#### Scenario: Session available for resume

- GIVEN a previous sync stored a cursor for the same profile and provider
- WHEN a new sync starts after adapter or application restart
- THEN the previous cursor is used to resume pagination

#### Scenario: Session status is durable

- GIVEN a sync status was recorded before application restart
- WHEN status is requested after restart with the same `syncId`
- THEN the stored snapshot is returned with persistent durability semantics

#### Scenario: Entry counts avoid lost updates

- GIVEN multiple page imports update the same sync session
- WHEN counts are recorded separately from cursor updates
- THEN the final durable count MUST include all imported entries

### Requirement: Durable Session Storage Migration

The system MUST provision durable provider sync session storage before the application uses the JDBC-backed session adapter.

#### Scenario: Migration creates durable session storage

- GIVEN the application database starts with provider sync migrations enabled
- WHEN migrations run successfully
- THEN storage exists for sync id, profile/provider identity, status, cursor, counts, timestamps, durability, and safe errors

#### Scenario: Migration failure prevents false durability

- GIVEN provider sync session storage cannot be migrated
- WHEN the application attempts to start durable provider sync support
- THEN startup or sync wiring MUST fail safely rather than reporting durable status from incomplete storage

### Requirement: Provider Sync Trigger API

The system MUST expose a synchronous fixture-backed trigger endpoint at `POST /api/cashflow/provider-syncs`.

| Request field | Rule |
|---|---|
| `profileId`, `providerType`, `credentialRef`, `dateFrom`, `dateTo` | Required and validated before sync |
| Credentials | MUST be references only; raw secrets MUST NOT be accepted or echoed |
| Provider | MUST use fixture provider behavior only for this MVP |

#### Scenario: Trigger returns safe sync report

- GIVEN a valid trigger request for the fixture provider
- WHEN the client posts to `/api/cashflow/provider-syncs`
- THEN the response contains `syncId`, counts, cursor/session metadata, provider type, completion flags, and durable/persistent durability
- AND no credential material is present

#### Scenario: Invalid trigger request is rejected safely

- GIVEN missing fields, invalid dates, or unsupported provider values
- WHEN the client posts the request
- THEN the API returns a validation error DTO with safe field-level details
- AND sync is not invoked

### Requirement: Provider Sync Status API

The system MUST expose `GET /api/cashflow/provider-syncs/{syncId}` to inspect the last safe durable status snapshot for a sync.

#### Scenario: Status lookup returns last snapshot

- GIVEN a sync has completed and its session was persisted
- WHEN the client requests its `syncId`
- THEN the response contains status, counts, cursor/session metadata, provider errors, retry hints, and durable/persistent durability

#### Scenario: Unknown status returns safe not found

- GIVEN the `syncId` has never been recorded or durable storage has no matching session
- WHEN the client requests status
- THEN the API returns a safe not-found response without implying an in-memory restart limitation

### Requirement: Safe Provider Error DTOs

The system MUST map and persist provider failures to stable API DTOs containing only safe `code`, `message`, `field`, and `retryAfterSeconds` values where applicable.

#### Scenario: Provider failure hides internals

- GIVEN provider sync records auth, rate-limit, unavailable, or data errors
- WHEN the API returns the trigger report or status
- THEN errors are normalized to safe DTO fields
- AND provider internals and credentials are not exposed

#### Scenario: Persisted errors remain safe after restart

- GIVEN a sync records provider errors before application restart
- WHEN status is requested after restart
- THEN the returned errors contain only safe DTO fields and no raw credentials, provider payloads, stack traces, or internal exception details

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
