package za.co.urbaneye.reporthole.admin.contractor.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.security.Aes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Pending invite for a contractor to self-register.
 *
 * <p>Created by an admin via {@code POST /admin/contractors/invite}. The contractor
 * receives an email with a link containing {@code token}. They complete registration
 * at {@code POST /contractors/complete-registration}, after which {@code used} is set
 * to {@code true} and the token is no longer valid.</p>
 */
@Entity
@Table(name = "contractor_invite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractorInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "INVITE_ID", updatable = false, nullable = false)
    private UUID id;

    /** Encrypted email of the invited contractor. */
    @Column(name = "INVITE_EMAIL", nullable = false)
    @Convert(converter = Aes.class)
    private String email;

    /** Deterministic hash of the invited email — used for uniqueness checks. */
    @Column(name = "INVITE_EMAIL_HASH", nullable = false, unique = true)
    private String emailHash;

    /** Single-use token sent to the contractor in the invite email. */
    @Column(name = "INVITE_TOKEN", nullable = false, unique = true)
    private UUID token;

    @ElementCollection(targetClass = IssueType.class)
    @CollectionTable(name = "contractor_invite_specialisation", joinColumns = @JoinColumn(name = "INVITE_ID"))
    @Enumerated(EnumType.STRING)
    @Column(name = "ISSUE_TYPE")
    @Builder.Default
    private Set<IssueType> specialisations = new HashSet<>();

    @Column(name = "INVITE_EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "INVITE_USED", nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "INVITE_CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
