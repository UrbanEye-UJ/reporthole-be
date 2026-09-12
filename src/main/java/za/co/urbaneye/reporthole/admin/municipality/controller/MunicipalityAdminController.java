package za.co.urbaneye.reporthole.admin.municipality.controller;

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
import za.co.urbaneye.reporthole.admin.municipality.dto.CreateMunicipalityRequest;
import za.co.urbaneye.reporthole.admin.municipality.dto.IssueTokenRequest;
import za.co.urbaneye.reporthole.admin.municipality.dto.MunicipalityResponse;
import za.co.urbaneye.reporthole.admin.municipality.dto.MunicipalityTokenResponse;
import za.co.urbaneye.reporthole.admin.municipality.service.interfaces.IMunicipalityService;
import za.co.urbaneye.reporthole.global.entity.AppResponse;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for managing municipalities and their registration tokens.
 *
 * <p>Requires an authenticated JWT. Every operation is restricted to {@code SECURITY_ADMIN},
 * enforced in the service layer (unauthorised callers receive 403 via the global handler).
 * Tokens issued here are entered on the registration form — a usable token registers the
 * applicant straight as {@code ADMIN}, bound to the token's municipality.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@RestController
@RequestMapping("admin/municipalities")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Municipalities", description = "Security-admin management of municipalities and admin registration tokens.")
public class MunicipalityAdminController {

    private final IMunicipalityService municipalityService;

    @PostMapping
    @Operation(
            summary = "Create a municipality",
            description = "Registers a municipality that admin accounts can belong to. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Municipality created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "409", description = "A municipality with that name already exists")
    })
    public ResponseEntity<AppResponse<MunicipalityResponse>> create(
            @Valid @RequestBody CreateMunicipalityRequest request) {
        MunicipalityResponse created = municipalityService.createMunicipality(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponse.of(created, "Municipality created.", 201));
    }

    @GetMapping
    @Operation(
            summary = "List municipalities",
            description = "Returns every municipality with its issued-token count. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Municipalities returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin")
    })
    public ResponseEntity<AppResponse<List<MunicipalityResponse>>> list() {
        return ResponseEntity.ok(AppResponse.ok(municipalityService.listMunicipalities()));
    }

    @PostMapping("/{id}/tokens")
    @Operation(
            summary = "Issue a registration token for a municipality",
            description = "Creates a multi-use token bound to the municipality. Optional expiry (days) and note. " +
                    "Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Token issued"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Municipality not found")
    })
    public ResponseEntity<AppResponse<MunicipalityTokenResponse>> issueToken(
            @PathVariable UUID id,
            @Valid @RequestBody IssueTokenRequest request) {
        MunicipalityTokenResponse token = municipalityService.issueToken(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponse.of(token, "Token issued.", 201));
    }

    @GetMapping("/tokens")
    @Operation(
            summary = "List issued registration tokens",
            description = "Returns every issued token, newest first, with a derived ACTIVE / EXPIRED / REVOKED " +
                    "status. Optionally filtered to one municipality with ?municipalityId=. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tokens returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin")
    })
    public ResponseEntity<AppResponse<List<MunicipalityTokenResponse>>> listTokens(
            @RequestParam(name = "municipalityId", required = false) UUID municipalityId) {
        return ResponseEntity.ok(AppResponse.ok(municipalityService.listTokens(municipalityId)));
    }

    @PostMapping("/tokens/{tokenId}/revoke")
    @Operation(
            summary = "Revoke a registration token",
            description = "Stops the token being used to register further admins. Existing admins are unaffected. " +
                    "Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token revoked"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Token not found"),
            @ApiResponse(responseCode = "409", description = "Token is already revoked")
    })
    public ResponseEntity<AppResponse<Void>> revokeToken(@PathVariable UUID tokenId) {
        municipalityService.revokeToken(tokenId);
        return ResponseEntity.ok(AppResponse.of(null, "Token revoked.", 200));
    }
}
