# Module: notification

Email dispatch. Currently used only for password-reset emails. The module is designed as a thin wrapper so the mail provider can be swapped without touching the `user` module.

---

## Package structure

```
notification/
├── entity/
│   └── Notification.java              — optional: persisted notification record (if used)
├── repository/
│   └── (notification repository, if persisted)
├── exception/
│   └── (notification exceptions)
└── service/
    ├── interfaces/
    │   └── IMailService.java          — sendPasswordResetEmail(to, resetLink)
    └── impl/
        └── MailServiceImpl.java       — JavaMailSender backed by MailHog (local) or SMTP (dev)
```

---

## Mail configuration

Mail settings are in `application.yml` / `application-local.yml`, encrypted with Jasypt. For local development, mail is routed to **MailHog** — a fake SMTP server that captures outgoing emails without delivering them.

| Service | SMTP port | Web UI |
|---------|-----------|--------|
| MailHog (local) | 1025 | http://localhost:8025 |

Start MailHog alongside the database:
```bash
docker compose -f docker-compose.local.yml up postgres mailhog -d
```

In dev/staging the `application-dev.yml` contains `ENC(...)` Jasypt-encrypted SMTP credentials pointing to a real mail provider.

---

## Adding new email types

1. Add a method to `IMailService`.
2. Implement it in `MailServiceImpl` using `JavaMailSender` + a Thymeleaf template in `src/main/resources/templates/`.
3. Inject `IMailService` wherever the email needs to be triggered.
