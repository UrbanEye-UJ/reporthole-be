package za.co.urbaneye.reporthole.admin.contractor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

import java.util.List;

/**
 * Submitted by an admin to invite a contractor to self-register.
 *
 * @param email           contractor's email address — an invite link will be sent here
 * @param specialisations issue types this contractor is qualified to handle
 */
public record InviteContractorRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @NotEmpty(message = "At least one specialisation is required")
        List<IssueType> specialisations
) {}
