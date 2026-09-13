package za.co.urbaneye.reporthole.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a single in-app notification.
 *
 * @param id        notification identifier
 * @param message   human-readable message shown in the notification drawer
 * @param read      whether the recipient has already seen this notification
 * @param createdAt when the notification was created
 */
public record NotificationResponse(
        UUID id,
        String message,
        boolean read,
        LocalDateTime createdAt
) {}
