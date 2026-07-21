package za.co.urbaneye.reporthole.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating the authenticated user's profile.
 * All fields are required — partial updates are not supported.
 *
 * @param firstName   updated first name
 * @param lastName    updated last name
 * @param phoneNumber updated phone number
 */
public record UpdateProfileRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Phone number is required")
        String phoneNumber
) {}
