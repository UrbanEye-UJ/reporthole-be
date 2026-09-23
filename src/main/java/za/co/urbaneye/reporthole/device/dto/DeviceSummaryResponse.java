package za.co.urbaneye.reporthole.device.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary of a registered dashcam device, returned by {@code GET /devices}.
 *
 * <p>The full token is never returned again after generation — only a short
 * {@code tokenPreview} (its last 8 characters) so a civilian can tell which
 * physical device a row corresponds to without re-exposing the secret.</p>
 *
 * @param deviceId     the {@code DashcamDevice} row id — pass this to
 *                      {@code DELETE /devices/token/{id}} to revoke
 * @param tokenPreview the last 8 characters of the device token, prefixed with dots
 * @param createdAt    when the token was generated
 * @author Refentse
 * @since 1.0
 */
public record DeviceSummaryResponse(
        UUID deviceId,
        String tokenPreview,
        LocalDateTime createdAt
) {}
