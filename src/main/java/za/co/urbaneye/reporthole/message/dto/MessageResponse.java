package za.co.urbaneye.reporthole.message.dto;

import za.co.urbaneye.reporthole.message.entity.Message;
import za.co.urbaneye.reporthole.message.entity.MessageCategory;
import za.co.urbaneye.reporthole.user.entity.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of a {@link Message} returned to admin inboxes.
 *
 * @param id          message id
 * @param senderName  display name of the sender
 * @param senderEmail email address (for contact-us replies)
 * @param senderRole  the sender's role (CIVILIAN, CONTRACTOR, or ADMIN) — null for an
 *                    anonymous contact-form submission, where there is no account at all
 * @param subject     optional subject line
 * @param content     full message body
 * @param category    USER_MESSAGE or CONTACT_US
 * @param read        whether the message has been read
 * @param createdAt   when the message was submitted
 *
 * @author Refentse
 * @since 1.0
 */
public record MessageResponse(
        UUID id,
        UUID senderUserId,
        String senderName,
        String senderEmail,
        UserRole senderRole,
        String subject,
        String content,
        MessageCategory category,
        boolean read,
        LocalDateTime createdAt
) {
    /** Convenience factory from a {@link Message} entity. */
    public static MessageResponse from(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getSenderUserId(),
                m.getSenderName(),
                m.getSenderEmail(),
                m.getSenderRole(),
                m.getSubject(),
                m.getContent(),
                m.getCategory(),
                m.isRead(),
                m.getCreatedAt()
        );
    }
}
