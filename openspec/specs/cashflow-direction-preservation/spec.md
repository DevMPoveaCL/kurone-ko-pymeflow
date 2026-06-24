# Cashflow Direction Preservation

## Purpose

Model, store, and return bank movement direction (`DEBIT`/`CREDIT`) separately from category direction (`INFLOW`/`OUTFLOW`/`TRANSFER`), keeping amounts as positive magnitudes.

## Requirements

### Requirement: Distinct Direction Enums

The system MUST define `TransactionDirection` with values `DEBIT` and `CREDIT`. The system MUST keep `CashflowDirection` with values `INFLOW`, `OUTFLOW`, and `TRANSFER` for category semantics. The two enums MUST NOT be conflated.

#### Scenario: Domain enums are separate

- GIVEN a cashflow domain model
- WHEN reviewed
- THEN `TransactionDirection` and `CashflowDirection` exist as separate enums

### Requirement: Positive Amount Preservation

The system MUST store and process all cashflow amounts as positive magnitudes. The sign of a bank amount MUST be encoded in `TransactionDirection`, not in the amount field.

#### Scenario: Debit preserves positive amount

- GIVEN a bank row with `amount = -15000`
- WHEN ingested
- THEN the persisted movement has `amount = 15000` and `movementDirection = DEBIT`

#### Scenario: Credit preserves positive amount

- GIVEN a bank row with `amount = 15000`
- WHEN ingested
- THEN the persisted movement has `amount = 15000` and `movementDirection = CREDIT`

### Requirement: Persistence Storage

The system MUST persist `movementDirection` on every cashflow movement record. The column MUST be `NOT NULL` with a default of `CREDIT`.

#### Scenario: New movement stores direction

- GIVEN a valid ingestion item with `movementDirection = DEBIT`
- WHEN persisted
- THEN the movement record stores `movementDirection = DEBIT`

#### Scenario: Migration defaults existing rows

- GIVEN existing cashflow movements created before this change
- WHEN the migration runs
- THEN all existing rows receive `movementDirection = CREDIT`

### Requirement: Mismatch Tolerance

The system MUST allow `movementDirection` and `CashflowDirection` to differ for the same movement. The system MUST NOT auto-correct mismatches.

#### Scenario: Debit with INFLOW category is allowed

- GIVEN a movement with `movementDirection = DEBIT` and category `INFLOW`
- WHEN validated or projected
- THEN the system does not modify either direction and does not reject the movement

### Requirement: Smoke Test Expectations

The system MUST include smoke tests verifying that movements with `movementDirection = DEBIT` and `CREDIT` are persisted and retrievable with correct direction in history responses.

#### Scenario: Smoke test covers both directions

- GIVEN smoke tests run after deployment
- WHEN a DEBIT and a CREDIT movement are imported and queried
- THEN both movements retain their original `movementDirection` end-to-end
