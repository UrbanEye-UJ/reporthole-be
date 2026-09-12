package za.co.urbaneye.reporthole.admin.contractor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.urbaneye.reporthole.admin.contractor.dto.CompleteContractorRegistrationRequest;
import za.co.urbaneye.reporthole.admin.contractor.dto.ContractorResponse;
import za.co.urbaneye.reporthole.admin.contractor.service.interfaces.IContractorService;
import za.co.urbaneye.reporthole.global.entity.AppResponse;

/**
 * Public endpoint for contractors to complete their registration via an invite token.
 *
 * <p>No authentication required — the invite token acts as the credential for this step.</p>
 */
@RestController
@RequestMapping("contractors")
@RequiredArgsConstructor
@Tag(name = "Contractor Registration", description = "Public endpoint for completing contractor account setup from an invite.")
public class ContractorRegistrationController {

    private final IContractorService contractorService;

    @PostMapping("/complete-registration")
    @Operation(
            summary = "Complete contractor registration",
            description = "Validates the invite token from the email link and creates the contractor account with the supplied personal details and password. The token is single-use and expires after 48 hours."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Validation error, token invalid, token expired, or token already used"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public ResponseEntity<AppResponse<ContractorResponse>> completeRegistration(
            @Valid @RequestBody CompleteContractorRegistrationRequest request) {
        return ResponseEntity.ok(AppResponse.ok(contractorService.completeContractorRegistration(request)));
    }
}
