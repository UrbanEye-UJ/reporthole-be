package za.co.urbaneye.reporthole.admin.security.dto;

import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Row shape for {@code GET /admin/security/users} — enough for a security admin to pick an
 * account and act on it (grant/revoke role, suspend, force-logout).
 *
 * <p>Name and email are masked (e.g. {@code "Jane D."}, {@code "j***@example.com"}) — full
 * authority over every account is not, by itself, a reason to render everyone's PII in plain
 * text on every list load. Use {@code POST /{id}/reveal} (a step-up password re-check) to view
 * an account's decrypted name and email; that view is itself recorded as a
 * {@code PII_REVEALED} audit row.</p>
 *
 * @param userId    account id (used as the path variable for the action endpoints)
 * @param name      masked display name
 * @param email     masked email address
 * @param role      current role
 * @param status    current account status ({@code ACTIVE}, {@code SUSPENDED}, {@code LOCKED}, …)
 * @param createdAt when the account was created
 * @author Refentse
 * @since 1.0
 */
public record SecurityUserResponse(
        UUID userId,
        String name,
        String email,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt
) {}
