package za.co.urbaneye.reporthole.incident.service.interfaces;

import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;

import java.util.List;

public interface IncidentService {
    IncidentResponseDTO createIncident(IncidentRequestDTO request);
    List<IncidentResponseDTO> getMyIncidents();
}
