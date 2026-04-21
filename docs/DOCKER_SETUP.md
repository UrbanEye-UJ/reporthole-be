# Reporthole Backend — Docker Guide

This guide is for team members who want to **run the backend to test it** without setting up a full development environment. You do not need Java or Maven installed.

If you want to write code and make changes, see [DEV_SETUP.md](DEV_SETUP.md) instead.

---

## What you need to install

| Tool | Version | Download |
|------|---------|----------|
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop |
| Git | Any | https://git-scm.com |

That's it.

> Make sure Docker Desktop is **open and running** before you follow any steps below.

---

## 1. Clone the repo

```bash
git clone <your-repo-url>
cd reporthole/reporthole-be
```

---

## 2. Create your `.env` file

In the `reporthole-be` folder, create a file called `.env`:

```bash
# Mac/Linux
touch .env

# Windows (PowerShell)
New-Item .env
```

Open it and add the following — **ask a teammate for the actual password value**:

```env
JASYPT_ENCRYPTOR_PASSWORD=ask_a_teammate_for_this
```

> ⚠️ Never commit this file to Git. It is already listed in `.gitignore`.

---

## 3. Start the backend

```bash
docker compose -f docker-compose-local.yml up --build
```

The first run takes a few minutes while Docker downloads the base images and Maven downloads dependencies. Subsequent starts are much faster.

You'll know it's ready when you see something like:

```
reporthole-be  | Started ReportHoleApplication in 4.3 seconds
```

---

## 4. Verify it's running

Open your browser and check the following:

| URL | What you should see |
|-----|---------------------|
| http://localhost:8080/api/actuator/health | `{"status":"UP"}` |
| http://localhost:8080/api/swagger-ui/index.html | Swagger API documentation UI |
| http://localhost:8080/api/h2-console | H2 database browser |

---

## 5. Testing the API with Swagger

Swagger UI gives you a visual interface to test all API endpoints without needing Postman.

1. Go to http://localhost:8080/api/swagger-ui/index.html
2. Find the endpoint you want to test
3. Click **Try it out**
4. Fill in any required fields and click **Execute**
5. The response appears below

For endpoints that require authentication, you'll need to log in first and paste the JWT token into the **Authorize** button at the top of the page.

---

## 6. Stopping the backend

```bash
# Stop containers but keep data
docker compose -f docker-compose-local.yml down

# Stop and reset everything (fresh database on next start)
docker compose -f docker-compose-local.yml down -v
```

---

## Common issues

**App crashes immediately on startup**

Your `JASYPT_ENCRYPTOR_PASSWORD` in `.env` is likely wrong. The app cannot decrypt its config values without the correct password. Double-check it with a teammate.

**Port 8080 is already in use**

Something else on your machine is using port 8080. Either stop that process, or change the port in `docker-compose-local.yml`:

```yaml
ports:
  - "9090:8080"   # change the left number only, then access via :9090
```

**Docker build fails**

Try clearing the cache and rebuilding from scratch:

```bash
docker compose -f docker-compose-local.yml build --no-cache
docker compose -f docker-compose-local.yml up
```

**H2 console shows a blank page or connection error**

- Make sure you're using `http://` not `https://`
- Make sure the full path includes `/api/` — the context path is required
- Ask a teammate for the correct JDBC URL and credentials from `application-local.yml`

**Database is empty after restart**

This is expected. The local profile uses `ddl-auto: create-drop`, which means the H2 database is wiped and recreated every time the app restarts. This keeps the local environment clean.

---

## What is H2?

H2 is a lightweight in-memory database used for local development and testing. It means:

- No PostgreSQL installation needed on your machine
- The database is created fresh every time the app starts
- All data is lost when the app stops — this is intentional for local testing

The production environment uses PostgreSQL + PostGIS, but you don't need to worry about that for local testing.