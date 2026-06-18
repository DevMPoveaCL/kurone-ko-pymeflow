# Archive Report: PymeFlow Fingerprint Fallback

**Archived at**: 2026-06-18
**Change**: `pymeflow-fingerprint-fallback`
**Artifact store mode**: OpenSpec

## Verification Gate

| Check | Result |
|-------|--------|
| Verify report verdict | ✅ PASS |
| Tasks total | 15 |
| Tasks complete | 15 |
| Tasks incomplete | 0 |
| CRITICAL issues | None |
| WARNING issues | None |
| Build | ✅ `BUILD SUCCESSFUL` |
| Test suite | ✅ 105 tests, 0 failures |
| JaCoCo | ✅ PASS |

**Gate verdict**: ✅ PASS — no blocking issues. Archive proceeds.

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| `cashflow-ingestion-idempotency` | Created | New domain spec — 7 requirements with 9 GWT scenarios covering explicit reference preservation, blank normalization, deterministic fingerprint fallback, no-reference idempotency, profile-scoped dedup, and history consistency. |

## Archive Contents

| Artifact | Status |
|----------|--------|
| `proposal.md` | ✅ |
| `exploration.md` | ✅ |
| `specs/cashflow-ingestion-idempotency/spec.md` | ✅ |
| `design.md` | ✅ |
| `tasks.md` | ✅ |
| `apply-progress.md` | ✅ |
| `verify-report.md` | ✅ |
| `archive-report.md` | ✅ |

## Source of Truth Updated

The following main spec now reflects the change's new behavior:
- `openspec/specs/cashflow-ingestion-idempotency/spec.md`

## Design Decisions Archived

- **TransactionFingerprint**: Application-layer stateless helper (`fp:v1:<sha256-hex>`)
- **Column reuse**: `source_reference` reused with `fp:v1:` prefix — zero migration
- **Hash algorithm**: SHA-256 lowercase hex, fits `VARCHAR(80)` (70 chars)
- **Normalization**: Pipe-delimited input template `pymeflow|v1|{profileId}|{amount}|{currency}|{date}|{safeDescription}`
- **H2 divergence**: H2 rejected PostgreSQL-style partial index; unfiltered unique index retained for tests

## Implementation Summary

- **Files created**: `TransactionFingerprint.java` (~50 lines)
- **Files modified**: `CashflowIngestionService.java`, `CashflowIngestionServiceTest.java`, `CashflowMovementHistoryServiceTest.java`, `CashflowMovementHistoryJdbcAdapterTest.java`, `CashflowIngestionController.java`
- **Total changed lines**: ~190 (under 400-line budget — single PR)
- **TDD**: Strict RED-GREEN-REFACTOR with triangulation

## SDD Cycle Complete

The change has been fully planned, implemented, verified, and archived.
Ready for the next change.
