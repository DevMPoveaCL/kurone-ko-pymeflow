# Design: PymeFlow Cockpit Brand UX Refresh MVP

## Technical Approach

Implement a static HTML/CSS brand-shell refresh for the existing Spring Boot cockpit. Serve the root brand assets from `src/main/resources/static`, wire favicon and brand lockup in `index.html`, map Farmacia Uniacc colors into PymeFlow semantic tokens, and add system dark mode with `prefers-color-scheme` plus a visible light/dark switch. Keep APIs, data selectors, guide state, and Spanish demo-safe copy contracts unchanged; `app.js` may only change for visual theme preference wiring.

## Architecture Decisions

| Decision | Choice | Alternative | Rationale |
|---|---|---|---|
| Assets | Copy `favicon.png`/`branding.png` into static resources and reference `/favicon.png`, `/branding.png`. | Controller/resource handler. | Follows current no-build static model; no backend risk. |
| Theme | `:root` tokens + `@media (prefers-color-scheme: dark)` + `data-theme="light\|dark"` browser override. | System-only theme. | User explicitly needs a visible switch; localStorage stores only visual preference, not business/demo progress. |
| Scope | HTML/CSS/static-test refresh plus minimal visual-preference JS. | API/business redesign. | This is visual identity work; app behavior remains specified. |
| Expression | Lavender/violet/cyan light base with sparse magenta rails; dark mode remaps accent semantics to cyan/blue only. | Full redesign/gradients. | Stronger brand fit while preserving cockpit clarity and the requested dark palette constraint. |

## Data Flow

No runtime API flow changes.

```text
Browser / -> Spring static index.html
  -> /styles.css (light/dark tokens)
  -> /favicon.png + /branding.png
  -> /app.js (existing same-origin API calls + visual theme switch)
```

## Token Map

| Token | Light | Dark | Use |
|---|---:|---:|---|
| `--flow-canvas` | `#fbf8ff` | `#20262e` | page |
| `--flow-surface` | `#f4effb` | `#151f29` | panels/nav |
| `--flow-elevated` | `#ffffff` | `#162331` | cards |
| `--flow-raised` | `#ffffff` | `#1c2b38` | active states |
| `--flow-inset` | `#f0e8fa` | `#101820` | inputs |
| `--flow-text` | `#111827` | `#f4f8fb` | primary text |
| `--flow-muted` | `#52616d` | `#aab9c7` | support text |
| `--flow-violet` | `#7a4db7` | `#0477a0` | actions |
| `--flow-cyan` | `#009fe3` | `#7adfff` | focus/signal |
| `--flow-magenta` | `#c72a8c` | `#7adfff` | semantic accent; cyan/blue in dark mode |
| `--flow-border` | `rgba(17,24,39,.14)` | `rgba(244,248,251,.16)` | borders |

Keep credit/success green, debit/warning amber, and error red as semantic cashflow tokens with dark-mode contrast adjustments. Dark theme brand accents MUST remain in the dark/cyan/blue family; do not render magenta in dark mode.

## Layout / UX Refinements

- Add `<meta name="color-scheme" content="light dark">`, favicon link, a `.brand-lockup` `<img>` with explicit dimensions, `object-fit: contain`, and `max-inline-size`, plus an accessible `#theme-toggle` in the topbar/status area.
- Use max width `min(1120px, calc(100% - 32px))`; reduce `h1` to `clamp(2rem, 5vw, 4rem)`.
- Use `Raleway, system-ui, sans-serif` without remote fetching; keep tabular monospace for CLP amounts.
- Normalize spacing on 8px multiples; card padding `1rem–1.5rem`; radius `18–24px`; quiet borders/shadows.
- Use `repeat(auto-fit, minmax(...))` for guide/cash cards; stack projection/workbench/review below `860px`; single-column controls below `520px`.
- Preserve alignment with `font-variant-numeric: tabular-nums`, visible focus, DEBIT/CREDIT pills, and no horizontal scroll.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/resources/static/favicon.png` | Create | Served browser favicon. |
| `src/main/resources/static/branding.png` | Create | Served header brand lockup. |
| `src/main/resources/static/index.html` | Modify | Metadata, favicon, constrained brand markup. |
| `src/main/resources/static/styles.css` | Modify | Tokens, dark mode, responsive polish. |
| `src/main/resources/static/app.js` | Modify narrowly | Add `data-theme="light\|dark"` visual preference wiring; no API/business behavior change. |
| `src/test/java/com/kuroneko/pymeflow/interfaces/web/CockpitStaticResourceTest.java` | Modify | Asset/theme/no-tooling static contracts. |

## Interfaces / Contracts

Do not change backend endpoints, `data-api-target`, `data-guide-*`, safe Spanish copy semantics, session-only guide behavior, `DEBIT`/`CREDIT` visibility, positive CLP amounts, or Node-free static delivery. Theme storage is limited to the visual `pymeflow.theme` localStorage preference and MUST NOT persist guide/demo/business progress.

## Testing Strategy

| Layer | What | Approach |
|---|---|---|
| Static | Assets, metadata, token markers, dark media query, no JS/API drift. | Extend `CockpitStaticResourceTest`; run `./gradlew.bat test --rerun-tasks`. |
| Visual smoke | Brand containment, light/dark contrast, focus, responsive layout. | Playwright MCP against running app. |
| Flow smoke | Reset, import/sync, review, projection unchanged. | Existing app plus browser interactions. |

## Playwright Smoke Checklist

- Open `/`; favicon and brand image load without stretch/crop.
- Desktop/mobile: no horizontal scroll, hidden controls, or broken card stacking.
- Dark mode: text, focus ring, buttons, inputs, and DEBIT/CREDIT pills remain readable.
- Run reset, manual import/sync, categorization if available, and projection with opening balance.
- Confirm no real-bank claims, secrets, stack traces, or broken landmarks.

## Migration / Rollout

No migration required. Revert by restoring prior `index.html`/`styles.css` and removing copied static assets.

## Review Forecast / Risks

Estimated change: ~250–360 lines, mostly CSS/test assertions. `Decision needed before apply: No`. `Chained PRs recommended: No`. `400-line budget risk: Medium`.

Risks: copied assets may be missed; dark semantic contrast may regress; wide brand image can dominate header; broad CSS replacement can break responsive rows.

## Open Questions

None.
