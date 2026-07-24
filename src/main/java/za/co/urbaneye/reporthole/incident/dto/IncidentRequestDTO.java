package za.co.urbaneye.reporthole.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import za.co.urbaneye.reporthole.incident.entity.IncidentSource;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

public record IncidentRequestDTO(
        @NotNull(message = "Issue type is required")
        IssueType incidentType,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Source is required")
        IncidentSource source,

        double latitude,
        double longitude,

        @NotBlank(message = "Image is required")
        String imageBase64,

        boolean forceCreate,
        String locationAddress
) {}
