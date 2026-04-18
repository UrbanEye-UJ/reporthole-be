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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "USER_ID", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "USER_FIRSTNAME", nullable = false)
    @Convert(converter = Aes.class)
    private String firstName;

    @Column(name = "USER_LASTNAME", nullable = false)
    @Convert(converter = Aes.class)
    private String lastName;

    @Column(name = "USER_EMAIL", nullable = false, unique = true)
    @Convert(converter = Aes.class)
    private String email;

    @Column(name = "USER_EMAIL_HASH", nullable = false, unique = true)
    private String emailHash;

    @Column(name = "USER_HASH", nullable = false, length = 255)
    @Convert(converter = Aes.class)
    private String password;

    @Column(name = "USER_PHONENUMBER")
    @Convert(converter = Aes.class)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_ROLE", nullable = false)
    private UserRole role;

    @Column(name = "USER_CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
