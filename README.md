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

## Running locally — standalone (IntelliJ)

Use this when you want to write and debug code.

### Prerequisites

- Java 21 (e.g. via [SDKMAN](https://sdkman.io): `sdk install java 21-tem`)
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- IntelliJ IDEA (Community or Ultimate)
- Docker Desktop — needed for the PostgreSQL database

### 1. Start the database only

The backend needs PostgreSQL with the PostGIS extension. Start just the DB container:

```bash
cd reporthole-be
docker compose up postgres -d
```

### 2. Open the project in IntelliJ

Open the `reporthole-be` folder as a Maven project. IntelliJ will import dependencies automatically.

Enable annotation processing for Lombok:
`Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing`

### 3. Configure the run configuration

The `application-local.yml` uses `reporthole-postgres` as the DB hostname — that only resolves inside Docker. When running from IntelliJ you need to override it.

In your IntelliJ run configuration add these environment variables:

```
SPRING_PROFILES_ACTIVE=local
JASYPT_ENCRYPTOR_PASSWORD=<ask a teammate>
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/reporthole
```

### 4. Run the application

Hit **Run** in IntelliJ. The app starts at `http://localhost:8080/api`.

Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`

### 5. Run tests

```bash
./mvnw test
```

Tests use H2 in-memory — no Docker required to run tests.

---

## Running via Docker (full stack)

Use this when you want to test the backend without installing Java, or to run the full stack together.

### Prerequisites

- Docker Desktop

### 1. Create your `.env` file

```bash
cd reporthole-be
cp .env.example .env
```

Open `.env` and set the required values:

```env
JASYPT_ENCRYPTOR_PASSWORD=<ask a teammate>
POSTGRES_PASSWORD=reporthole

# URL used to construct image URLs stored in the database.
# Set this to your machine's local IP so images are accessible from other devices.
# Example: SERVICES_WEB_BASE_URL=http://192.168.1.10:8080/api
# Leave blank to auto-detect (works for single-machine testing).
SERVICES_WEB_BASE_URL=
```

### 2. Build and start

```bash
docker compose up --build
```

The backend will be available at `http://localhost:8080/api`.

Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`

### 3. Stopping

```bash
docker compose down
```

To also wipe the database volume:

```bash
docker compose down -v
```

---

## Spring profiles

| Profile | Database | When to use |
|---------|----------|-------------|
| *(none / default)* | H2 in-memory | Running tests — no DB setup needed |
| `local` | PostgreSQL via Docker | IntelliJ dev against the Dockerised DB |
| `dev` | PostgreSQL (encrypted credentials) | Shared dev/staging environment |

---

## H2 Console (tests / local default profile only)

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
