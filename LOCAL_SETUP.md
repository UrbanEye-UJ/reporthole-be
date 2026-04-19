# Running Reporthole Locally

## Prerequisites

Make sure you have the following installed before you start:

| Tool | Version | Download |
|------|---------|----------|
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop |
| Git | Any | https://git-scm.com |

> That's all you need. Java, Maven, and Node are handled inside Docker.

---

## 1. Clone the repo

```bash
git clone <your-repo-url>
cd reporthole
```

---

## 2. Create your `.env` file

At the **project root** (same level as `docker-compose.yml`), create a file called `.env`:

```bash
touch .env
```

Add the following line — ask a teammate for the actual password value:

```env
JASYPT_ENCRYPTOR_PASSWORD=ask_a_teammate_for_this
```

> ⚠️ Never commit `.env` to Git. It's already in `.gitignore`.

---

## 3. Start the app

```bash
docker compose up --build
```

The first run will take a few minutes while Maven downloads dependencies and Docker builds the images. Subsequent starts are much faster.

---

## 4. Verify everything is running

| Service | URL |
|---------|-----|
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui/index.html |
| H2 Console | http://localhost:8080/api/h2-console |
| Actuator Health | http://localhost:8080/api/actuator/health |
| Frontend | http://localhost:3000 |

For the H2 console, use these connection details:
- **JDBC URL:** check with a teammate (it's Jasypt-encrypted in `application-local.yml`)
- **Username / Password:** also Jasypt-encrypted — ask a teammate

---

## 5. Stopping the app

```bash
# Stop containers but keep data
docker compose down

# Stop and wipe H2 volume (fresh DB on next start)
docker compose down -v
```

---

## Common issues

**`JASYPT_ENCRYPTOR_PASSWORD` is wrong**
The app will start but crash immediately trying to decrypt config values. Double-check the value in your `.env` file with a teammate.

**Port 8080 or 3000 already in use**
Something else is running on that port. Either stop it, or change the port mapping in `docker-compose.yml`:
```yaml
ports:
  - "9090:8080"   # change left side only
```

**Docker build fails on Maven**
Try clearing the Docker cache and rebuilding:
```bash
docker compose build --no-cache
```

**H2 console shows blank screen**
Make sure you're accessing it via `http` not `https`, and that the context path `/api/` is included in the URL.

---

## Project structure

```
reporthole/
├── reporthole-be/          # Spring Boot backend (Java 21)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── reporthole-fe/          # PWA frontend (TypeScript)
│   └── Dockerfile
├── docker-compose.yml      # Runs everything together
├── .env                    # Your local secrets (never commit this)
└── .env.example            # Template — copy this to .env
```

---

## Tech stack (for reference)

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.5.9, Java 21 |
| ORM | Spring Data JPA + Hibernate Spatial |
| Database (local) | H2 (in-memory) |
| Database (prod) | PostgreSQL + PostGIS |
| Security | Spring Security, JWT (jjwt 0.12.6) |
| Secrets | Jasypt (PBEWITHHMACSHA512ANDAES_256) |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Frontend | TypeScript PWA (orval for API codegen) |