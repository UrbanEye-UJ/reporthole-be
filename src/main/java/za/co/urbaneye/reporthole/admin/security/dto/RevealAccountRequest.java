package za.co.urbaneye.reporthole.admin.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /admin/security/users/{id}/reveal}.
 *
 * <p>Requires the calling security admin's own current password as a step-up
 * re-authentication check before the target account's decrypted PII is returned.</p>
 *
 * @param password the calling security admin's own current password
 * @author Refentse
 * @since 1.0
 */
public record RevealAccountRequest(
        @NotBlank(message = "Password is required") String password
) {}
