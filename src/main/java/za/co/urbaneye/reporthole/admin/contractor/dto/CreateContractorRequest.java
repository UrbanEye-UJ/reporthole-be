package za.co.urbaneye.reporthole.admin.contractor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Submitted by an admin to register a new contractor account.
 *
 * @param firstName   contractor's first name
 * @param lastName    contractor's last name
 * @param email       contractor's email address (used for login)
 * @param phoneNumber contractor's contact phone number
 * @param password    initial password set by the admin (min 8 chars, 1 number, 1 special char)
 */
public record CreateContractorRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
                message = "Password must be at least 8 characters and contain at least one number and one special character"
        )
        String password
) {
}
