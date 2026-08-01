package za.co.urbaneye.reporthole.admin.application.entity;

/**
 * Lifecycle states for an {@link AdminApplication}.
 *
 * <p>Applications begin as {@code PENDING} and are moved to
 * {@code APPROVED} by a developer running the promotion SQL script.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public enum AdminApplicationStatus {
    PENDING,
    APPROVED
}
