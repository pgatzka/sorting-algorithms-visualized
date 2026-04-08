# Sorting Algorithms Visualized

Spring Boot 4.0.5 + Vaadin 25.1.1 (Flow) application with Maven.

## Tech Stack

- **Java 21**, **Spring Boot 4.0.5**, **Vaadin 25.1.1** (server-side Flow model)
- **JPA/Hibernate** with PostgreSQL (runtime) and H2 (tests)
- **Flyway** for database migrations (`src/main/resources/db/migration/`)
- **Lombok** for boilerplate reduction
- **Lumo** theme (configured via `@StyleSheet(Lumo.STYLESHEET)` on `Application.java`)

## Common Commands

```bash
./mvnw                      # Default goal: spring-boot:run (activates quiet + dev profiles)
./mvnw verify -B            # Build, test, spotless check
./mvnw spotless:check       # Check formatting
./mvnw spotless:apply       # Auto-format code
./mvnw sonar:sonar          # Run SonarCloud analysis (requires SONAR_TOKEN)
```

## Project Structure

```
src/main/java/io/github/pgatzka/
  Application.java              # Entry point, AppShellConfigurator
  views/                        # Vaadin Flow views (@Route)
src/main/resources/
  application.yml               # Prod defaults (no credentials, ddl-auto: validate)
  application-quiet.yml         # Banner off, atmosphere logging reduced
  application-dev.yml           # Dev DB credentials, ddl-auto: update
  db/migration/                 # Flyway migrations
src/test/resources/
  application.yml               # H2 in-memory DB for tests
```

## Profiles

- **quiet** - Disables Spring banner, sets `org.atmosphere.cpr` to warn. Active via Maven by default.
- **dev** - PostgreSQL credentials for local development, `ddl-auto: update`. Active via Maven by default.
- Prod datasource is configured via environment variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

## Code Style

- **Google Java Format** enforced via Spotless. Run `./mvnw spotless:apply` before committing.
- Always run spotless before committing to avoid CI failures.

## CI/CD (GitHub Actions)

Pipeline: Spotless Check -> Tests -> SonarCloud (parallel with Docker) -> Docker build & push to GHCR.
- Triggers on push to `main` and pull requests to `main`.
- Docker image published to GHCR, tagged with POM version and `latest`.
- Required secret: `SONAR_TOKEN`.

## Docker

```bash
docker build -t sorting-algorithms-visualized .
docker run -p 8090:8090 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/sorting \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  sorting-algorithms-visualized
```

App runs on port **8090**.
