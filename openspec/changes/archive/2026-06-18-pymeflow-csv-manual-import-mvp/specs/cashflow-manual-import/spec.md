# Cashflow Manual Import

## Purpose

JSON batch endpoint for manual CSV-like cashflow row ingestion with per-row tolerance, delegating persistence to the existing ingestion service.

## Requirements

### Requirement: Batch Endpoint Contract

The system MUST expose and document in OpenAPI `POST /api/cashflow/imports/manual` accepting `profileId`, optional `importLabel`, and a `rows` array containing `rowNumber`, `description`, `amount`, `date`, optional `currency` (defaulting to CLP), and optional `externalReference`.

#### Scenario: Valid batch accepted

- GIVEN a request with `profileId` and valid rows
- WHEN posted to the endpoint
- THEN HTTP 200 is returned with a summary response

#### Scenario: All-invalid response documented

- GIVEN the manual import endpoint is exposed through OpenAPI
- WHEN clients inspect the endpoint responses
- THEN HTTP 200 and HTTP 400 responses are documented

### Requirement: Row-Level Tolerance

The system MUST process valid rows independently; invalid rows MUST NOT block valid rows.

#### Scenario: Mixed valid and invalid rows

- GIVEN a batch where row 1 is valid and row 2 has a negative amount
- WHEN posted
- THEN row 1 is persisted and row 2 returns an error

#### Scenario: All rows invalid

- GIVEN a batch where every row fails validation
- WHEN posted
- THEN HTTP 400 is returned with all row errors and zero accepted rows

### Requirement: Row Field Validation

The system MUST validate every row for `description` (non-blank), `amount` (positive), `date` (valid ISO), and `currency` (CLP only). Optional `externalReference` and `rowNumber` are accepted.

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

### Requirement: Delegation to Ingestion Service

The system MUST map each valid row to an `IngestionItem` and delegate to `CashflowIngestionService.ingest(...)`.

#### Scenario: Valid row ingestion delegation

- GIVEN a valid row with `profileId`, `description`, `amount`, `date`, and optional `externalReference`
- WHEN processed
- THEN `CashflowIngestionService.ingest` is called with the mapped item

### Requirement: Deduplication via Fingerprint Fallback

The system MUST ensure rows without `externalReference` (null, empty, or whitespace-only) deduplicate via the existing fingerprint fallback.

#### Scenario: No reference uses fingerprint

- GIVEN a valid row with omitted `externalReference`
- WHEN ingested
- THEN `source_reference` is generated as `fp:v1:<sha256>` and re-importing identical rows returns the existing movement

### Requirement: Idempotency on Re-import

The system MUST return existing movement IDs when the same valid rows are re-imported; no duplicate movements are created. If an explicit `externalReference` is rejected as sensitive, the system MUST NOT persist that sensitive reference and MUST instead use the safe fingerprint fallback as the effective deduplication reference.

#### Scenario: Re-import returns existing movement

- GIVEN a valid row was previously imported
- WHEN the same row is submitted again
- THEN the existing movement ID is returned in the response

#### Scenario: Sensitive reference replay returns existing rejection

- GIVEN a valid row with a sensitive explicit `externalReference` was rejected and persisted with a safe fingerprint fallback
- WHEN the same row is submitted again
- THEN the existing rejected movement ID is returned and no duplicate rejected movement is inserted

### Requirement: Sensitive Data Protection

The system MUST reject rows containing sensitive descriptions or references and MUST NOT echo the sensitive value in errors.

#### Scenario: Sensitive description rejected safely

- GIVEN a row with a sensitive `description`
- WHEN validated
- THEN the row is rejected and the error message does not contain the sensitive value

#### Scenario: Sensitive reference rejected safely

- GIVEN a row with a sensitive `externalReference`
- WHEN validated
- THEN the row is rejected and the error message does not contain the sensitive value

### Requirement: Summary Response

The system MUST return a response containing `importId` (response-only), `profileId`, flat counts for `accepted`, `categorizedCount`, `manualReviewCount`, `rejectedCount`, and `invalid`, plus per-row results. Successful result entries in `categorized`, `manualReview`, and `rejected` and validation entries in `errors` MUST include a `row` value that echoes the submitted `rowNumber` when present, falling back to the submitted 1-based position only when `rowNumber` is absent.

#### Scenario: Mixed batch response summary

- GIVEN a batch with 2 accepted, 1 rejected, and 1 invalid row
- WHEN processed
- THEN the response reflects counts 2 accepted, 1 rejected, 1 invalid, and each row has a result entry

#### Scenario: Successful results include source row traceability

- GIVEN a mixed batch with non-sequential `rowNumber` values that produces categorized, manual-review, rejected, and invalid outcomes
- WHEN processed
- THEN each successful result entry contains `row` with the provided `rowNumber` and invalid rows remain reported in `errors[].row`

#### Scenario: Missing rowNumber falls back to source position

- GIVEN a mixed batch where rows omit `rowNumber`
- WHEN processed
- THEN successful and invalid result entries contain `row` with the original 1-based submitted row position

### Requirement: CLP-Only Currency Enforcement

The system MUST reject any row with a currency other than CLP.

#### Scenario: USD row rejected

- GIVEN a row with `currency = "USD"`
- WHEN validated
- THEN the row is rejected with a CLP-only error

## Out of Scope

- Multipart CSV file upload.
- Persistent import batches or import history.
- Async preview/confirm workflow.
