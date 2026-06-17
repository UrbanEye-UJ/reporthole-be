package za.co.urbaneye.reporthole.incident.dto;

import za.co.urbaneye.reporthole.incident.entity.IncidentSource;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

public record IncidentRequestDTO(
        IssueType incidentType,
        String description,
        IncidentSource source,
        double latitude,
        double longitude,
        String imageBase64,
        boolean forceCreate,
        String locationAddress
) {}
