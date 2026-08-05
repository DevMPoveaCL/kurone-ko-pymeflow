## Exploration: PymeFlow fullviewport dashboard shell MVP

### Current State
The dashboard is a static Spring Boot frontend served from `src/main/resources/static` using plain `index.html`, `styles.css`, and `app.js`; there is no Node/Vite/React build step. The current page already uses the preferred user-facing title `Dashboard de caja`, safe Spanish demo copy, PymeFlow branding assets, light/dark theme preference, guide progress, manual import, provider sync, demo reset, manual review, recommendations, projection, cartola, and receipts.

The problem is not missing capability; it is information density and hierarchy. `index.html` renders every capability inline: topbar, quick nav, guide, cash cards, actions, projection form/results, comprobantes, cartola, and review grid. `styles.css` stacks these as multiple large panels with `1rem` gaps and generous card padding, so desktop users must scroll before completing the core flow. `app.js` is behavior-heavy but selector-driven; most interactions target stable `data-api-target`, `data-action`, and guide attributes, so the shell can be refactored mostly through HTML/CSS if those hooks are preserved.

### Affected Areas
- `src/main/resources/static/index.html` — primary IA change: reorder the dashboard into one fullviewport shell, preserve existing selectors, shorten visible copy, and move secondary evidence out of the main viewport.
- `src/main/resources/static/styles.css` — primary layout change: introduce a viewport-constrained shell grid, compact density tokens, consistent card sizing, fixed panel proportions, overflow boundaries, and modal/drawer styling.
- `src/main/resources/static/app.js` — should stay mostly stable; only add minimal drawer/modal open-close wiring if native anchors/details are not enough. Do not change API contracts.
- `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` — update static contracts for the new IA, preserved selectors, shorter copy, no frontend tooling, modal/drawer affordances, and smokeable accessibility markers.
- `openspec/specs/pymeflow-mvp-cockpit/spec.md` — existing source spec already requires static Spring Boot, safe copy, accessibility, responsive layout, and smokeability; a delta should add fullviewport shell behavior.

### Approaches
1. **CSS-only compression** — Keep current section order and reduce sizes until most content fits.
   - Pros: smallest change, lowest JS risk, likely one PR under the 400-line review budget.
   - Cons: treats symptoms; still leaves redundant sections and weak hierarchy, and narrow laptops may still scroll.
   - Effort: Low

2. **Fullviewport shell MVP** — Recompose the page into a one-viewport operating shell: topbar, key metrics, primary workflow, projection result, and compact review/action area; move evidence-heavy panels to drawers/modals.
   - Pros: directly answers the UX complaint, keeps API/backend behavior intact, preserves static architecture, and gives a reviewable foundation for later polish.
   - Cons: requires coordinated HTML/CSS/test changes and careful keyboard/focus handling for drawers/modals.
   - Effort: Medium

3. **Full dashboard redesign plus interaction model** — Rebuild controls, projection visualization, modal system, and guided flow as a deeper product redesign.
   - Pros: highest UX upside.
   - Cons: too large for the 400-line budget, high regression risk, and unnecessary before proving the shell IA.
   - Effort: High

### Recommendation
Use **Fullviewport shell MVP** as the next change. The recommended IA is:

- **Topbar**: compact brand lockup, `Dashboard de caja`, demo-safe badge, theme toggle, and a single-line status; remove quick-nav redundancy from the primary viewport.
- **Key metrics row**: three equal-height cards for `Caja`, `Entradas`, and `Salidas`; fixed rhythm, tabular CLP, no paragraph-heavy explanations.
- **Primary workflow**: one compact horizontal/segmented flow for `Reiniciar demo`, `Revisar`, `Categorizar`, `Proyectar`; keep guide semantics but shrink it into action state, not a large teaching rail.
- **Projection result**: the visual center/right panel. Show opening balance input, 7/30 controls, CTA, closing projected balance, totals, and alerts in one bounded panel. Long daily balances should scroll inside the panel or open a drawer.
- **Compact review/action area**: show the next pending movement and category selector as a focused task card; keep recommendations as compact chips or secondary list.

Move these to modal/drawer surfaces:
- **Cartola**: right drawer because it is evidence/history, not primary decision content.
- **Comprobantes**: bottom sheet or right drawer because sync/import receipts are audit evidence after actions.
- **Sync/import details**: drawer detail state from the action status, preserving safe provider copy and no secrets.
- **Help/demo copy**: small help modal or disclosure; visible shell copy should stay short.
- **Projection daily rows**: drawer when there are many days; keep only closing balance/totals/alerts in the viewport.

Design rules for the MVP shell:
- Typography: keep `Raleway`/system fallback; use `12px` uppercase labels, `14px` helper/status, `16px` body/control, `20-24px` section titles, `28-36px` primary CLP values. Use monospace/tabular numbers only for amounts.
- Spacing: use an 8px grid; shell gap `12-16px`, card padding `16px`, dense controls `10-12px`, no mixed ad-hoc spacing.
- Card sizing: metrics equal height; projection panel largest; action/review panels bounded; no panel should grow the page unless explicitly scrollable inside.
- Symmetry: align cards to a shared grid, use consistent radius/border intensity, and avoid decorative circles/extra copy where they compete with task hierarchy.

Suggested viewport grid for desktop/laptop: `100svh` body shell with topbar `72-96px`, metrics `104-128px`, and main work area filling the remainder as a 12-column grid: workflow/review on the left, projection/result center/right, drawers overlaid. Mobile should become task-first stacked sections and may scroll; the no-scroll requirement should target desktop/laptop full viewport.

### Risks
- True one-viewport fit depends on actual target height; define the desktop acceptance target explicitly, e.g. `1366x768` or `1440x900`, otherwise the requirement is ambiguous.
- Drawer/modal focus management can become an accessibility regression if implemented casually; prefer native `<dialog>` only if tested, otherwise use simple drawers with buttons, `aria-controls`, focus return, Escape, and visible close controls.
- Static tests currently assert many literal strings and layout markers; they must be updated deliberately without masking selector/API drift.
- Reducing copy may accidentally weaken demo-safe/no-real-bank semantics; keep a persistent safe badge and short disclaimers near sync/projection actions.
- The combined HTML/CSS/test refactor is likely over 400 changed lines if done with drawer behavior and contract updates in one PR.

### Ready for Proposal
Yes — propose a frontend-only static shell refactor with preserved APIs and selectors. Split delivery to protect the 400-line review budget: PR 1 should introduce the fullviewport shell IA and compact CSS while preserving existing inline secondary sections as hidden/drawer-ready surfaces if possible; PR 2 should add modal/drawer polish, detailed focus behavior, and expanded smoke/static coverage. No backend endpoint tweak is currently justified.
