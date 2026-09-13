package za.co.urbaneye.reporthole.incident.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;
import za.co.urbaneye.reporthole.global.entity.AppResponse;
import za.co.urbaneye.reporthole.incident.clustering.IncidentClusterDTO;
import za.co.urbaneye.reporthole.incident.clustering.IncidentClusteringService;
import za.co.urbaneye.reporthole.incident.dto.AssignIncidentRequest;
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentStatsDTO;
import za.co.urbaneye.reporthole.incident.dto.RejectAssignmentRequest;
import za.co.urbaneye.reporthole.incident.dto.ResolveIncidentRequest;
import za.co.urbaneye.reporthole.incident.service.impl.IncidentSseService;
import za.co.urbaneye.reporthole.incident.service.interfaces.IncidentService;

@RestController
@RequestMapping("incidents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Incidents", description = "Endpoints for incidents created.")
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentSseService incidentSseService;
    private final IncidentClusteringService incidentClusteringService;

    @PostMapping("/create")
    @Operation(
            summary = "Create incident",
            description = "Creates a new incident. If a similar incident exists nearby, returns it with duplicate=true so the client can prompt the user to confirm."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "New incident created"),
            @ApiResponse(responseCode = "200", description = "Potential duplicate found — client should prompt user to confirm"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AppResponse<IncidentResponseDTO>> createIncident(@Valid @RequestBody IncidentRequestDTO request) {
        IncidentResponseDTO result = incidentService.createIncident(request);
        if (result.duplicate()) {
            return ResponseEntity.ok(AppResponse.of(result, "A similar incident was found nearby. Please confirm if this is the same issue.", 200));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(AppResponse.created(result));
    }

    @PostMapping("/{id}/confirm")
    @Operation(
            summary = "Confirm duplicate",
            description = "Called when the user confirms they are reporting the same issue as an existing incident. Increments the report count to signal severity."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report count incremented"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<AppResponse<IncidentResponseDTO>> confirmDuplicate(@PathVariable UUID id) {
        IncidentResponseDTO result = incidentService.confirmDuplicate(id);
        return ResponseEntity.ok(AppResponse.ok(result));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my incidents", description = "Returns all incidents reported by the authenticated user.")
    public ResponseEntity<AppResponse<List<IncidentResponseDTO>>> getMyIncidents() {
        return ResponseEntity.ok(AppResponse.ok(incidentService.getMyIncidents()));
    }

    @GetMapping("/recent")
    @Operation(
            summary = "Get recent incidents",
            description = "Returns the most recently logged incidents across all users, ordered by date descending. Intended for admin dashboards."
    )
    public ResponseEntity<AppResponse<List<IncidentResponseDTO>>> getRecentIncidents(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(AppResponse.ok(incidentService.getRecentIncidents(limit)));
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get incident stats",
            description = "Returns platform-wide totals (total logged, total resolved) for admin dashboard KPI cards."
    )
    public ResponseEntity<AppResponse<IncidentStatsDTO>> getIncidentStats() {
        return ResponseEntity.ok(AppResponse.ok(incidentService.getIncidentStats()));
    }

    @GetMapping("/pending-review")
    @Operation(
            summary = "Get AI incidents pending review",
            description = "Returns AI-generated incidents whose detection confidence fell below the auto-approval " +
                    "threshold, so they were left as REPORTED instead of being auto-verified. Admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending-review incidents returned (may be empty)"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin")
    })
    public ResponseEntity<AppResponse<List<IncidentResponseDTO>>> getIncidentsPendingAiReview() {
        return ResponseEntity.ok(AppResponse.ok(incidentService.getIncidentsPendingAiReview()));
    }

    @GetMapping("/clusters")
    @Operation(
            summary = "Cluster incidents by location",
            description = "Groups non-deleted incidents into up to k clusters of nearby locations using K-Means, " +
                    "optionally restricted to a single issue type. Intended for admin dashboard hotspot maps."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clusters returned (empty if there are no matching incidents)"),
            @ApiResponse(responseCode = "500", description = "k is less than 1")
    })
    public ResponseEntity<AppResponse<List<IncidentClusterDTO>>> getIncidentClusters(
            @RequestParam(defaultValue = "5") int k,
            @RequestParam(required = false) IssueType type) {
        return ResponseEntity.ok(AppResponse.ok(incidentClusteringService.clusterIncidents(k, type)));
    }

    @GetMapping("/my/search")
    @Operation(
            summary = "Search my incidents",
            description = "Filters the authenticated user's incidents by a free-text keyword (matched against " +
                    "description and location address) and/or by issue type. Omit either parameter to skip that filter."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filtered list returned (may be empty)"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ResponseEntity<AppResponse<List<IncidentResponseDTO>>> searchMyIncidents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) IssueType type) {
        log.info("Search request — keyword: '{}', type: {}", keyword, type);
        return ResponseEntity.ok(AppResponse.ok(incidentService.searchMyIncidents(keyword, type)));
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "SSE stream",
            description = "Opens a persistent SSE connection for real-time incident update notifications. " +
                    "Authenticate via ?token=<jwt> query param (EventSource does not support custom headers). " +
                    "Emits an 'incident-updated' event whose payload is the incident UUID whenever an incident " +
                    "linked to this user is changed by another reporter."
    )
    public SseEmitter subscribeToIncidentEvents() {
        UUID userId = UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return incidentSseService.subscribe(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident by ID", description = "Returns a single incident by its UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident found"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<AppResponse<IncidentResponseDTO>> getIncidentById(@PathVariable UUID id) {
        return ResponseEntity.ok(AppResponse.ok(incidentService.getIncidentById(id)));
    }

    @PostMapping("/{id}/verify")
    @Operation(
            summary = "Verify incident",
            description = "Marks a REPORTED incident as VERIFIED, confirming it is a genuine issue. Must happen before the incident can be assigned to a contractor. Admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident verified"),
            @ApiResponse(responseCode = "400", description = "Incident is not in REPORTED status"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<AppResponse<IncidentResponseDTO>> verifyIncident(@PathVariable UUID id) {
        return ResponseEntity.ok(AppResponse.ok(incidentService.verifyIncident(id)));
    }

    @PostMapping("/{id}/assign")
    @Operation(
            summary = "Assign incident to contractor",
            description = "Assigns the incident to a contractor, creating an Assignment and advancing its status to ASSIGNED. Admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident assigned"),
            @ApiResponse(responseCode = "400", description = "Selected user is not a contractor"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
            @ApiResponse(responseCode = "404", description = "Incident or contractor not found")
    })
    public ResponseEntity<AppResponse<IncidentResponseDTO>> assignIncident(
            @PathVariable UUID id, @Valid @RequestBody AssignIncidentRequest request) {
        return ResponseEntity.ok(AppResponse.ok(incidentService.assignIncident(id, request.contractorId())));
    }

    @PostMapping("/{id}/accept")
    @Operation(
            summary = "Accept assignment",
            description = "Called by the assigned contractor to accept the incident, advancing its status to IN_PROGRESS."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment accepted"),
            @ApiResponse(responseCode = "400", description = "Assignment is not pending acceptance"),
            @ApiResponse(responseCode = "404", description = "No assignment found for this contractor and incident")
    })
    public ResponseEntity<AppResponse<IncidentResponseDTO>> acceptAssignment(@PathVariable UUID id) {
        return ResponseEntity.ok(AppResponse.ok(incidentService.acceptAssignment(id)));
    }

    @PostMapping("/{id}/reject")
    @Operation(
            summary = "Reject assignment",
            description = "Called by the assigned contractor to reject the incident, giving a required reason. " +
                    "The assignment is removed from the contractor and the incident reverts to VERIFIED so an " +
                    "admin can assign it to someone else."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment rejected"),
            @ApiResponse(responseCode = "400", description = "Assignment is not pending acceptance, or reason is missing"),
            @ApiResponse(responseCode = "404", description = "No assignment found for this contractor and incident")
    })
    public ResponseEntity<AppResponse<IncidentResponseDTO>> rejectAssignment(
            @PathVariable UUID id, @Valid @RequestBody RejectAssignmentRequest request) {
        return ResponseEntity.ok(AppResponse.ok(incidentService.rejectAssignment(id, request)));
    }

    @GetMapping("/my-assignments")
    @Operation(
            summary = "Get my assignments",
            description = "Returns every incident assigned to the authenticated contractor, any status."
    )
    public ResponseEntity<AppResponse<List<IncidentResponseDTO>>> getMyAssignments() {
        return ResponseEntity.ok(AppResponse.ok(incidentService.getMyAssignments()));
    }

    @PostMapping("/{id}/resolve")
    @Operation(
            summary = "Resolve incident",
            description = "Marks the caller's assignment for this incident as RESOLVED, storing a repair photo and note. " +
                    "Caller must be the contractor this incident is assigned to."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident resolved"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "No assignment found for this contractor and incident")
    })
    public ResponseEntity<AppResponse<IncidentResponseDTO>> resolveIncident(
            @PathVariable UUID id, @Valid @RequestBody ResolveIncidentRequest request) {
        return ResponseEntity.ok(AppResponse.ok(incidentService.resolveIncident(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete incident", description = "Soft-deletes an incident. Only the original reporter may delete their own incident.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Incident deleted"),
            @ApiResponse(responseCode = "403", description = "Not the original reporter"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<Void> deleteIncident(@PathVariable UUID id) {
        incidentService.deleteIncident(id);
        return ResponseEntity.noContent().build();
    }
}
