# Cashflow Ingestion Idempotency

## Purpose

Deterministic idempotency for cashflow ingestion when `externalReference` is omitted, preventing duplicate movements and preserving history consistency.

## Requirements

### Requirement: Explicit Reference Behavior Preserved

The system MUST preserve existing explicit `externalReference` behavior, including sensitive-value rejection.

#### Scenario: Explicit reference stores normally

- GIVEN a transaction with `externalReference = "REF-123"`
- WHEN ingested
- THEN `source_reference` equals `"REF-123"`

#### Scenario: Sensitive reference rejected without echo

- GIVEN a transaction with a sensitive `externalReference`
- WHEN ingested
- THEN the request is rejected and the value is not echoed

### Requirement: Blank Reference Normalization

The system MUST treat null, empty, or whitespace-only `externalReference` as omitted.

#### Scenario: Whitespace triggers fallback

- GIVEN `externalReference = "   "`
- WHEN ingested
- THEN a fingerprint fallback is generated

### Requirement: Deterministic Fingerprint Fallback

The system MUST generate `source_reference` as `fp:v1:<sha256-hex>` when `externalReference` is omitted.

#### Scenario: Hash from normalized fields

- GIVEN profileId="p1", amount=1000.00, currency="clp", date="2024-06-18", description="Pago"
- WHEN ingested without reference
- THEN `source_reference` is `fp:v1:` + SHA-256 of `pymeflow|v1|p1|1000.00|CLP|2024-06-18|Pago`

### Requirement: No-Reference Idempotency

The system MUST return the existing movement on re-ingestion of identical no-reference transactions.

#### Scenario: Re-ingestion returns existing row

- GIVEN a no-reference transaction was persisted
- WHEN re-ingested with identical normalized fields
- THEN the existing movement is returned and no duplicate is created

#### Scenario: Materially different fields create new row

- GIVEN a no-reference transaction was persisted
- WHEN re-ingested with a different amount
- THEN a new movement is created

#### Scenario: Identical cash transactions dedupe (MVP accepted)

- GIVEN two legitimate but identical no-reference cash transactions
- WHEN both are ingested
- THEN only one movement is created (documented MVP limitation)

### Requirement: Profile-Scoped Dedup

The system MUST scope fingerprint deduplication per profile.

#### Scenario: Cross-profile same fields are distinct

- GIVEN profile A and profile B ingest identical no-reference transactions
- WHEN processed
- THEN two separate movements are created

### Requirement: History Consistency

The system MUST preserve projection-ready behavior and history consistency for fallback-generated movements.

#### Scenario: Fallback reference visible in history

- GIVEN a movement created via fingerprint fallback
- WHEN history is queried
- THEN the `fp:v1:` reference is returned
