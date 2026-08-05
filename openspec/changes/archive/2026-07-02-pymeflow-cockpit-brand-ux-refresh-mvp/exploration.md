## Exploration: PymeFlow cockpit brand UX refresh MVP

### Current State
The cockpit is a static Spring Boot frontend served from `src/main/resources/static` with plain `index.html`, `styles.css`, and `app.js`; it intentionally has no Node/Vite/React build step. The current visual system already uses receipt/cashflow metaphors, Spanish demo-safe copy, accessible landmarks, and responsive grids, but it is light-only (`color-scheme: light`) and the root-level `favicon.png` / `branding.png` are not served by Spring Boot static resources unless copied or explicitly wired into `src/main/resources/static`.

The existing CSS uses a warm paper/bank palette (`#fbf3df`, `#16345c`, `#197048`, `#a85d1c`) with Georgia/Trebuchet/Courier. The requested source palette from `E:\farmaciauniacc` lives mainly in `src/theme/variables.scss` and uses Raleway, soft lavender surfaces, purple/magenta/cyan accents in light mode, and blue-teal elevated surfaces in dark mode.

### Affected Areas
- `src/main/resources/static/index.html` — needs favicon/brand image links, `color-scheme` metadata, possible theme toggle or system-dark affordance, and minimally adjusted header identity markup.
- `src/main/resources/static/styles.css` — primary refresh surface: token migration, light/dark mode variables, typography, spacing, card sizing, responsive polish, contrast, and asset sizing.
- `src/main/resources/static/app.js` — should remain mostly untouched; only needed if implementing a manual dark-mode toggle with persisted state. Prefer CSS `prefers-color-scheme` for MVP to avoid behavior risk.
- `favicon.png` / `branding.png` — current root assets show a pink/magenta K cap + black cat ears + PymeFlow wordmark; must be copied/wired into static resources without stretching or cropping.
- `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` — existing static contract tests should be extended for favicon/branding, palette/dark-mode markers, no frontend tooling, and safe copy.
- `openspec/specs/pymeflow-mvp-cockpit/spec.md` — current source-of-truth already requires static resources, accessibility, responsive layout, safe Spanish copy, and smokeability.

### Product Domain Exploration
- **Domain:** daily cash cockpit, ledger/cartola, cash runway, receipts/comprobantes, manual review queue, fixture/demo evidence, Chilean CLP movements, SMB operator confidence.
- **Color world:** pharmacy lavender canvas, elevated white cards, deep violet actions, logo magenta highlight, clinical cyan signal, dark operations slate, projected-cash teal, soft paper receipt neutrals.
- **Signature:** a `cash cockpit strip` header: brand image anchored left, demo-safety badge anchored right, and a compact ledger status line below; the visual signature repeats as thin magenta/cyan accent rails on important cards instead of decorative gradients.
- **Defaults to reject:** generic dashboard metric boxes → ledger cards with tabular CLP alignment; random colorful cards → strict token-driven purple/magenta/cyan accents; oversized hero art → contained brand lockup with fixed aspect ratio and max dimensions.

### Palette Found
From `E:\farmaciauniacc\src\theme\variables.scss`:

| Role | Light | Dark |
|------|-------|------|
| Canvas | `#fbf8ff` | `#20262e` |
| Surface | `#f4effb` | `#151f29` |
| Elevated | `#ffffff` | `#162331` |
| Raised | `#ffffff` | `#1c2b38` |
| Inset | `#f0e8fa` | `#101820` |
| Text strong | `#111827` fallback | `#f4f8fb` |
| Text muted | `#52616d` | `#aab9c7` |
| Primary accent | `#7a4db7` | `#0477a0` |
| Secondary accent | `#009fe3` | `#7adfff` |
| Logo magenta | `#c72a8c` | keep as brand highlight, use sparingly |
| Final/action | `#5f329b` / `#512987` | `#0b6f94` / `#0d84ae` |

