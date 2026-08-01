# Reporthole Backend — Developer Setup Guide

This guide is for team members who want to run the backend locally for development (writing code, debugging, making changes).

---

## What you need

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 21 | https://adoptium.net/temurin/releases/?version=21 |
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop |
| IntelliJ IDEA | Latest (Community is free) | https://www.jetbrains.com/idea/download |
| Maven | 3.9+ (or use `./mvnw`) | Bundled with the project |

Make sure Docker Desktop is running before you start.

---

## 1. Get the `.env` file

The project uses a `.env` file for secrets. **Ask a teammate for this file** — never create it from scratch yourself. Place it in the `reporthole-be/` directory (same folder as this guide).

The file must be named exactly `.env`. Your `reporthole-be/` folder should contain:

```
reporthole-be/
  .env              ← paste here
  pom.xml
  docker-compose.local.yml
  src/
  ...
```

Key variables the file must contain (values from a teammate):
```env
JASYPT_ENCRYPTOR_PASSWORD=
POSTGRES_USERNAME=
POSTGRES_PASSWORD=
POSTGRES_DATA_DIR=postgres-data
REPORTHOLE_AES_KEY=
REPORTHOLE_JWT_KEY=
```

---

## 2. Start the database and mail server

The `local` profile connects to a PostgreSQL + PostGIS database. Start it with Docker:

```bash
docker compose -f docker-compose.local.yml up postgres mailhog -d
```

Verify both containers are running:

```bash
docker ps
```

| Container | Purpose | Port |
|-----------|---------|------|
| `reporthole-postgres` | PostgreSQL 16 + PostGIS | 5432 |
| `reporthole-mailhog` | Fake SMTP for local email testing | SMTP: 1025, Web: 8025 |

MailHog web UI (view sent emails): `http://localhost:8025`

To stop:
```bash
docker compose -f docker-compose.local.yml down
```

---

## 3. Open the project in IntelliJ

1. **File → Open** → select the `reporthole-be` folder
2. IntelliJ detects `pom.xml` and imports it as a Maven project
3. Wait for Maven to finish downloading dependencies

**Enable annotation processing for Lombok:**
`Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing`

**Reload Maven** if needed: right-click `pom.xml` → **Maven → Reload Project**

---

## 4. Configure the run configuration

Click **Edit Configurations** (top-right dropdown) → select or create a Spring Boot configuration for `ReportholeBeApplication`.

Under **Environment variables**, add:

```
SPRING_PROFILES_ACTIVE=local
JASYPT_ENCRYPTOR_PASSWORD=<value from your .env file>
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/reporthole
```

> `application-local.yml` uses `reporthole-postgres` as the DB hostname (only resolvable inside Docker).
> The `SPRING_DATASOURCE_URL` override redirects IntelliJ to the container's exposed port on your local machine.

---

## 5. Run the backend

Click the green **Run** button. The app starts at `http://localhost:8080/api`.

| URL | What it shows |
|-----|--------------|
| `http://localhost:8080/api/actuator/health` | `{"status":"UP"}` when healthy |
| `http://localhost:8080/api/swagger-ui/index.html` | Interactive API documentation |

---

## 6. Run tests

Tests use **H2 in-memory database** (the default profile — no Docker needed):

```bash
./mvnw test --no-transfer-progress
```

This is the same command the GitHub Actions workflow runs.

> **Note:** H2 does not support PostGIS. Integration tests use `forceCreate: true` on `IncidentRequestDTO` to bypass the `ST_DWithin` duplicate-check query.

---

## Spring profiles

| Profile | Database | When to use |
|---------|----------|-------------|
| *(none / default)* | H2 in-memory | Running tests |
| `local` | PostgreSQL 16 + PostGIS via Docker | IntelliJ dev against the Dockerised DB |
| `prod` | PostgreSQL (encrypted credentials) | Production Docker deployment |

The default profile is set to `local` in `application.yaml`. Tests run without a profile set (the H2 auto-configuration kicks in automatically).

---

## Tech stack reference

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.5.9 |
| Language | Java 21 |
| ORM | Spring Data JPA + Hibernate Spatial |
| Database (tests) | H2 in-memory |
| Database (local/prod) | PostgreSQL 16 + PostGIS |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Secrets | Jasypt (PBEWITHHMACSHA512ANDAES_256) |
| Mapping | MapStruct 1.5.5 |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Build | Maven 3.9+ |
