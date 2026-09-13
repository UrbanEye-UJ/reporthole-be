package za.co.urbaneye.reporthole.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.urbaneye.reporthole.incident.entity.IncidentComment;

import java.util.List;
import java.util.UUID;

/** Repository for incident comments, ordered oldest-first for chronological display. */
public interface IncidentCommentRepository extends JpaRepository<IncidentComment, UUID> {

    List<IncidentComment> findByIncident_IncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
