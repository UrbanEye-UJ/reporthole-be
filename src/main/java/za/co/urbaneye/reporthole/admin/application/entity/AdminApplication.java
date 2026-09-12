package za.co.urbaneye.reporthole.admin.application.entity;

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
import lombok.Setter;
import za.co.urbaneye.reporthole.admin.municipality.entity.Municipality;
import za.co.urbaneye.reporthole.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The record of how a user obtained (or requested) the ADMIN role.
 *
 * <p>Two ways a row is created:</p>
 * <ul>
 *   <li>a prospective admin registers with a valid {@link Municipality} token — a row is written
 *       immediately as {@code APPROVED}, with {@link #municipality} set;</li>
 *   <li>the legacy path: a CIVILIAN submits {@code POST /admin/applications} and a
 *       {@code SECURITY_ADMIN} approves or rejects it.</li>
 * </ul>
 *
 * <p>{@code GET /admin/applications} lists every row regardless of status.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Entity
@Table(name = "admin_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "APPLICATION_ID", updatable = false, nullable = false)
    private UUID applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(name = "MUNICIPALITY_TOKEN", nullable = false)
    private String municipalityToken;

    /**
     * The municipality this admin belongs to. Set when the row came from token registration;
     * null for legacy free-text applications.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MUNICIPALITY_ID")
    private Municipality municipality;

    @Column(name = "SUBMITTED_AT", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    @Builder.Default
    private AdminApplicationStatus status = AdminApplicationStatus.PENDING;

    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
    }
}
