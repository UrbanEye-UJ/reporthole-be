package za.co.urbaneye.reporthole.incident.service.interfaces;

import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.entity.Incident;

public interface IncidentService {
    Incident createIncident(IncidentRequestDTO request);
}
