# Delta for PymeFlow MVP Cockpit

## ADDED Requirements

### Requirement: Branded Theme Assets and System Dark Mode

The cockpit MUST serve explicit PymeFlow brand assets and SHOULD preserve visual parity with the Farmacia Uniacc light/dark palette through static CSS tokens plus a minimal visible theme preference switch.

#### Scenario: Brand assets load from static resources

- GIVEN the Spring Boot application is running
- WHEN the cockpit loads `/`, `/favicon.png`, and `/branding.png`
- THEN the favicon and brand lockup are served from static resources
- AND the brand image is contained without crop, stretch, or layout overflow.

#### Scenario: Farmacia Uniacc palette parity is available

- GIVEN the cockpit stylesheet is loaded
- WHEN light and `prefers-color-scheme: dark` modes are evaluated
- THEN lavender/violet/cyan/magenta brand tokens are present in light mode
- AND dark mode brand accents remain in the dark/cyan/blue family without visible magenta
- AND semantic cashflow colors remain readable against their surfaces.

#### Scenario: Visible theme switch overrides system preference

- GIVEN the cockpit loads with `prefers-color-scheme` as the initial visual mode
- WHEN the user activates the topbar theme switch
- THEN the document root receives `data-theme="light"` or `data-theme="dark"`
- AND the switch updates its accessible label and pressed state
- AND the stored preference is only a visual UI preference, not business or demo progress.

#### Scenario: Layout remains readable and responsive

- GIVEN desktop and mobile-width viewports
- WHEN the refreshed cockpit renders
- THEN typography, spacing, contrast, focus, cards, and controls remain legible
- AND primary actions do not require horizontal scrolling.

### Requirement: Behavior and Copy Non-Regression

The refresh MUST NOT change API behavior, JavaScript flow contracts, selectors, or demo-safe Spanish copy semantics.

#### Scenario: Existing cockpit behavior is preserved

- GIVEN the refreshed static resources are served
- WHEN reset, import/sync, review, and projection flows are exercised
- THEN existing API targets, guide selectors, button behavior, and receipts still work
- AND no new backend endpoint or frontend build tooling is required.

#### Scenario: Demo-safe copy is preserved

- GIVEN refreshed visual styling is applied
- WHEN users read reset, sync, review, projection, and guide copy
- THEN the cockpit still uses neutral Spanish demo language
- AND it makes no real bank, credential, provider, or live-connectivity claim.

## MODIFIED Requirements

### Requirement: Smokeability and Accessibility

The system MUST be smoke-testable against a running Spring Boot app using API calls and, when present, Playwright. It MUST also provide landmarks, keyboard controls, visible focus, readable contrast, responsive layout, branded asset loading, and light/dark visual smoke evidence.
(Previously: Required smokeability, landmarks, keyboard controls, focus, contrast, and responsive layout without explicit brand asset or dark-mode evidence.)

#### Scenario: Smoke proves visible safe flow

- GIVEN the application is running with fixture data support
- WHEN smoke opens the cockpit and exercises import or provider sync
- THEN receipts and DEBIT/CREDIT movement direction are visible
- AND no secrets or real bank connectivity claims are visible.

#### Scenario: Keyboard and landmarks are usable

- GIVEN a keyboard-only user opens the refreshed cockpit
- WHEN they navigate controls, brand header, guide, and result sections
- THEN controls are reachable in logical order with visible focus and accessible names.

#### Scenario: Narrow viewport preserves task flow

- GIVEN the viewport is mobile-width
- WHEN the cockpit renders receipts, history, status, and brand elements
- THEN content stacks without horizontal scrolling for primary actions.

#### Scenario: Visual smoke covers light and dark themes

- GIVEN Playwright or browser smoke is available
- WHEN light and dark modes are captured
- THEN brand assets, token colors, focus rings, contrast, and card hierarchy are readable.
