# PymeFlow MVP Cockpit Specification

## Purpose

Provide a static Spring Boot cockpit for existing PymeFlow MVP cashflow APIs without a frontend build step or real-bank claims.

## Requirements

### Requirement: Static Spring Boot Cockpit

The system MUST serve the cockpit from Spring Boot static resources using plain HTML, CSS, and JavaScript. It MUST NOT require Vite, React, bundling, transpilation, or a frontend build command.

#### Scenario: Cockpit loads from the application

- GIVEN the Spring Boot application is running
- WHEN a user opens the root cockpit page
- THEN the page loads without executing any frontend build step
- AND it can call same-origin APIs.

### Requirement: Chilean PyME Cashflow Identity

The cockpit MUST use professional neutral Spanish copy for Chilean PyME cashflow work. It MUST include `caja`, `abonos`, and `cargos`; it MUST NOT use `mostrador`.

#### Scenario: Localized cashflow language is visible

- GIVEN the cockpit page is loaded
- WHEN the main sections render
- THEN the user sees Spanish labels for caja, abonos, cargos, movimientos, historial, revisión, and sync status.

#### Scenario: Connectivity claims stay honest

- GIVEN provider sync content is shown
- WHEN the user reads the copy
- THEN it states fixture/simulated sync behavior only
- AND it makes no real bank connectivity claim.

### Requirement: Safe MVP Cashflow Flow

The cockpit MUST let users trigger existing safe manual import and fixture provider sync APIs, then inspect receipt-style evidence and durable sync status.

#### Scenario: Import receipt is inspectable

- GIVEN valid sample cashflow rows with DEBIT and CREDIT directions
- WHEN the user triggers the import flow
- THEN the cockpit shows accepted, manual review, rejected, and invalid counts
- AND row evidence keeps `movementDirection` visible.

#### Scenario: Provider sync status is durable and safe

- GIVEN a provider sync has returned a `syncId`
- WHEN the user inspects the sync receipt and status
- THEN the cockpit shows counts, status, provider type, durability, and safe errors
- AND no credential, secret, token, cursor, stack trace, or provider payload is displayed.

### Requirement: Movement Evidence and Review Data

The cockpit MUST use existing read APIs for active profile, recommendations, projection-ready history, pending manual review, and provider sync status. It MAY add one read-only cockpit projection endpoint for the active profile when required by the projection feature.

(Previously: The cockpit SHOULD add no backend endpoint; if blocked, the only permitted addition is one read-only safe movement summary for the active profile.)

#### Scenario: Direction is visible across movement evidence

- GIVEN persisted movements include DEBIT and CREDIT entries with positive CLP amounts
- WHEN history and review sections render
- THEN visible movement rows show `DEBIT` or `CREDIT`
- AND amounts remain positive CLP values.

#### Scenario: Missing optional data degrades safely

- GIVEN one existing read API returns an empty list or safe validation error
- WHEN the cockpit renders that section
- THEN the page shows an actionable Spanish empty/error state
- AND the rest of the cockpit remains usable.

#### Scenario: Projection endpoint is permitted

- GIVEN the projection feature requires a new read-only endpoint
- WHEN the cockpit requests projection data
- THEN the read-only projection endpoint is called
- AND no other new backend endpoints are introduced.

### Requirement: Smokeability and Accessibility

The system MUST be smoke-testable against a running Spring Boot app using API calls and, when present, Playwright. It MUST also provide landmarks, keyboard controls, visible focus, readable contrast, and responsive layout.

#### Scenario: Smoke proves visible safe flow

- GIVEN the application is running with fixture data support
- WHEN smoke opens the cockpit and exercises import or provider sync
- THEN receipts and DEBIT/CREDIT movement direction are visible
- AND no secrets or real bank connectivity claims are visible.

#### Scenario: Keyboard and landmarks are usable

- GIVEN a keyboard-only user opens the cockpit
- WHEN they navigate controls and result sections
- THEN controls are reachable in logical order with focus and accessible names.

#### Scenario: Narrow viewport preserves task flow

- GIVEN the viewport is mobile-width
- WHEN the cockpit renders receipts, history, and status sections
- THEN content stacks without horizontal scrolling for primary actions.

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

### Requirement: Demo Reset Control

The cockpit MUST expose a "Reiniciar demo" action that calls the reset-and-seed endpoint. On success, it MUST refresh all visible cockpit evidence.

#### Scenario: User triggers demo reset

- GIVEN the cockpit is loaded with demo data
- WHEN the user clicks "Reiniciar demo"
- THEN the reset-and-seed endpoint is invoked

#### Scenario: Success refreshes evidence

- GIVEN the reset-and-seed call succeeds
- WHEN the cockpit processes the response
- THEN pending review, history, projection, sync status, and preferences refresh

#### Scenario: Failure shows safe state

- GIVEN the reset-and-seed call fails
- WHEN the cockpit processes the error
- THEN a safe Spanish error message is shown
- AND the existing cockpit data remains visible

### Requirement: Demo-Only Copy

The cockpit MUST use demo-only copy for the reset action and results. It MUST NOT claim real bank or provider connectivity.

#### Scenario: Reset action indicates demo-only

- GIVEN the cockpit renders the reset control
- WHEN the user reads the label and result
- THEN copy states the action is for demo purposes only

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

