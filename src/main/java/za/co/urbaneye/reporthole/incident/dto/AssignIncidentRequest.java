package za.co.urbaneye.reporthole.incident.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignIncidentRequest(
        @NotNull(message = "Contractor is required")
        UUID contractorId
) {
}
