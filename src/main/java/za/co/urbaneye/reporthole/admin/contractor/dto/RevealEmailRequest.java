package za.co.urbaneye.reporthole.admin.contractor.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /admin/contractors/{id}/reveal-email}.
 *
 * <p>Requires the calling admin's own current password as a step-up
 * re-authentication check before their decrypted PII view is granted.</p>
 *
 * @param password the calling admin's own current password
 * @author Refentse
 * @since 1.0
 */
public record RevealEmailRequest(
        @NotBlank(message = "Password is required") String password
) {}
