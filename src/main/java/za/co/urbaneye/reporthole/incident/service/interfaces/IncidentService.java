package za.co.urbaneye.reporthole.incident.service.interfaces;

import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentStatsDTO;
import za.co.urbaneye.reporthole.incident.dto.RejectAssignmentRequest;
import za.co.urbaneye.reporthole.incident.dto.ResolveIncidentRequest;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

import java.util.List;
import java.util.UUID;

public interface IncidentService {
    IncidentResponseDTO createIncident(IncidentRequestDTO request);
    List<IncidentResponseDTO> getMyIncidents();

    /** Returns the most recently logged incidents across all users, ordered by date descending. */
    List<IncidentResponseDTO> getRecentIncidents(int limit);

    /** Platform-wide totals for dashboard KPI cards. */
    IncidentStatsDTO getIncidentStats();

    /** Assigns the incident to a contractor. Caller must be an ADMIN; the target user must be a CONTRACTOR. */
    IncidentResponseDTO assignIncident(UUID incidentId, UUID contractorId);

    /** Marks a REPORTED incident as VERIFIED, confirming it's genuine before it can be assigned. Caller must be an ADMIN. */
    IncidentResponseDTO verifyIncident(UUID incidentId);

    /** Returns every incident assigned to the authenticated contractor, any status. */
    List<IncidentResponseDTO> getMyAssignments();

    /** Contractor accepts their pending assignment, advancing the incident to IN_PROGRESS. */
    IncidentResponseDTO acceptAssignment(UUID incidentId);

    /**
     * Contractor rejects their pending assignment, giving a required reason; it's removed
     * from them and the incident reverts to VERIFIED so an admin can reassign it.
     */
    IncidentResponseDTO rejectAssignment(UUID incidentId, RejectAssignmentRequest request);

    /** Marks the caller's assignment for this incident as RESOLVED, storing the repair photo and note. */
    IncidentResponseDTO resolveIncident(UUID incidentId, ResolveIncidentRequest request);

    /** Returns the authenticated user's incidents filtered by keyword and/or issue type. */
    List<IncidentResponseDTO> searchMyIncidents(String keyword, IssueType issueType);

    IncidentResponseDTO confirmDuplicate(UUID incidentId);
    IncidentResponseDTO getIncidentById(UUID incidentId);

    /**
     * Returns AI-generated incidents still awaiting human review — i.e. their detection
     * confidence was below the auto-approval threshold, so they were left as {@code REPORTED}
     * instead of being auto-verified. Caller must be an ADMIN.
     */
    List<IncidentResponseDTO> getIncidentsPendingAiReview();

    /** Soft-deletes the incident. Only the original reporter may delete their own incident. */
    void deleteIncident(UUID incidentId);
}
