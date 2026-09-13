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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationRequest;
import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationResponse;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplicationStatus;
import za.co.urbaneye.reporthole.admin.application.service.interfaces.IAdminApplicationService;
import za.co.urbaneye.reporthole.global.entity.AppResponse;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoint for submitting and reviewing admin access applications.
 *
 * <p>Requires an authenticated JWT. Only CIVILIAN users may apply, and only
 * SECURITY_ADMIN users may list, approve, or reject applications — approval is a
 * role grant, so it belongs to the security admin rather than to operational
 * ADMINs. Both guards are enforced in the service layer, and approval writes an
 * append-only access-control audit row.</p>
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
                    "A notification email is sent to the project inbox, and the application appears in the " +
                    "ADMIN review queue at GET /admin/applications."
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

    @GetMapping
    @Operation(
            summary = "List admin applications",
            description = "Returns admin-access records for the reviewing security admin. Without a filter it " +
                    "returns every record — PENDING, APPROVED and REJECTED — newest first. Pass " +
                    "?status=PENDING|APPROVED|REJECTED to filter. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applications returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin")
    })
    public ResponseEntity<AppResponse<List<AdminApplicationResponse>>> listApplications(
            @RequestParam(name = "status", required = false) AdminApplicationStatus status) {
        return ResponseEntity.ok(AppResponse.ok(adminApplicationService.listApplications(status)));
    }

    @PostMapping("/{id}/approve")
    @Operation(
            summary = "Approve an admin application",
            description = "Promotes the applicant to ADMIN and marks the application APPROVED. Sends a decision " +
                    "email to the applicant. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application approved"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Application not found"),
            @ApiResponse(responseCode = "409", description = "Application already processed")
    })
    public ResponseEntity<AppResponse<Void>> approve(@PathVariable UUID id) {
        adminApplicationService.approve(id);
        return ResponseEntity.ok(AppResponse.of(null, "Application approved.", 200));
    }

    @PostMapping("/{id}/reject")
    @Operation(
            summary = "Reject an admin application",
            description = "Marks the application REJECTED without changing the applicant's role. Sends a decision " +
                    "email to the applicant. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application rejected"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Application not found"),
            @ApiResponse(responseCode = "409", description = "Application already processed")
    })
    public ResponseEntity<AppResponse<Void>> reject(@PathVariable UUID id) {
        adminApplicationService.reject(id);
        return ResponseEntity.ok(AppResponse.of(null, "Application rejected.", 200));
    }
}
