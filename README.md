# Reporthole — Backend

Reporthole is a civic infrastructure reporting system for road incidents across Gauteng, South Africa. Residents and dashcam devices can report potholes, cracked surfaces, damaged road signs, blocked drains, and broken traffic lights in real time. Reports are routed to a municipal admin dashboard for tracking and resolution.

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
└─────────────────┘     │  - REST API           │     ┌─────────────────┐
                        │  - JWT Auth           │────▶│   Redis Cache   │
┌─────────────────┐     │  - Image handling     │     └─────────────────┘
│  Dashcam Device │────▶│  - Duplicate detect   │     ┌─────────────────┐
│  (YOLO on-      │     │  - PostGIS queries    │────▶│   Cloudinary    │
│   device)       │     └──────────────────────┘     │   (images)      │
└─────────────────┘              │                    └─────────────────┘
                                 │
                        ┌────────▼─────────┐
                        │  FastAPI ML       │
                        │  Service (Python) │
                        │  YOLO inference   │
                        └──────────────────┘
```

Two reporting modes are supported:

- **Manual** — a civilian photographs a road issue via the mobile app. GPS is captured at the moment of the photo.
- **Dashcam** — an on-device YOLO model detects road damage while driving. High-confidence detections are logged directly; borderline detections are escalated to the backend ML service.

---

## Documentation

Choose the guide that matches what you want to do:

| Guide | Description |
|-------|-------------|
| [DEV_SETUP.md](DEV_SETUP.md) | Set up your machine to **write and run code** — IntelliJ, Java 21, Maven, Lombok, environment variables |
| [DOCKER.md](DOCKER.md) | **Run and test the backend** without installing Java — Docker only |
| [GIT_GUIDE.md](GIT_GUIDE.md) | Git workflow, branch strategy, commit conventions, and how to resolve conflicts |

---

## Quick start (Docker)

If you just want to get the backend running as fast as possible:

```bash
git clone <your-repo-url>
cd reporthole/reporthole-be
touch .env   # add JASYPT_ENCRYPTOR_PASSWORD=<ask a teammate>
docker compose -f docker-compose-local.yml up --build
```

Then open http://localhost:8080/api/swagger-ui/index.html to explore the API.

Full instructions → [DOCKER.md](DOCKER.md)

---

## Quick start (local dev)

If you want to write code and run the app from IntelliJ:

1. Install IntelliJ IDEA and Java 21
2. Open the `reporthole-be` folder as a Maven project
3. Add `JASYPT_ENCRYPTOR_PASSWORD` to your run configuration environment variables
4. Enable annotation processing for Lombok
5. Hit Run

Full instructions → [DEV_SETUP.md](DEV_SETUP.md)

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.5.9 |
| Language | Java 21 |
| ORM | Spring Data JPA + Hibernate Spatial |
| Database (local) | H2 in-memory |
| Database (prod) | PostgreSQL + PostGIS |
| Cache | Redis (Spring Cache) |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Secrets | Jasypt (PBEWITHHMACSHA512ANDAES_256) |
| Image storage | Cloudinary |
| File validation | Apache Tika |
| Mapping | MapStruct 1.5.5 |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Build | Maven 3.9+ |
| Deployment | Railway / Render (free tier) |

---

## Project structure

```
reporthole-be/
├── src/
│   ├── main/
│   │   ├── java/za/co/urbaneye/reporthole/
│   │   │   ├── config/         # Security, CORS, Jasypt config
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── dto/            # Request / response DTOs
│   │   │   ├── entity/         # JPA entities
│   │   │   ├── mapper/         # MapStruct mappers
│   │   │   ├── repository/     # Spring Data JPA repositories
│   │   │   └── service/        # Business logic
│   │   └── resources/
│   │       ├── application.yml           # Base config (all profiles)
│   │       └── application-local.yml     # Local dev config (H2)
│   └── test/
├── docker-compose-local.yml    # Docker setup for local testing
├── Dockerfile
├── DEV_SETUP.md                # How to set up your dev environment
├── DOCKER.md                   # How to run via Docker
├── GIT_GUIDE.md                # Git workflow and conventions
└── pom.xml
```

---

## Key rules for contributors

- All API endpoints require authentication — no anonymous access
- Never commit `.env` or any file containing plain-text secrets
- PII fields must be AES-256 encrypted — GPS coordinates must not be encrypted
- Always branch off `develop`, never commit directly to `main`
- Open a Pull Request and get a review before merging

See [GIT_GUIDE.md](GIT_GUIDE.md) for the full workflow.