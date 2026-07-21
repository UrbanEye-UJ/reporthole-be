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
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
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
