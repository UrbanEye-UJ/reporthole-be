package za.co.urbaneye.reporthole.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
 * @param password    user's plaintext password (min 8 chars, 1 number, 1 special char)
 * @param phoneNumber user's contact phone number
 *
 * @author Refentse
 * @since 1.0
 */
public record RegisterRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @NotNull(message = "Role is required")
        UserRole role,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
                message = "Password must be at least 8 characters and contain at least one number and one special character"
        )
        String password,

        @NotBlank(message = "Phone number is required")
        String phoneNumber
) {
}