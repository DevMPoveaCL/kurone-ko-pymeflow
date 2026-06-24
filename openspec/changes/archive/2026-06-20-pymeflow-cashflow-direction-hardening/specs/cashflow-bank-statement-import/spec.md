# Delta for Cashflow Bank Statement Import

## ADDED Requirements

### Requirement: Signed Amount Direction Mapping

The system MUST map the sign of the bank statement `amount` to `movementDirection`: negative amounts MUST map to `DEBIT`, and positive amounts MUST map to `CREDIT`.

#### Scenario: Negative amount maps to DEBIT

- GIVEN a row with `amount = -15000`
- WHEN mapped through the anti-corruption adapter
- THEN the ingestion item has `movementDirection = DEBIT` and `amount = 15000`

#### Scenario: Positive amount maps to CREDIT

- GIVEN a row with `amount = 15000`
- WHEN mapped through the anti-corruption adapter
- THEN the ingestion item has `movementDirection = CREDIT` and `amount = 15000`

### Requirement: Response Direction Exposure

The system MUST include `movementDirection` in bank-statement import responses where relevant.

#### Scenario: Successful import result includes direction

- GIVEN a bank statement row is imported successfully
- WHEN the response is returned
- THEN the result entry for that row includes `movementDirection`

## MODIFIED Requirements

### Requirement: Anti-Corruption Mapping

The system MUST translate bank-like rows into `IngestionItem` through a bank-agnostic application port (`ExternalStatementImportPort`), mapping `bankTransactionId` to `externalReference`, `amount.abs()` to positive `amount`, sign to `movementDirection` (`DEBIT` for negative, `CREDIT` for positive), `bookingDate` to `date`, and prepending `counterpartyName` to `description` when present. `accountAlias` MUST be dropped for MVP.
(Previously: Direction was lost; only positive amount was preserved.)

#### Scenario: Signed debit mapped to DEBIT with positive amount

- GIVEN a row with `amount = -15000`
- WHEN mapped
- THEN the ingestion item has `amount = 15000` and `movementDirection = DEBIT`

#### Scenario: Signed credit mapped to CREDIT with positive amount

- GIVEN a row with `amount = 15000`
- WHEN mapped
- THEN the ingestion item has `amount = 15000` and `movementDirection = CREDIT`

#### Scenario: Counterparty enriches description

- GIVEN a row with `description = "Pago"` and `counterpartyName = "Farmacia"`
- WHEN mapped
- THEN the ingestion item has `description = "Farmacia | Pago"`

#### Scenario: Missing counterparty preserves original description

- GIVEN a row with `description = "Pago"` and no `counterpartyName`
- WHEN mapped
- THEN the ingestion item has `description = "Pago"`

## REMOVED Requirements

### Requirement: Direction Loss Documentation

(Reason: Direction is now preserved via movementDirection; the accepted MVP tradeoff is superseded.)
