package za.co.urbaneye.reporthole.incident.dto;

import lombok.Data;
import za.co.urbaneye.reporthole.incident.entity.IncidentSource;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

@Data
public class IncidentRequestDTO {
    private IssueType incidentType;
    private String description;
    private IncidentSource source;
    private double latitude;
    private double longitude;
    private String imageBase64;
}
