package za.co.urbaneye.reporthole.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import za.co.urbaneye.reporthole.incident.entity.AssignmentWorkflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentWorkflowRepository extends JpaRepository<AssignmentWorkflow, UUID> {

    /** Most recent workflow entry for the incident, i.e. its current status. */
    Optional<AssignmentWorkflow> findFirstByIncident_IncidentIdOrderByUpdatedDateDesc(UUID incidentId);

    /** Full ordered history for one incident, oldest first — used to populate the progress timeline. */
    List<AssignmentWorkflow> findAllByIncident_IncidentIdOrderByUpdatedDateAsc(UUID incidentId);

    /** Number of distinct incidents whose most recent workflow entry is RESOLVED. */
    @Query("""
            SELECT COUNT(DISTINCT aw.incident.incidentId) FROM AssignmentWorkflow aw
            WHERE aw.status = za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.RESOLVED
              AND aw.updatedDate = (
                  SELECT MAX(aw2.updatedDate) FROM AssignmentWorkflow aw2 WHERE aw2.incident = aw.incident
              )
            """)
    long countResolvedIncidents();
}
