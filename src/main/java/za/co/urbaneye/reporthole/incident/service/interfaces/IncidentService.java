package za.co.urbaneye.reporthole.incident.service.interfaces;

import za.co.urbaneye.reporthole.incident.dto.IncidentAnalyticsDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentPageResponse;
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

    /**
     * Returns the most recently logged incidents, ordered by date descending.
     *
     * @param limit          max number of incidents to return
     * @param municipalityId when non-null, restricts results to exactly this municipality —
     *                       used by the SECURITY_ADMIN map view to inspect one municipality at
     *                       a time; when null, falls back to the caller's own scoping (an
     *                       ADMIN sees their municipality plus unverified incidents, anyone
     *                       else sees everything)
     */
    List<IncidentResponseDTO> getRecentIncidents(int limit, UUID municipalityId);

    /** Platform-wide totals for dashboard KPI cards. */
    IncidentStatsDTO getIncidentStats();

    /**
     * Aggregated operational analytics (status funnel, incident-type mix, monthly volume trend,
     * average resolution time and its trend) for the analytics dashboard.
     *
     * @param municipalityId when the caller is an ADMIN, ignored — their own municipality always
     *                       wins. Otherwise (SECURITY_ADMIN) honoured as an optional filter, or
     *                       {@code null} for platform-wide.
     */
    IncidentAnalyticsDTO getIncidentAnalytics(UUID municipalityId);

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

    /** Contractor posts a free-text progress note while the incident is IN_PROGRESS. */
    IncidentResponseDTO addProgressUpdate(UUID incidentId, String note);

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

    /**
     * Admin-initiated reopen: reverts a RESOLVED incident back to VERIFIED and removes the
     * current assignment so the admin can reassign it to a different (or the same) contractor.
     * Only ADMIN role can call this.
     *
     * @throws AssignmentException if the incident is not currently RESOLVED
     */
    IncidentResponseDTO reopenIncident(UUID incidentId);

    /**
     * Re-opens a RESOLVED incident: reverts it to VERIFIED so it can be reassigned, and
     * links the reporting user via {@code IncidentReporter} (idempotent).
     *
     * @throws za.co.urbaneye.reporthole.incident.exception.AssignmentException
     *         if the incident is not currently RESOLVED
     */
    IncidentResponseDTO reportStillUnresolved(UUID incidentId);

    /**
     * Returns non-deleted incidents within {@code radiusMeters} of the given point, closest
     * first, so a civilian can see what's already been reported nearby before submitting —
     * independent of issue type, unlike the submission-time duplicate check.
     */
    List<IncidentResponseDTO> getNearbyIncidents(double latitude, double longitude, double radiusMeters);

    /**
     * Paginated, filterable incident search for the SECURITY_ADMIN incidents view.
     *
     * @param municipalityId when non-null, restricts results to this municipality
     * @param issueType      when non-null, restricts results to this issue type
     * @param page           0-based page number
     * @param size           page size
     * @return the matching page of incidents, newest first
     */
    IncidentPageResponse searchIncidents(UUID municipalityId, IssueType issueType, int page, int size);
}
