package za.co.urbaneye.reporthole.admin.security.dto;

import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Row shape for {@code GET /admin/security/users} — enough for a security admin to pick an
 * account and act on it (grant/revoke role, suspend, force-logout).
 *
 * <p>Includes the decrypted email because identifying the right account is the whole point of
 * the screen and the caller already holds full authority over every account. It is not reporter
 * PII in an incident context.</p>
 *
 * @param userId    account id (used as the path variable for the action endpoints)
 * @param name      first + last name
 * @param email     decrypted email address
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
