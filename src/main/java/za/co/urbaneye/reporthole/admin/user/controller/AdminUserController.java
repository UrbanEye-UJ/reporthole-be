package za.co.urbaneye.reporthole.admin.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.urbaneye.reporthole.admin.user.dto.CivilianSummaryResponse;
import za.co.urbaneye.reporthole.admin.user.service.interfaces.IAdminUserService;
import za.co.urbaneye.reporthole.global.entity.AppResponse;

import java.util.List;

/**
 * Admin-facing endpoints for browsing registered civilian accounts.
 *
 * <p>Requires an authenticated JWT. Only {@code ADMIN} and {@code SECURITY_ADMIN} roles may
 * access these endpoints — the role check is enforced in the service layer so that any
 * misconfigured route still falls back to a service-level guard.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@RestController
@RequestMapping("admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Admin-facing view of registered civilian accounts.")
public class AdminUserController {

    private final IAdminUserService adminUserService;

    @GetMapping("/civilians")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_ADMIN')")
    @Operation(
            summary = "List civilian accounts",
            description = "Returns all CIVILIAN accounts with partially masked names and emails, and an " +
                    "incident count per user. Accessible to ADMIN and SECURITY_ADMIN roles."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Civilians returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Caller does not hold ADMIN or SECURITY_ADMIN role")
    })
    public ResponseEntity<AppResponse<List<CivilianSummaryResponse>>> getCivilians() {
        return ResponseEntity.ok(AppResponse.ok(adminUserService.getCivilians()));
    }
}
