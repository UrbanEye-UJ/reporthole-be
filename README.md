# Reporthole — Backend

Reporthole is a civic infrastructure reporting system for road incidents across Gauteng, South Africa. Residents can report potholes, cracked surfaces, damaged road signs, blocked drains, and broken traffic lights in real time. Reports are tracked through a municipal workflow until resolved.

This repository contains the **backend service** built with Spring Boot.

---

## The problem we're solving

Johannesburg alone has over 10,000 potholes and the Johannesburg Roads Agency holds a backlog of approximately 6,000 unresolved repair requests. The current reporting process lacks structure and transparency — residents have no reliable way to report issues, track repairs, or receive feedback. Reporthole fixes that.

---

## System overview

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────┐
│   Mobile / PWA  │────▶│  reporthole-be        │────▶│   PostgreSQL    │
│   (reporthole-  │     │  Spring Boot (Java 21)│     │   + PostGIS     │
│    fe)          │     │                       │     └─────────────────┘
└─────────────────┘     │  - REST API           │
                        │  - JWT Auth           │     ┌─────────────────┐
                        │  - Image handling     │────▶│  Local disk     │
                        │  - Duplicate detect   │     │  uploads/       │
                        │  - SSE push           │     └─────────────────┘
                        └──────────────────────┘
```

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.x |
| Language | Java 21 |
| ORM | Spring Data JPA + Hibernate Spatial |
| Database (tests) | H2 in-memory |
| Database (local/prod) | PostgreSQL + PostGIS extension |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Secrets | Jasypt (PBEWITHHMACSHA512ANDAES_256) |
| Image storage | Local disk (`uploads/incidents/`) |
| File validation | Apache Tika |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Build | Maven 3.9+ |
| Real-time | Server-Sent Events (SseEmitter) |

---

## Prerequisites

Install these before doing anything else.

| Tool | Download | Notes |
|------|----------|-------|
| Docker Desktop | [docs.docker.com/get-docker](https://docs.docker.com/get-docker/) | Required for PostgreSQL and MailHog |
| Java 21 (JDK) | [adoptium.net/temurin/releases/?version=21](https://adoptium.net/temurin/releases/?version=21) | Required to run and build the app |
| IntelliJ IDEA | [jetbrains.com/idea/download](https://www.jetbrains.com/idea/download/) | Required IDE; Community edition is free |

After installing Docker Desktop, make sure it is running before continuing.

---

## Step 1 — Get the `.env` file

The project uses a `.env` file for secrets. **Do not create this yourself** — ask a teammate for the current `.env` file and paste it directly into the `reporthole-be/` directory (the same folder as this README).

The file must be named exactly `.env` with no extension. Windows Explorer hides files starting with a dot by default — make sure it is saved correctly.

Your `reporthole-be/` folder should look like this once the file is in place:

```
reporthole-be/
  .env                    ← paste here
  docker-compose.local.yml
  pom.xml
  src/
  ...
```

---

## Step 2 — Start PostgreSQL and MailHog

The project uses `docker-compose.local.yml` for local development. This file defines three services: `postgres`, `mailhog`, and `app` (the app container is optional — see below).

Start just the database and mail server:

```bash
docker compose -f docker-compose.local.yml up postgres mailhog -d
```

Verify they are running:

```bash
docker ps
```

You should see `reporthole-postgres` and `reporthole-mailhog` listed.

| Service | What it is | Port |
|---------|-----------|------|
| PostgreSQL + PostGIS | The main database | `5432` |
| MailHog | Fake SMTP server for local email testing | SMTP: `1025`, Web UI: `8025` |

MailHog web UI (view sent emails): [http://localhost:8025](http://localhost:8025)

To stop the containers when you are done:

```bash
docker compose -f docker-compose.local.yml down
```

---

## Step 3 — Run the backend

1. Open the `reporthole-be` folder in IntelliJ as a Maven project. IntelliJ will import dependencies automatically.

2. Enable annotation processing for Lombok:
   `Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing`

3. Open the application entry file:
   `src/main/java/za/co/urbaneye/reporthole/ReportholeBeApplication.java`

4. Before running, add environment variables to the run configuration. Click **Edit Configurations** (top-right dropdown next to the run button), then under **Environment variables** add:

   ```
   SPRING_PROFILES_ACTIVE=local
   JASYPT_ENCRYPTOR_PASSWORD=<value from your .env file>
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/reporthole
   ```

   > `application-local.yml` uses `reporthole-postgres` as the DB hostname, which only resolves inside Docker. The override above redirects IntelliJ to the container's exposed port on your machine.

5. Click the green **Run** button next to the `main` method in `ReportholeBeApplication.java`. The app starts at `http://localhost:8080/api`.

   Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`

---

## Spring profiles

| Profile | Database | When to use |
|---------|----------|-------------|
| *(none / default)* | H2 in-memory | Running tests — no DB setup needed |
| `local` | PostgreSQL via Docker | IntelliJ or CLI dev against the Dockerised DB |
| `dev` | PostgreSQL (encrypted credentials) | Shared dev/staging environment |

---

## H2 Console (tests / default profile only)

When running without a profile (H2 mode):

**URL:** `http://localhost:8080/api/h2-console`

| Field | Value |
|-------|-------|
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:mem:reporthole` |
| User Name | `sa` |
| Password | `password` |

> The H2 database is in-memory with `ddl-auto: create-drop` — data is wiped on every restart. This is expected.

---

## Key rules for contributors

- All API endpoints require authentication — no anonymous access
- Never commit `.env` or any file containing plain-text secrets
- PII fields must be AES-256 encrypted — GPS coordinates must **not** be encrypted
- Every new feature requires unit tests (Mockito) and integration tests (SpringBootTest)
- H2 lacks PostGIS — integration tests use `forceCreate: true` on requests to bypass `ST_DWithin`
- Always branch off `develop`, never commit directly to `main`
- Open a Pull Request and get a review before merging

See [GIT_GUIDE.md](docs/GIT_GUIDE.md) for the full workflow.