package za.co.urbaneye.reporthole.message.entity;

import jakarta.persistence.Column;
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
import za.co.urbaneye.reporthole.user.entity.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores a message or complaint sent either by an authenticated user or anonymously
 * via the public contact form.
 *
 * <p>{@code senderUserId} is nullable — it is set for authenticated submissions and
 * null for public contact-form messages where the sender has not signed in.</p>
 *
 * <p>{@code senderName} and {@code senderEmail} are always stored in plain text because:
 * (a) the contact-form sender volunteers them explicitly for the purpose of receiving a reply,
 * and (b) authenticated senders' real identity is already held in the {@code User} / {@code UserAuth}
 * tables — storing a reference copy here avoids a join on every inbox load.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "MSG_ID", updatable = false, nullable = false)
    private UUID id;

    /**
     * User ID of the authenticated sender; null for anonymous contact-form submissions.
     */
    @Column(name = "MSG_SENDER_USER_ID")
    private UUID senderUserId;

    /**
     * The sender's role at the time of sending (CIVILIAN, CONTRACTOR, or ADMIN) — null for
     * anonymous contact-form submissions, where there is no account at all. Lets the recipient
     * see who is actually messaging them instead of assuming every message is from a civilian.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "MSG_SENDER_ROLE")
    private UserRole senderRole;

    /**
     * Display name of the sender as submitted (may be a masked name for authenticated users
     * or a self-reported name for contact-form submissions).
     */
    @Column(name = "MSG_SENDER_NAME", nullable = false, length = 200)
    private String senderName;

    /**
     * Sender's email address — required for contact-form messages so admins can reply;
     * populated from the user record for authenticated senders.
     */
    @Column(name = "MSG_SENDER_EMAIL", nullable = false, length = 320)
    private String senderEmail;

    /** Optional short subject line. */
    @Column(name = "MSG_SUBJECT", length = 200)
    private String subject;

    @Column(name = "MSG_CONTENT", nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "MSG_CATEGORY", nullable = false)
    private MessageCategory category;

    @Column(name = "MSG_READ", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "MSG_CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
