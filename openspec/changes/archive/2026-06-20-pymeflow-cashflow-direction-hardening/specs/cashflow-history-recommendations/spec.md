# Delta for Cashflow History Recommendations

## ADDED Requirements

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
