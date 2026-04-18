# Reporthole Backend — TODO

---

## 1. YAML Secrets Encryption
- [ ] Add Jasypt dependency to `pom.xml`:
  ```xml
  <dependency>
      <groupId>com.github.ulisesbocchio</groupId>
      <artifactId>jasypt-spring-boot-starter</artifactId>
      <version>4.0.4</version>
  </dependency>
  ```
- [ ] Encrypt the following secrets using Jasypt and wrap them in `ENC(...)` in `application-prod.yaml`:
    - `DB_URL`
    - `DB_USERNAME`
    - `DB_PASSWORD`
    - `ENCRYPTION_SECRET_KEY`
- [ ] Set `JASYPT_ENCRYPTOR_PASSWORD` as the only environment variable on Railway/Render and locally in IntelliJ run configuration
- [ ] Never commit the master password — store it in a password manager
- [ ] The encrypted `ENC(...)` values are safe to commit to the repo

> **Note:** Spring Boot was downgraded from 4.0.5 to 3.5.9 specifically for Jasypt `4.0.4` compatibility. Jasypt has known issues with Spring Boot 4.0.x. Do not upgrade Spring Boot to 4.x without verifying Jasypt compatibility first.

---

## 2. AES-256 PII Encryption
- [ ] Place `AesEncryptionConverter.java` in the `common/` package
- [ ] Apply `@Convert(converter = AesEncryptionConverter.class)` to the following `User` fields:
    - `USER_FIRSTNAME`
    - `USER_LASTNAME`
    - `USER_EMAIL`
    - `USER_PHONENUMBER`
- [ ] Add `encryption.secret-key: ${ENCRYPTION_SECRET_KEY}` to `application.yaml`
- [ ] Generate a secure 32-character key using `openssl rand -base64 32` and store it as `ENCRYPTION_SECRET_KEY` in your environment

> **Important notes:**
> - The secret key must be exactly 32 characters (256 bits) for true AES-256 encryption
> - A fresh random IV is generated on every encryption — the same plaintext will produce different ciphertext each time, which is correct behaviour
> - Because of the random IV, you cannot query the database by encrypted email directly
> - To solve the email lookup problem for login, store a separate `EMAIL_HASH` column using SHA-256 (deterministic) purely for lookup — the actual email column remains AES encrypted
> - If the secret key is lost, all encrypted data becomes unrecoverable — store it safely in a password manager

---

## 3. JWT Implementation
- [ ] Add Spring Security and JWT dependencies to `pom.xml`
- [ ] Implement JWT token generation on login (short-lived access token)
- [ ] Implement JWT token validation
- [ ] Implement token refresh strategy
- [ ] Store signing secret as an environment variable `JWT_SECRET`
- [ ] Configure token expiry (access token: short-lived, refresh token: longer-lived)

---

## 4. Security Filters
- [ ] Implement JWT authentication filter to intercept and validate tokens on every request
- [ ] Implement role-based access control (RBAC) using `@PreAuthorize` at the controller level
- [ ] Configure Spring Security filter chain:
    - Permit public endpoints (register, login)
    - Require authentication on all other endpoints
- [ ] Add rate limiting to prevent abuse
- [ ] Add CSP headers for the PWA
- [ ] Ensure all PostGIS queries use parameterised inputs

---

## 5. LOGGING
- [ ] Add log messages with unique ID's to make searching logs easier
- [ ] Add aspect logging for controllers, services and maybe repos
- [ ] Add aspect logging for matrics, to check how long a method took to run

---

## 6. API Documentation
- [ ] Verify `springdoc-openapi-starter-webmvc-ui:2.8.17` is resolving correctly
- [ ] Annotate controllers with `@Operation` and `@ApiResponse`
- [ ] Annotate DTOs with `@Schema`
- [ ] Configure Swagger UI to be disabled in production via `application-prod.yaml`:
  ```yaml
  springdoc:
    swagger-ui:
      enabled: false
    api-docs:
      enabled: false
  ```
- [ ] Confirm OpenAPI spec is accessible at `/api/v3/api-docs` locally
- [ ] Confirm orval can generate typed client code from the OpenAPI spec

---

## 7. Tests
- [ ] **Unit tests** — test service layer logic in isolation using Mockito
    - User registration logic
    - AES encryption/decryption converter
    - JWT token generation and validation
    - Duplicate incident detection logic
- [ ] **Integration tests** — test full request/response cycle using `@SpringBootTest`
    - Auth endpoints (register, login)
    - Incident creation flow
    - Assignment workflow status transitions
    - Notification creation on incident status change
- [ ] Use H2 in-memory database for integration tests (already configured in `application-local.yaml`)
- [ ] Aim for meaningful coverage on service and controller layers

---

## 8. GitHub Actions Workflow
- [ ] Create `.github/workflows/test.yml`
- [ ] Workflow should trigger on push and pull request to `main`
- [ ] Workflow steps:
    - Checkout code
    - Set up Java 21
    - Run `mvn test`
- [ ] Set `ENCRYPTION_SECRET_KEY` and `JWT_SECRET` as GitHub repository secrets for the workflow
- [ ] Ensure tests use the `local` profile (H2) so no real database is needed in CI