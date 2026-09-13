package za.co.urbaneye.reporthole.admin.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for the identity actions that need no payload beyond a justification:
 * {@code revoke-role}, {@code suspend}, {@code reactivate} and {@code force-logout} under
 * {@code /admin/security/users/{userId}/**}.
 *
 * @param reason free-text justification, recorded verbatim in the append-only audit trail
 * @author Refentse
 * @since 1.0
 */
public record AccountActionRequest(
        @NotBlank(message = "A reason is required") @Size(max = 500, message = "Reason must be at most 500 characters") String reason
) {}
