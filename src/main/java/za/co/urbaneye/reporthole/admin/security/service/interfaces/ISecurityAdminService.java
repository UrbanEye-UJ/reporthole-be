package za.co.urbaneye.reporthole.admin.security.service.interfaces;

import za.co.urbaneye.reporthole.admin.security.dto.AuditEntryResponse;
import za.co.urbaneye.reporthole.admin.security.dto.GrantRoleRequest;
import za.co.urbaneye.reporthole.admin.security.dto.SecurityUserResponse;

import java.util.List;
import java.util.UUID;

/**
 * Identity and accountability operations reserved for {@code SECURITY_ADMIN} accounts.
 *
 * <p>Every mutating method here:</p>
 * <ul>
 *     <li>verifies the caller is a {@code SECURITY_ADMIN} (throws
 *         {@link za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException} otherwise);</li>
 *     <li>refuses to act on the caller's own account, so a security admin cannot lock themselves out;</li>
 *     <li>writes exactly one append-only
 *         {@link za.co.urbaneye.reporthole.admin.security.entity.AccessControlAuditEntry}; and</li>
 *     <li>bumps the target's {@code credentialsValidFrom} watermark where the change must take
 *         effect immediately (role change, suspension, forced logout).</li>
 * </ul>
 *
 * @author Refentse
 * @since 1.0
 */
public interface ISecurityAdminService {

    /**
     * Grants or changes the role on an account and forces its existing sessions to re-authenticate.
     *
     * @param targetUserId the account to change
     * @param request      the new role and a mandatory justification
     * @throws za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException
     *         if the caller is not a security admin, the target does not exist, the caller is
     *         targeting themselves, or the account already has that role
     */
    void grantRole(UUID targetUserId, GrantRoleRequest request);

    /**
     * Revokes an account's elevated role back to {@code CIVILIAN} and forces re-authentication.
     *
     * @param targetUserId the account to demote
     * @param reason        mandatory justification, recorded in the audit trail
     * @throws za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException
     *         if the caller is not a security admin, the target does not exist, the caller is
     *         targeting themselves, or the account is already a {@code CIVILIAN}
     */
    void revokeRole(UUID targetUserId, String reason);

    /**
     * Suspends an account so it can no longer authenticate, and revokes its current sessions.
     *
     * @param targetUserId the account to suspend
     * @param reason        mandatory justification, recorded in the audit trail
     * @throws za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException
     *         if the caller is not a security admin, the target does not exist, the caller is
     *         targeting themselves, or the account is already suspended
     */
    void suspendAccount(UUID targetUserId, String reason);

    /**
     * Reactivates a suspended account, returning it to {@code ACTIVE}.
     *
     * @param targetUserId the account to reactivate
     * @param reason        mandatory justification, recorded in the audit trail
     * @throws za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException
     *         if the caller is not a security admin, the target does not exist, or the account
     *         is not currently suspended
     */
    void reactivateAccount(UUID targetUserId, String reason);

    /**
     * Invalidates every outstanding session for an account without changing its role or status.
     *
     * @param targetUserId the account whose sessions should be revoked
     * @param reason        mandatory justification, recorded in the audit trail
     * @throws za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException
     *         if the caller is not a security admin, the target does not exist, or the caller is
     *         targeting themselves
     */
    void forceLogout(UUID targetUserId, String reason);

    /**
     * Lists every account so a security admin can pick one to act on.
     *
     * @return all users with id, name, email, role and status, ordered by creation time
     * @throws za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException
     *         if the caller is not a security admin
     */
    List<SecurityUserResponse> listUsers();

    /**
     * Returns the access-control audit trail, newest first.
     *
     * @param targetUserId optional filter — when non-null, only entries for that account
     * @return the (optionally filtered) audit entries
     * @throws za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException
     *         if the caller is not a security admin
     */
    List<AuditEntryResponse> listAudit(UUID targetUserId);
}
