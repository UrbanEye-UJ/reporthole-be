# Module: config

Spring configuration classes that wire together the security filter chain, async executor, static file serving, and web base-URL binding.

---

## Files

```
config/
├── SecurityConfig.java        — SecurityFilterChain: public vs protected routes, JWT filter, CORS
├── AsyncConfig.java           — @EnableAsync; configures the thread pool for @Async inference jobs
├── StaticResourceConfig.java  — serves uploads/incidents/ as /uploads/** over HTTP
└── WebProperties.java         — @ConfigurationProperties binding for web.base-url
```

---

## SecurityConfig

Defines which endpoints are public and which require a valid JWT.

**Public (no auth required):**
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`
- `GET /actuator/health`
- `GET /swagger-ui/**`, `GET /v3/api-docs/**`
- `GET /uploads/**` (served by StaticResourceConfig)

All other endpoints require `Authorization: Bearer <jwt>` or `?token=<jwt>`.

`JwtAuthenticationFilter` (from the `security` module) is inserted before `UsernamePasswordAuthenticationFilter`.

CORS is configured here to allow requests from the Next.js frontend origin. Update `allowedOrigins` in `application.yml` if the frontend URL changes.

---

## AsyncConfig

Registers a `ThreadPoolTaskExecutor` for Spring's `@Async` machinery. Used by `AsyncFrameSubmissionService` in the `inference` module to run ONNX inference off the request thread.

Pool size and queue capacity can be tuned in `application.yml` under `inference.async.*`.

---

## StaticResourceConfig

Maps `uploads/incidents/` (relative to the working directory) to the URL path `/uploads/**`. This allows the stored image URLs to be served directly by the backend in development.

In production, images should be served by a CDN or object store (Cloudinary is planned). When that is implemented, this config class can be removed.

---

## WebProperties

`@ConfigurationProperties(prefix = "web")` binding. Exposes `web.base-url` from `application.yml` as a typed bean so services can construct absolute image URLs without hardcoding the host.

`SERVICES_WEB_BASE_URL` environment variable overrides this at runtime in Docker.
