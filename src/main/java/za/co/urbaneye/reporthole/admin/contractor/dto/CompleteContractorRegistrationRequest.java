package za.co.urbaneye.reporthole.admin.contractor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Submitted by an invited contractor to complete their account registration.
 *
 * @param token       single-use UUID from the invite email
 * @param firstName   contractor's first name
 * @param lastName    contractor's last name
 * @param phoneNumber contractor's contact phone number
 * @param password    chosen password (min 8 chars, 1 number, 1 special char)
 */
public record CompleteContractorRegistrationRequest(
        @NotNull(message = "Invite token is required")
        UUID token,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
                message = "Password must be at least 8 characters and contain at least one number and one special character"
        )
        String password
) {}
