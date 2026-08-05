# PymeFlow MVP

PymeFlow helps a Chilean small business turn a scattered view of deposits, charges, and supporting records into one daily cash-flow decision. Instead of treating an incoming bank movement as immediately usable cash, the cockpit separates review, categorization, and projection so the owner can see what is ready, what needs attention, and what the next week or month may look like.

This MVP demonstrates that workflow with a pharmacy-oriented Chilean demo profile, simulated provider data, and PostgreSQL persistence. It is designed for local evaluation and demos, not production financial operations.

![Light PymeFlow dashboard showing the guided demo, cash summary, projection controls, receipt evidence, statement movements, and pending review queue](docs/images/pymeflow-dashboard-light.png)

*The main cockpit keeps the operating sequence visible: review movements first, then project cash from categorized history.*

## The Journey

1. **Start or reset the demo.** Seed the `pharmacy-cl` example to work from a known set of simulated movements. The reset replaces that profile's demo data; never use it with non-demo data.
2. **Review movements.** Read the pending queue before treating incoming or outgoing amounts as projection-ready.
3. **Categorize compatible inflows and outflows.** Assign one active category to each movement. Bank direction (`CREDIT` or `DEBIT`) remains distinct from the business flow classification (`INFLOW` or `OUTFLOW`).
4. **Calculate a seven- or 30-day projection.** Enter a manual opening balance and use categorized movements to explore the expected closing cash position.
5. **Inspect the cartola and evidence.** The statement view preserves movement context, while the receipts area makes the simulated sync and manual-import evidence visible.

![Dark PymeFlow projection view with a manual opening balance, seven- and 30-day choices, projected closing cash, obligations, and alerts](docs/images/pymeflow-projection-dark.png)

*The projection is a decision-support view: its opening balance is manual and does not represent a live bank balance.*

![Mobile dark PymeFlow cockpit showing the guided workflow, projection controls, evidence cards, statement movements, and review queue in a single responsive column](docs/images/pymeflow-mobile-dark.png)

*The same review trail remains available on a narrow screen when a cash check has to happen away from a desk.*

## What You Can Evaluate

- Whether the workflow makes the distinction between a bank movement and a cash-flow category understandable.
- Whether the guided review queue produces enough evidence before a movement enters a projection.
- Whether seven- and 30-day projections, alerts, obligations, cartola, and receipts support a practical daily cash conversation.
- Whether the desktop and responsive mobile cockpit are clear enough for a small-business operator.

## Quick Start

Install Docker Desktop, then run:

```bash
git clone https://github.com/DevMPoveaCL/kurone-ko-pymeflow.git
cd kurone-ko-pymeflow
docker compose up --build
```

Open [PymeFlow](http://localhost:8080). The application waits for PostgreSQL to become healthy; its Docker health endpoint is [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

## Reference

### Demo Reset

Use **Reiniciar demo** in the cockpit to restore the guided `pharmacy-cl` dataset. For API-level evaluation, the same action is available in Swagger UI as `POST /api/cockpit/demo/reset-and-seed?profileId=pharmacy-cl`. Resetting deletes and replaces that profile's demo data.

### Local Java Development

Use JDK 21. The repository includes the Gradle 8.10.2 wrapper, so a separate Gradle installation is not required. Start PostgreSQL, then run the application from the repository root:

```bash
docker compose up -d postgres

# Windows PowerShell
.\gradlew.bat bootRun

# macOS/Linux
./gradlew bootRun
```

The local datasource defaults to `jdbc:postgresql://localhost:5432/pymeflow`. Copy `.env.example` to `.env` only when changing development defaults, and export those values before `bootRun`.

### URLs And Ports

| Resource | URL or port |
| --- | --- |
| Application | [http://localhost:8080](http://localhost:8080) |
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| OpenAPI document | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| Health | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) |
| Application port | `8080` |
| PostgreSQL port | `5432` |

### Environment Variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `PYMEFLOW_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/pymeflow` | JDBC URL for local Java execution; Compose uses the `postgres` container hostname. |
| `PYMEFLOW_DATASOURCE_USERNAME` | `pymeflow` | PostgreSQL username. |
| `PYMEFLOW_DATASOURCE_PASSWORD` | `pymeflow_local` | PostgreSQL password for local development only. |
| `PYMEFLOW_ACTIVE_PROFILE` | `pharmacy-cl` | Active vertical profile. |
| `PYMEFLOW_MOCK_BANK_SETTLEMENTS` | `false` | Enables simulated bank settlements. |
| `PYMEFLOW_MOCK_ACQUIRER_SETTLEMENTS` | `false` | Enables simulated acquirer settlements. |
| `POSTGRES_DB` | `pymeflow` | Database created by the Compose PostgreSQL service. |
| `POSTGRES_USER` | `pymeflow` | PostgreSQL service user. |
| `POSTGRES_PASSWORD` | `pymeflow_local` | PostgreSQL service password for local development only. |

`.env.example` contains non-secret development values. Use real secret management and TLS-enabled PostgreSQL before a production deployment.

### Tests

The complete suite needs the PostgreSQL service because Flyway and persistence integration tests connect to the local development database. The latest verification passed **365 tests**.

```bash
docker compose up -d postgres

# Windows PowerShell
.\gradlew.bat test
.\gradlew.bat jacocoTestReport

# macOS/Linux
./gradlew test
./gradlew jacocoTestReport
```

JaCoCo writes HTML coverage to `build/reports/jacoco/test/html/index.html` and XML coverage to `build/reports/jacoco/test/jacocoTestReport.xml`.

### Architecture

PymeFlow is a Java 21 service built with Spring Boot 3.3.6 and PostgreSQL 16. Its hexagonal structure keeps domain rules separate from application use cases and their web, persistence, provider, and demo adapters.

```text
src/main/java/com/kuroneko/pymeflow/
  domain/          Business concepts and rules
  application/     Use cases and port contracts
  infrastructure/  PostgreSQL, configuration, provider, and demo adapters
  interfaces/web/  REST controllers and HTTP error handling
src/main/resources/
  db/migration/    Flyway schema migrations and development seed data
  static/          Cockpit assets served by Spring Boot
src/test/          Unit, architecture, web, persistence, and integration tests
```

### Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Port `5432` or `8080` is in use | Stop the conflicting service or change the host mapping in `docker-compose.yml`. |
| Application cannot connect to PostgreSQL | Run `docker compose ps`, wait for `postgres` to become healthy, and verify datasource variables. |
| Flyway migration error after changing local schemas | For disposable local data only, run `docker compose down -v` and start again. |
| `JAVA_HOME` error | Install JDK 21 and set `JAVA_HOME` to that JDK. |

### Limitations

- The MVP uses simulated provider integrations and a pharmacy-focused demo profile.
- It does not provide production authentication, authorization, tenant isolation, audit trails, backups, or operational monitoring.
- Development PostgreSQL credentials are intentionally public and must not be reused outside local development.
- Forecasts are decision-support demonstrations, not accounting, tax, or financial advice.

### Author And License

Created by [DevMPoveaCL](https://github.com/DevMPoveaCL). No license is granted; all rights are reserved unless the repository owner adds an explicit license.
