package za.co.urbaneye.reporthole.user.dto;

/**
 * Data Transfer Object (DTO) representing a user login request.
 *
 * <p>This record is used to capture authentication credentials
 * submitted by a client when attempting to log in.</p>
 *
 * <p>Contains:</p>
 * <ul>
 *     <li>Email address used as the username</li>
 *     <li>Password for authentication</li>
 * </ul>
 *
 * <p>Typically consumed by authentication endpoints such as
 * {@code /auth/login}.</p>
 *
 * @param email    user's registered email address
 * @param password user's plaintext password submitted for login
 *
 * @author Refentse
 * @since 1.0
 */
public record LoginRequest(
        String email,
        String password
) {
}