package za.co.urbaneye.reporthole.incident.dto;

import lombok.Builder;
import lombok.Data;
import za.co.urbaneye.reporthole.incident.entity.IncidentSource;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class IncidentResponseDTO {
    private UUID incidentId;
    private IssueType incidentType;
    private String description;
    private IncidentSource source;
    private LocalDateTime incidentDate;
    private double latitude;
    private double longitude;
    private String imageUrl;
    private UUID userId;
    private int reportCount;
    private int reporterCount;
    private String locationAddress;
    private boolean duplicate;
    private boolean alreadyConfirmed;
    private UUID existingIncidentId;
}
