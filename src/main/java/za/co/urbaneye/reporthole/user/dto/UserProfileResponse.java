package za.co.urbaneye.reporthole.user.dto;

import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.user.entity.UserRole;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO returned by the user profile endpoints.
 *
 * @param userId           unique user identifier
 * @param firstName        decrypted first name
 * @param lastName         decrypted last name
 * @param email            decrypted email address
 * @param phoneNumber      decrypted phone number
 * @param role             assigned role
 * @param municipalityName  name of the admin or contractor's municipality; null for civilians
 * @param createdAt         account creation timestamp
 * @param specialisations   issue types the contractor can handle; null for non-contractor roles
 */
public record UserProfileResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        UserRole role,
        String municipalityName,
        LocalDateTime createdAt,
        Set<IssueType> specialisations
) {}
