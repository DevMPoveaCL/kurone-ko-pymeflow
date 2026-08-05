# Delta for PymeFlow MVP Cockpit

## ADDED Requirements

### Requirement: Dashboard de caja Framing

The system MUST present the primary user-facing shell as `Dashboard de caja` instead of `cockpit` where visible in the main UI. Internal capability names, selectors, API URLs, and tests MAY keep cockpit naming when not user-facing.

#### Scenario: Main UI uses dashboard wording

- GIVEN the static page is loaded
- WHEN the user reads the topbar, main heading, and primary landmarks
- THEN visible framing says `Dashboard de caja`
- AND the primary UI is not presented to users as a cockpit.

#### Scenario: Technical contracts remain stable

- GIVEN existing static tests inspect selectors and scripts
- WHEN the dashboard wording is applied
- THEN existing `data-api-target`, `data-action`, guide hooks, and API URLs still exist.

### Requirement: Fullviewport Shell Layout

The system MUST target a `100dvh` dashboard shell where the primary demo flow is visible above the fold on common desktop viewports. On mobile-width viewports, it MUST NOT create horizontal overflow.

#### Scenario: Desktop primary flow is above fold

- GIVEN a common desktop viewport such as 1366x768
- WHEN the dashboard renders
- THEN topbar, key metrics, demo guide/actions, projection, and review action area are visible without page-level vertical scrolling.

#### Scenario: Mobile avoids horizontal overflow

- GIVEN a mobile-width viewport such as 390x844
- WHEN the dashboard renders
- THEN primary controls and cards stack within the viewport
- AND the document has no horizontal scrolling.

### Requirement: Compact Dashboard Information Architecture

The dashboard MUST prioritize a compact IA: topbar, key metrics, demo guide/actions, projection, and review summary/action area. Cartola, comprobantes, and detailed evidence MUST remain accessible but MAY be visually secondary or below the fold for PR1.

#### Scenario: Primary sections are prioritized

- GIVEN the dashboard loads with demo data
- WHEN the first screen is inspected
- THEN the compact primary sections appear before secondary evidence sections.

#### Scenario: Secondary evidence remains reachable

- GIVEN cartola, comprobantes, or details exist
- WHEN the user navigates beyond the primary shell area
- THEN those sections remain reachable
- AND full drawers or modals are not required for PR1.

### Requirement: Concise Didactic Demo Copy

The dashboard MUST use reduced, simple Spanish labels while preserving safe demo semantics. It MUST NOT claim real bank connectivity, credentials, provider access, or live production behavior.

#### Scenario: Concise labels preserve meaning

- GIVEN labels, hints, badges, and action text render
- WHEN the user reads the primary shell
- THEN copy is brief, didactic, and demo-safe
- AND safe simulated/manual-data meaning remains visible.

#### Scenario: Forbidden claims remain absent

- GIVEN sync, reset, projection, and review copy render
- WHEN static tests inspect visible text
- THEN no real bank, credential, provider, or live-connectivity claim is present.

### Requirement: Frontend-Only Accessibility and Smokeability

The change MUST NOT alter backend/API behavior. It MUST be smokeable through static resource tests and browser evidence for layout, accessibility, preserved contracts, and no frontend build tooling.

#### Scenario: No backend behavior changes

- GIVEN the dashboard shell is implemented
- WHEN reset, sync, review, categorization, projection, and preference flows run
- THEN they use the existing backend contracts
- AND no new endpoint, persistence behavior, or frontend build tooling is introduced.

#### Scenario: Accessibility and smoke checks pass

- GIVEN static tests or browser smoke inspect the dashboard
- WHEN landmarks, headings, controls, focus, contrast, and responsive layout are checked
- THEN controls have accessible names and logical keyboard order
- AND desktop/mobile smoke evidence confirms the shell is usable.
