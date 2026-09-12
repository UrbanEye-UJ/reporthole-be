package za.co.urbaneye.reporthole.admin.contractor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.urbaneye.reporthole.admin.contractor.dto.ContractorResponse;
import za.co.urbaneye.reporthole.admin.contractor.dto.InviteContractorRequest;
import za.co.urbaneye.reporthole.admin.contractor.dto.RevealEmailRequest;
import za.co.urbaneye.reporthole.admin.contractor.dto.RevealEmailResponse;
import za.co.urbaneye.reporthole.admin.contractor.service.interfaces.IContractorService;
import za.co.urbaneye.reporthole.global.entity.AppResponse;

import java.util.List;
import java.util.UUID;

/**
 * Admin-only endpoints for inviting and listing contractor accounts.
 *
 * <p>Requires an authenticated JWT. Only ADMIN users may call these — the service layer enforces the role guard.</p>
 */
@RestController
@RequestMapping("admin/contractors")
@RequiredArgsConstructor
@Tag(name = "Admin Contractors", description = "Admin endpoints for inviting and listing contractor accounts.")
public class AdminContractorController {

    private final IContractorService contractorService;

    @PostMapping("/invite")
    @Operation(
            summary = "Invite a contractor",
            description = "Sends an invite email to the given address. The contractor follows the link to complete their own registration. Admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invite sent"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
            @ApiResponse(responseCode = "409", description = "Email already registered or already invited")
    })
    public ResponseEntity<AppResponse<Void>> inviteContractor(@Valid @RequestBody InviteContractorRequest request) {
        contractorService.inviteContractor(request);
        return ResponseEntity.ok(AppResponse.ok(null));
    }

    @GetMapping
    @Operation(
            summary = "List contractors",
            description = "Returns all CONTRACTOR accounts with their active-job counts. Admin only. " +
                    "Emails are masked (e.g. \"jo***@example.com\") — use POST /{id}/reveal-email to view one in full."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contractors returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin")
    })
    public ResponseEntity<AppResponse<List<ContractorResponse>>> getContractors() {
        return ResponseEntity.ok(AppResponse.ok(contractorService.getContractors()));
    }

    @PostMapping("/{id}/reveal-email")
    @Operation(
            summary = "Reveal a contractor's email",
            description = "Returns the contractor's decrypted email after verifying the calling admin's own " +
                    "current password as a step-up re-authentication check. Admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email revealed"),
            @ApiResponse(responseCode = "401", description = "Incorrect password"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
            @ApiResponse(responseCode = "404", description = "Contractor not found")
    })
    public ResponseEntity<AppResponse<RevealEmailResponse>> revealEmail(
            @PathVariable UUID id, @Valid @RequestBody RevealEmailRequest request) {
        return ResponseEntity.ok(AppResponse.ok(contractorService.revealEmail(id, request.password())));
    }
}
