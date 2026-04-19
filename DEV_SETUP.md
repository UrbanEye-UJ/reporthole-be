# Reporthole Backend — Developer Setup Guide

This guide is for team members who want to run the backend **locally for development** (coding, debugging, making changes). If you just want to test the app without coding, see [DOCKER.md](DOCKER.md) instead.

---

## What you need to install

| Tool | Version | Download |
|------|---------|----------|
| IntelliJ IDEA | Latest (Community or Ultimate) | https://www.jetbrains.com/idea/download |
| Java JDK | 21 | https://adoptium.net/temurin/releases/?version=21 |
| Maven | 3.9+ | https://maven.apache.org/download.cgi — or use the IntelliJ bundled version |
| Git | Any | https://git-scm.com |
| Postman *(optional)* | Latest | https://www.postman.com/downloads |

> **Note:** You do not need PostgreSQL installed locally. The project uses H2 (an in-memory database) for local development. PostgreSQL is only used in production.

---

## 1. Clone the repo

```bash
git clone <your-repo-url>
cd reporthole/reporthole-be
```

---

## 2. Open the project in IntelliJ

1. Open IntelliJ IDEA
2. Click **File → Open** and select the `reporthole-be` folder
3. IntelliJ will detect the `pom.xml` and import it as a Maven project automatically
4. Wait for Maven to finish downloading dependencies (bottom progress bar)

---

## 3. Set up the Jasypt password

All sensitive config values (database credentials, JWT key, AES key) are encrypted using Jasypt. You need a password to decrypt them at runtime.

**Ask a teammate for the `JASYPT_ENCRYPTOR_PASSWORD` value.**

Once you have it, add it as an environment variable in IntelliJ:

1. Open the **Run/Debug Configurations** (top right dropdown → **Edit Configurations**)
2. Select your Spring Boot run configuration (or create one if it doesn't exist: **+ → Spring Boot → select main class**)
3. Click **Modify options → Environment variables**
4. Add the following:

```
JASYPT_ENCRYPTOR_PASSWORD=<password_from_teammate>
```

5. Click **Apply → OK**

> ⚠️ Never hardcode this password in any file. Never commit it to Git.

---

## 4. Set the active profile

The project uses Spring profiles. For local development, the `local` profile is already set as default in `application.yml`:

```yaml
spring:
  profiles:
    active: local
```

This means it will use H2 (in-memory database) and `application-local.yml` config automatically. You don't need to change anything.

---

## 5. Run the application

In IntelliJ, click the **green Run button** or press `Shift + F10`.

The app starts on port `8080` with context path `/api/`.

---

## 6. Verify it's running

Open your browser and check:

| URL | What it shows |
|-----|--------------|
| http://localhost:8080/api/actuator/health | Should return `{"status":"UP"}` |
| http://localhost:8080/api/swagger-ui/index.html | Interactive API documentation |
| http://localhost:8080/api/h2-console | H2 database browser (local only) |

---

## 7. H2 Console access

The H2 console lets you browse the in-memory database directly.

1. Go to http://localhost:8080/api/h2-console
2. Use the JDBC URL, username, and password from `application-local.yml`
3. Ask a teammate if you're unsure what these are (they are Jasypt-encrypted in the config)

> The H2 database resets every time you restart the app because `ddl-auto` is set to `create-drop` in the local profile. This is intentional.

---

## 8. Useful IntelliJ plugins to install

Go to **File → Settings → Plugins** and search for:

| Plugin | Why |
|--------|-----|
| **Lombok** | Required — the project uses Lombok annotations. Without this, you'll see red errors everywhere |
| **MapStruct Support** | Helps with mapper code generation |
| **EnvFile** | Lets you load `.env` files into run configurations |

> After installing Lombok, go to **File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors** and tick **Enable annotation processing**.

---

## Project structure (backend)

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
├── Dockerfile
└── pom.xml
```

---

## Tech stack reference

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.5.9 |
| Language | Java 21 |
| ORM | Spring Data JPA + Hibernate Spatial |
| Database (local) | H2 in-memory |
| Database (prod) | PostgreSQL + PostGIS |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Secrets | Jasypt (PBEWITHHMACSHA512ANDAES_256) |
| Mapping | MapStruct 1.5.5 |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Build | Maven 3.9+ |