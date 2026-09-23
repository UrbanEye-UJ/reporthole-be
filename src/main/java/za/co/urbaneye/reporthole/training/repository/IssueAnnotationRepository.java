package za.co.urbaneye.reporthole.training.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.urbaneye.reporthole.training.entity.IssueAnnotation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IssueAnnotationRepository extends JpaRepository<IssueAnnotation, UUID> {

    List<IssueAnnotation> findByIncident_IncidentIdOrderByCreatedAtAsc(UUID incidentId);

    long countByIncident_IncidentId(UUID incidentId);

    List<IssueAnnotation> findByIncident_IncidentIdIn(Collection<UUID> incidentIds);
}
