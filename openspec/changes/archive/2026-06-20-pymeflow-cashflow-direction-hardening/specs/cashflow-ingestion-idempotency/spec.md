# Delta for Cashflow Ingestion Idempotency

## ADDED Requirements

### Requirement: Direction Exclusion from Fingerprint

The system MUST NOT include `movementDirection` in the `fp:v1` fingerprint hash. Re-ingestion of the same row with different `movementDirection` values MUST return the existing movement if all other fingerprint fields match.

#### Scenario: Direction change does not alter fingerprint

- GIVEN a row with `profileId="p1", amount=1000.00, currency="clp", date="2024-06-18", description="Pago"`
- WHEN ingested without reference with `movementDirection = DEBIT` and later with `movementDirection = CREDIT`
- THEN both ingestions generate the identical `fp:v1:` hash and return the same existing movement

## MODIFIED Requirements

### Requirement: Deterministic Fingerprint Fallback

The system MUST generate `source_reference` as `fp:v1:<sha256-hex>` when `externalReference` is omitted. The fingerprint MUST exclude `movementDirection` to preserve backward compatibility with existing `fp:v1` hashes.
(Previously: The algorithm did not mention direction because the field did not exist.)

#### Scenario: Hash from normalized fields

- GIVEN profileId="p1", amount=1000.00, currency="clp", date="2024-06-18", description="Pago"
- WHEN ingested without reference
- THEN `source_reference` is `fp:v1:` + SHA-256 of `pymeflow|v1|p1|1000.00|CLP|2024-06-18|Pago`

#### Scenario: Direction excluded from fingerprint

- GIVEN a row with `movementDirection = DEBIT`
- WHEN ingested without reference
- THEN the generated `source_reference` hash is identical to the hash for the same row with `movementDirection = CREDIT`
