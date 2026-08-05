package za.co.urbaneye.reporthole.admin.contractor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.urbaneye.reporthole.admin.contractor.dto.ContractorResponse;
import za.co.urbaneye.reporthole.admin.contractor.dto.CreateContractorRequest;
import za.co.urbaneye.reporthole.admin.contractor.service.interfaces.IContractorService;
import za.co.urbaneye.reporthole.global.entity.AppResponse;

import java.util.List;

/**
 * Endpoints for admins to register and list contractor accounts.
 *
 * <p>Requires an authenticated JWT. Only ADMIN users may call these — the service layer enforces the role guard.</p>
 */
@RestController
@RequestMapping("admin/contractors")
@RequiredArgsConstructor
@Tag(name = "Admin Contractors", description = "Endpoints for admins to register and list contractor accounts.")
public class AdminContractorController {

    private final IContractorService contractorService;

    @PostMapping
    @Operation(summary = "Register a contractor", description = "Creates an active CONTRACTOR account with the given initial password. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Contractor created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public ResponseEntity<AppResponse<ContractorResponse>> createContractor(@Valid @RequestBody CreateContractorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponse.created(contractorService.createContractor(request)));
    }

    @GetMapping
    @Operation(summary = "List contractors", description = "Returns all CONTRACTOR accounts with their active-job counts. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contractors returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin")
    })
    public ResponseEntity<AppResponse<List<ContractorResponse>>> getContractors() {
        return ResponseEntity.ok(AppResponse.ok(contractorService.getContractors()));
    }
}
