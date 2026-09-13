package za.co.urbaneye.reporthole.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /users/verify-password}.
 * Used by clients to confirm the currently authenticated user's password
 * before revealing sensitive profile fields in the UI.
 */
public record VerifyPasswordRequest(
        @NotBlank(message = "Password is required") String password
) {}
