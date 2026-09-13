package za.co.urbaneye.reporthole.user.dto;

import jakarta.validation.constraints.NotEmpty;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

import java.util.Set;

/**
 * Request body for {@code PATCH /users/profile/specialisations}.
 * Replaces the contractor's entire specialisation set in one call.
 *
 * @param specialisations the new set of issue types; must contain at least one entry
 */
public record UpdateSpecialisationsRequest(
        @NotEmpty(message = "At least one specialisation is required")
        Set<IssueType> specialisations
) {}
