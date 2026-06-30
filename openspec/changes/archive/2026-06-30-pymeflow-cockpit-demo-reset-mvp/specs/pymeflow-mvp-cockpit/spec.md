# Delta for PymeFlow MVP Cockpit

## ADDED Requirements

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
