# Cockpit Demo Data Specification

## Purpose

Define the demo-only reset and seed contract for the active profile.

## Requirements

### Requirement: Demo Reset and Seed Endpoint

The system MUST expose `POST /api/cockpit/demo/reset-and-seed`. It MUST validate the active profile and execute reset and seed atomically.

#### Scenario: Successful reset and seed

- GIVEN the active demo profile has existing demo data
- WHEN `POST /api/cockpit/demo/reset-and-seed` is called
- THEN the response is HTTP 200 with a success indicator
- AND the cockpit data reflects the newly seeded state

#### Scenario: Non-demo profile rejected

- GIVEN the active profile is not a demo profile
- WHEN the endpoint is called
- THEN HTTP 403 returns a safe error indicating demo-only access

### Requirement: Scoped Data Deletion

Reset MUST delete only active profile demo transactional data. It MUST NOT touch reference tables, global data, or other profiles.

#### Scenario: Scoped deletion

- GIVEN two profiles have demo data
- WHEN reset runs for profile A
- THEN profile A demo data is removed
- AND profile B data and reference tables remain intact

### Requirement: Deterministic Seed

Seed MUST create deterministic fixture data including projection-ready movements, manual-review movements, provider sync status, and cockpit preferences.

#### Scenario: Seed populates projectable movements

- GIVEN reset completed for the active profile
- WHEN seed runs
- THEN `PROJECTABLE` movements exist for the profile

#### Scenario: Seed populates manual review movements

- GIVEN reset completed for the active profile
- WHEN seed runs
- THEN `MANUAL_REVIEW` movements exist for the profile

#### Scenario: Seed populates sync status

- GIVEN reset completed for the active profile
- WHEN seed runs
- THEN provider sync session status is available

#### Scenario: Seed populates preferences

- GIVEN reset completed for the active profile
- WHEN seed runs
- THEN cockpit preferences exist with safe defaults

### Requirement: Safe Error Reporting

Errors MUST be safe and contain no secrets, stack traces, or provider payloads.

#### Scenario: Seed failure is safe

- GIVEN seed encounters an error
- WHEN the endpoint responds
- THEN HTTP 500 returns a generic safe message
- AND no internal details are leaked

### Requirement: Smokeability

The reset and seed flow MUST be smoke-testable.

#### Scenario: Smoke verifies cycle

- GIVEN the application is running
- WHEN smoke calls reset-and-seed and reads cockpit data
- THEN seeded movements, status, and preferences are visible
