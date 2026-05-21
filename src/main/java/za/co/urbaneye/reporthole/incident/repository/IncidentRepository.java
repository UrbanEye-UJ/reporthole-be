package za.co.urbaneye.reporthole.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.urbaneye.reporthole.incident.entity.Incident;

import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
}