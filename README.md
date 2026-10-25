# StaffSheet

Enterprise Workforce & Leave Engine: Spring Boot 3.4, Java 21, JPA/Hibernate, PostgreSQL, Flyway, Spring Security + JWT, MapStruct, springdoc-openapi, Testcontainers.

Project governance docs live in `.ai/`. See `.ai/progress.md` for roadmap status and [`.ai/architecture.md`](.ai/architecture.md) for the design.

## Prerequisites

- JDK 21 or newer (bytecode targets Java 21; verified while building on JDK 25)
- Maven 3.9+
- Docker Desktop (PostgreSQL via Compose, and Testcontainers integration tests)

## Run locally

```bash
cp .env.example .env             # Windows cmd: copy .env.example .env
# Edit .env: set DB_PASSWORD and generate JWT_SECRET with: openssl rand -hex 32
docker compose up -d             # starts PostgreSQL 16
mvn spring-boot:run              # default profile: dev
```

Smoke checks:

- Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Spring Boot reads `.env` through `spring.config.import` (dev convenience), and Docker Compose reads it automatically. `.env` is gitignored. The `dev` and `prod` profiles have no hardcoded credentials: `DB_PASSWORD` and `JWT_SECRET` must be supplied, so startup fails fast if they are missing.

## Profiles

| Profile | Datasource | Schema |
|---|---|---|
| `dev` (default) | PostgreSQL from env vars | Flyway, `ddl-auto: validate` |
| `prod` | PostgreSQL from env vars (required) | Flyway, `ddl-auto: validate` |
| `test` | H2 in-memory | `ddl-auto: create-drop`, Flyway off for fast tests |

Run production locally with `SPRING_PROFILES_ACTIVE=prod` plus all env vars set.

## Tests

```bash
mvn test
```

Unit and H2 integration tests always run. Testcontainers tests require a running Docker daemon; when Docker is unavailable they are skipped via `@Testcontainers(disabledWithoutDocker = true)`. With Docker available, `HealthEndpointTestcontainersTest` starts PostgreSQL 16, applies all Flyway migrations, lets Hibernate validate the schema, and asserts `/actuator/health` is `UP`.
