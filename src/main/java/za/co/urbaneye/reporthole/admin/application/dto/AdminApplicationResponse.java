package za.co.urbaneye.reporthole.admin.application.dto;

import za.co.urbaneye.reporthole.admin.application.entity.AdminApplicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response shape for {@code GET /admin/applications} — one admin-access record
 * (any status), enough for a reviewing security admin to act on it without
 * querying the database directly.
 *
 * @param applicationId      the record's own ID, used to approve/reject it
 * @param userId             the applicant's user ID
 * @param applicantFirstName applicant's first name
 * @param applicantLastName  applicant's last name
 * @param applicantEmail     applicant's decrypted email address
 * @param municipalityToken  the token string associated with the record
 * @param municipalityName   the resolved municipality name, or null for legacy free-text applications
 * @param submittedAt        when the record was created
 * @param status             current lifecycle status ({@code PENDING} / {@code APPROVED} / {@code REJECTED})
 * @author Refentse
 * @since 1.0
 */
public record AdminApplicationResponse(
        UUID applicationId,
        UUID userId,
        String applicantFirstName,
        String applicantLastName,
        String applicantEmail,
        String municipalityToken,
        String municipalityName,
        LocalDateTime submittedAt,
        AdminApplicationStatus status
) {}
