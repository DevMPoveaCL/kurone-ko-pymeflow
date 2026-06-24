# Delta for Cashflow Bank Statement Import

## ADDED Requirements

### Requirement: Simulated Import OpenAPI Request Example

The system MUST publish an OpenAPI request-body example for `POST /api/cashflow/imports/bank-statement/simulated` that matches the accepted simulated bank statement request shape.

#### Scenario: Example uses row-level booking date and account alias

- GIVEN a consumer reviews the OpenAPI documentation for the simulated bank import endpoint
- WHEN the request-body example is displayed
- THEN each row example uses `bookingDate`
- AND each row example contains `accountAlias` inside the row object

#### Scenario: Example avoids misleading field names and nesting

- GIVEN the OpenAPI request-body example is generated from code annotations
- WHEN the example is inspected by automated documentation tests
- THEN it does not contain `bookedAt`
- AND it does not present `accountAlias` as a root-level request field

## MODIFIED Requirements

None

## REMOVED Requirements

None
