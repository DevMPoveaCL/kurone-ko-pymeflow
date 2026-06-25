## Exploration: Provider sync observability and operational failure surfaces MVP

### Current State
Provider sync is a synchronous fixture-backed flow exposed by `POST /api/cashflow/provider-syncs` and `GET /api/cashflow/provider-syncs/{syncId}`. The application service orchestrates provider fetch, statement import, cursor/count persistence, and final safe status recording through `SyncSessionPort`. Durable session storage now exists through `JdbcSyncSessionAdapter` and `provider_sync_sessions`, and the API returns safe status/error snapshots without credential echo. Spring Boot Actuator is already on the classpath and `health,info` are exposed, but there is no provider-sync-specific logging, metrics, health/info contributor, or operator-oriented summary beyond the per-`syncId` API/status snapshot.

### Affected Areas
- `src/main/java/com/kuroneko/pymeflow/application/cashflow/ProviderSyncUseCase.java` — central place to emit start/page/result/failure observations without coupling to infrastructure details.
- `src/main/java/com/kuroneko/pymeflow/application/port/out/SyncSessionPort.java` — existing durable snapshot contract can support lightweight operational queries if needed, but should not become a logging/metrics abstraction.
- `src/main/java/com/kuroneko/pymeflow/infrastructure/persistence/JdbcSyncSessionAdapter.java` — source for durable last-known status and potential count queries for actuator contributors.
- `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowProviderSyncController.java` — existing status endpoint already covers per-sync inspection; only minimal response additions should be considered.
- `src/main/java/com/kuroneko/pymeflow/infrastructure/config/ApplicationServiceConfiguration.java` — natural wiring point for any metrics/logging decorator or actuator contributor beans.
- `src/main/resources/application.yml` — actuator exposure already includes `health,info`; metrics exposure/configuration would be opt-in if required.
- `src/test/java/com/kuroneko/pymeflow/**` — tests should protect safe fields, actuator contributor output, and hexagonal boundaries.

### Approaches
1. **Structured logging + Micrometer counters/timer** — Emit safe correlation fields and aggregate metrics for sync attempts, results, duration, entries, and provider error categories.
   - Pros: Smallest operational value; no UI; no new production provider dependency; Micrometer is already available via Actuator; useful before real-provider integration.
   - Cons: Operators need logs/metrics tooling; logs alone do not provide current health; must prevent label/cardinality leaks.
   - Effort: Low

2. **Actuator health/info for provider sync subsystem** — Add a contributor that reports whether durable session storage is reachable and a safe last-known summary.
   - Pros: Fits existing `health,info` exposure; gives operators a single low-friction endpoint; useful for startup/runtime smoke.
   - Cons: Health can be misleading if it claims provider readiness without real providers; details must stay credential-free and low-cardinality.
   - Effort: Low/Medium

3. **Lightweight operational API/audit listing** — Add an operator endpoint such as a recent sync list or summary over durable sessions.
   - Pros: Directly answers “what failed recently?” without knowing a `syncId`; no frontend required.
   - Cons: Expands public API surface, needs pagination/filtering/security decisions, and starts becoming audit history before the model is ready.
   - Effort: Medium/High

4. **Retry/manual recovery readiness** — Add retry eligibility fields/actions based on safe error taxonomy and cursor state.
   - Pros: Prepares real-provider recovery workflows; aligns with rate-limit hints and durable cursor resume.
   - Cons: Without real providers and operator authentication, actions are premature; risks designing recovery semantics from fixtures.
   - Effort: Medium/High

### Recommendation
Use a narrow observability MVP combining approaches 1 and 2. Add structured logs around provider-sync start, page/import progress, and final outcome using only safe fields: `syncId`, `profileId`, `providerType`, `status`, `pagesFetched`, `entriesFetched`, `importedEntries`, `hasMorePages`, `truncated`, `authAborted`, `errorCodes`, and `retryAfterSeconds`; never log `credentialRef`, raw provider payloads, stack traces, cursor contents if considered sensitive, or raw exception messages. Add Micrometer counters/timers with bounded tags (`providerType`, `status`, `errorCode`) and an actuator contributor that reports durable storage reachability plus minimal subsystem metadata. Defer a recent-sync listing API and manual retry action until after real/sandbox provider behavior reveals actual operator needs.

### Risks
- Metrics tags can accidentally create high cardinality if `syncId`, `profileId`, credential references, or raw error text are used as labels.
- Actuator health can overstate readiness; it should say “durable session storage reachable”, not “bank provider healthy”.
- Adding a recent-sync API now may force pagination/security/audit semantics before authentication and real provider contracts exist.
- Structured logs must not duplicate secrets from request DTOs or provider exceptions.
- ProviderSyncUseCase is in the application layer, so direct Spring/Micrometer dependencies there would break hexagonal boundaries unless instrumentation is introduced through a port/decorator or infrastructure adapter.

### Ready for Proposal
Yes — propose a small observability hardening change focused on safe structured logs, bounded Micrometer metrics, and a provider-sync actuator health/info contribution. Explicitly defer UI, real credentials, production provider checks, recent-sync listing APIs, and manual retry/recovery commands.
