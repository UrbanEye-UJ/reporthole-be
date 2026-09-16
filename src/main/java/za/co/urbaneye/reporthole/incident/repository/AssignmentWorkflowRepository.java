package za.co.urbaneye.reporthole.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Current-status breakdown (most recent workflow entry per incident) across the given
     * incidents, as {@code [AssignmentStatus, Long]} pairs — used by the analytics status funnel.
     */
    @Query("""
            SELECT aw.status, COUNT(DISTINCT aw.incident.incidentId) FROM AssignmentWorkflow aw
            WHERE aw.incident.incidentId IN :incidentIds
              AND aw.updatedDate = (
                  SELECT MAX(aw2.updatedDate) FROM AssignmentWorkflow aw2 WHERE aw2.incident = aw.incident
              )
            GROUP BY aw.status
            """)
    List<Object[]> findStatusBreakdown(@Param("incidentIds") List<UUID> incidentIds);
}
