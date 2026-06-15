package za.co.urbaneye.reporthole.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.urbaneye.reporthole.incident.entity.Incident;

import java.util.List;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findByUser_UserId(UUID userId);
}