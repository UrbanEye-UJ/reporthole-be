package za.co.urbaneye.reporthole.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.urbaneye.reporthole.global.entity.AppResponse;
import za.co.urbaneye.reporthole.user.dto.UpdateProfileRequest;
import za.co.urbaneye.reporthole.user.dto.UpdateSpecialisationsRequest;
import za.co.urbaneye.reporthole.user.dto.UserProfileResponse;
import za.co.urbaneye.reporthole.user.dto.VerifyPasswordRequest;
import za.co.urbaneye.reporthole.user.service.interfaces.IUserProfileService;

/**
 * Endpoints for the authenticated user to view, update, and delete their own profile.
 */
@RestController
@RequestMapping("users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Profile management endpoints for the authenticated user.")
public class UserController {

    private final IUserProfileService userProfileService;

    @GetMapping("/profile")
    @Operation(summary = "Get profile", description = "Returns the authenticated user's profile details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ResponseEntity<AppResponse<UserProfileResponse>> getProfile() {
        return ResponseEntity.ok(AppResponse.ok(userProfileService.getProfile()));
    }

    @PatchMapping("/profile")
    @Operation(summary = "Update profile", description = "Updates the authenticated user's first name, last name, and phone number.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ResponseEntity<AppResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(AppResponse.ok(userProfileService.updateProfile(request)));
    }

    @PostMapping("/verify-password")
    @Operation(
            summary = "Verify password",
            description = "Confirms the current user's password without changing session state. "
                    + "Used by the UI to gate access to sensitive profile fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password correct"),
            @ApiResponse(responseCode = "400", description = "Validation error — password field missing"),
            @ApiResponse(responseCode = "401", description = "Password incorrect or unauthenticated")
    })
    public ResponseEntity<AppResponse<Void>> verifyPassword(@Valid @RequestBody VerifyPasswordRequest request) {
        userProfileService.verifyPassword(request.password());
        return ResponseEntity.ok(AppResponse.ok(null));
    }

    @PatchMapping("/profile/specialisations")
    @Operation(
            summary = "Update specialisations",
            description = "Replaces the authenticated contractor's specialisation set. " +
                    "Use IssueType.OTHER to mark a contractor as able to handle any incident type. " +
                    "Returns 403 if the caller is not a CONTRACTOR."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Specialisations updated"),
            @ApiResponse(responseCode = "400", description = "Validation error — empty set"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Caller is not a contractor")
    })
    public ResponseEntity<AppResponse<UserProfileResponse>> updateSpecialisations(
            @Valid @RequestBody UpdateSpecialisationsRequest request) {
        return ResponseEntity.ok(AppResponse.ok(
                userProfileService.updateSpecialisations(request.specialisations())));
    }

    @DeleteMapping("/profile")
    @Operation(summary = "Delete account", description = "Soft-deletes the authenticated user's account. The account is marked as DELETED and login is blocked.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Account deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ResponseEntity<Void> deleteAccount() {
        userProfileService.deleteAccount();
        return ResponseEntity.noContent().build();
    }
}
