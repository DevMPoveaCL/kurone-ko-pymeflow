# Delta for Cashflow Provider Sync

## ADDED Requirements

### Requirement: Safe Provider Sync Lifecycle Logs

The system MUST emit structured lifecycle logs for provider sync start, page progress, completion, and failure using safe correlation fields only.

Allowed log fields are `syncId`, `providerType`, `profileId`, lifecycle `event`, `status`, safe counts, duration, and stable `errorCode`. Logs MUST NOT include credentials, raw provider payloads, cursors, stack traces, or raw exception messages.

#### Scenario: Successful sync emits safe lifecycle logs

- GIVEN a fixture-backed provider sync completes successfully
- WHEN lifecycle logs are captured
- THEN start, progress, and completion events contain safe correlation fields
- AND no secret, raw payload, cursor, stack trace, or raw exception message appears

#### Scenario: Failed sync emits sanitized failure log

- GIVEN provider sync fails with an internal exception
- WHEN the failure event is logged
- THEN the log contains a stable `errorCode` and safe status
- AND the raw exception message and stack trace are not logged

### Requirement: Bounded Provider Sync Metrics

The system MUST publish Micrometer metrics for provider sync attempts and durations using bounded tags only: `providerType`, `status`, and `errorCode`.

Metrics MUST NOT tag `syncId`, `profileId`, raw messages, exception text, payload values, cursors, or any unbounded identifier.

#### Scenario: Metrics record successful sync with bounded tags

- GIVEN a provider sync completes successfully
- WHEN metrics are inspected
- THEN attempt and duration measurements include `providerType`, `status`, and `errorCode` only
- AND `syncId`, `profileId`, raw message, and cursor tags are absent

#### Scenario: Metrics record failed sync without high-cardinality tags

- GIVEN provider sync fails with a provider error
- WHEN metrics are inspected
- THEN the failure is tagged by stable `providerType`, `status`, and `errorCode`
- AND no raw exception or per-sync identifier is used as a tag

### Requirement: Storage-Only Actuator Health and Info

The system MUST expose provider sync actuator health/info limited to durable session storage reachability and safe subsystem capability metadata.

Health/info MUST NOT claim external bank, sandbox, production provider, credential, or network connectivity.

#### Scenario: Storage reachable reports subsystem readiness

- GIVEN durable provider sync session storage is reachable
- WHEN actuator health/info is requested
- THEN provider sync reports storage reachability and safe capability metadata
- AND no external provider connectivity claim is present

#### Scenario: Storage unreachable reports degraded subsystem health

- GIVEN durable provider sync session storage is unreachable
- WHEN actuator health is requested
- THEN provider sync reports a storage-only degraded or down state
- AND it does not infer bank/provider availability

### Requirement: Observability MVP Boundary

The system MUST keep this MVP limited to observability for the existing fixture-backed trigger/status behavior.

This change MUST NOT add UI, list/audit APIs, manual retry, real bank dependencies, credential flows, or public cashflow API expansion beyond existing trigger/status behavior.

#### Scenario: No operator action surface is introduced

- GIVEN the observability MVP is delivered
- WHEN public API and UI surfaces are reviewed
- THEN no UI, list/audit API, or manual retry capability is added

#### Scenario: No real provider dependency is introduced

- GIVEN provider sync observability is active
- WHEN sync runs in this MVP
- THEN telemetry describes fixture-backed sync behavior only
- AND no sandbox, production bank, credential, or provider network dependency is required
