# Delta for Cashflow Provider Sync

## ADDED Requirements

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
- THEN the response contains `syncId`, counts, cursor/session metadata, provider type, and completion flags
- AND no credential material is present

#### Scenario: Invalid trigger request is rejected safely

- GIVEN missing fields, invalid dates, or unsupported provider values
- WHEN the client posts the request
- THEN the API returns a validation error DTO with safe field-level details
- AND sync is not invoked

### Requirement: Provider Sync Status API

The system MUST expose `GET /api/cashflow/provider-syncs/{syncId}` to inspect the last safe in-memory status snapshot for a sync.

#### Scenario: Status lookup returns last snapshot

- GIVEN a sync has completed in the current process
- WHEN the client requests its `syncId`
- THEN the response contains status, counts, cursor/session metadata, provider errors, and retry hints

#### Scenario: Unknown or expired status returns safe not found

- GIVEN the `syncId` is unknown or lost after process restart
- WHEN the client requests status
- THEN the API returns a safe not-found response explaining the in-memory MVP limitation

### Requirement: Safe Provider Error DTOs

The system MUST map provider failures to stable API DTOs containing only safe `code`, `message`, `field`, and `retryAfterSeconds` values where applicable.

#### Scenario: Provider failure hides internals

- GIVEN provider sync records auth, rate-limit, unavailable, or data errors
- WHEN the API returns the trigger report or status
- THEN errors are normalized to safe DTO fields
- AND provider internals and credentials are not exposed

## MODIFIED Requirements

### Requirement: Sync Session Traceability

The system MUST track sync sessions behind `SyncSessionPort` with `syncId`, cursor state, `lastSyncAt`, entry counts, provider errors, and a safe in-memory status snapshot retrievable by `syncId` for the current process only.
(Previously: sessions tracked cursor state and counts for resume, but status lookup by `syncId` and explicit non-durable API status semantics were not required.)

#### Scenario: Session updated after each page

- GIVEN a successful page fetch
- WHEN the page is imported
- THEN the sync session stores the next cursor and increments counts

#### Scenario: Session available for resume

- GIVEN a previous sync stored a cursor
- WHEN a new sync starts for the same profile and provider
- THEN the previous cursor is used to resume pagination

#### Scenario: Session status is non-durable

- GIVEN a sync status exists only in memory
- WHEN the application process restarts
- THEN status lookup for the previous `syncId` is not guaranteed

## REMOVED Requirements

None.
