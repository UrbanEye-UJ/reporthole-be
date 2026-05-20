package za.co.urbaneye.reporthole.user.service.interfaces;

import za.co.urbaneye.reporthole.user.dto.AuthResponse;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;

/**
 * Service contract for user authentication
 * and registration operations.
 *
 * <p>This interface defines the core business
 * functionality related to user account access,
 * including:</p>
 *
 * <ul>
 *     <li>Registering new users</li>
 *     <li>Authenticating existing users</li>
 * </ul>
 *
 * <p>Implemented by the application service layer.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public interface IUserAuthService {

    /**
     * Registers a new user account in the system.
     *
     * @param user registration request containing
     *             user details and credentials
     */
    void registerUser(final RegisterRequest user);

    /**
     * Authenticates a user and returns an access token.
     *
     * @param user login request containing
     *             credentials
     * @return authentication response containing token and role
     */
    AuthResponse loginUser(final LoginRequest user);
}