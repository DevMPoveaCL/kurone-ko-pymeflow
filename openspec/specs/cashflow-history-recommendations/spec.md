# Cashflow History Recommendations

## Purpose

Deterministic recommendations generated from persisted cashflow movement history.

## Requirements

### Requirement: Recommendations Endpoint

The system MUST expose recommendations for a requested profile using persisted cashflow history.

#### Scenario: Valid profile returns recommendation response

- GIVEN a request with `profileId`
- WHEN recommendations are requested
- THEN HTTP 200 is returned with deterministic recommendation items

#### Scenario: Missing profile id

- GIVEN a request without `profileId`
- WHEN recommendations are requested
- THEN HTTP 400 is returned with a validation error

### Requirement: History Response Direction Exposure

The system MUST include `movementDirection` in history and projection-ready responses for each movement.

#### Scenario: History entry includes direction

- GIVEN a persisted movement with `movementDirection = DEBIT`
- WHEN the movement appears in a history response
- THEN the response includes `movementDirection = DEBIT`

### Requirement: Direction Mismatch Recommendation

The system MUST generate a recommendation when movements exist where `movementDirection` and category `CashflowDirection` mismatch. The recommendation MUST use aggregate counts and MUST NOT expose sensitive data.

#### Scenario: Mismatch triggers recommendation

- GIVEN at least one movement has `movementDirection = DEBIT` and category `INFLOW`
- WHEN recommendations are generated
- THEN an informational recommendation highlights the direction mismatch using aggregate counts only

#### Scenario: No mismatch yields no signal

- GIVEN all movements have aligned direction and category
- WHEN recommendations are generated
- THEN no direction mismatch recommendation is returned

### Requirement: Manual Review Backlog

The system MUST recommend resolving pending manual reviews when the manual-review backlog reaches the configured MVP threshold.

#### Scenario: Manual review threshold reached

- GIVEN at least 5 `MANUAL_REVIEW` movements for the profile
- WHEN recommendations are generated
- THEN a warning recommendation asks the user to resolve pending reviews

### Requirement: Rejection Rate Signal

The system MUST recommend improving source data quality when rejected movements represent at least 30 percent of total persisted movements.

#### Scenario: Rejection rate threshold reached

- GIVEN rejected movements are at least 30 percent of total history
- WHEN recommendations are generated
- THEN a warning recommendation asks the user to review sensitive or invalid source data

### Requirement: Category Concentration Signal

The system MUST recommend reviewing category concentration when one category represents at least 60 percent of projectable cashflow amount.

#### Scenario: Category concentration threshold reached

- GIVEN one projectable category represents at least 60 percent of projectable amount
- WHEN recommendations are generated
- THEN an informational recommendation highlights the concentration

### Requirement: Data Sufficiency

The system MUST report insufficient data when there are no projectable movements or history is empty.

#### Scenario: Empty history

- GIVEN no persisted movements exist for the profile
- WHEN recommendations are generated
- THEN an `INSUFFICIENT_DATA` recommendation is returned

#### Scenario: No projectable movements

- GIVEN history exists but no `PROJECTABLE` movements exist
- WHEN recommendations are generated
- THEN an `INSUFFICIENT_DATA` recommendation is returned

### Requirement: Safe Recommendation Content

The system MUST NOT expose rejected descriptions, source references, or sensitive identifiers in recommendation responses.

#### Scenario: Rejected movement safety

- GIVEN rejected movements exist
- WHEN recommendations are generated
- THEN recommendations use aggregate counts only and do not include rejected row descriptions or source references

### Requirement: Stateless Generation

The system MUST NOT persist recommendation snapshots in the MVP.

#### Scenario: Repeated request

- GIVEN the same history data
- WHEN recommendations are requested repeatedly
- THEN recommendations are recomputed deterministically and no recommendation snapshot is persisted

### Requirement: Spanish Response Copy

The system MUST return user-facing severity labels, titles, and actions in neutral Spanish suitable for the Chilean market.

#### Scenario: Response copy

- GIVEN any recommendation is returned
- WHEN the client reads the response
- THEN the recommendation includes a neutral Spanish title and action
