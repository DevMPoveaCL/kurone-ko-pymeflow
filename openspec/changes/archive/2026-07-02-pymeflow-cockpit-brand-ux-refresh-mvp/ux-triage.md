# UX triage: projection empty state and fullviewport refactor

## Status

Triage completed without implementation. The projection empty state is most likely a date/horizon semantics mismatch, not stale UI state. The broader UX complaint is valid: the current cockpit is a branded long-form page, not a fullviewport operating cockpit.

## Root cause projection

### Most likely cause

The frontend requests cockpit projection with `startDate = todayIsoDate()` from `app.js`, while demo/reset and smoke data are deterministic June 2026 fixtures:

- `app.js` builds `/api/cashflow/cockpit/projection?...startDate=${todayIsoDate()}&horizonDays=7|30`.
- `CockpitDemoResetService` seeds projectable movements on `2026-06-20`, `2026-06-21`, `2026-06-22` and manual-review movements on `2026-06-23`, `2026-06-24`.
- `CockpitProjectionService.projectFromHistory(...)` calls `historyService.projectionReady(profileId, startDate, endDate)` and returns empty `dailyBalances` when no projectable movements exist in that inclusive range.
- Today for this session is `2026-07-02`; even a newly categorized June 23/24 movement remains outside a 7-day or 30-day projection window starting July 2.
- `renderProjection(...)` maps empty `dailyBalances` to “Categoriza movimientos primero para proyectar caja.”, which is misleading when the real issue is “no hay movimientos dentro del período solicitado”.

### Other candidates checked

| Candidate | Evidence | Likelihood |
|---|---|---|
| Backend categorization not persisting | `CashflowMovementHistoryJdbcAdapter.resolveManualReview(...)` updates `MANUAL_REVIEW` to `PROJECTABLE` and sets `category_key`; controller returns persisted projection-ready response. | Low |
| UI stale state after categorization | `resolveManualReviewMovement(...)` calls `refreshCockpitEvidence()`, which reloads history and calls `fetchProjection(balance)` when opening balance exists. | Low |
| Projection endpoint intentionally empty without projectables | True, but only after date filtering; endpoint returns empty result by design when no in-window `PROJECTABLE` rows exist. | Medium |
| Incorrect smoke assumption | High: verify smoke checked reset summary/projection-ready totals, not “categorize then project from current date”. | High |

## UX diagnosis

The current UI is visually branded but structurally redundant:

- The header/hero consumes too much vertical space before the user reaches the job-to-be-done.
- Quick nav, guide, actions, review, projection, cartola and receipts all compete as page sections.
- Primary work requires scrolling: reset → review → categorize → projection are not visible as one operating flow.
- Cartola and comprobantes are useful evidence, but they are secondary and currently occupy main layout real estate.
- Projection copy and controls are too verbose for a cockpit; it should be a concise decision panel with clear state reasons.
- Empty/error states collapse different conditions into the same message, especially “categorize first” vs “no movements in selected period”.

## Recommended refactor

Do not keep patching cosmetics into the current long page. Move to a fullviewport dashboard shell:

- `100dvh` app shell with compact topbar, brand/status/theme, and no page-level scrolling for the primary cockpit.
- Above-fold primary grid: cash summary, guided next action, manual review queue, and compact projection result/control.
- Replace quick-nav with task-oriented flow state: `Reset demo → Review → Categorize → Project`.
- Keep projection concise: opening balance, horizon, calculated result, and explicit empty-state reason.
- Move cartola, comprobantes, sync/import receipts, and detailed recommendations into modal/drawer panels opened from summary chips/buttons.
- Add state-specific projection messages:
  - no projectable movements at all;
  - projectable movements exist but outside selected period;
  - missing opening balance;
  - projection successful.

## Scope and risk

| Work | Expected size | 400-line risk | Notes |
|---|---:|---|---|
| Projection date/empty-state fix only | ~120-220 changed lines | Low/Medium | Needs tests around seeded dates/current date and copy semantics. |
| Fullviewport shell HTML/CSS refactor | ~400-700 changed lines | High | Major layout rewrite, accessibility and responsive risk. |
| JS state/dialog/drawer behavior | ~250-500 changed lines | High | Needs careful focus management, no Node tooling, static tests. |
| Full UX refactor total | ~700-1200 changed lines | High | Should be split into chained/reviewable work units. |

Suggested split:

1. PR/change A: projection semantics and honest empty states.
2. PR/change B: fullviewport shell + primary cockpit above fold.
3. PR/change C: modal/drawer secondary evidence panels and accessibility smoke.

## Next recommended

Do **not** revert the current brand refresh: it passed its scoped verification and useful assets/theme work can remain. Do **not** force the fullviewport refactor into this active change either; its original proposal explicitly excluded full information-architecture redesign.

Recommended path:

1. Fix or specify the projection date/empty-state issue as a narrow release blocker before archive, if the current branch must be demoable immediately.
2. Archive `pymeflow-cockpit-brand-ux-refresh-mvp` after that narrow correction or after deciding the projection fix belongs to a new change.
3. Start a new SDD change for the serious refactor, e.g. `pymeflow-cockpit-fullviewport-operating-shell`.

## Artifacts

- `openspec/changes/pymeflow-cockpit-brand-ux-refresh-mvp/ux-triage.md`

## Skill resolution

`paths-injected` — read `sdd-explore`, `frontend-design`, `interface-design`, `cognitive-doc-design`, plus shared SDD/OpenSpec conventions.
