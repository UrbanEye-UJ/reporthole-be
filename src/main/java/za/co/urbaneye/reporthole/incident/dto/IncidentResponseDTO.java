package za.co.urbaneye.reporthole.incident.dto;

import lombok.Builder;
import za.co.urbaneye.reporthole.incident.entity.AssignmentStatus;
import za.co.urbaneye.reporthole.incident.entity.IncidentSource;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record IncidentResponseDTO(
        UUID incidentId,
        IssueType incidentType,
        String description,
        IncidentSource source,
        LocalDateTime incidentDate,
        double latitude,
        double longitude,
        String imageUrl,
        UUID userId,
        int reportCount,
        int reporterCount,
        String locationAddress,
        boolean duplicate,
        boolean alreadyConfirmed,
        UUID existingIncidentId,
        AssignmentStatus status
) {}
