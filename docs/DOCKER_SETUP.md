# Reporthole — Docker Setup Guide

This guide starts the **full local stack** (PostgreSQL, MailHog, backend, frontend) using Docker Compose. It is intended for people who want to run the whole system without setting up a Java or Node development environment.

If you want to write backend code and run the BE from IntelliJ, see [DEV_SETUP.md](DEV_SETUP.md) instead.

---

## What you need

| Tool | Download |
|------|---------|
| Docker Desktop (latest) | https://www.docker.com/products/docker-desktop |
| Git | https://git-scm.com |

Make sure Docker Desktop is open and running before continuing.

---

## 1. Clone the repo

```bash
git clone <your-repo-url>
cd reporthole/reporthole-be
```

---

## 2. Create your `.env` file

Copy the example file:

```bash
cp .env.example .env
```

Open `.env` and fill in the values — **ask a teammate for the secret values**. The variables the stack needs are:

```env
# Required — ask a teammate
JASYPT_ENCRYPTOR_PASSWORD=
POSTGRES_USERNAME=
POSTGRES_PASSWORD=
REPORTHOLE_AES_KEY=
REPORTHOLE_JWT_KEY=
MAIL_USERNAME=
MAIL_APP_PASSWORD=

# Defaults that work for local Docker
POSTGRES_DATA_DIR=postgres-data
SPRING_PROFILES_ACTIVE=local
SERVICES_WEB_BASE_URL=http://localhost:8080/api
SERVICES_WEB_BASE_URL_FRONTEND=http://localhost:3000
NEXT_PUBLIC_API_URL=http://localhost:8080/api
CADDY_SITE_ADDRESS=localhost
CADDY_TLS_MODE=internal
BACKEND_HOST=reporthole-be
BACKEND_PORT=8080
FRONTEND_HOST=reporthole-fe
FRONTEND_PORT=3000
```

> Never commit `.env`. It is listed in `.gitignore`.

---

## 3. Start infrastructure only (recommended for BE development)

If you are running the backend from IntelliJ and only need the database and mail server:

```bash
docker compose -f docker-compose.local.yml up postgres mailhog -d
```

| Container | Purpose | Port |
|-----------|---------|------|
| `reporthole-postgres` | PostgreSQL 16 + PostGIS | 5432 |
| `reporthole-mailhog` | Fake SMTP (view emails in browser) | SMTP: 1025, Web: 8025 |

MailHog web UI (view outgoing emails): `http://localhost:8025`

---

## 4. Start the full stack (BE + FE containerised)

The `reporthole-be` and `reporthole-fe` services are in the `full` profile. Use `--profile full` to include them:

```bash
docker compose -f docker-compose.local.yml --profile full up --build -d
```

The first build downloads base images and Maven/npm dependencies — this takes a few minutes. Subsequent starts are much faster.

You'll know it's ready when:

```bash
docker compose -f docker-compose.local.yml ps
```

…shows all containers as `healthy` or `running`.

---

## 5. Verify it's working

| URL | Expected result |
|-----|----------------|
| `http://localhost:8080/api/actuator/health` | `{"status":"UP"}` |
| `http://localhost:8080/api/swagger-ui/index.html` | Swagger API docs |
| `http://localhost:3000` | Reporthole frontend |
| `http://localhost:8025` | MailHog email viewer |

For endpoints that require authentication: log in via Swagger (`POST /auth/login`), copy the JWT, then click **Authorize** at the top of the Swagger page and paste it.

---

## 6. Stop the stack

```bash
# Stop containers but keep database data
docker compose -f docker-compose.local.yml --profile full down

# Stop and wipe database (fresh DB on next start)
docker compose -f docker-compose.local.yml --profile full down -v
```

---

## Common issues

**App crashes immediately on startup**

`JASYPT_ENCRYPTOR_PASSWORD` in `.env` is wrong. The app cannot decrypt the `ENC(...)` values in `application-local.yml` without the correct password. Double-check with a teammate.

**Port 5432 or 8080 already in use**

Another process is using that port. Stop it, or temporarily change the left-hand port number in `docker-compose.local.yml`:

```yaml
ports:
  - "9090:8080"   # left side = host port; right side = container port — only change the left
```

**Images uploaded locally are not showing in the FE**

The backend stores image URLs using `SERVICES_WEB_BASE_URL`. When running fully containerised, this must point to the externally reachable BE address (default `http://localhost:8080/api`). The FE image proxy (`/api/image-proxy`) rewrites the URL to use the internal Docker hostname (`reporthole-be`) when fetching — so the FE container can always reach stored images.

**Database is empty after a fresh start**

Expected for a first start — there is no seed data. Register a user via the FE or Swagger and submit your first incident.

**Build fails for the FE container**

The FE Dockerfile receives `NEXT_PUBLIC_API_URL` as a build arg. Make sure it is set in `.env`. The default is `http://localhost:8080/api`.

---

## What database does this stack use?

PostgreSQL 16 with the PostGIS extension. The `local` Spring profile connects to `reporthole-postgres` inside the Docker network. **H2 is only used for automated tests** (`./mvnw test`) — it is never used in Docker.
