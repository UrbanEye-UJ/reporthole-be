package za.co.urbaneye.reporthole.user.entity;

/**
 * Enumeration representing the available user roles
 * within the ReportHole system.
 *
 * <p>Roles determine access levels and responsibilities
 * within the application.</p>
 *
 * <ul>
 *     <li>{@code CIVILIAN} - Standard public user who reports issues</li>
 *     <li>{@code CONTRACTOR} - User responsible for resolving assigned issues</li>
 *     <li>{@code ADMIN} - Operational admin: incident verification, assignment and status transitions</li>
 *     <li>{@code SECURITY_ADMIN} - Identity &amp; accountability: grants/revokes roles, suspends and
 *         reactivates accounts, forces logout and reads the access-control audit trail. Deliberately
 *         separated from {@code ADMIN} so that whoever performs operational actions is not also the
 *         person who controls access to them (separation of duties).</li>
 * </ul>
 *
 * <p>This enum is typically persisted as a string value
 * in the database and used for authorization checks.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public enum UserRole {

    /**
     * Standard citizen user role.
     */
    CIVILIAN,

    /**
     * Contractor or service provider role.
     */
    CONTRACTOR,

    /**
     * Operational administrative role: verifies, assigns and transitions incidents,
     * and approves or denies admin access applications. Has no power over identity.
     */
    ADMIN,

    /**
     * Identity and accountability administrator.
     *
     * <p>Owns the grant path <em>and</em> the revoke path for every privilege: role changes,
     * account suspension/reactivation and forced logout. Every action a security admin takes is
     * itself written to the append-only access-control audit trail. A security admin cannot mutate
     * incidents — that separation is what makes the audit story credible.</p>
     */
    SECURITY_ADMIN
}