package za.co.urbaneye.reporthole.incident.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a single incident comment.
 *
 * @param id          comment identifier
 * @param content     comment text
 * @param createdAt   timestamp the comment was posted
 * @param authorName  decrypted full name of the author
 * @param authorRole  role of the author (CIVILIAN, ADMIN, CONTRACTOR)
 */
public record IncidentCommentResponse(
        UUID id,
        String content,
        LocalDateTime createdAt,
        String authorName,
        String authorRole
) {}
