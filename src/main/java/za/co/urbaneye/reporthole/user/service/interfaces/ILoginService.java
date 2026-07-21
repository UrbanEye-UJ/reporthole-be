package za.co.urbaneye.reporthole.user.service.interfaces;

import za.co.urbaneye.reporthole.user.dto.AuthResponse;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;

public interface ILoginService {
    /**
     * Authenticates a user and returns an access token.
     *
     * @param user login request containing
     *             credentials
     * @return authentication response containing token and role
     */
    AuthResponse loginUser(final LoginRequest user);
}
