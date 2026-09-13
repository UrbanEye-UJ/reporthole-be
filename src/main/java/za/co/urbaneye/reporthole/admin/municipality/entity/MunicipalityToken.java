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
 * An access token issued by a {@code SECURITY_ADMIN} against a {@link Municipality}.
 *
 * <p>A prospective administrator enters the token string on the registration form; a
 * usable token registers them straight as {@code ADMIN}, bound to the token's
 * municipality. Tokens are <b>multi-use</b> — one token can onboard several admins for a
 * municipality until it is revoked or expires.</p>
 *
 * <p>The {@code issuedBy} / {@code issuedAt} / {@code revokedAt} fields are the token's own
 * audit trail. Mapped to database table: {@code municipality_tokens}.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Entity
@Table(name = "municipality_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MunicipalityToken {

    /**
     * Surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "MUNICIPALITY_TOKEN_ID", updatable = false, nullable = false)
    private UUID id;

    /**
     * The opaque token string handed to prospective admins. Unique.
     */
    @Column(name = "MUNICIPALITY_TOKEN_VALUE", nullable = false, unique = true, updatable = false)
    private String token;

    /**
     * The municipality this token grants admin access to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MUNICIPALITY_TOKEN_MUNICIPALITY", nullable = false, updatable = false)
    private Municipality municipality;

    /**
     * The security admin who issued the token.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MUNICIPALITY_TOKEN_ISSUED_BY", nullable = false, updatable = false)
    private User issuedBy;

    /**
     * When the token was issued. Server-assigned.
     */
    @Column(name = "MUNICIPALITY_TOKEN_ISSUED_AT", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    /**
     * Optional expiry. Null means the token never expires on its own.
     */
    @Column(name = "MUNICIPALITY_TOKEN_EXPIRES_AT")
    private LocalDateTime expiresAt;

    /**
     * Set when a security admin revokes the token. Null while the token is live.
     */
    @Column(name = "MUNICIPALITY_TOKEN_REVOKED_AT")
    private LocalDateTime revokedAt;

    /**
     * Free-text note recorded at issue time (who it's for, why).
     */
    @Column(name = "MUNICIPALITY_TOKEN_NOTE", length = 500)
    private String note;

    /**
     * Stamps {@link #issuedAt} before the first persist.
     */
    @PrePersist
    protected void onCreate() {
        this.issuedAt = LocalDateTime.now();
    }

    /**
     * @return {@code true} if the token can currently be used to register an admin — i.e. it has
     *         not been revoked and has not passed its expiry.
     */
    public boolean isUsable() {
        return revokedAt == null
                && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }
}
