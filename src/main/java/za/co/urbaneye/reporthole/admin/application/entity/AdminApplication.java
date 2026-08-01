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
import za.co.urbaneye.reporthole.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a CIVILIAN user's request to be promoted to the ADMIN role.
 *
 * <p>Applications are created via {@code POST /admin/applications} and fulfilled
 * manually: a developer verifies the municipality token and runs
 * {@code scripts/promote-to-admin.sql} to promote the user.</p>
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