### Approaches
1. **Token-only palette transplant** — Replace current CSS variables with Farmacia Uniacc token equivalents and add dark-mode overrides.
   - Pros: Smallest implementation, low JS risk, no backend impact, quick smoke via static tests.
   - Cons: Identity asset may still feel bolted-on unless header is adjusted; typography/layout issues only partially solved.
   - Effort: Low

2. **MVP brand shell refresh** — Wire assets, map Farmacia Uniacc light/dark tokens, refine topbar/guide/card rhythm, fix responsive grids and data alignment, keep JS behavior unchanged.
   - Pros: Best identity gain for smallest safe scope; addresses symmetry, block sizing, contrast, dark mode, and brand coherence without destabilizing APIs.
   - Cons: Requires careful CSS regression/smoke across light/dark and narrow viewport.
   - Effort: Medium

3. **Full cockpit redesign** — Rework information architecture, custom controls, motion, and card components.
   - Pros: Highest visual lift.
   - Cons: Too risky for MVP; likely exceeds 400-line review budget and can destabilize static contracts.
   - Effort: High

### Recommendation
Use **MVP brand shell refresh**. Keep the static Spring Boot architecture and existing `app.js` behavior, then focus implementation on:

- Copy or expose `favicon.png` and `branding.png` under static resources; use `<link rel="icon" href="/favicon.png">` and a constrained `<img>` brand lockup with `width`, `height`, `object-fit: contain`, and `max-inline-size`.
- Replace the existing paper/bank token set with app-specific tokens derived from Farmacia Uniacc: `--flow-canvas`, `--flow-surface`, `--flow-surface-elevated`, `--flow-text`, `--flow-muted`, `--flow-violet`, `--flow-cyan`, `--flow-magenta`, `--flow-border`, `--flow-focus`.
- Add `@media (prefers-color-scheme: dark)` CSS overrides and `<meta name="color-scheme" content="light dark">`; avoid a JS toggle in MVP unless explicitly requested.
- Use Raleway if a local/static font can be safely served; otherwise use `font-family: Raleway, system-ui, sans-serif` without fetching remote assets. Keep tabular monospace only for CLP amounts.
- Normalize spacing on an 8px scale, reduce the oversized hero headline, set consistent card padding/radius, and ensure the guide steps and projection rows do not become tiny/cropped at mobile widths.
- Preserve receipt/cashflow semantics, demo-safe Chilean Spanish copy, `DEBIT`/`CREDIT` visibility, positive CLP amounts, landmarks, status roles, and keyboard focus.

### Tests / Smoke Needed
- Extend `CockpitStaticResourceTest` to verify favicon/branding references, `color-scheme` metadata, dark-mode CSS markers, static resource availability for `/favicon.png` and `/branding.png`, and absence of Node/build tooling references.
- Run `./gradlew.bat test --rerun-tasks` after implementation.
- Browser smoke with the running Spring Boot app: open `/`, verify brand image is not stretched/cropped in desktop and mobile viewport, force/inspect dark mode if possible, trigger `Reiniciar demo`, import/sync, review/projection states, and confirm no horizontal scrolling or hidden controls.
- Accessibility smoke: keyboard through skip link, guide, buttons, input/radios/selects; visible focus and readable contrast in light/dark.

### Risks
- Root assets are not automatically served by Spring Boot static resources; implementation must copy or configure them deliberately.
- The brand image has a gray background and wide aspect ratio; using it as a huge hero can feel heavy or cropped. It should be contained, not used as a full-width banner background.
- Farmacia Uniacc dark mode is class-based (`.dark`, `.ion-palette-dark`); PymeFlow static MVP should adapt it to `prefers-color-scheme` unless a toggle is required.
- Palette transplant can reduce semantic clarity if magenta/cyan are overused; keep green/debit/caution semantics accessible or remap them deliberately.
- Existing static tests assert literal copy and static markers; visual refresh should avoid unnecessary copy/JS churn.

### Ready for Proposal
Yes — propose a CSS/HTML-only MVP brand refresh with asset wiring, tokenized light/dark palette, header identity, layout/typography polish, and smoke coverage. Keep JavaScript behavior and backend APIs unchanged unless a manual theme toggle becomes a user requirement.
