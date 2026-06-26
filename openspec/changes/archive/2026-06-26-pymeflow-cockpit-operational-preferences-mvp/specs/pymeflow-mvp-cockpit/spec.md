# Delta for pymeflow-mvp-cockpit

> Scope boundary: This delta MUST NOT introduce auth, multi-user, tenant model, broad settings module, or localStorage-only persistence.

## MODIFIED Requirements

### Requirement: Period Projection Controls

The cockpit MUST provide 7-day and 30-day projection controls. The cockpit MUST load the preferred projection period from persisted preferences on startup and pre-fill the control when available.
(Previously: Only required providing controls; did not require loading or persisting the selected period.)

#### Scenario: Select projection period

- GIVEN the projection section is visible
- WHEN the user selects a period control
- THEN the cockpit requests a projection for that period with the current opening balance

#### Scenario: Load persisted projection period

- GIVEN saved preferred_horizon_days of 30 for the active profile
- WHEN the cockpit initializes
- THEN the 30-day control is pre-selected

### Requirement: Opening Balance Input

The cockpit MUST provide an opening balance input with copy stating it is user-entered, not from a live bank. The cockpit MUST load the persisted opening balance on startup and auto-save changes with debounce.
(Previously: Only required providing the input with honest copy; did not require persisting or restoring the value.)

#### Scenario: Enter opening balance

- GIVEN the projection section is visible
- WHEN the user enters an opening balance
- THEN the label states the balance is manual, not bank-provided

#### Scenario: Opening balance required

- GIVEN the user has not entered an opening balance
- WHEN the user attempts to request a projection
- THEN the cockpit prevents the request and prompts for the balance

#### Scenario: Load persisted opening balance

- GIVEN a saved opening balance of 1500000 for the active profile
- WHEN the cockpit initializes
- THEN the input is pre-filled with 1500000
- AND the copy still states the balance is manual

#### Scenario: Auto-save preference change

- GIVEN the user changes the opening balance or period
- WHEN the debounce period elapses
- THEN the cockpit persists the new value via PUT

## ADDED Requirements

### Requirement: Cockpit Preference Persistence API

The system MUST expose GET and PUT /api/cockpit/preferences endpoints scoped to the active profile. The backend MUST validate preferred_horizon_days is exactly 7 or 30, and opening_balance is a safe numeric value.

#### Scenario: Load preferences

- GIVEN preferences exist for the active profile
- WHEN GET /api/cockpit/preferences is called
- THEN HTTP 200 returns opening_balance and preferred_horizon_days

#### Scenario: Save preferences

- GIVEN valid preference values
- WHEN PUT /api/cockpit/preferences is called
- THEN HTTP 200 persists the values durably

#### Scenario: Reject invalid horizon days

- GIVEN a PUT request with preferred_horizon_days of 15
- WHEN the backend validates
- THEN HTTP 400 returns a safe validation error

#### Scenario: Reject unsafe opening balance

- GIVEN a PUT request with an extreme or non-numeric opening_balance
- WHEN the backend validates
- THEN HTTP 400 returns a safe validation error

### Requirement: Preference Safe Defaults

The system MUST provide safe defaults when no preferences exist for a profile.

#### Scenario: Empty preferences

- GIVEN no preferences exist for the active profile
- WHEN the cockpit initializes
- THEN the opening balance input is empty
- AND the period control defaults to 7 days

### Requirement: Durable Preference Persistence

Preferences MUST survive application restart.

#### Scenario: Preferences survive restart

- GIVEN preferences have been saved
- WHEN the application restarts
- THEN a subsequent GET returns the previously saved values

### Requirement: Preference Smokeability

The system MUST be smoke-testable for preference load, save, and validation.

#### Scenario: Smoke verifies preference round-trip

- GIVEN the application is running with saved preferences
- WHEN smoke opens the cockpit, changes values, and refreshes
- THEN controls reflect the persisted values after reload
