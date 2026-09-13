package za.co.urbaneye.reporthole.admin.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import za.co.urbaneye.reporthole.user.entity.UserRole;

/**
 * Request body for {@code POST /admin/security/users/{userId}/role} — grant or change
 * an account's role.
 *
 * <p>A {@code reason} is mandatory: every grant is written to the append-only access-control
 * audit trail, and the trail is only useful if it says <em>why</em>.</p>
 *
 * @param role   the role to set on the account
 * @param reason free-text justification, recorded verbatim in the audit trail
 * @author Refentse
 * @since 1.0
 */
public record GrantRoleRequest(
        @NotNull(message = "Role is required") UserRole role,
        @NotBlank(message = "A reason is required") @Size(max = 500, message = "Reason must be at most 500 characters") String reason
) {}
