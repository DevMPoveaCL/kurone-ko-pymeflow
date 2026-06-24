# Tasks: Simulated Bank Import OpenAPI Examples

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 60-100 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | auto-chain |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Add OpenAPI example and docs guard | PR 1 | Single branch `fix/simulated-bank-openapi-examples`; run focused tests. |

## Phase 1: Documentation Contract

- [x] 1.1 Update `CashflowBankStatementSimulatedController.java` with an OpenAPI request-body example using `bookingDate` and row-level `accountAlias`.

## Phase 2: Test Guard

- [x] 2.1 Extend `documentsOpenApiResponsesAndDirectionPreservation()` or nearby test in `CashflowBankStatementSimulatedControllerTest.java` to read the request example.
- [x] 2.2 Assert the example contains `rows`, `bookingDate`, and row-nested `accountAlias`.
- [x] 2.3 Assert the example does not contain `bookedAt` or root-level `accountAlias`.

## Phase 3: Verification

- [x] 3.1 Run `./gradlew.bat test --tests com.kuroneko.pymeflow.interfaces.web.CashflowBankStatementSimulatedControllerTest --rerun-tasks`.
- [x] 3.2 Confirm no DTO, validation, endpoint, or service behavior changed in the diff.

## Phase 4: Cleanup

- [x] 4.1 Keep imports unambiguous between Spring `@RequestBody` and OpenAPI `@RequestBody`.
- [x] 4.2 Review generated example text for concise, consumer-oriented payload shape.
