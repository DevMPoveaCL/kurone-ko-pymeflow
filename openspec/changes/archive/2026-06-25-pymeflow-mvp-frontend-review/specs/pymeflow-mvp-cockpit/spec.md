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

The cockpit MUST use existing read APIs for active profile, recommendations, projection-ready history, pending manual review, and provider sync status. It SHOULD add no backend endpoint; if blocked, the only permitted addition is one read-only safe movement summary for the active profile.

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
