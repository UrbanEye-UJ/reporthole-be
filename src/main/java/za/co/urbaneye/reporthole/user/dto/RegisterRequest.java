package za.co.urbaneye.reporthole.user.dto;

import za.co.urbaneye.reporthole.user.entity.UserRole;

/**
 * Data Transfer Object (DTO) representing a user registration request.
 *
 * <p>This record is used to capture information submitted by a client
 * when creating a new user account.</p>
 *
 * <p>Contains personal, authentication, and role-related details
 * required during registration.</p>
 *
 * <p>Typical usage: consumed by {@code /auth/register} endpoints.</p>
 *
 * @param firstName   user's first name
 * @param lastName    user's last name
 * @param email       user's email address
 * @param role        requested or assigned user role
 * @param password    user's plaintext password submitted for hashing
 * @param phoneNumber user's contact phone number
 *
 * @author Refentse
 * @since 1.0
 */
public record RegisterRequest(
        String firstName,
        String lastName,
        String email,
        UserRole role,
        String password,
        String phoneNumber
) {
}