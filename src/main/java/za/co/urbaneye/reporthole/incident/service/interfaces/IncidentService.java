package za.co.urbaneye.reporthole.incident.service.interfaces;

import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;

import java.util.List;
import java.util.UUID;

public interface IncidentService {
    IncidentResponseDTO createIncident(IncidentRequestDTO request);
    List<IncidentResponseDTO> getMyIncidents();
    IncidentResponseDTO confirmDuplicate(UUID incidentId);
    IncidentResponseDTO getIncidentById(UUID incidentId);
}
