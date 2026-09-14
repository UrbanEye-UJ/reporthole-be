package za.co.urbaneye.reporthole.admin.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * One immutable record of a state-changing action somewhere in the platform that isn't already
 * covered by {@link AccessControlAuditEntry} — that class is specifically for identity/
 * accountability actions where both actor and target are {@link User} accounts, which doesn't
 * fit a municipality being created, a comment being posted, or a public contact-form submission
 * with no authenticated actor at all. This is the general-purpose counterpart: a free-form
 * {@link #entityType}/{@link #entityId} pair instead of a fixed {@code target} User FK, and a
 * nullable {@link #actor} for the rare unauthenticated case.
 *
 * <p>Deliberately not a merge of the two tables: {@code AccessControlAuditEntry} stays exactly
 * as it is (actor/target both mandatory Users) for the identity actions already built and
 * tested against it — this table only picks up what that shape can't represent.</p>
 *
 * <p><b>Append-only by design</b>, same as {@link AccessControlAuditEntry}: no setters, and
 * {@link za.co.urbaneye.reporthole.admin.security.repository.IAuditLogRepository} exposes only
 * {@code save} and read operations.</p>
 *
 * <p>Mapped to database table: {@code audit_log}.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntry {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "AUDIT_LOG_ID", updatable = false, nullable = false)
    private UUID id;

    /**
     * Who did it. Null for the handful of actions with no authenticated caller (e.g. the
     * public landing-page contact form).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AUDIT_LOG_ACTOR", updatable = false)
    private User actor;

    /**
     * What happened, e.g. {@code "CONTRACTOR_INVITED"}, {@code "MUNICIPALITY_CREATED"}. A
     * free-form code (SCREAMING_SNAKE_CASE by convention) rather than a closed enum — this
     * table spans too many unrelated domains for one shared enum to stay meaningful.
     */
    @Column(name = "AUDIT_LOG_ACTION", nullable = false, updatable = false, length = 100)
    private String action;

    /**
     * The kind of thing acted on, e.g. {@code "MUNICIPALITY"}, {@code "COMMENT"}, {@code
     * "CONTRACTOR_INVITE"}.
     */
    @Column(name = "AUDIT_LOG_ENTITY_TYPE", nullable = false, updatable = false, length = 100)
    private String entityType;

    /** The affected row's id, when it has one (e.g. municipality id, comment id). */
    @Column(name = "AUDIT_LOG_ENTITY_ID", updatable = false)
    private UUID entityId;

    /** Human-readable one-line description of what happened, shown directly in the audit UI. */
    @Column(name = "AUDIT_LOG_SUMMARY", nullable = false, length = 500, updatable = false)
    private String summary;

    /** When the action was recorded. Server-assigned, never updatable. */
    @Column(name = "AUDIT_LOG_CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Stamps {@link #createdAt} immediately before the row is first (and only) written. */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
