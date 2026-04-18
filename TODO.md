# Reporthole Backend — TODO

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

## 8. GitHub Actions Workflow
- [ ] Create `.github/workflows/test.yml`
- [ ] Workflow should trigger on push and pull request to `main`
- [ ] Workflow steps:
    - Checkout code
    - Set up Java 21
    - Run `mvn test`
- [ ] Set `ENCRYPTION_SECRET_KEY` and `JWT_SECRET` as GitHub repository secrets for the workflow
- [ ] Ensure tests use the `local` profile (H2) so no real database is needed in CI