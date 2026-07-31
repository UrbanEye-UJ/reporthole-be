# Module: user

Handles all user identity concerns — registration, login, profile management, and password reset. All reporters must be authenticated; anonymous reporting is not supported.

---

## Package structure

```
user/
├── controller/
│   ├── AuthServiceController.java   — /auth/register, /auth/login, /auth/forgot-password, /auth/reset-password
│   └── UserController.java          — /users/me (GET, PUT)
├── dto/
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── AuthResponse.java
│   ├── ForgotPasswordRequest.java
│   ├── ResetPasswordRequest.java
│   ├── UpdateProfileRequest.java
│   ├── UserProfileResponse.java
│   └── IUserMapper.java             — MapStruct mapper
├── entity/
│   ├── User.java                    — core user, AES-encrypted PII fields
│   ├── UserAuth.java                — hashed password and password-reset token
│   ├── UserRole.java                — CIVILIAN | ADMIN | CONTRACTOR
│   └── UserStatus.java             — ACTIVE | LOCKED | PENDING_VERIFICATION
├── exception/
│   └── UserServiceException.java
├── repository/
│   ├── IUserRepository.java
│   └── IUserAuthRepository.java
├── service/
│   ├── interfaces/
│   │   ├── ILoginService.java
│   │   ├── IRegistrationService.java
│   │   ├── IPasswordResetService.java
│   │   ├── IUserAuthService.java
│   │   └── IUserProfileService.java
│   └── impl/
│       ├── LoginServiceImpl.java
│       ├── RegistrationServiceImpl.java
│       ├── PasswordResetServiceImpl.java
│       └── UserProfileServiceImpl.java
└── util/
    └── LoginServiceUtil.java
```

---

## Entities

### User
Stores the public-facing identity. PII fields (name, email, phone) are AES-256 encrypted at rest using the key from `Aes.java` in the `security` module. GPS coordinates in the `incident` module are **not** encrypted — road locations carry no PII.

### UserAuth
Separate table holding the bcrypt-hashed password and an optional password-reset token with expiry. Kept separate from `User` so auth credentials are never accidentally serialised in API responses.

### UserRole
Enum stored on `User`: `CIVILIAN`, `ADMIN`, `CONTRACTOR`. Contractor is a role on `User`, not a separate entity.

### UserStatus
`ACTIVE` — normal login permitted.
`LOCKED` — blocked from logging in (login-attempt lockout or admin action).
`PENDING_VERIFICATION` — email not yet confirmed.

---

## Auth flow

1. `POST /auth/register` — validates input, AES-encrypts PII, hashes password with BCrypt, persists `User` + `UserAuth`, returns JWT.
2. `POST /auth/login` — loads `UserAuth` by username, verifies BCrypt hash, checks `UserStatus`, signs JWT, returns `AuthResponse`.
3. JWT is validated per-request by `JwtAuthenticationFilter` in the `security` module.

---

## Password reset flow

1. `POST /auth/forgot-password` — generates a UUID token, stores it hashed in `UserAuth`, sends a reset link via `IMailService`.
2. `POST /auth/reset-password` — validates the token and expiry, updates the hashed password, clears the reset token.

---

## Tests

| Test class | Type |
|-----------|------|
| `IUserAuthServiceImplTest` | Unit (Mockito) |
| `IUserAuthRepositoryTest` | Repository (H2) |
| `AuthServiceControllerTest` | Unit (Mockito) |
| `AuthIntegrationTest` | Integration (SpringBootTest + H2) |
