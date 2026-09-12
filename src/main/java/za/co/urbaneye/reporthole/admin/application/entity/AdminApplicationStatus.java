package za.co.urbaneye.reporthole.admin.application.entity;

/**
 * Lifecycle states for an {@link AdminApplication}.
 *
 * <p>Applications begin as {@code PENDING} and are moved to
 * {@code APPROVED} or {@code REJECTED} by an existing admin via
 * {@code POST /admin/applications/{id}/approve} or {@code /reject}.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public enum AdminApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
