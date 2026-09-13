package za.co.urbaneye.reporthole.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /messages} — sent by an authenticated user (civilian, admin, or
 * contractor) to the platform admin team.
 *
 * @param subject optional short subject line
 * @param content message body (max 2000 characters)
 *
 * @author Refentse
 * @since 1.0
 */
public record SendMessageRequest(
        @Size(max = 200)
        String subject,

        @NotBlank(message = "Message content is required")
        @Size(max = 2000, message = "Message must be 2000 characters or fewer")
        String content
) {}
