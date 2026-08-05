package za.co.urbaneye.reporthole.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.urbaneye.reporthole.incident.entity.Assignment;
import za.co.urbaneye.reporthole.incident.entity.AssignmentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    /** Number of assignments for this contractor that haven't reached the given status (e.g. not yet RESOLVED). */
    int countByContractor_UserIdAndStatusNot(UUID contractorId, AssignmentStatus status);

    /** Number of assignments for this contractor currently at the given status. */
    int countByContractor_UserIdAndStatus(UUID contractorId, AssignmentStatus status);

    /** All assignments (any status) handed to this contractor. */
    List<Assignment> findByContractor_UserId(UUID contractorId);

    /** This contractor's assignment for a specific incident, if any — also doubles as an ownership check. */
    Optional<Assignment> findByIncident_IncidentIdAndContractor_UserId(UUID incidentId, UUID contractorId);
}
