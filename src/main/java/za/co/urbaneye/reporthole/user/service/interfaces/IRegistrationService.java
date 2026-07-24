package za.co.urbaneye.reporthole.user.service.interfaces;

import za.co.urbaneye.reporthole.user.dto.RegisterRequest;

public interface IRegistrationService {
    /**
     * Registers a new user account in the system.
     *
     * @param user registration request containing
     *             user details and credentials
     */
    void registerUser(final RegisterRequest user);

    /**
     * Verifies the user's email address using the token from the verification email.
     * Sets account status to ACTIVE and clears the token.
     *
     * @param token the verification token stored on {@code UserAuth}
     */
    void verifyEmail(String token);
}
