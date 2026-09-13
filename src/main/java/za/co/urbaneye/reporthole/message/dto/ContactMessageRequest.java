package za.co.urbaneye.reporthole.message.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for the public {@code POST /messages/contact} endpoint (landing-page contact form).
 * No authentication is required — the sender's name and email are explicitly provided so
 * security admins can reply.
 *
 * @param name    sender's self-reported full name
 * @param email   sender's email address for replies
 * @param subject optional short subject line
 * @param content message body (max 2000 characters)
 *
 * @author Refentse
 * @since 1.0
 */
public record ContactMessageRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200)
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "A valid email address is required")
        @Size(max = 320)
        String email,

        @Size(max = 200)
        String subject,

        @NotBlank(message = "Message content is required")
        @Size(max = 2000, message = "Message must be 2000 characters or fewer")
        String content
) {}
