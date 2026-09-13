package za.co.urbaneye.reporthole.admin.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.urbaneye.reporthole.admin.security.dto.AccountActionRequest;
import za.co.urbaneye.reporthole.admin.security.dto.AuditEntryResponse;
import za.co.urbaneye.reporthole.admin.security.dto.GrantRoleRequest;
import za.co.urbaneye.reporthole.admin.security.dto.SecurityUserResponse;
import za.co.urbaneye.reporthole.admin.security.service.interfaces.ISecurityAdminService;
import za.co.urbaneye.reporthole.global.entity.AppResponse;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for the {@code SECURITY_ADMIN} responsibility: granting and revoking roles,
 * suspending and reactivating accounts, forcing logout, and reading the access-control audit
 * trail.
 *
 * <p>Requires an authenticated JWT. The {@code SECURITY_ADMIN} role check — and the rule that a
 * security admin may not act on their own account — are enforced in the service layer
 * ({@link ISecurityAdminService}), consistent with the rest of this codebase; unauthorised
 * callers receive 403 and self-targeting receives 400 via the global exception handler.</p>
 *
 * <p>None of these endpoints can mutate incidents, and none can edit or delete an audit row —
 * that separation is deliberate.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@RestController
@RequestMapping("admin/security")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Security Admin", description = "Identity & accountability: role grants/revokes, account suspension, forced logout, and the access-control audit trail.")
public class SecurityAdminController {

    private final ISecurityAdminService securityAdminService;

    @GetMapping("/users")
    @Operation(
            summary = "List all user accounts",
            description = "Returns every account — id, name, email, role and status — so a security admin can " +
                    "pick one to act on. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin")
    })
    public ResponseEntity<AppResponse<List<SecurityUserResponse>>> listUsers() {
        return ResponseEntity.ok(AppResponse.ok(securityAdminService.listUsers()));
    }

    @PostMapping("/users/{userId}/role")
    @Operation(
            summary = "Grant or change an account's role",
            description = "Sets the account's role and immediately invalidates its existing sessions so the " +
                    "new role takes effect on the next sign-in. Writes a ROLE_GRANTED audit row. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role granted"),
            @ApiResponse(responseCode = "400", description = "Account already has that role, or caller targeted themselves"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Target user not found")
    })
    public ResponseEntity<AppResponse<Void>> grantRole(@PathVariable UUID userId,
                                                       @Valid @RequestBody GrantRoleRequest request) {
        securityAdminService.grantRole(userId, request);
        return ResponseEntity.ok(AppResponse.of(null, "Role granted.", 200));
    }

    @PostMapping("/users/{userId}/revoke-role")
    @Operation(
            summary = "Revoke an account's elevated role",
            description = "Demotes the account to CIVILIAN and invalidates its existing sessions. Writes a " +
                    "ROLE_REVOKED audit row. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role revoked"),
            @ApiResponse(responseCode = "400", description = "Account has no elevated role, or caller targeted themselves"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Target user not found")
    })
    public ResponseEntity<AppResponse<Void>> revokeRole(@PathVariable UUID userId,
                                                        @Valid @RequestBody AccountActionRequest request) {
        securityAdminService.revokeRole(userId, request.reason());
        return ResponseEntity.ok(AppResponse.of(null, "Role revoked.", 200));
    }

    @PostMapping("/users/{userId}/suspend")
    @Operation(
            summary = "Suspend an account",
            description = "Moves the account to SUSPENDED so it can no longer authenticate, and revokes its " +
                    "current sessions. Writes an ACCOUNT_SUSPENDED audit row. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account suspended"),
            @ApiResponse(responseCode = "400", description = "Account already suspended, or caller targeted themselves"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Target user not found")
    })
    public ResponseEntity<AppResponse<Void>> suspend(@PathVariable UUID userId,
                                                     @Valid @RequestBody AccountActionRequest request) {
        securityAdminService.suspendAccount(userId, request.reason());
        return ResponseEntity.ok(AppResponse.of(null, "Account suspended.", 200));
    }

    @PostMapping("/users/{userId}/reactivate")
    @Operation(
            summary = "Reactivate a suspended account",
            description = "Returns a SUSPENDED account to ACTIVE. Writes an ACCOUNT_REACTIVATED audit row. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account reactivated"),
            @ApiResponse(responseCode = "400", description = "Account is not suspended"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Target user not found")
    })
    public ResponseEntity<AppResponse<Void>> reactivate(@PathVariable UUID userId,
                                                        @Valid @RequestBody AccountActionRequest request) {
        securityAdminService.reactivateAccount(userId, request.reason());
        return ResponseEntity.ok(AppResponse.of(null, "Account reactivated.", 200));
    }

    @PostMapping("/users/{userId}/force-logout")
    @Operation(
            summary = "Revoke all of an account's sessions",
            description = "Bumps the account's credentialsValidFrom watermark so every outstanding JWT is " +
                    "rejected on its next request, without changing role or status. Writes a SESSIONS_REVOKED " +
                    "audit row. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sessions revoked"),
            @ApiResponse(responseCode = "400", description = "Caller targeted themselves"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Target user not found")
    })
    public ResponseEntity<AppResponse<Void>> forceLogout(@PathVariable UUID userId,
                                                         @Valid @RequestBody AccountActionRequest request) {
        securityAdminService.forceLogout(userId, request.reason());
        return ResponseEntity.ok(AppResponse.of(null, "Sessions revoked.", 200));
    }

    @GetMapping("/audit")
    @Operation(
            summary = "Read the access-control audit trail",
            description = "Returns every role grant/revoke, suspension, reactivation and forced logout, newest " +
                    "first. Optionally filtered to a single account with ?userId=. Append-only — there is no " +
                    "endpoint to edit or delete entries. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit trail returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin")
    })
    public ResponseEntity<AppResponse<List<AuditEntryResponse>>> listAudit(
            @RequestParam(name = "userId", required = false) UUID userId) {
        return ResponseEntity.ok(AppResponse.ok(securityAdminService.listAudit(userId)));
    }
}
