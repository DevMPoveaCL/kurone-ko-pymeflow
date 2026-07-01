# Delta for pymeflow-mvp-cockpit

## ADDED Requirements

### Requirement: Guided Demo Rail

The cockpit MUST show a compact `Guía de demo` rail with four ordered steps: `Reiniciar demo`, `Revisar pendientes`, `Categorizar`, and `Proyectar caja`. Each step MUST guide users to existing cockpit evidence without becoming a strict wizard.

#### Scenario: Four-step guide is visible

- GIVEN the cockpit page is loaded
- WHEN the guide renders
- THEN the four demo steps appear in the required order
- AND each step points to the matching existing cockpit section.

#### Scenario: Guide does not block cockpit use

- GIVEN any guide step is current or incomplete
- WHEN the user interacts with reset, review, categorization, or projection controls directly
- THEN the cockpit allows the existing interaction to proceed.

### Requirement: Demo-Safe Chilean Spanish Copy

The cockpit MUST use neutral Spanish copy suitable for the Chilean market. It MUST describe the flow as a demo with fixture/simulated data and MUST NOT claim real bank, provider, credential, or live connectivity behavior.

#### Scenario: Copy is demo-safe

- GIVEN the guide and related cockpit sections render
- WHEN the user reads labels, hints, and statuses
- THEN the copy uses neutral Spanish demo language
- AND it avoids real bank/provider connectivity claims.

#### Scenario: Provider-neutral wording remains honest

- GIVEN sync, reset, review, or projection evidence is visible
- WHEN copy references data origin or account state
- THEN it states demo, simulated, fixture, or manually entered semantics
- AND it does not imply production bank access.

### Requirement: Session-Only Demo Progress Hints

The guide MUST present progress only as browser-session demo hints inferred from successful existing user actions. It MUST NOT persist progress, use durable storage, or present completion as backend workflow state.

#### Scenario: Successful actions advance hints

- GIVEN the user completes reset, loads pending review, categorizes a movement, or runs projection successfully
- WHEN the guide updates
- THEN the matching step may be marked complete for the current browser session
- AND the next step hint is shown.

#### Scenario: Progress is not durable

- GIVEN guide progress was shown in one browser session
- WHEN the page starts a fresh session
- THEN the guide does not claim prior progress is persisted
- AND existing cockpit evidence remains the source of truth.

### Requirement: Static Accessibility Basics

The guide MUST preserve cockpit accessibility basics: meaningful headings, accessible control labels, status semantics for guide progress, visible focus, and logical navigation order.

#### Scenario: Assistive structure is available

- GIVEN the cockpit page is loaded
- WHEN static accessibility checks inspect the guide
- THEN headings, labels, and status text identify the guide and step state
- AND keyboard navigation can pass through the guide to cockpit controls.

### Requirement: Guided Demo Smokeability

The guided demo MVP MUST be covered by static contract tests and smoke evidence. Tests MUST verify step order, safe copy, frontend-only scope, non-blocking interactions, accessibility markers, and absence of live bank/provider claims.

#### Scenario: Static contract covers guide requirements

- GIVEN cockpit static resources are tested
- WHEN assertions inspect guide markup, copy, and scripts
- THEN required steps, safe Spanish labels, session-only semantics, and forbidden live-connectivity claims are verified.

#### Scenario: Browser smoke covers the guided path

- GIVEN the application is running with demo fixture support
- WHEN smoke exercises reset, review, categorization, and projection through the cockpit
- THEN guide progress reflects successful actions only
- AND the original cockpit interactions remain usable.