### Requirement: Period Projection Controls

The cockpit MUST provide 7-day and 30-day projection controls. The cockpit MUST load the preferred projection period from persisted preferences on startup and pre-fill the control when available.
(Previously: Only required providing controls; did not require loading or persisting the selected period.)

#### Scenario: Select projection period

- GIVEN the projection section is visible
- WHEN the user selects a period control
- THEN the cockpit requests a projection for that period with the current opening balance

#### Scenario: Load persisted projection period

- GIVEN saved preferred_horizon_days of 30 for the active profile
- WHEN the cockpit initializes
- THEN the 30-day control is pre-selected

### Requirement: Opening Balance Input

The cockpit MUST provide an opening balance input with copy stating it is user-entered, not from a live bank. The cockpit MUST load the persisted opening balance on startup and auto-save changes with debounce.
(Previously: Only required providing the input with honest copy; did not require persisting or restoring the value.)

#### Scenario: Enter opening balance

- GIVEN the projection section is visible
- WHEN the user enters an opening balance
- THEN the label states the balance is manual, not bank-provided

#### Scenario: Opening balance required

- GIVEN the user has not entered an opening balance
- WHEN the user attempts to request a projection
- THEN the cockpit prevents the request and prompts for the balance

#### Scenario: Load persisted opening balance

- GIVEN a saved opening balance of 1500000 for the active profile
- WHEN the cockpit initializes
- THEN the input is pre-filled with 1500000
- AND the copy still states the balance is manual

#### Scenario: Auto-save preference change

- GIVEN the user changes the opening balance or period
- WHEN the debounce period elapses
- THEN the cockpit persists the new value via PUT

### Requirement: Cockpit Projection Endpoint

The system MUST expose a read-only `GET /api/cashflow/cockpit/projection` endpoint accepting `profileId`, `startDate`, `horizonDays`, and `openingBalance`. It MUST use persisted `PROJECTABLE` movements and the existing projection service.

#### Scenario: Valid projection request

- GIVEN valid parameters and existing `PROJECTABLE` movements
- WHEN the endpoint is called
- THEN HTTP 200 returns projection data with daily balances, totals, and alerts

#### Scenario: No projectable movements

- GIVEN no `PROJECTABLE` movements exist for the profile
- WHEN the endpoint is called
- THEN HTTP 200 returns an empty projection and a categorization signal

### Requirement: Projection Rendering

The cockpit MUST render daily balances, closing balance, abonos total, cargos total, obligations total, and alerts. It MUST preserve `DEBIT`/`CREDIT` direction and positive CLP amounts.

#### Scenario: Projection results render

- GIVEN a successful projection response
- WHEN the cockpit renders the projection section
- THEN daily balances, totals, closing balance, and alerts are visible
- AND movement direction and amounts remain positive CLP values

### Requirement: Empty State for No Projectable Movements

The cockpit MUST show an empty state guiding the user to categorize movements when no `PROJECTABLE` movements exist.

#### Scenario: No projectable empty state

- GIVEN the projection response indicates no projectable movements
- WHEN the cockpit renders the projection section
- THEN a neutral Spanish message explains that movements must be categorized to project cashflow

### Requirement: Projection Smokeability

The system MUST be smoke-testable for the projection flow.

#### Scenario: Smoke verifies projection flow

- GIVEN the application is running with fixture data
- WHEN smoke exercises the projection controls and API
- THEN daily balances, totals, alerts, and safe empty states are visible
- AND no real bank connectivity claims are shown

### Requirement: Cockpit Preference Persistence API

The system MUST expose GET and PUT /api/cockpit/preferences endpoints scoped to the active profile. The backend MUST validate preferred_horizon_days is exactly 7 or 30, and opening_balance is a safe numeric value.

#### Scenario: Load preferences

- GIVEN preferences exist for the active profile
- WHEN GET /api/cockpit/preferences is called
- THEN HTTP 200 returns opening_balance and preferred_horizon_days

#### Scenario: Save preferences

- GIVEN valid preference values
- WHEN PUT /api/cockpit/preferences is called
- THEN HTTP 200 persists the values durably

#### Scenario: Reject invalid horizon days

- GIVEN a PUT request with preferred_horizon_days of 15
- WHEN the backend validates
- THEN HTTP 400 returns a safe validation error

#### Scenario: Reject unsafe opening balance

- GIVEN a PUT request with an extreme or non-numeric opening_balance
- WHEN the backend validates
- THEN HTTP 400 returns a safe validation error

### Requirement: Preference Safe Defaults

The system MUST provide safe defaults when no preferences exist for a profile.

#### Scenario: Empty preferences

- GIVEN no preferences exist for the active profile
- WHEN the cockpit initializes
- THEN the opening balance input is empty
- AND the period control defaults to 7 days

### Requirement: Durable Preference Persistence

Preferences MUST survive application restart.

#### Scenario: Preferences survive restart

- GIVEN preferences have been saved
- WHEN the application restarts
- THEN a subsequent GET returns the previously saved values

### Requirement: Preference Smokeability

The system MUST be smoke-testable for preference load, save, and validation.

#### Scenario: Smoke verifies preference round-trip

- GIVEN the application is running with saved preferences
- WHEN smoke opens the cockpit, changes values, and refreshes
- THEN controls reflect the persisted values after reload
