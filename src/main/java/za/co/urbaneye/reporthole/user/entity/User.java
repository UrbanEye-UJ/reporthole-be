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

/**
 * JPA entity representing an application user.
 *
 * <p>This entity stores authentication, personal profile,
 * and authorization information for users of the system.</p>
 *
 * <p>Sensitive fields are encrypted at rest using the
 * {@link Aes} attribute converter.</p>
 *
 * <p>Mapped to database table: {@code users}</p>
 *
 * <p>Includes:</p>
 * <ul>
 *     <li>Generated UUID primary key</li>
 *     <li>Encrypted personal details</li>
 *     <li>Email hash for secure searching</li>
 *     <li>Encrypted password value</li>
 *     <li>User role</li>
 *     <li>Automatic creation timestamp</li>
 * </ul>
 *
 * @author Refentse
 * @since 1.0
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "USER_ID", updatable = false, nullable = false)
    private UUID userId;

    /**
     * User's first name (encrypted).
     */
    @Column(name = "USER_FIRSTNAME", nullable = false)
    @Convert(converter = Aes.class)
    private String firstName;

    /**
     * User's last name (encrypted).
     */
    @Column(name = "USER_LASTNAME", nullable = false)
    @Convert(converter = Aes.class)
    private String lastName;

    /**
     * User's email address (encrypted).
     */
    @Column(name = "USER_EMAIL", nullable = false, unique = true)
    @Convert(converter = Aes.class)
    private String email;

    /**
     * Deterministic hash of the email used for secure lookup and uniqueness checks.
     */
    @Column(name = "USER_EMAIL_HASH", nullable = false, unique = true)
    private String emailHash;

    /**
     * User password value (encrypted / stored securely).
     */
    @Column(name = "USER_HASH", nullable = false, length = 255)
    @Convert(converter = Aes.class)
    private String password;

    /**
     * User contact phone number (encrypted).
     */
    @Column(name = "USER_PHONENUMBER")
    @Convert(converter = Aes.class)
    private String phoneNumber;

    /**
     * User authorization role.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "USER_ROLE", nullable = false)
    private UserRole role;

    /**
     * Timestamp when the user account was created.
     */
    @Column(name = "USER_CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Sets the creation timestamp before persisting a new entity.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}