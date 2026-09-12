package za.co.urbaneye.reporthole.admin.contractor.dto;

import za.co.urbaneye.reporthole.incident.entity.IssueType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ContractorResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        int activeJobs,
        int completedJobs,
        LocalDateTime createdAt,
        List<IssueType> specialisations
) {
}
