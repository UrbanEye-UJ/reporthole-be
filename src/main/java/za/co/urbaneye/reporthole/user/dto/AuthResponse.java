package za.co.urbaneye.reporthole.user.dto;

import za.co.urbaneye.reporthole.user.entity.UserRole;

/**
 * Response payload returned on successful authentication.
 *
 * @param token JWT access token
 * @param role  role assigned to the authenticated user
 *
 * @author Refentse
 * @since 1.0
 */
public record AuthResponse(
        String token,
        UserRole role
) {}
