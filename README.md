# PymeFlow MVP

PymeFlow is a cash-flow cockpit for small businesses. This MVP demonstrates safe transaction ingestion, manual review, cash-flow projections, and a pharmacy-oriented demo profile backed by PostgreSQL.

> **MVP status:** active development. It is suitable for local evaluation and demos, not production financial operations.

## Quick Start

1. Install Docker Desktop.
2. Clone the repository and enter it.
3. Run `docker compose up --build`.
4. Open [the application](http://localhost:8080), [Swagger UI](http://localhost:8080/swagger-ui.html), or [health](http://localhost:8080/actuator/health).

Stop the stack with `docker compose down`. Add `-v` only when you intentionally want to remove local database data.

## Features

- PostgreSQL persistence managed by Flyway migrations.
- Configurable vertical profile with the `pharmacy-cl` development profile.
- Safe transaction ingestion and idempotent source references.
- Manual-review workflow and projection-ready history.
- Transient cash-flow projections, alerts, and recurring obligations.
- Static cockpit UI, REST API, OpenAPI, and Actuator health endpoint.

## Prerequisites

| Tool | Required for | Version |
| --- | --- | --- |
| Docker Desktop with Docker Compose | Full container workflow and PostgreSQL | Current stable release |
| JDK | Local Gradle workflow | 21 |
| Git | Clone the repository | Current stable release |

Gradle is provided by the repository wrapper; do not install Gradle separately.

## Run With Docker

The full stack starts the application only after PostgreSQL reports healthy:

```bash
docker compose up --build
```

The development database is exposed on `localhost:5432`; the application is exposed on `localhost:8080`.

Useful commands:

```bash
docker compose logs -f app
docker compose down
docker compose down -v
```

`docker compose down -v` deletes the `postgres-data` volume and all local demo data.

## Run Java Locally With Docker PostgreSQL

Start only the database:

```bash
docker compose up -d postgres
```

Start the application from the repository root:

```bash
# Windows PowerShell
.\gradlew.bat bootRun

# macOS/Linux
./gradlew bootRun
```

The default local datasource is `jdbc:postgresql://localhost:5432/pymeflow`. Copy `.env.example` to `.env` only if you need to change development defaults; export the variables in your shell before running `bootRun`.

## URLs

| Resource | URL |
| --- | --- |
| Application | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI document | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |

## Demo Workflow

1. Start the application using either workflow above.
2. Open Swagger UI and call `POST /api/cockpit/demo/reset-and-seed?profileId=pharmacy-cl`.
3. Refresh the cockpit at http://localhost:8080.
4. Inspect the seeded history with `GET /api/cashflow/history/manual-review?profileId=pharmacy-cl` and `GET /api/cashflow/history/projection-ready?profileId=pharmacy-cl`.

The reset endpoint deletes and replaces demo data for `pharmacy-cl`; do not use it against non-demo data.

## Environment Variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `PYMEFLOW_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/pymeflow` | JDBC URL for local Java execution. Compose overrides it to the `postgres` container hostname. |
| `PYMEFLOW_DATASOURCE_USERNAME` | `pymeflow` | PostgreSQL username. |
| `PYMEFLOW_DATASOURCE_PASSWORD` | `pymeflow_local` | PostgreSQL password for local development only. |
| `PYMEFLOW_ACTIVE_PROFILE` | `pharmacy-cl` | Active vertical profile. |
| `PYMEFLOW_MOCK_BANK_SETTLEMENTS` | `false` | Enables simulated bank settlements. |
| `PYMEFLOW_MOCK_ACQUIRER_SETTLEMENTS` | `false` | Enables simulated acquirer settlements. |
| `POSTGRES_DB` | `pymeflow` | Database created by the Compose PostgreSQL service. |
| `POSTGRES_USER` | `pymeflow` | PostgreSQL service user. |
| `POSTGRES_PASSWORD` | `pymeflow_local` | PostgreSQL service password for local development only. |

`.env.example` contains non-secret development values. Use real secret management and TLS-enabled PostgreSQL before any production deployment.

## Build, Tests, And Coverage

Start PostgreSQL before running the complete suite because the Flyway and persistence integration tests connect to the local development database:

```bash
docker compose up -d postgres
```

```bash
# Windows PowerShell
.\gradlew.bat test
.\gradlew.bat jacocoTestReport

# macOS/Linux
./gradlew test
./gradlew jacocoTestReport
```

Stop PostgreSQL after the tests with `docker compose down`.

JaCoCo writes HTML coverage to `build/reports/jacoco/test/html/index.html` and XML coverage to `build/reports/jacoco/test/jacocoTestReport.xml`.

## Architecture

The service follows a hexagonal structure: domain rules are isolated from application use cases, which depend on ports implemented by web, persistence, provider, and demo adapters.

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

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Port `5432` or `8080` is in use | Stop the conflicting service or change the host mapping in `docker-compose.yml`. |
| Application cannot connect to PostgreSQL | Run `docker compose ps`, wait for `postgres` to become healthy, and verify datasource variables. |
| Flyway migration error after changing local schemas | For disposable local data only, run `docker compose down -v` and start again. |
| Testcontainers tests fail | Start Docker Desktop and confirm `docker version` succeeds. |
| `JAVA_HOME` error | Install JDK 21 and set `JAVA_HOME` to that JDK. |

## Limitations

- The MVP uses simulated provider integrations and a pharmacy-focused demo profile.
- It has no authentication, authorization, tenant isolation, audit trail, backups, or operational monitoring suitable for production.
- Development PostgreSQL credentials are intentionally public and must not be reused outside local development.
- Forecasts are decision-support demonstrations, not accounting, tax, or financial advice.

## Author

[DevMPoveaCL](https://github.com/DevMPoveaCL)

## License

No license is currently granted. All rights are reserved unless the repository owner adds an explicit license.
