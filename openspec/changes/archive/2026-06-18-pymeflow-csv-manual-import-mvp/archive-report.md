# Archive Report: pymeflow-csv-manual-import-mvp

**Archived**: 2026-06-18
**Mode**: openspec
**Domain**: cashflow-manual-import

## Summary

Per-row-tolerant JSON batch endpoint for manual CSV-like cashflow import. Added `POST /api/cashflow/imports/manual` with flat row contract, programmatic per-row validation, CLP default currency, and delegation to existing `CashflowIngestionService`.

## Verdict

PASS — archived change plus post-verify smoke fixes complete: 31/31 tasks complete, full Gradle suite passing, JaCoCo regenerated, 18/18 spec scenarios compliant, all prior warnings and smoke gaps resolved.

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| cashflow-manual-import | Created (new domain) | Full spec copied to `openspec/specs/cashflow-manual-import/spec.md` |

## Archive Contents

- exploration.md ✅
- proposal.md ✅
- specs/cashflow-manual-import/spec.md ✅ (promoted to main specs)
- design.md ✅
- tasks.md ✅ (31/31 tasks complete after post-verify smoke fixes)
- apply-progress.md ✅
- verify-report.md ✅
- archive-report.md ✅

## Source of Truth Updated

The following main spec now reflects the new behavior:
- `openspec/specs/cashflow-manual-import/spec.md`

## Risks

None. CRITICAL/WARNING-grade issues: none verified. Post-archive smoke gaps were fixed and re-verified.

## Change Notes

- Strict TDD: PR1 (RED tests) + PR2 (GREEN implementation + REFACTOR) + Phase 3 (verification warning fixes) + Phase 4 (post-verify smoke fixes)
- Force-chained PR strategy (feature-branch-chain) kept each review slice under 400 lines
- 15 focused `@WebMvcTest` controller tests + service sensitive-reference replay coverage + full Gradle suite green
- No database migration required; no schema changes; no new infrastructure code
- Reuses `CashflowIngestionService`; post-smoke fix ensures sensitive explicit references use safe fingerprint fallback before lookup/persistence
- Successful manual import response entries now echo provided `rowNumber`, with 1-based positional fallback only when `rowNumber` is absent; flat summary counts remain the documented API shape
- Manual import OpenAPI annotations document both HTTP 200 and all-invalid HTTP 400 responses
- Rollback: delete controller + test + exception-handler additions
