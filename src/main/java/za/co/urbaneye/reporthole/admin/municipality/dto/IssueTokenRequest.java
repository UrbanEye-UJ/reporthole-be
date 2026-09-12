package za.co.urbaneye.reporthole.admin.municipality.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /admin/municipalities/{id}/tokens}.
 *
 * @param expiresInDays  optional lifetime in days; null / omitted means the token never expires
 * @param note           optional free-text note recorded against the token
 * @param recipientEmail optional email address to send the token to after it is issued
 * @author Refentse
 * @since 1.0
 */
public record IssueTokenRequest(
        @Positive(message = "expiresInDays must be positive")
        Integer expiresInDays,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note,

        @Email(message = "recipientEmail must be a valid email address")
        String recipientEmail
) {}
