# Delta for Cashflow Provider Sync

## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Sync Session Traceability

The system MUST track sync sessions behind `SyncSessionPort` with `syncId`, cursor state, `lastSyncAt`, entry counts, provider errors, and a safe durable status snapshot retrievable by `syncId` across application restarts.
(Previously: status snapshots were safe but in-memory and current-process only.)

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

### Requirement: Provider Sync Status API

The system MUST expose `GET /api/cashflow/provider-syncs/{syncId}` to inspect the last safe durable status snapshot for a sync.
(Previously: the endpoint inspected only the current process in-memory snapshot.)

#### Scenario: Status lookup returns last snapshot

- GIVEN a sync has completed and its session was persisted
- WHEN the client requests its `syncId`
- THEN the response contains status, counts, cursor/session metadata, provider errors, retry hints, and durable/persistent durability

#### Scenario: Unknown status returns safe not found

- GIVEN the `syncId` has never been recorded or durable storage has no matching session
- WHEN the client requests status
- THEN the API returns a safe not-found response without implying an in-memory restart limitation

#### Scenario: Restart does not lose status

- GIVEN the application restarts after a successful fixture-backed sync
- WHEN the client requests the previous `syncId`
- THEN the API returns the persisted status instead of a restart-related not found response

### Requirement: Safe Provider Error DTOs

The system MUST map and persist provider failures to stable API DTOs containing only safe `code`, `message`, `field`, and `retryAfterSeconds` values where applicable.
(Previously: provider failures were mapped safely but only needed to survive in the in-memory session snapshot.)

#### Scenario: Provider failure hides internals

- GIVEN provider sync records auth, rate-limit, unavailable, or data errors
- WHEN the API returns the trigger report or status
- THEN errors are normalized to safe DTO fields
- AND provider internals and credentials are not exposed

#### Scenario: Persisted errors remain safe after restart

- GIVEN a sync records provider errors before application restart
- WHEN status is requested after restart
- THEN the returned errors contain only safe DTO fields and no raw credentials, provider payloads, stack traces, or internal exception details

### Requirement: Provider Sync Trigger API

The system MUST expose a synchronous fixture-backed trigger endpoint at `POST /api/cashflow/provider-syncs`.
(Previously: trigger responses could report in-memory session durability.)

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

## REMOVED Requirements

### Requirement: In-memory provider sync status semantics

(Reason: Provider sync status MUST no longer be described as current-process only for the durable session MVP.)
