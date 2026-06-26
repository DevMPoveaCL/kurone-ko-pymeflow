# Delta for pymeflow-mvp-cockpit

## ADDED Requirements

### Requirement: Period Projection Controls

The cockpit MUST provide 7-day and 30-day projection controls.

#### Scenario: Select projection period
- GIVEN the projection section is visible
- WHEN the user selects a period control
- THEN the cockpit requests a projection for that period with the current opening balance

### Requirement: Opening Balance Input

The cockpit MUST provide an opening balance input with copy stating it is user-entered, not from a live bank.

#### Scenario: Enter opening balance
- GIVEN the projection section is visible
- WHEN the user enters an opening balance
- THEN the label states the balance is manual, not bank-provided

#### Scenario: Opening balance required
- GIVEN the user has not entered an opening balance
- WHEN the user attempts to request a projection
- THEN the cockpit prevents the request and prompts for the balance

### Requirement: Cockpit Projection Endpoint

The system MUST expose a read-only `GET /api/cashflow/cockpit/projection` endpoint accepting `profileId`, `startDate`, `horizonDays`, and `openingBalance`. It MUST use persisted `PROJECTABLE` movements and the existing projection service.

#### Scenario: Valid projection request
- GIVEN valid parameters and existing `PROJECTABLE` movements
- WHEN the endpoint is called
- THEN HTTP 200 returns projection data with daily balances, totals, and alerts

#### Scenario: No projectable movements
- GIVEN no `PROJECTABLE` movements exist for the profile
- WHEN the endpoint is called
- THEN HTTP 200 returns an empty projection and a categorization signal

### Requirement: Projection Rendering

The cockpit MUST render daily balances, closing balance, abonos total, cargos total, obligations total, and alerts. It MUST preserve `DEBIT`/`CREDIT` direction and positive CLP amounts.

#### Scenario: Projection results render
- GIVEN a successful projection response
- WHEN the cockpit renders the projection section
- THEN daily balances, totals, closing balance, and alerts are visible
- AND movement direction and amounts remain positive CLP values

### Requirement: Empty State for No Projectable Movements

The cockpit MUST show an empty state guiding the user to categorize movements when no `PROJECTABLE` movements exist.

#### Scenario: No projectable empty state
- GIVEN the projection response indicates no projectable movements
- WHEN the cockpit renders the projection section
- THEN a neutral Spanish message explains that movements must be categorized to project cashflow

### Requirement: Projection Smokeability

The system MUST be smoke-testable for the projection flow.

#### Scenario: Smoke verifies projection flow
- GIVEN the application is running with fixture data
- WHEN smoke exercises the projection controls and API
- THEN daily balances, totals, alerts, and safe empty states are visible
- AND no real bank connectivity claims are shown

## MODIFIED Requirements

### Requirement: Movement Evidence and Review Data

The cockpit MUST use existing read APIs for active profile, recommendations, projection-ready history, pending manual review, and provider sync status. It MAY add one read-only cockpit projection endpoint for the active profile when required by the projection feature.

(Previously: The cockpit SHOULD add no backend endpoint; if blocked, the only permitted addition is one read-only safe movement summary for the active profile.)

#### Scenario: Direction is visible across movement evidence
- GIVEN persisted movements include DEBIT and CREDIT entries with positive CLP amounts
- WHEN history and review sections render
- THEN visible movement rows show `DEBIT` or `CREDIT`
- AND amounts remain positive CLP values.

#### Scenario: Missing optional data degrades safely
- GIVEN one existing read API returns an empty list or safe validation error
- WHEN the cockpit renders that section
- THEN the page shows an actionable Spanish empty/error state
- AND the rest of the cockpit remains usable.

#### Scenario: Projection endpoint is permitted
- GIVEN the projection feature requires a new read-only endpoint
- WHEN the cockpit requests projection data
- THEN the read-only projection endpoint is called
- AND no other new backend endpoints are introduced.

## REMOVED Requirements

None.
