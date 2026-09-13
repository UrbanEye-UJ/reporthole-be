package za.co.urbaneye.reporthole.admin.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import za.co.urbaneye.reporthole.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One immutable record of a privileged identity/accountability action taken by a
 * {@code SECURITY_ADMIN} — a role grant or revoke, an account suspension or reactivation,
 * or a forced logout.
 *
 * <p><b>Append-only by design.</b> This class exposes no setters, the repository
 * ({@link za.co.urbaneye.reporthole.admin.security.repository.IAccessControlAuditRepository})
 * offers no update or delete operations, and there is no endpoint that mutates a row. Entries
 * are created once, via the builder, and thereafter only read and exported. Nobody — the
 * security admin included — can alter the trail; that is what makes it usable as evidence.</p>
 *
 * <p>{@link #createdAt} is stamped in {@link #onCreate()} and marked non-updatable so even a
 * stray managed-entity change cannot move it.</p>
 *
 * <p>Mapped to database table: {@code access_control_audit}.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Entity
@Table(name = "access_control_audit")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessControlAuditEntry {

    /**
     * Surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ACCESS_CONTROL_AUDIT_ID", updatable = false, nullable = false)
    private UUID auditId;

    /**
     * What was done.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ACCESS_CONTROL_AUDIT_ACTION", nullable = false, updatable = false)
    private AccessControlAction action;

    /**
     * The security admin who performed the action.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ACCESS_CONTROL_AUDIT_ACTOR", nullable = false, updatable = false)
    private User actor;

    /**
     * The account the action was performed on.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ACCESS_CONTROL_AUDIT_TARGET", nullable = false, updatable = false)
    private User target;

    /**
     * Previous value where the action changes one (e.g. the old role or status). Null otherwise.
     */
    @Column(name = "ACCESS_CONTROL_AUDIT_FROM_VALUE", updatable = false)
    private String fromValue;

    /**
     * New value where the action sets one (e.g. the new role or status). Null otherwise.
     */
    @Column(name = "ACCESS_CONTROL_AUDIT_TO_VALUE", updatable = false)
    private String toValue;

    /**
     * Free-text justification supplied by the security admin at the time of the action.
     */
    @Column(name = "ACCESS_CONTROL_AUDIT_REASON", length = 500, updatable = false)
    private String reason;

    /**
     * When the action was recorded. Server-assigned, never updatable.
     */
    @Column(name = "ACCESS_CONTROL_AUDIT_CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Stamps {@link #createdAt} immediately before the row is first (and only) written.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
