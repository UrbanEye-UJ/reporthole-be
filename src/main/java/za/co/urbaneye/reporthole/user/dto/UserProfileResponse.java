package za.co.urbaneye.reporthole.user.dto;

import za.co.urbaneye.reporthole.user.entity.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO returned by the user profile endpoints.
 *
 * @param userId      unique user identifier
 * @param firstName   decrypted first name
 * @param lastName    decrypted last name
 * @param email       decrypted email address
 * @param phoneNumber decrypted phone number
 * @param role        assigned role
 * @param createdAt   account creation timestamp
 */
public record UserProfileResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        UserRole role,
        LocalDateTime createdAt
) {}
