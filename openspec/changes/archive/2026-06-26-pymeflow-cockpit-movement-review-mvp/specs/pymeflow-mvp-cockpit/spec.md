# Delta for PymeFlow MVP Cockpit

## ADDED Requirements

### Requirement: Pending Movement Review Panel

The cockpit MUST load pending manual-review movements and active profile categories using existing APIs. It MUST render them as a review task separate from projection recommendations.

#### Scenario: Pending review data loads

- GIVEN the cockpit has an active profile
- WHEN the review panel initializes
- THEN pending `MANUAL_REVIEW` movements are shown
- AND active profile categories are available for selection.

#### Scenario: Empty review queue is safe

- GIVEN there are no pending manual-review movements
- WHEN the review panel renders
- THEN it shows a neutral Spanish empty state
- AND recommendation and evidence sections remain usable.

#### Scenario: Review data error is contained

- GIVEN pending movements or categories fail to load with a safe error
- WHEN the cockpit renders
- THEN the review panel shows an actionable Spanish error state
- AND no secret, stack trace, token, or provider payload is displayed.

### Requirement: Single Movement Categorization

The cockpit MUST let the user choose one active category for a pending movement and resolve it through the existing manual-review resolution API. It MUST NOT require a new backend endpoint for the MVP path.

#### Scenario: Movement is resolved with selected category

- GIVEN a pending movement and active categories are visible
- WHEN the user selects a category and confirms categorization
- THEN the cockpit submits the selected category to the existing resolution endpoint
- AND the user receives clear completion feedback.

#### Scenario: Category is required

- GIVEN a pending movement is visible without a selected category
- WHEN the user attempts to categorize it
- THEN the cockpit prevents submission
- AND asks the user to select a category first.

### Requirement: Review Resolution Refreshes Evidence

After a successful resolution, the cockpit MUST refresh pending review, projection-ready history, recommendations, and cockpit evidence so the resolved movement no longer appears as unresolved stale data.

#### Scenario: Resolved movement leaves review queue

- GIVEN a pending movement is successfully categorized
- WHEN refresh completes
- THEN that movement is removed from the pending review panel or shown as resolved
- AND projection/recommendation evidence reflects the latest persisted state.

#### Scenario: Resolution failure preserves task context

- GIVEN the resolution API returns a safe validation or server error
- WHEN the cockpit handles the failure
- THEN the pending movement remains available for retry
- AND the chosen category is not presented as persisted.

### Requirement: Direction and Amount Invariants Stay Visible

The cockpit MUST keep bank movement direction and category direction visually distinct. Movement rows MUST keep `DEBIT`/`CREDIT` visible, and CLP amounts MUST remain positive values.

#### Scenario: Movement direction is not category direction

- GIVEN a movement has bank direction `DEBIT` or `CREDIT`
- WHEN categories with `INFLOW` or `OUTFLOW` semantics are displayed
- THEN the row still labels the bank movement direction separately
- AND category copy describes classification, not amount sign or bank direction.

#### Scenario: Positive CLP amount is preserved

- GIVEN a pending or resolved movement is displayed
- WHEN the user reviews the row and refreshed evidence
- THEN the amount is shown as a positive CLP value
- AND direction is communicated by `DEBIT` or `CREDIT`, not by a negative amount.

### Requirement: Smokeable Review Interaction

The review MVP MUST be smokeable through static resource tests and/or Playwright MCP evidence, including review copy, selector/action wiring, safe states, and unchanged direction/amount invariants.

#### Scenario: Static smoke verifies review affordances

- GIVEN cockpit static resources are tested
- WHEN the smoke checks review markup and copy
- THEN review labels, category selection, action text, and direction/amount evidence targets are present.

#### Scenario: Browser smoke verifies safe behavior

- GIVEN Playwright MCP is available against the running app
- WHEN smoke opens the cockpit review flow
- THEN pending, empty, or safe error states are visible without frontend build tooling.
