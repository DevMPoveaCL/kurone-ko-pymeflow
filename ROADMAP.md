# PymeFlow Roadmap: Trustworthy Cashflow Decisions

PymeFlow should help a Chilean small-business operator understand whether a financial decision protects or weakens cash availability. The product outcome is not a generic recommendation: it is an explainable comparison between the business's current cash outlook and a proposed decision, with the assumptions, evidence, uncertainty, and next actions visible.

This roadmap is an ordered delivery guide for future work. It intentionally uses **Now**, **Next**, and **Later** rather than calendar commitments.

## Product Vision

PymeFlow becomes a decision-support workspace that connects current and projected **Entradas**, **Salidas**, recurring obligations, balances, and cash gaps. A user can ask what happens if they take financing, defer a purchase, negotiate supplier terms, reduce an expense, or prepay debt. The system must show the baseline, simulate the proposed change deterministically, and explain the trade-offs without making the decision for the user.

**Target outcome:** a user can determine whether a proposed financial action appears affordable for their business cashflow, why that conclusion follows from the available data, which assumptions matter most, and what information or alternatives should be considered next.

## Current Baseline

| Area | Current state |
| --- | --- |
| Product | Public MVP for local evaluation and demos, using a pharmacy-oriented Chilean demo profile and simulated provider data. |
| User flow | Reset the demo; review pending movements; categorize them with **Entrada**/**Salida**-compatible categories; calculate a 7- or 30-day projection from a manual opening balance; inspect alerts, **Cartola**, and **Comprobantes**. |
| Platform | Java 21, Spring Boot 3.3.6, PostgreSQL 16, Flyway, REST/OpenAPI, a static responsive cockpit, Docker Compose, and hexagonal architecture. |
| Existing safeguards | Projection-ready history excludes manual-review and rejected movements; ingestion is idempotent; provider sync is fixture-backed; sensitive text is rejected from projection paths. |
| Verification | Latest verified full suite: **365 tests**. The suite requires PostgreSQL because Flyway and persistence integration tests use it. |
| Not production-ready | No authentication, authorization, tenant isolation, production audit trail, real banking/provider integrations, backups, or complete observability. |

## Delivery Principles

| Principle | Rule |
| --- | --- |
| Human decision authority | PymeFlow supports a decision; it does not approve credit, submit an application, move money, or make a financial action. |
| Deterministic financial core | All financial calculations and scenario comparisons are deterministic, versioned, testable domain or application services. |
| AI is explanatory only | An LLM may summarize, explain, ask clarifying questions, and compare already-computed scenarios. It must never invent values, perform authoritative mathematics, modify financial records, or execute a financial action. |
| Evidence before confidence | The product must state data freshness, provenance, completeness, assumptions, and known gaps. It must say “insufficient data” rather than overclaim. |
| Safe defaults | Scenario evaluation is read-only. Any later persistence or external action requires explicit user confirmation and an auditable boundary. |
| Chilean context, honest claims | Use localized Chilean cashflow terminology and CLP where supported. Do not invent legal, tax, accounting, banking, or regulatory claims. |

## Ordered Delivery Plan

The sequence is deliberate. A persuasive interface cannot compensate for weak identity, incomplete evidence, or untestable financial arithmetic.

| Order | Epic | Outcome | Why it comes here | Suggested stable SDD change name |
| --- | --- | --- | --- | --- |
| 1 | Identity, security, and company isolation | Every record, projection, and scenario is owned by an authenticated user and isolated company. | Decision support cannot safely operate on ambiguous or cross-company data. | `pymeflow-identity-company-isolation` |
| 2 | Audit trail and data provenance | Users can see where a movement, obligation, input, and scenario result came from and when it changed. | Trust and later explainability depend on immutable evidence before using real data. | `pymeflow-audit-provenance-foundation` |
| 3 | Real movement adapters and reconciliation | Provider adapters ingest real movements through ports with durable idempotency, reconciliation, and failure handling. | Projections improve only when source data is reliable and duplicates or gaps are controlled. | `pymeflow-provider-adapter-reconciliation` |
| 4 | Deterministic scenario engine | Read-only, versioned calculations compare a cash baseline against financing and operating scenarios. | This is the authoritative decision-support core and must exist before AI explanation. | `pymeflow-financial-scenario-foundation` |
| 5 | Explainable intelligent assistant | A constrained assistant helps users understand computed scenarios, uncertainty, and next questions. | AI can improve comprehension only after the calculation and evidence contracts are reliable. | `pymeflow-explainable-decision-assistant` |
| 6 | Observability and operational readiness | Production operations can detect data, integration, calculation, and security failures without exposing sensitive information. | A trusted public product needs measurable, recoverable operations. | `pymeflow-operational-readiness` |
| 7 | Controlled public demo and production path | A deliberate release path separates demo data and capabilities from production tenants and providers. | Production exposure must follow, not precede, the security and operational foundations. | `pymeflow-controlled-public-release` |

## Epics And Acceptance Evidence

### 1. Identity, Security, and Company Isolation

| Item | Definition |
| --- | --- |
| Outcome | Authenticated users access only companies and roles explicitly assigned to them. |
| Scope | Identity integration, session security, role/permission model, company identifier at every boundary, tenant-scoped persistence queries, authorization tests, and secret-management policy. |
| Acceptance evidence | Cross-company access tests fail safely; every read/write/scenario API derives company scope from trusted identity; privileged actions require authorization; logs and errors do not disclose tokens or another company's data. |
| Dependencies | Production identity-provider and deployment decisions. |
| Non-goals | Credit approval logic, an AI assistant, or real bank connectivity. |

### 2. Audit Trail and Data Provenance

| Item | Definition |
| --- | --- |
| Outcome | A user can trace a displayed number to a dated source, input, rule, and calculation version. |
| Scope | Immutable audit events for movement lifecycle, manual categorization, source imports, obligation edits, scenario inputs, calculation version, and actor/time metadata; safe evidence views. |
| Acceptance evidence | A scenario response identifies its source snapshot, calculation version, and user-entered inputs; corrections preserve history rather than silently replacing it; audit access is tenant- and role-scoped. |
| Dependencies | Identity and tenant isolation. |
| Non-goals | Full accounting ledger replacement or unrestricted event browsing. |

### 3. Real Movement Integrations and Reconciliation

| Item | Definition |
| --- | --- |
| Outcome | Real provider data can enter through replaceable adapters and become dependable cashflow evidence. |
| Scope | Provider-specific adapters behind application ports, credential references/secrets handling, cursor recovery, idempotency keys, duplicate detection, reconciliation states, source freshness, retries, and operator-safe errors. |
| Acceptance evidence | Replaying provider pages does not duplicate movements; unmatched, missing, and conflicting entries are visible for review; each imported movement retains provider and ingestion provenance; outages degrade safely. |
| Dependencies | Identity/security, audit provenance, provider agreements, and data-retention decisions. |
| Non-goals | Claiming universal bank coverage, storing raw credentials in application data, or automated correction of reconciliation conflicts. |

### 4. Deterministic Scenario Engine

| Item | Definition |
| --- | --- |
| Outcome | The same baseline snapshot and scenario terms always yield the same, explainable result. |
| Scope | Versioned value objects, amortization and cash-impact calculators, scenario comparison, stress inputs, explicit rounding rules, calculation-basis metadata, read-only APIs, and unit/integration tests. |
| Acceptance evidence | Tests cover expected schedules, grace periods, payment frequencies, fees, prepayment rules, invalid terms, boundary dates, and baseline-versus-scenario deltas; API responses expose calculation version and no side effects occur. |
| Dependencies | Current projection contract, data-quality gates, and an agreed money/rate/rounding policy. |
| Non-goals | AI chat, automatic loan applications, financial-record mutation, or legal/tax conclusions. |

### 5. Explainable Intelligent Assistant

| Item | Definition |
| --- | --- |
| Outcome | Users can ask plain-language questions about computed scenarios and receive grounded explanations and next questions. |
| Scope | A read-only orchestration layer that retrieves authorized scenario outputs, assembles a minimized evidence packet, validates structured assistant output, records safe interaction metadata, and supports deterministic fallback. |
| Acceptance evidence | The assistant cites only supplied computed data; it never changes records or invokes financial actions; unavailable or unsafe AI responses fall back to deterministic scenario views; adversarial prompt-injection tests pass. |
| Dependencies | Epics 1, 2, and 4, plus an approved model/vendor, privacy review, and prompt/logging policy. |
| Non-goals | Autonomous agents, authoritative financial advice, hidden scoring, or use of imported free text as trusted instructions. |

### 6. Observability and Operational Readiness

| Item | Definition |
| --- | --- |
| Outcome | Operators can observe availability, data freshness, import health, scenario errors, and security events while minimizing sensitive data exposure. |
| Scope | Health checks, bounded metrics, structured sanitized logs, traces/correlation, alerts, backup/restore drills, migration controls, incident runbooks, and retention controls. |
| Acceptance evidence | Restore procedures are exercised; alerts identify stale data and failed imports; metrics avoid high-cardinality personal or financial values; a failed dependency has a documented degraded mode. |
| Dependencies | Hosting, monitoring, backup, and incident-response choices. |
| Non-goals | Exposing raw provider payloads, credentials, prompts, or financial records in telemetry. |

### 7. Controlled Public Demo and Production Path

| Item | Definition |
| --- | --- |
| Outcome | Demo use remains explicit and isolated while production access is gated by completed controls. |
| Scope | Environment separation, demo-data reset isolation, onboarding, feature flags, release checklist, support boundary, consent copy, and production-readiness review. |
| Acceptance evidence | Demo endpoints and fixture data cannot affect production tenants; public capability is feature-gated; release evidence confirms security, audit, backup, and observability requirements. |
| Dependencies | Completion of the preceding operational and security foundations. |
| Non-goals | A calendar launch commitment or a claim that a demo is a production financial service. |

## Intelligent Decision Support: Required Capability

The long-term assistant is a presentation layer over a deterministic financial model. It must build its answer from an authorized, time-stamped business snapshot that includes:

- Current and projected **Entradas** and **Salidas** by date and category.
- Known recurring obligations and their due dates.
- Current opening balance and projected balances over the selected horizon.
- Expected cash gaps, the date and depth of each gap, and the 30-day outlook.
- Existing known debt, scheduled payments, and configured liquidity policy.
- Data freshness, categorization coverage, income confidence, and missing information.

It should answer questions such as “Can the business absorb this purchase?”, “What if a customer payment arrives late?”, “Does prepaying this debt improve the buffer?”, and “Which assumptions make this loan unsafe?” It must use only scenario outputs calculated by the deterministic engine.

### Loan Scenario Contract

The loan scenario input must capture, at minimum:

| Input | Required evaluation use |
| --- | --- |
| Principal | Amount financed and cash received. |
| Term | Payment count and end date. |
| Rate type and rate | Fixed, variable, or indexed treatment; rate basis and reset assumptions must be explicit. |
| Fees | Upfront, periodic, and closing costs. |
| Taxes and insurance | Cash outflows and whether they are financed or paid separately. |
| Grace period | Interest-only, payment holiday, or deferred-payment behavior. |
| Payment frequency | Monthly, weekly, or other supported schedule. |
| Prepayment terms | Allowed amount/timing, fees or penalties, and recalculation rule. |
| Collateral or guarantees | Qualitative exposure to disclose; not a legal assessment. |
| Intended use | Expected timing and cashflow effect of the financed investment or purchase. |

For each supported set of inputs, the engine must calculate and display:

- Payment schedule and component breakdown according to the documented calculation basis.
- Total cash cost and, where inputs permit, an effective-cost calculation with its method and exclusions.
- Monthly or weekly cash impact aligned to payment frequency.
- Minimum projected balance and liquidity buffer after every payment.
- Baseline-versus-scenario impact over time, including cash-gap dates and changes in the 30-day outlook.
- Stress cases, such as lower Entradas, delayed inflows, higher variable rates, or an earlier/later expense, with stated ranges.
- Break-even and affordability indicators defined by explicit policy, not a hidden score.

The resulting explanation must answer: whether the loan **appears** affordable under the selected assumptions; the reasons for that result; material risks; missing data; and confidence limits. It must not represent the result as regulated financial, accounting, tax, or legal advice.

### Recommendation Playbook

When a scenario exposes a weak liquidity buffer or excessive cost, the assistant may present applicable, evidence-backed options:

| Topic | Questions and actions to surface |
| --- | --- |
| Expense reduction | Which discretionary or adjustable **Salidas** could be reduced, delayed, or phased without assuming they are actually removable? |
| Credit-cost levers | Can the user request a lower rate, fewer origination fees, different payment frequency, insurance alternatives, or better prepayment terms? |
| Principal prepayment | Under the documented contract assumptions, when does prepayment reduce interest enough to justify the cash reduction, and when does it weaken the liquidity buffer? |
| Bank conversation | Ask for the payment table, rate basis and reset rules, total cost disclosure, fees, taxes, insurance, grace-period treatment, prepayment formula, and required guarantees. |
| Contract inspection | Inspect rate-change clauses, fees, insurance requirements, penalties, collateral/guarantees, payment-date rules, delinquency consequences, and early-settlement terms. This is a checklist, not legal advice. |
| Alternatives | Compare delaying the decision, reducing principal, shortening or lengthening term, a staged purchase, supplier payment terms, and internal cash accumulation. |

The system must label these as options to investigate. It must never imply that a negotiated outcome, legal interpretation, tax treatment, or bank approval is known unless supplied as verified input.

## Explainability Contract

Every scenario answer, whether rendered directly or summarized by an assistant, must include the following fields. No unexplained recommendation score is permitted.

| Required disclosure | What the user sees |
| --- | --- |
| Data snapshot | Timestamp, company scope, source freshness, and data included/excluded. |
| Assumptions | User inputs, defaults, policy choices, rate treatment, and stress assumptions. |
| Calculation basis | Formula or method, rounding rule, schedule convention, and calculation version. |
| Comparison | Baseline versus scenario results, including changes by date and over the selected horizon. |
| Projected impact | Payment schedule, balances, cash gaps, minimum balance, and liquidity buffer over time. |
| Reasons and risks | The specific cashflow conditions driving the result and the risks that could invalidate it. |
| Missing data and confidence | Gaps in categorization, debt terms, source freshness, or expected income, and their impact on confidence. |
| Sensitivity | The material variables and tested stress cases that change the outcome. |
| Next questions/actions | Concrete information to collect or alternatives to compare before acting. |

## Data Prerequisites And Quality Gates

Scenario evaluation must validate input quality before calculating a recommendation-like conclusion.

| Gate | Minimum condition | Graceful degradation |
| --- | --- | --- |
| Freshness | The snapshot has a visible as-of timestamp and source status. | Show stale-data warning; do not present a strong affordability conclusion. |
| Completeness | Required movement periods and opening balance are available. | Calculate only disclosed partial scope or state “insufficient data.” |
| Categorization | Movements affecting the horizon are classified as **Entrada** or **Salida** where required. | Exclude or flag uncategorized items and explain the exposure. |
| Recurring obligations | Detected obligations are confirmed, rejected, or labeled unconfirmed. | Use only confirmed obligations in a decisive result; disclose unconfirmed candidates separately. |
| Income confidence | Expected **Entradas** have a confidence/source status. | Apply conservative stress assumptions or avoid affordability claims. |
| Existing debt | Known debt payments and timing are captured. | State that debt-service capacity is incomplete. |
| Loan terms | Rate, fees, payment timing, and prepayment terms are sufficient for the requested calculation. | Calculate only what the inputs support and name omitted cost components. |
| Emergency liquidity | A company liquidity-buffer policy is supplied or explicitly absent. | Show balances without passing a buffer-based affordability threshold. |

The UI and API must distinguish “calculation completed” from “decision confidence is adequate.” A valid arithmetic result does not erase incomplete source data.

## Safety, Privacy, and Ethics

- PymeFlow provides decision support, not regulated financial, accounting, tax, or legal advice.
- A user must explicitly confirm any later action that persists a scenario, changes a financial record, or contacts an external provider. Scenario calculation itself remains read-only.
- Collect and send only the data needed for the stated calculation or explanation. Do not include sensitive movement descriptions, credentials, personal data, or raw financial records in prompts or logs.
- Treat imported movement descriptions, uploaded documents, and provider text as untrusted data. Do not let them alter system instructions, tool permissions, calculation rules, or data-access scope.
- Validate and constrain assistant input/output; isolate system instructions; allow only authorized computed scenario data; and test prompt-injection and data-exfiltration attempts.
- When AI is unavailable, blocked, or unsafe, return the deterministic scenario result and explainability contract without an AI-generated narrative.
- Localize terminology for Chilean small businesses while requiring verified sources for any legal, tax, banking, or regulatory statement. Do not infer those claims from a model response.

## Decisions Already Made

| Decision | Consequence for future work |
| --- | --- |
| The current product is a public MVP and demo, not production financial operations. | Do not represent fixture data, manual balances, or demo behavior as live financial truth. |
| The cockpit uses **Entradas**, **Salidas**, **Cartola**, **Comprobantes**, and a manual opening balance. | Preserve current UI terminology and make data origin visible in new flows. |
| The codebase is hexagonal with REST/OpenAPI, PostgreSQL/Flyway, and static cockpit assets. | Add providers and scenario APIs through ports/adapters and keep financial rules outside web/UI code. |
| Existing ingestion and provider sync emphasize safe, idempotent, fixture-backed behavior. | Extend idempotency and provenance; do not bypass the anti-corruption boundary. |
| Financial decisions must be explainable and human-controlled. | Build deterministic calculations before adding any LLM-assisted interaction. |

## Open Product Decisions

Resolve these with product, security, and domain stakeholders before the dependent epic becomes implementation work.

| Decision | Why it matters |
| --- | --- |
| Company and user model | Defines tenant boundaries, roles, delegation, and data retention. |
| Supported financial products | Determines initial calculators: term loans only, lines of credit, supplier finance, leasing, or others. |
| Money, rate, and rounding policy | Affects reproducibility of schedules and comparison results. |
| Affordability and liquidity policy | Defines thresholds, emergency buffer behavior, and when a result is merely informational. |
| Data freshness windows | Determines when source data becomes stale for each provider and manual workflow. |
| Obligation detection workflow | Clarifies what can be inferred, what needs user confirmation, and how corrections are audited. |
| AI vendor and data boundary | Determines residency, retention, model access, prompt minimization, and fallback requirements. |
| Production provider scope | Determines adapter priorities, agreements, credential model, and reconciliation expectations. |

## Risks And Success Metrics

| Risk | Control |
| --- | --- |
| Confident answers from incomplete or stale cash data | Data gates, explicit confidence limits, and “insufficient data” outcomes. |
| Incorrect or untraceable financial math | Versioned deterministic services, golden test cases, published calculation basis, and audit evidence. |
| Cross-company or sensitive-data exposure | Identity, tenant isolation, least privilege, data minimization, sanitized logs, and adversarial tests. |
| AI hallucination or prompt injection | Computed-data-only context, structured output validation, no action tools, and deterministic fallback. |
| Users interpret support as regulated advice | Persistent disclosure, transparent assumptions, no approval language, and explicit user confirmation. |
| Real provider data creates duplicates or gaps | Adapter contracts, idempotency, reconciliation states, freshness indicators, and safe recovery. |

| Metric | Signal of success |
| --- | --- |
| Data readiness | Share of evaluated scenarios meeting freshness, categorization, obligation, and debt-data gates. |
| Calculation reliability | Deterministic schedule and comparison test pass rate across supported products and edge cases. |
| Explainability coverage | Share of scenario responses containing every explainability-contract field. |
| User comprehension | Users can identify the baseline, minimum balance, key assumption, principal risk, and next action after a scenario review. |
| Safety | No cross-tenant reads, no sensitive prompt/log leakage, and no assistant-originated financial mutations. |
| Operational quality | Measured import freshness, reconciliation exceptions, scenario failures, restore evidence, and degraded-mode behavior. |

## Definition Of Done: Intelligent Assistant Milestone

The intelligent assistant milestone is complete only when all conditions below are met:

- [ ] Authorized users can request a read-only explanation of an existing deterministic scenario for their company only.
- [ ] The explanation uses a versioned scenario result and displays every field in the explainability contract.
- [ ] The loan scenario supports the documented terms and produces a payment schedule, total cost where inputs permit, cash impact, minimum balance, liquidity buffer, stress cases, and baseline comparison.
- [ ] Recommendations distinguish computed facts, assumptions, risks, alternatives, and missing data; no hidden score is used.
- [ ] The assistant never supplies authoritative financial math, invents data, writes financial records, or executes an external/financial action.
- [ ] Prompt input is minimized and sanitized; imported descriptions and documents cannot override instructions or access controls.
- [ ] AI failure or unavailability returns a usable deterministic scenario view.
- [ ] Tests cover authorization, tenant isolation, calculation correctness, incomplete-data degradation, output schema validation, privacy/logging controls, and prompt-injection defenses.
- [ ] Product, security, privacy, and domain owners approve the supported-use and disclosure boundary.

## Recommended Next Implementation Slice

Start with **`pymeflow-financial-scenario-foundation`**. It is intentionally small enough for a new session and establishes the financial core before any chat or LLM work.

This is a bounded exception to the production delivery order above: it may be developed now only as a local, read-only, single-demo calculation capability with no real customer data. Identity, company isolation, and audit provenance remain mandatory before scenarios use production data, are persisted, or become available to an intelligent assistant.

| Item | First-slice scope |
| --- | --- |
| Outcome | Compare one manual, read-only fixed-rate term-loan scenario against an existing cash projection baseline. |
| Domain | Loan-terms value objects with principal, term, fixed rate, payment frequency, optional upfront fees, and explicit validation. |
| Calculation | Deterministic amortization calculator with documented rounding and payment schedule output. Do not claim effective cost when the supplied inputs cannot support it. |
| Comparison | Apply scheduled payments to the existing baseline projection and return baseline-versus-scenario balances, minimum balance, and per-payment liquidity buffer. |
| API | A read-only endpoint returning calculation version, inputs, schedule, comparison, assumptions, and data gaps. |
| Tests | Unit tests for normal/invalid terms, rounding, final payment, payment dates, and baseline comparison; web/integration tests for the read-only contract. Run with PostgreSQL. |
| Explicit non-goals | No chat, LLM, persistence of scenarios, variable rates, grace periods, prepayment, bank integration, credit application, or automatic recommendation. |

This slice should first reuse the current projection concepts and terminology rather than redesigning the cockpit. The next slice can add additional terms only after the fixed-rate schedule and comparison contract are well tested.

## Fresh Session Handoff

Use this sequence before continuing development:

1. Read `README.md`, this `ROADMAP.md`, and the current specifications in `openspec/specs/`, starting with `pymeflow-mvp-cockpit`, `cashflow-provider-sync`, and `cashflow-ingestion-idempotency`.
2. Inspect the working tree with `git status --short`, confirm the current branch and `main` state with `git branch --show-current` and `git log -1 --oneline`, and do not overwrite unrelated user changes.
3. Confirm the current implementation and test baseline before changing behavior; use the source and tests as the authority when documentation differs.
4. Use SDD only if the user explicitly selects it in that future session. Do not create or advance an SDD change by default.
5. Propose the first focused change: `pymeflow-financial-scenario-foundation`, limited to deterministic loan terms, amortization, baseline-versus-scenario comparison, tests, and a read-only API.
6. Preserve terminology matching the current UI: **Entrada**, **Salida**, **Cartola**, **Comprobantes**, **Reiniciar demo**, and the fact that the opening balance is manual rather than a live bank balance.
7. Run the test suite with PostgreSQL available: `docker compose up -d postgres`, then `.\gradlew.bat test` on Windows or `./gradlew test` on macOS/Linux. Confirm Flyway/persistence tests can connect before relying on test results.

## Roadmap Review Checklist

- [ ] New work follows the ordered dependencies or documents why an exception is safe.
- [ ] New financial calculations are deterministic, versioned, tested, and exposed through a read-only boundary first.
- [ ] New recommendation UX surfaces data quality, assumptions, formulas/basis, and uncertainty.
- [ ] No change claims production readiness, real provider connectivity, or regulated advice without verified implementation and approval.
- [ ] The roadmap and affected specifications are updated when product scope or a completed epic changes.
