# Delta for Cashflow Manual Import

## ADDED Requirements

### Requirement: Optional Movement Direction Input

The system MUST accept an optional `movementDirection` field in manual import rows. If omitted, the system MUST default to `CREDIT`.

#### Scenario: Omitted direction defaults to CREDIT

- GIVEN a valid row without `movementDirection`
- WHEN ingested
- THEN the ingestion item has `movementDirection = CREDIT`

#### Scenario: Explicit DEBIT is accepted

- GIVEN a valid row with `movementDirection = DEBIT`
- WHEN ingested
- THEN the ingestion item has `movementDirection = DEBIT`

### Requirement: Response Direction Exposure

The system MUST include `movementDirection` in manual import responses where relevant.

#### Scenario: Successful import result includes direction

- GIVEN a manual import row is accepted
- WHEN the response is returned
- THEN the result entry for that row includes `movementDirection`

## MODIFIED Requirements

### Requirement: Batch Endpoint Contract

The system MUST expose and document in OpenAPI `POST /api/cashflow/imports/manual` accepting `profileId`, optional `importLabel`, and a `rows` array containing `rowNumber`, `description`, `amount`, `date`, optional `currency` (defaulting to CLP), optional `externalReference`, and optional `movementDirection` (defaulting to `CREDIT`).
(Previously: movementDirection was not part of the contract.)

#### Scenario: Valid batch accepted

- GIVEN a request with `profileId` and valid rows
- WHEN posted to the endpoint
- THEN HTTP 200 is returned with a summary response

#### Scenario: All-invalid response documented

- GIVEN the manual import endpoint is exposed through OpenAPI
- WHEN clients inspect the endpoint responses
- THEN HTTP 200 and HTTP 400 responses are documented

### Requirement: Row Field Validation

The system MUST validate every row for `description` (non-blank), `amount` (positive), `date` (valid ISO), `currency` (CLP only), and optional `movementDirection` (if present, must be a valid `TransactionDirection` value). Optional `externalReference` and `rowNumber` are accepted.
(Previously: movementDirection did not exist and was not validated.)

#### Scenario: Missing description

- GIVEN a row with blank `description`
- WHEN validated
- THEN an error is returned identifying the row and field without echoing the blank value

#### Scenario: Non-positive amount

- GIVEN a row with `amount` <= 0
- WHEN validated
- THEN an error is returned for the amount field

#### Scenario: Invalid date

- GIVEN a row with `date` not in ISO format
- WHEN validated
- THEN an error is returned for the date field

#### Scenario: Non-CLP currency

- GIVEN a row with `currency` other than CLP
- WHEN validated
- THEN an error is returned stating CLP is the only supported currency

#### Scenario: Invalid movementDirection rejected

- GIVEN a row with `movementDirection = "INVALID"`
- WHEN validated
- THEN an error is returned for the movementDirection field

### Requirement: Delegation to Ingestion Service

The system MUST map each valid row to an `IngestionItem`, including resolved `movementDirection`, and delegate to `CashflowIngestionService.ingest(...)`.
(Previously: movementDirection was not mapped because it did not exist.)

#### Scenario: Valid row ingestion delegation

- GIVEN a valid row with `profileId`, `description`, `amount`, `date`, optional `externalReference`, and resolved `movementDirection`
- WHEN processed
- THEN `CashflowIngestionService.ingest` is called with the mapped item including `movementDirection`
