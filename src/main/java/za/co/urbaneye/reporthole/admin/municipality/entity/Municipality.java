package za.co.urbaneye.reporthole.admin.municipality.entity;

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
import lombok.Setter;
import za.co.urbaneye.reporthole.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A municipality that road-maintenance admins belong to.
 *
 * <p>Created by a {@code SECURITY_ADMIN}. Serves two purposes: it is the thing a
 * {@link MunicipalityToken} is issued against, and it identifies which authority an
 * ADMIN account acts on behalf of (recorded on the
 * {@link za.co.urbaneye.reporthole.admin.application.entity.AdminApplication} that
 * onboarded them).</p>
 *
 * <p>Mapped to database table: {@code municipalities}.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Entity
@Table(name = "municipalities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Municipality {

    /**
     * Surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "MUNICIPALITY_ID", updatable = false, nullable = false)
    private UUID id;

    /**
     * Display name of the municipality, e.g. "City of Johannesburg". Unique (case-insensitively
     * enforced in the service layer).
     */
    @Column(name = "MUNICIPALITY_NAME", nullable = false, unique = true)
    private String name;

    /**
     * Province the municipality falls under. Defaults to "Gauteng" — the platform's launch region.
     */
    @Column(name = "MUNICIPALITY_PROVINCE", nullable = false)
    @Builder.Default
    private String province = "Gauteng";

    /**
     * The security admin who created this record.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MUNICIPALITY_CREATED_BY", updatable = false)
    private User createdBy;

    /**
     * When the record was created. Server-assigned.
     */
    @Column(name = "MUNICIPALITY_CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Stamps {@link #createdAt} before the first persist.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
