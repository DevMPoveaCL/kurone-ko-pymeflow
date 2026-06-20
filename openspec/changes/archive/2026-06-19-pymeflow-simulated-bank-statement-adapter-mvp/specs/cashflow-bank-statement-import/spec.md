# Cashflow Bank Statement Import

## Purpose

Simulated bank-statement-shaped cashflow import through an infrastructure anti-corruption adapter, proving the provider boundary before real bank integrations.

## Requirements

### Requirement: Simulated Bank Statement Endpoint

The system MUST expose `POST /api/cashflow/imports/bank-statement/simulated` accepting `profileId`, optional `importLabel`, and `rows` containing `bankTransactionId`, `bookingDate`, `description`, `amount` (signed), `currency`, `accountAlias`, and optional `counterpartyName`.

#### Scenario: Valid statement accepted

- GIVEN a request with `profileId` and valid bank rows
- WHEN posted to the endpoint
- THEN HTTP 200 is returned with a summary response

#### Scenario: Empty rows rejected

- GIVEN a request with an empty `rows` array
- WHEN posted
- THEN HTTP 400 is returned

### Requirement: Anti-Corruption Mapping

The system MUST translate bank-like rows into `IngestionItem` through a bank-agnostic application port (`ExternalStatementImportPort`), mapping `bankTransactionId` to `externalReference`, `amount.abs()` to positive `amount`, `bookingDate` to `date`, and prepending `counterpartyName` to `description` when present. `accountAlias` MUST be dropped for MVP.

#### Scenario: Signed debit mapped to positive amount

- GIVEN a row with `amount = -15000`
- WHEN mapped
- THEN the ingestion item has `amount = 15000`

#### Scenario: Counterparty enriches description

- GIVEN a row with `description = "Pago"` and `counterpartyName = "Farmacia"`
- WHEN mapped
- THEN the ingestion item has `description = "Farmacia | Pago"`

#### Scenario: Missing counterparty preserves original description

- GIVEN a row with `description = "Pago"` and no `counterpartyName`
- WHEN mapped
- THEN the ingestion item has `description = "Pago"`

### Requirement: Idempotency via bankTransactionId

The system MUST use `bankTransactionId` as the `externalReference` for idempotency after validation. Re-importing the same `bankTransactionId` MUST return the existing movement ID.

#### Scenario: Re-import returns existing movement

- GIVEN a row with `bankTransactionId = "BT-123"` was previously imported
- WHEN the same row is submitted again
- THEN the existing movement ID is returned

### Requirement: Sensitive Data Protection

The system MUST reject rows containing a sensitive `bankTransactionId` and MUST NOT echo the sensitive value in errors or responses.

#### Scenario: Sensitive transaction ID rejected safely

- GIVEN a row with a sensitive `bankTransactionId`
- WHEN validated
- THEN the row is rejected and the error message does not contain the sensitive value

### Requirement: Direction Loss Documentation

The system MUST document that debit/credit direction is lost when signed amounts are converted to positive values. This is an accepted MVP tradeoff.

#### Scenario: Direction loss is explicit

- GIVEN a row with `amount = -5000` and another with `amount = 5000`
- WHEN both are processed
- THEN both result in positive `amount = 5000` with no direction distinction

### Requirement: CLP-Only Currency Enforcement

The system MUST reject any row with a currency other than CLP.

#### Scenario: Non-CLP row rejected

- GIVEN a row with `currency = "USD"`
- WHEN validated
- THEN the row is rejected with a CLP-only error

### Requirement: Row-Level Validation and Partial Success

The system MUST validate every row independently; invalid rows MUST NOT block valid rows. `bankTransactionId` MUST be non-blank and unique within the request. Duplicate nonblank `bankTransactionId` values in the same request MUST be rejected as row-level errors without echoing the duplicated value. `amount` MUST be non-zero. `bookingDate` MUST be valid ISO. `description` MUST be non-blank.

#### Scenario: Mixed valid and invalid rows

- GIVEN a batch where row 1 is valid and row 2 has a blank `bankTransactionId`
- WHEN posted
- THEN row 1 is persisted and row 2 returns an error

#### Scenario: All rows invalid

- GIVEN a batch where every row fails validation
- WHEN posted
- THEN HTTP 400 is returned with all row errors and zero accepted rows

#### Scenario: Duplicate transaction IDs rejected with partial success

- GIVEN a batch where rows 1 and 3 share the same nonblank `bankTransactionId` and row 2 is unique and valid
- WHEN posted
- THEN rows 1 and 3 return safe row-level duplicate ID errors without echoing the duplicated value, and row 2 is persisted

#### Scenario: All rows duplicate and invalid

- GIVEN a batch where every row has a duplicated nonblank `bankTransactionId`
- WHEN posted
- THEN HTTP 400 is returned with duplicate ID errors for every row, zero accepted rows, and no adapter delegation

#### Scenario: Zero amount rejected

- GIVEN a row with `amount = 0`
- WHEN validated
- THEN the row is rejected with a non-zero amount error

### Requirement: Response Row Traceability

The system MUST return a response mirroring the manual import contract: `importId`, `profileId`, counts (`accepted`, `categorizedCount`, `manualReviewCount`, `rejectedCount`, `invalid`), and per-row results. Each result entry MUST include `row` with the 1-based source position.

#### Scenario: Mixed batch response includes row positions

- GIVEN a batch with valid, rejected, and invalid rows
- WHEN processed
- THEN each result entry contains `row` with the original 1-based position

### Requirement: No Real Bank Integration

The system MUST NOT invoke real bank APIs, OAuth flows, balance queries, or persisted batch state.

#### Scenario: Simulated endpoint is self-contained

- GIVEN the simulated endpoint receives a request
- WHEN processed
- THEN no external bank API is called and no bank batch is persisted
