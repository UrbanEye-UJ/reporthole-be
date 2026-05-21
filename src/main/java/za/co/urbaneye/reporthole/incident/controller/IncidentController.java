package za.co.urbaneye.reporthole.incident.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import za.co.urbaneye.reporthole.global.entity.AppResponse;
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
import za.co.urbaneye.reporthole.incident.service.interfaces.IncidentService;

@RestController
@RequestMapping("incidents")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Incidents",
        description = "Endpoints for incidents created."
)
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping("/create")
    @Operation(
            summary = "Create incident",
            description = "Creates a new incident and stores in the database."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Incident created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or incident already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AppResponse<IncidentResponseDTO>> createIncident(@RequestBody IncidentRequestDTO request) {
        IncidentResponseDTO incident = incidentService.createIncident(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AppResponse.created(incident));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my incidents", description = "Returns all incidents reported by the authenticated user.")
    public ResponseEntity<AppResponse<List<IncidentResponseDTO>>> getMyIncidents() {
        return ResponseEntity.ok(AppResponse.ok(incidentService.getMyIncidents()));
    }
}
