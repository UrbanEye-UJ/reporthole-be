package za.co.urbaneye.reporthole.admin.user.dto;

import za.co.urbaneye.reporthole.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary view of a civilian account shown in the admin directory.
 *
 * <p>Names and emails are partially masked server-side to reduce unnecessary exposure of PII
 * to municipal admins — e.g. "Jane D." instead of "Jane Doe" and "j***@gmail.com" instead
 * of the full address. The civilian's incident count is included so admins can gauge engagement
 * without needing to visit the incident list.</p>
 *
 * @param userId        unique account id
 * @param maskedName    first name + last initial (e.g. "Jane D.")
 * @param maskedEmail   first character + "***@" + domain (e.g. "j***@gmail.com")
 * @param incidentCount number of incident_reporter rows for this user
 * @param status        current account status
 * @param createdAt     when the account was created
 *
 * @author Refentse
 * @since 1.0
 */
public record CivilianSummaryResponse(
        UUID userId,
        String maskedName,
        String maskedEmail,
        long incidentCount,
        UserStatus status,
        LocalDateTime createdAt
) {}
