package za.co.urbaneye.reporthole.admin.contractor.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContractorResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        int activeJobs,
        int completedJobs,
        LocalDateTime createdAt
) {
}
