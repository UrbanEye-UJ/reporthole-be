# Reporthole — Backend

Spring Boot REST API for the Reporthole civic road-incident reporting platform. Civilians in Gauteng photograph and submit road damage (potholes, cracked surfaces, damaged signs, blocked drains, broken traffic lights) via the mobile web frontend. The backend validates submissions, detects duplicates with PostGIS, stores incidents, and pushes real-time updates to connected users over SSE.

---

## Architecture

```
reporthole-fe (Next.js)
        │
        ▼  REST / SSE
reporthole-be (Spring Boot)
        │
        ├──▶ PostgreSQL + PostGIS   (incidents, users, workflows)
        ├──▶ uploads/incidents/     (image files on local disk)
        └──▶ MailHog / SMTP         (password-reset emails)
```

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.x |
| Language | Java 21 |
| ORM | Spring Data JPA + Hibernate Spatial |
| Database (tests) | H2 in-memory |
| Database (local/dev) | PostgreSQL 15 + PostGIS extension |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Secrets | Jasypt (PBEWITHHMACSHA512ANDAES_256) |
| Mapping | MapStruct 1.5.5 |
| Image storage | Local disk (`uploads/incidents/`) |
| File validation | Apache Tika |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven 3.9+ |
| Real-time | Server-Sent Events (SseEmitter) |

---

## Spring profiles

| Profile | Database | When to use |
|---------|----------|-------------|
| *(none / default)* | H2 in-memory | Running tests — no DB setup needed |
| `local` | PostgreSQL via Docker | IntelliJ dev against the Dockerised DB |
| `dev` | PostgreSQL (encrypted credentials) | Shared dev/staging environment |

---

## Running locally

See [`docs/DEV_SETUP.md`](docs/DEV_SETUP.md) for IntelliJ setup and environment variable configuration.

See [`docs/DOCKER_SETUP.md`](docs/DOCKER_SETUP.md) to run the full stack (FE + BE + DB) with Docker.

**Swagger UI** (when running): `http://localhost:8080/api/swagger-ui/index.html`

---

## Module overview

The codebase is split into vertical slices, each containing its own controller, service, repository, entity, and DTO layers.

| Module | Path | Purpose |
|--------|------|---------|
| `user` | [`src/.../user/`](src/main/java/za/co/urbaneye/reporthole/user/README.md) | Registration, login, profile, password reset |
| `incident` | [`src/.../incident/`](src/main/java/za/co/urbaneye/reporthole/incident/README.md) | Report submission, duplicate detection, SSE, image storage |
| `security` | [`src/.../security/`](src/main/java/za/co/urbaneye/reporthole/security/README.md) | JWT filter, AES encryption, Jasypt config |
| `device` | [`src/.../device/`](src/main/java/za/co/urbaneye/reporthole/device/README.md) | Dashcam device registration and token management |
| `inference` | [`src/.../inference/`](src/main/java/za/co/urbaneye/reporthole/inference/README.md) | On-device ONNX inference and confidence routing |
| `notification` | [`src/.../notification/`](src/main/java/za/co/urbaneye/reporthole/notification/README.md) | Email dispatch (password reset) |
| `global` | [`src/.../global/`](src/main/java/za/co/urbaneye/reporthole/global/README.md) | Shared response envelope, error model, global exception handler |
| `config` | [`src/.../config/`](src/main/java/za/co/urbaneye/reporthole/config/README.md) | Security, async, static resources, web properties |
| `aspect` | [`src/.../aspect/`](src/main/java/za/co/urbaneye/reporthole/aspect/README.md) | AOP execution-time metrics logging |

---

## API endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | Public | Register a new civilian account |
| POST | `/auth/login` | Public | Login; returns JWT in response body |
| POST | `/incidents/create` | JWT | Submit incident or receive duplicate match |
| POST | `/incidents/{id}/confirm` | JWT | Confirm duplicate; increments count, triggers SSE |
| GET | `/incidents/my` | JWT | All incidents the user reported or confirmed |
| GET | `/incidents/{id}` | JWT | Single incident by ID |
| GET | `/incidents/events` | JWT (`?token=`) | SSE stream for real-time incident updates |
| POST | `/devices/register` | JWT | Register a dashcam device, returns token |
| POST | `/inference/frame` | Device token | Submit dashcam frame for ONNX inference |
| GET | `/users/me` | JWT | Authenticated user profile |
| PUT | `/users/me` | JWT | Update profile |
| POST | `/auth/forgot-password` | Public | Request password-reset email |
| POST | `/auth/reset-password` | Public | Reset password via token |

---

## Testing

Every feature must have:
- **Unit tests** (Mockito) — service layer logic, in `src/test/.../service/`
- **Integration tests** (SpringBootTest + H2) — controller behaviour, in `src/test/.../integration/`

H2 does not have PostGIS. Integration tests set `forceCreate: true` on `IncidentRequestDTO` to bypass the `ST_DWithin` duplicate-check query.

Run all tests:
```bash
mvn clean test
```

---

## Contributor rules

- Never commit `.env` or any file with plain-text secrets
- PII fields must be AES-256 encrypted; GPS coordinates must **not** be encrypted (they are public road locations)
- Always branch off `develop` — never commit directly to `main`
- Open a pull request and get a review before merging; the GitHub workflow runs `mvn clean test` automatically
- Every new endpoint needs SpringDoc annotations so the OpenAPI spec stays current for orval regeneration on the frontend
