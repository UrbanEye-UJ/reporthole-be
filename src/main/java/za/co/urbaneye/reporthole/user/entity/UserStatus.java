package za.co.urbaneye.reporthole.user.entity;

/**
 * Lifecycle state of an account, stored on {@link UserAuth} and consulted by
 * {@link za.co.urbaneye.reporthole.security.JwtAuthenticationFilter} on every
 * authenticated request.
 *
 * <p>Only {@link #ACTIVE} accounts may authenticate. Every other state causes
 * the filter to reject an otherwise-valid JWT, which is what makes server-side
 * revocation possible for a stateless token.</p>
 *
 * <ul>
 *     <li>{@link #PENDING_VERIFICATION} - registered but the email link has not been clicked yet</li>
 *     <li>{@link #ACTIVE} - normal, usable account</li>
 *     <li>{@link #LOCKED} - automatically locked after repeated failed logins; cleared by a password reset</li>
 *     <li>{@link #SUSPENDED} - deliberately disabled by a {@code SECURITY_ADMIN}; cleared by reactivation</li>
 *     <li>{@link #DELETED} - soft-deleted; treated as non-existent</li>
 * </ul>
 *
 * @author Refentse
 * @since 1.0
 */
public enum UserStatus {

    /** Registered but not yet email-verified. */
    PENDING_VERIFICATION,

    /** Normal, usable account — the only state that may authenticate. */
    ACTIVE,

    /** Auto-locked after too many failed login attempts. Resolved by a password reset. */
    LOCKED,

    /**
     * Administratively suspended by a {@code SECURITY_ADMIN}.
     *
     * <p>Distinct from {@link #LOCKED}: a suspension is a deliberate identity decision, is audited,
     * and is only reversed by a {@code SECURITY_ADMIN} reactivating the account — not by the user.</p>
     */
    SUSPENDED,

    /** Soft-deleted. Login treats this the same as "user not found" to avoid leaking account existence. */
    DELETED
}
