package za.co.urbaneye.reporthole.user.service.interfaces;

import za.co.urbaneye.reporthole.user.dto.AuthResponse;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;

import java.util.UUID;

public interface ILoginService {
    /**
     * Authenticates a user and returns an access token.
     *
     * @param user login request containing
     *             credentials
     * @return authentication response containing token and role
     */
    AuthResponse loginUser(final LoginRequest user);

    /**
     * Logs the caller out by bumping their own {@code credentialsValidFrom} watermark to now,
     * so the {@link za.co.urbaneye.reporthole.security.JwtAuthenticationFilter} rejects the
     * JWT that was just used to call this endpoint (and any other outstanding token for this
     * account) on its very next request — logout is otherwise only a client-side cookie clear
     * and the token would stay valid until it naturally expired.
     *
     * @param userId the authenticated caller's user id
     */
    void logout(final UUID userId);
}
