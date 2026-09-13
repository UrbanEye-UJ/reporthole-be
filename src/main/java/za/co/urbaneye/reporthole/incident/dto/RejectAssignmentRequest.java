package za.co.urbaneye.reporthole.incident.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectAssignmentRequest(
        @NotBlank(message = "A reason is required to reject an assignment")
        String reason
) {
}
