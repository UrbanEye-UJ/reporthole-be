package za.co.urbaneye.reporthole.security;

import za.co.urbaneye.reporthole.user.entity.UserStatus;

import java.time.LocalDateTime;

/**
 * Cached projection of the {@code UserAuth} fields {@link JwtAuthenticationFilter} checks on
 * every request, so repeated requests for the same account don't each cost a database round trip.
 *
 * @param exists               whether a {@code UserAuth} row exists for the account
 * @param status               the account's current status, or {@code null} if it doesn't exist
 * @param credentialsValidFrom the session-revocation watermark, or {@code null} if it doesn't exist or has none set
 */
public record AccountStatusSnapshot(boolean exists, UserStatus status, LocalDateTime credentialsValidFrom) {
}
