package za.co.urbaneye.reporthole.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.urbaneye.reporthole.security.Aes;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "user_auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuth{
    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "AUTH_ID", updatable = false, nullable = false)
    private UUID authId;
    /**
     * User's email address (encrypted).
     */
    @Column(name = "AUTH_EMAIL", nullable = false, unique = true)
    @Convert(converter = Aes.class)
    private String email;

    /**
     * Deterministic hash of the email used for secure lookup and uniqueness checks.
     */
    @Column(name = "AUTH_EMAIL_HASH", nullable = false, unique = true)
    private String emailHash;

    /**
     * User password value (encrypted / stored securely).
     */
    @Column(name = "AUTH_PASSWORD_HASH", nullable = false, length = 255)
    @Convert(converter = Aes.class)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "AUTH_STATUS", nullable = false)
    private UserStatus status;


    @Builder.Default
    @Column(name = "AUTH_RETRIES", nullable = false)
    private Integer retries = 0;

    /**
     * Watermark for session validity: any JWT whose {@code issuedAt} is before this instant is
     * rejected by {@link za.co.urbaneye.reporthole.security.JwtAuthenticationFilter}, even though
     * its signature and expiry are still valid.
     *
     * <p>Bumping this timestamp to "now" is how a {@code SECURITY_ADMIN} revokes every outstanding
     * session for the account in one move — a forced logout, a role change, or a suspension all set
     * it. Stored truncated to whole seconds because a JWT {@code iat} claim only has second
     * precision; a token minted in the same second as the bump is allowed to survive.</p>
     *
     * <p>Null means "no watermark" — the filter applies no session cut-off (relevant only for rows
     * created before this column existed).</p>
     */
    @Column(name = "AUTH_CREDENTIALS_VALID_FROM")
    private LocalDateTime credentialsValidFrom;

    /** UUID token used to verify the user's email address. Null once verified. */
    @Column(name = "AUTH_VERIFICATION_TOKEN", unique = true)
    private String verificationToken;

    /** Expiry timestamp for the verification token. Null once verified. */
    @Column(name = "AUTH_VERIFICATION_TOKEN_EXPIRES_AT")
    private LocalDateTime verificationTokenExpiresAt;

    /** UUID token used to reset the password. Null when no reset is pending. */
    @Column(name = "AUTH_RESET_TOKEN", unique = true)
    private String passwordResetToken;

    /** Expiry timestamp for the password reset token. */
    @Column(name = "AUTH_RESET_TOKEN_EXPIRES_AT")
    private LocalDateTime passwordResetTokenExpiresAt;

    /**
     * Timestamp when the user account was created.
     */
    @Column(name = "USER_CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Sets the creation timestamp before persisting a new entity, and seeds
     * {@link #credentialsValidFrom} so tokens issued from first login onward are accepted.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.credentialsValidFrom == null) {
            this.credentialsValidFrom = this.createdAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        }
    }

}
