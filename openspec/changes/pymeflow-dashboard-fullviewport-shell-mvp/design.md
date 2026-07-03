# Design: PymeFlow Dashboard Modular Fullviewport Shell MVP

## Technical Approach

Revise the static dashboard into a real application shell, not a long tutorial page. Keep Spring Boot static assets only: `index.html`, `styles.css`, optional selector-safe `app.js`, and `CockpitStaticResourceTest`. The shell uses `100dvh`: topbar plus compact KPIs stay visible, while a single workspace module is active at a time. Modules are `Revisión`, `Proyección`, `Cartola`, and `Comprobantes`; details move into a drawer/dialog or an in-panel detail area. Existing APIs, `data-*` hooks, IDs, demo guide flow, theme storage, and endpoint URLs remain unchanged.

## Architecture Decisions

| Option | Tradeoff | Decision |
|---|---|---|
| One active module vs. all sections visible | Requires tab state; prevents crammed above-fold UI | Use accessible tabs/module switch with one primary panel visible. |
| Drawer/detail for secondary data vs. page dump | Slight JS/focus cost; feels like software | Cartola/comprobantes/details open in drawer/dialog or panel detail, not as always-visible blocks. |
| Preserve legacy selectors vs. rename around new IA | Less semantic purity; protects demo/API flows | Keep `#revision`, `#proyeccion`, `#cartola`, `#comprobantes`, `data-api-target`, `data-action`, guide hooks. |
| Compact decision copy vs. guide paragraphs | Less explanatory onboarding; better operational density | Remove tutorial paragraphs; keep help only near controls where it changes a user decision. |
| Single oversized PR vs. chained split | Current diff already exceeds budget | Recommend chained split before apply continues. |

## Data Flow

No backend/data-flow changes. UI state only decides which existing target is visible.

    app.js events/data targets ──→ Static DOM targets preserved
             │                         │
             ├── tabs toggle panels ───┤
             └── same APIs unchanged ──┘

Preserve `data-api-target`, `data-action`, `data-guide-*`, `#demo-reset-btn`, `#opening-balance`, `[name="horizonDays"]`, API URLs, and `pymeflow.theme`.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/resources/static/index.html` | Modify | Restructure into topbar, persistent KPI strip, tablist, one active workspace, and drawer/detail host. |
| `src/main/resources/static/styles.css` | Modify | `100dvh` app grid, compact KPI strip, tab panels, internal panel scrolling, mobile no-overflow layout. |
| `src/main/resources/static/app.js` | Modify if needed | Add minimal tab/drawer state and focus restoration only; no API/storage/endpoint changes. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Modify | Static contracts for tabs, dialog/drawer semantics, selector preservation, concise copy, responsive guards. |

## Interfaces / Contracts

Frontend-only structure:

```html
<main id="contenido" class="dashboard-shell" tabindex="-1">
  <header class="topbar">...</header>
  <section class="kpi-strip" data-api-target="cash-summary">...</section>
  <nav role="tablist" aria-label="Módulos del dashboard">...</nav>
  <section id="revision" role="tabpanel">...</section>
  <section id="proyeccion" role="tabpanel" hidden>...</section>
  <section id="cartola" role="tabpanel" hidden>...</section>
  <section id="comprobantes" role="tabpanel" hidden>...</section>
  <aside class="detail-drawer" role="dialog" aria-modal="true" hidden>...</aside>
</main>
```

Accessibility: tabs use `role="tab"`, `aria-selected`, `aria-controls`, roving keyboard or native buttons; inactive panels are `hidden`. Drawer/dialog has labelled title, close button, focus trap while open, Escape close, and focus returns to trigger. Status regions keep `role="status"`/`aria-live="polite"`.

## Responsive Behavior

Desktop `1366x768`: no page-level scroll for the operating shell; topbar, KPI strip, tabs, and one workspace fit within viewport. Workspace contents may scroll internally. KPIs use compact cards, not large hero blocks.

Mobile `390x844`: topbar compresses, KPI strip becomes horizontal snap or tight stacked cards, tabs remain reachable as a segmented scroll row, one panel displays at a time, drawer becomes full-screen. Assert `scrollWidth <= clientWidth`.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Static contracts | Tablist/tab/panel/dialog markup, preserved selectors/APIs, concise copy, no Node tooling | Extend `CockpitStaticResourceTest`; run focused static test. |
| TDD | RED tests for modular IA before implementation | Add failing contracts before changing HTML/CSS/JS. |
| Browser smoke | 1366x768 and 390x844 viewport fit, no horizontal overflow, one active panel, drawer focus | Playwright/MCP smoke; verify keyboard tab order, Escape close, focus return. |

## Migration / Rollout

No migration required. Current working diff is over the 400-line budget (`394 insertions`, `199 deletions` before this pivot). Split into chained PRs: PR1 modular shell/tabs/static contracts; PR2 drawer/detail polish and smoke evidence.

## Open Questions

- None.
