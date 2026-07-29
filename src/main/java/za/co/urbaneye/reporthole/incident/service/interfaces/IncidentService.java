package za.co.urbaneye.reporthole.incident.service.interfaces;

import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

import java.util.List;
import java.util.UUID;

public interface IncidentService {
    IncidentResponseDTO createIncident(IncidentRequestDTO request);
    List<IncidentResponseDTO> getMyIncidents();

    /** Returns the authenticated user's incidents filtered by keyword and/or issue type. */
    List<IncidentResponseDTO> searchMyIncidents(String keyword, IssueType issueType);

    IncidentResponseDTO confirmDuplicate(UUID incidentId);
    IncidentResponseDTO getIncidentById(UUID incidentId);

    /** Soft-deletes the incident. Only the original reporter may delete their own incident. */
    void deleteIncident(UUID incidentId);
}
