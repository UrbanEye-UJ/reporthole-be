package za.co.urbaneye.reporthole.admin.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationRequest;
import za.co.urbaneye.reporthole.admin.application.service.interfaces.IAdminApplicationService;
import za.co.urbaneye.reporthole.global.entity.AppResponse;

/**
 * REST endpoint for submitting admin access applications.
 *
 * <p>Requires an authenticated JWT. Only CIVILIAN users may apply —
 * the service layer enforces the role guard.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@RestController
@RequestMapping("admin/applications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Applications", description = "Endpoints for submitting and managing admin access requests.")
public class AdminApplicationController {

    private final IAdminApplicationService adminApplicationService;

    @PostMapping
    @Operation(
            summary = "Apply for admin access",
            description = "Allows an authenticated CIVILIAN to submit a municipality token as evidence of affiliation. " +
                    "A notification email is sent to the project inbox. A developer manually promotes the user " +
                    "via scripts/promote-to-admin.sql after verification."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Application submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Account is already ADMIN or CONTRACTOR"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "409", description = "Application already submitted for this account")
    })
    public ResponseEntity<AppResponse<Void>> apply(@Valid @RequestBody AdminApplicationRequest request) {
        adminApplicationService.apply(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AppResponse.of(null, "Application submitted. We'll be in touch.", 201));
    }

    @PostMapping("/{id}/approve")
    @Operation(
            summary = "Approve an admin application (no auth required)",
            description = "Promotes the applicant to ADMIN and marks the application APPROVED. " +
                    "This endpoint is intentionally unauthenticated — it is designed for bootstrap " +
                    "scenarios where no admin account exists yet. Once the first admin is in place, " +
                    "use the authenticated approve flow instead."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application approved and user promoted to ADMIN"),
            @ApiResponse(responseCode = "404", description = "Application not found"),
            @ApiResponse(responseCode = "409", description = "Application has already been approved")
    })
    public ResponseEntity<AppResponse<Void>> approveOpen(@PathVariable UUID id) {
        adminApplicationService.approveOpen(id);
        return ResponseEntity.ok(AppResponse.of(null, "Application approved.", 200));
    }
}
