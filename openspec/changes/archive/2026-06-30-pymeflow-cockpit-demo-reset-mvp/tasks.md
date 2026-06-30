# Tasks: Cockpit Demo Reset MVP

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~435 (365 backend, 70 frontend) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: Backend API + tests → PR 2: Cockpit UI + smoke |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Backend port, adapter, service, controller, config, tests | PR 1 | base: `feat/cockpit-demo-reset-mvp`; autonomous verify via `@WebMvcTest` |
| 2 | Cockpit button, safe copy, JS wiring, static smoke | PR 2 | base: main after PR 1; depends on PR 1 endpoint |

## Phase 0: Baseline Spec Correction

- [x] 0.1 Move `openspec/specs/cockpit-demo-data/spec.md` → `openspec/changes/pymeflow-cockpit-demo-reset-mvp/specs/cockpit-demo-data/spec.md`, delete baseline; new capability specs belong under change until archive per openspec convention

## Phase 1: RED — Backend Failing Tests (PR 1)

- [x] 1.1 RED: `CockpitDemoResetServiceTest.java` — mock `DemoDataPort`, `CashflowMovementHistoryPort`, `SyncSessionPort`, `CockpitPreferencesPort`; verify orchestration (reset before seed), seed determinism (3 PROJECTABLE, 2 MANUAL_REVIEW, 1 COMPLETED sync, preferences `openingBalance=350000, preferredHorizonDays=7`), profile validation
- [x] 1.2 RED: `JdbcDemoDataAdapterTest.java` — `@JdbcTest` with H2; prove `DELETE` scoped to `profile_id` on `cashflow_movement_history`, `provider_sync_sessions`, `cockpit_preferences`; other profile data intact; reference tables untouched
- [x] 1.3 RED: `DemoDataControllerTest.java` — `@WebMvcTest(DemoDataController.class)`; assert HTTP 200 + `DemoResetResponse(status, movementsSeeded, syncSessionId)` on valid `profileId`; HTTP 400 on missing/invalid param; safe error message on service failure

## Phase 2: GREEN — Backend Minimal Implementation (PR 1)

- [x] 2.1 GREEN: `application/port/out/DemoDataPort.java` — interface `void reset(ProfileId profileId)`
- [x] 2.2 GREEN: `infrastructure/demo/JdbcDemoDataAdapter.java` — implement `DemoDataPort` via `JdbcTemplate`; 3 parameterized `DELETE` queries per profile; `@Repository` stereotype
- [x] 2.3 GREEN: `application/cockpit/CockpitDemoResetService.java` — `@Transactional` service; constructor injection of `DemoDataPort`, `CashflowMovementHistoryPort`, `SyncSessionPort`, `CockpitPreferencesPort`; `resetAndSeed(ProfileId)`: validate profile, delete transactional data, seed 5 deterministic `CashflowMovementDraft`s via `saveAll`, seed sync session + preferences directly
- [x] 2.4 GREEN: `interfaces/web/DemoDataController.java` — `@RestController`, `POST /api/cockpit/demo/reset-and-seed`, `@RequestParam @NotBlank String profileId`, returns `ResponseEntity<DemoResetResponse>`
- [x] 2.5 GREEN: `infrastructure/config/ApplicationServiceConfiguration.java` — add `@Bean` for `JdbcDemoDataAdapter` and `CockpitDemoResetService`; reuse existing `JdbcTemplate`, `SyncSessionPort`, `CockpitPreferencesPort`, `CashflowMovementHistoryPort` beans

## Phase 3: REFACTOR — Backend Verification (PR 1)

- [x] 3.1 REFACTOR: Run `./gradlew.bat test --rerun-tasks`, ensure Phase 1 tests pass green; verify ArchUnit compliance (no framework imports in domain/application, no infrastructure imports in application)
- [x] 3.2 REFACTOR: Review seed counts match spec (3 PROJECTABLE, 2 MANUAL_REVIEW), controller response fields are safe (no stack traces, no internal IDs leaked)

## Phase 4: Cockpit UI — PR 2

- [x] 4.1 RED: `CockpitStaticResourceTest.java` — extend existing `@SpringBootTest` + `MockMvc`; assert `id="demo-reset-btn"` button with "Reiniciar demo" text, demo-only copy (no bank/provider claims), JS endpoint wiring exists
- [x] 4.2 GREEN: `resources/static/index.html` — add `<button id="demo-reset-btn">Reiniciar demo</button>` in cockpit actions panel
- [x] 4.3 GREEN: `resources/static/app.js` — wire button click → `POST /api/cockpit/demo/reset-and-seed?profileId=pharmacy-cl`; safe states: loading text, success → call `refreshCockpitEvidence()`, failure → Spanish error without stack traces
- [x] 4.4 REFACTOR: Run tests, verify UI button copy is demo-only, no bank/provider connectivity claims

## Phase 5: Verify & Archive

- [x] 5.1 Full test suite: `./gradlew.bat test --rerun-tasks`, confirm all green. Verified with `./gradlew.bat --stop` followed by `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks` after the default run hit JVM native memory pressure.
- [x] 5.2 E2E smoke: Playwright MCP — navigate cockpit, click "Reiniciar demo", verify seeded movements/sync/preferences refresh. Passed after fixing transactional sync id conflict handling.
- [x] 5.3 sdd-verify: produce `openspec/changes/pymeflow-cockpit-demo-reset-mvp/verify-report.md`
- [x] 5.4 sdd-archive: merge delta specs into main, move change to archive
