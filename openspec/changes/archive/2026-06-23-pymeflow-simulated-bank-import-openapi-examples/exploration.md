## Exploration: pymeflow-simulated-bank-import-openapi-examples

### Current State
`POST /api/cashflow/imports/bank-statement/simulated` is implemented in the web adapter and functionally expects `rows[]` entries with `bookingDate` and per-row `accountAlias`. The baseline OpenSpec already states that contract, and controller tests exercise valid/invalid rows using the correct JSON shape. The API docs currently expose an `@Operation` summary/description and 200/400 responses, but there is no explicit OpenAPI request-body example showing the correct row-level payload shape, so consumers can reasonably guess wrong names/placement such as `bookedAt` or root-level `accountAlias`.

### Affected Areas
- `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedController.java` — owns the endpoint, OpenAPI annotations, request DTO records, and validation fields.
- `src/test/java/com/kuroneko/pymeflow/interfaces/web/CashflowBankStatementSimulatedControllerTest.java` — already has reflection-based OpenAPI documentation assertions and request payload fixtures using the correct shape.
- `openspec/specs/cashflow-bank-statement-import/spec.md` — baseline already documents `bookingDate` and per-row `accountAlias`; likely no behavior/spec change needed.
- `build.gradle` — confirms springdoc OpenAPI is available via `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0`.

### Approaches
1. **Add an explicit request-body example on the endpoint** — Annotate the method parameter with springdoc/OpenAPI `@RequestBody(content = @Content(... @ExampleObject(value = """{...}""")))`, using `bookingDate` and per-row `accountAlias`.
   - Pros: Smallest user-facing fix; directly prevents the observed Playwright friction; keeps behavior unchanged; can be protected by the existing reflection-style docs test.
   - Cons: Example JSON lives as a string literal and can drift if DTO fields change.
   - Effort: Low

2. **Add `@Schema` descriptions/examples to request DTO record components** — Document each request field (`profileId`, `rows`, `bookingDate`, `accountAlias`, etc.) with component-level examples/descriptions.
   - Pros: Improves generated schema field docs and recognition of required field semantics.
   - Cons: More annotation noise; less effective than a full payload example for the root-vs-row `accountAlias` confusion; still may not show complete request shape.
   - Effort: Low/Medium

3. **Generate/validate the full OpenAPI document in a WebMvc test** — Call `/v3/api-docs` and assert the example/schema includes `bookingDate`, row-level `accountAlias`, and excludes `bookedAt`.
   - Pros: Tests what consumers actually see.
   - Cons: Larger test setup and more brittle JSON-path assertions for a docs-only debt fix.
   - Effort: Medium

### Recommendation
Use Approach 1 now: add one explicit OpenAPI request-body example to `CashflowBankStatementSimulatedController.importSimulated(...)` and extend `documentsOpenApiResponsesAndDirectionPreservation()` (or a nearby docs test) to assert the example contains `"bookingDate"`, `"rows"`, and row-level `"accountAlias"`, and does not contain `"bookedAt"`. This is the smallest behavior-free fix that addresses the exact consumer mistake. Approach 2 can be added only if reviewers want richer schema docs, but it is not required for this focused debt fix.

### Risks
- OpenAPI annotation imports can conflict with Spring's `@RequestBody`; use a fully qualified annotation or clear imports to avoid ambiguity.
- A string-literal example can drift from DTO fields; keep a focused reflection test near the controller docs assertions.
- Do not change DTO names or validation semantics; this change should be documentation-only.

### Ready for Proposal
Yes — tell the user this should be a tiny documentation/OpenAPI change: add a correct request-body example and a docs assertion, with no production behavior changes.
