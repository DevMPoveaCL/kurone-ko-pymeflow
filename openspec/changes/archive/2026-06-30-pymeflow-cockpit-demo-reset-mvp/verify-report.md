# Verification Report: Cockpit Demo Reset MVP

## Change

- Change: `pymeflow-cockpit-demo-reset-mvp`
- Mode: OpenSpec
- Verification date: 2026-06-26
- Verdict: **PASS WITH WARNINGS**

## Summary

The cockpit demo reset MVP satisfies the backend reset/seed requirements and the static cockpit UI requirements covered by automated tests. Focused demo, cockpit, architecture, full-suite, and coverage commands passed after reducing Gradle/JVM memory pressure.

Browser/API smoke was later run with Playwright MCP against the live Spring Boot app and PostgreSQL. The reset flow passed after a transactional sync id conflict fix in `JdbcSyncSessionAdapter`.

## Completeness

| Area | Status | Evidence |
|------|--------|----------|
| Phase 0 baseline spec correction | PASS | `cockpit-demo-data` spec is under the active change directory. |
| PR1 backend reset/seed | PASS | Service, adapter, controller, configuration, and demo tests present. |
| PR2 cockpit UI | PASS | Static HTML/JS and `CockpitStaticResourceTest` present. |
| Full automated test suite | PASS | Passed with `--no-daemon --max-workers=1`. |
| Coverage report | PASS | `jacocoTestReport` passed. |
| Manual browser/API smoke | PASS | Playwright clicked "Reiniciar demo"; reset endpoint returned 200; preferences/projection-ready/sync refresh calls returned 200. |
| Archive | PENDING | Archive must happen after smoke/acceptance. |

## Command Evidence

| Command | Result | Notes |
|---------|--------|-------|
| `./gradlew.bat test --rerun-tasks --tests "*Demo*"` | PASS | Completed in 1m31s. |
| `./gradlew.bat test --rerun-tasks --tests "*Cockpit*"` | PASS | Completed in 1m14s. |
| `./gradlew.bat test --rerun-tasks --tests "*ArchitectureTest*"` | PASS | Completed in 41s. |
| `./gradlew.bat test --rerun-tasks` | ENV FAIL then PASS | First run failed from JVM native memory/Mockito ByteBuddy initialization; after `./gradlew.bat --stop`, `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks` passed in 2m39s. |
| `./gradlew.bat --no-daemon --max-workers=1 jacocoTestReport` | PASS | Completed in 56s. |
| `./gradlew.bat test --rerun-tasks --tests "*JdbcSyncSessionAdapterTest"` | PASS | Regression for repeated sync id use passed after conflict fix. |
| `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks --tests "*Cockpit*"` | PASS | Passed after PostgreSQL was healthy. |
| `./gradlew.bat --no-daemon --max-workers=1 test --rerun-tasks` | PASS | Full suite passed after conflict fix. |

## Spec Compliance Matrix

| Requirement | Status | Evidence |
|-------------|--------|----------|
| Demo reset and seed endpoint | PASS | `POST /api/cockpit/demo/reset-and-seed`; `DemoDataControllerTest`; `*Demo*` tests passed. |
| Non-demo profile rejected | PASS | Controller/service tests cover demo-only rejection. |
| Scoped data deletion | PASS | `JdbcDemoDataAdapterTest` covers profile-scoped deletion and preservation of other profile/reference data. |
| Deterministic seed | PASS | `CockpitDemoResetServiceTest` covers projectable movements, manual-review movements, sync status, and preferences. |
| Safe error reporting | PASS | Controller/static tests cover generic safe errors and no raw backend message append. |
| Smokeability | PASS | Live Playwright/API smoke passed against the running app. |
| Cockpit reset control | PASS | `CockpitStaticResourceTest`, `index.html`, and `app.js` cover button and endpoint wiring. |
| Success refreshes evidence | PASS | Static JS contract covers refresh of preferences/profile/movements/recommendations/projection and sync receipt. |
| Failure shows safe state | PASS | Static JS contract covers safe Spanish failure copy while preserving visible data. |
| Demo-only copy | PASS | Static tests cover demo-only wording and no real bank/provider claims. |

## Issues

### CRITICAL

- None.

### WARNING

- The default full-suite command can fail on this Windows environment due native JVM memory pressure. Mitigation used successfully: stop Gradle daemons and run with `--no-daemon --max-workers=1`.

### SUGGESTION

- Consider documenting the low-memory Gradle verification command in project docs or CI notes if this workstation remains constrained.

## Final Verdict

**PASS WITH WARNINGS** — automated verification and live browser/API smoke passed. Remaining warning is environmental: use `--no-daemon --max-workers=1` for reliable full-suite verification on this workstation.

## Live Smoke Evidence

- Page: `http://localhost:8080/` loaded with title `PymeFlow | Cockpit de caja diaria`.
- Console: only `404 /favicon.ico`; no reset/runtime JavaScript error after fix.
- Clicked `Reiniciar demo` via Playwright MCP.
- Network evidence:
  - `POST /api/cockpit/demo/reset-and-seed?profileId=pharmacy-cl` → 200
  - `GET /api/cashflow/provider-syncs/{syncId}` → 200
  - `GET /api/cashflow/cockpit/preferences?profileId=pharmacy-cl` → 200
  - `GET /api/profiles/active/categories` → 200
  - `GET /api/cashflow/history/projection-ready?profileId=pharmacy-cl` → 200
  - `GET /api/cashflow/history/manual-review?profileId=pharmacy-cl` → 200
  - `GET /api/cashflow/recommendations?profileId=pharmacy-cl` → 200
  - `GET /api/cashflow/cockpit/projection?...openingBalance=350000` → 200
- API evidence:
  - reset response: `status=DEMO_RESET_SEEDED`, `movementsSeeded=5`, safe `syncSessionId` returned.
  - preferences response: `openingBalance=350000.00`, `preferredHorizonDays=7`, `balanceSource=USER_ENTERED_MANUAL`.
  - projection-ready response: 3 movements in CLP (`sales`, `acquirer-settlements`, `suppliers`) with CREDIT/CREDIT/DEBIT direction.
