# Proposal: Cockpit Demo Reset MVP

## Intent

Make PymeFlow MVP demos repeatable from the cockpit by resetting and seeding a known demo-only state, without manual database cleanup or app restarts.

## Scope

### In Scope
- Add hexagonal demo data port/service/adapter for active-profile demo reset + seed.
- Expose safe combined `POST /api/cockpit/demo/reset-and-seed`.
- Reset only active demo-profile transactional data: movement history, provider sync sessions, cockpit preferences.
- Seed demo movements for manual review, projection-ready history, provider sync status, and cockpit preferences.
- Add cockpit action copy: “Reiniciar demo”, with full evidence refresh after success.

### Out of Scope
- Auth, multi-user scoping, admin console, real provider data, destructive global truncate.
- Node/frontend tooling, schema-level `is_demo`, separate reset/seed endpoints unless design proves necessary.

## Capabilities

### New Capabilities
- `cockpit-demo-data`: demo-only reset/seed contract, active-profile safety, deterministic fixture state, and API behavior.

### Modified Capabilities
- `pymeflow-mvp-cockpit`: add “Reiniciar demo” control, Spanish demo-only copy, refresh behavior, and smokeability.

## Approach

Use Approach 1 from exploration: define `DemoDataPort` in application, orchestrate with a transactional demo service, implement JDBC deletion/seed support in infrastructure, and expose one controller endpoint. Validate the active profile before deletion. Do not touch reference tables or production/global data. Seed deterministic rows and preferences so the cockpit is immediately demoable.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/com/kuroneko/pymeflow/application/port/out/DemoDataPort.java` | New | Demo reset/seed output port. |
| `src/main/java/com/kuroneko/pymeflow/application/*Demo*Service.java` | New | Transactional orchestration and profile validation. |
| `src/main/java/com/kuroneko/pymeflow/infrastructure/demo/JdbcDemoDataAdapter.java` | New | Scoped DELETE/seed implementation. |
| `src/main/java/com/kuroneko/pymeflow/interfaces/web/DemoDataController.java` | New | `POST /api/cockpit/demo/reset-and-seed`. |
| `src/main/resources/static/index.html`, `app.js` | Modified | Add button, safe states, refresh evidence. |
| `src/test/**` | Modified | Controller, adapter/service, static smoke, ArchUnit coverage. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Accidental destructive cleanup | Med | Restrict to active profile, transactional tables only, demo endpoint/copy. |
| Seed drifts with ingestion rules | Med | Assert expected counts/statuses in tests. |
| Reset succeeds but seed fails | Low | Wrap combined operation in service transaction. |
| Review size exceeds budget | Med | Split backend API/adapter and cockpit UI/tests if forecast exceeds 400 lines. |

## Rollback Plan

Revert the controller, demo service/port/adapter, static cockpit button wiring, and tests. No database migration is expected, so rollback is code-only.

## Dependencies

- Existing active profile `pharmacy-cl`, movement history, provider sync session, and cockpit preferences storage.
- Existing cockpit refresh/read APIs.

## Review Workload Forecast

- Expected size: ~400 changed lines. Chained PRs recommended if implementation grows beyond budget: (1) backend demo API + tests, (2) cockpit button + smoke tests.

## Success Criteria

- [ ] Clicking “Reiniciar demo” leaves the cockpit with known movements, sync status, and preferences.
- [ ] Reset never touches reference tables or global/production data.
- [ ] Tests prove active-profile scoping, seeded review/projectable data, safe copy, and refresh behavior.
