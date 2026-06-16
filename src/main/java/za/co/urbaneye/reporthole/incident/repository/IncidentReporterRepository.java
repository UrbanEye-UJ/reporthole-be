package za.co.urbaneye.reporthole.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.urbaneye.reporthole.incident.entity.IncidentReporter;

import java.util.List;
import java.util.UUID;

public interface IncidentReporterRepository extends JpaRepository<IncidentReporter, UUID> {

    boolean existsByIncident_IncidentIdAndUser_UserId(UUID incidentId, UUID userId);

    int countByIncident_IncidentId(UUID incidentId);

    /**
     * Returns the user IDs of everyone who has been linked to an incident — both the
     * original reporter (who gets an entry created on {@code createIncident}) and any
     * users who later confirmed it as a duplicate via {@code confirmDuplicate}.
     *
     * <p>Used by the SSE push logic to narrow notifications to only the users who care
     * about a given incident rather than broadcasting to all connected clients.</p>
     *
     * @param incidentId the incident whose reporter user IDs are needed
     * @return list of distinct user IDs linked to the incident; may be empty but never null
     */
    @Query("SELECT ir.user.userId FROM IncidentReporter ir WHERE ir.incident.incidentId = :incidentId")
    List<UUID> findUserIdsByIncidentId(@Param("incidentId") UUID incidentId);
}
