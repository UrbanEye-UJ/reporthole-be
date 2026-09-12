package za.co.urbaneye.reporthole.admin.security.entity;

/**
 * The kind of identity/accountability action recorded in an {@link AccessControlAuditEntry}.
 *
 * <p>Every value here is something only a {@code SECURITY_ADMIN} can do, and every occurrence
 * writes exactly one audit row. The set is deliberately small and closed — new privileged
 * operations should be added here so they cannot bypass the trail.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public enum AccessControlAction {

    /** A role was granted or changed on an account (e.g. {@code CIVILIAN} → {@code ADMIN}). */
    ROLE_GRANTED,

    /** An account's elevated role was revoked back to {@code CIVILIAN}. */
    ROLE_REVOKED,

    /** An account was administratively suspended — it can no longer authenticate. */
    ACCOUNT_SUSPENDED,

    /** A suspended account was reactivated. */
    ACCOUNT_REACTIVATED,

    /**
     * Every outstanding session for an account was invalidated by bumping its
     * {@code credentialsValidFrom} watermark, without otherwise changing role or status.
     */
    SESSIONS_REVOKED,

    /** A user successfully authenticated and received a JWT. */
    USER_LOGIN
}
