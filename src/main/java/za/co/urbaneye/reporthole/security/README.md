# Module: security

JWT authentication filter, AES-256 field encryption, and Jasypt config decryption. This package is a utility layer — it does not expose any controllers.

---

## Files

```
security/
├── JwtAuthenticationFilter.java   — Spring Security OncePerRequestFilter; validates JWT on every request
├── Jwt.java                       — signs and parses JWTs using jjwt 0.12.6
├── Aes.java                       — AES-256 encrypt/decrypt for PII fields on User
├── JasyptEncryptionUtil.java      — Jasypt PBEWITHHMACSHA512ANDAES_256; used to encrypt config values
└── SecretUtil.java                — loads secrets (JWT key, AES key) from environment / Jasypt
```

---

## JWT

`Jwt.java` signs tokens with HMAC-SHA256. The signing key is loaded from environment via `SecretUtil`. Token expiry is configured in `application.yml`.

`JwtAuthenticationFilter` runs on every request before the controller layer. It checks for a JWT in two places (in priority order):
1. `Authorization: Bearer <token>` header
2. `?token=<jwt>` query parameter — fallback for SSE connections (`EventSource` cannot set custom headers)

Tokens are validated for signature, expiry, and subject. On success, a `UsernamePasswordAuthenticationToken` is placed into the `SecurityContextHolder`.

---

## AES-256 field encryption

`Aes.java` provides `encrypt(String)` and `decrypt(String)` backed by AES/GCM/NoPadding. Used by the `user` module to encrypt PII at rest (name, email, phone number).

**What is encrypted:** user PII fields in the `User` entity.
**What is NOT encrypted:** GPS coordinates on `Incident` — road locations are public and carry no PII.

The AES key is loaded at startup via `SecretUtil` from the `JASYPT_ENCRYPTOR_PASSWORD`-decrypted config.

---

## Jasypt config encryption

`JasyptEncryptionUtil.java` is a standalone command-line utility for generating `ENC(...)` values to store in `application.yml`. The algorithm is `PBEWITHHMACSHA512ANDAES_256`.

To encrypt a new secret:
```bash
# From the reporthole-be directory
mvn jasypt:encrypt-value -Djasypt.encryptor.password=<your-password> -Djasypt.plugin.value=<plain-text-value>
```

The `JASYPT_ENCRYPTOR_PASSWORD` environment variable must be set at runtime for Spring to decrypt `ENC(...)` values in config files. Never hardcode this password in any tracked file.

---

## Spring Security config

The actual `SecurityFilterChain` is in [`config/SecurityConfig.java`](../config/README.md). The `security` package only provides the filter and crypto utilities — it does not configure which endpoints are protected.
