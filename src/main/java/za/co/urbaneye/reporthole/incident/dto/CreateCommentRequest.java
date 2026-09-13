package za.co.urbaneye.reporthole.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /incidents/{id}/comments}.
 *
 * @param content the comment text; 1–500 characters
 */
public record CreateCommentRequest(
        @NotBlank(message = "Comment cannot be empty")
        @Size(max = 500, message = "Comment cannot exceed 500 characters")
        String content
) {}
