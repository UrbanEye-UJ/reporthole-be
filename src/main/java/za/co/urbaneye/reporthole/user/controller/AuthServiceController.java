package za.co.urbaneye.reporthole.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import za.co.urbaneye.reporthole.global.entity.AppResponse;
import za.co.urbaneye.reporthole.user.dto.AuthResponse;
import za.co.urbaneye.reporthole.user.dto.ForgotPasswordRequest;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.dto.ResetPasswordRequest;
import za.co.urbaneye.reporthole.user.service.interfaces.ILoginService;
import za.co.urbaneye.reporthole.user.service.interfaces.IPasswordResetService;
import za.co.urbaneye.reporthole.user.service.interfaces.IRegistrationService;

/**
 * REST controller for user authentication, registration, and account recovery.
 *
 * <p>Base URL: <b>/auth</b> — all endpoints are publicly accessible.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@RestController
@RequestMapping("auth")
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, and account recovery.")
@RequiredArgsConstructor
public class AuthServiceController {

    private final ILoginService loginService;
    private final IRegistrationService registrationService;
    private final IPasswordResetService passwordResetService;

    /**
     * Registers a new user account.
     *
     * @param request registration details
     * @return 201 Created on success
     */
    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Creates a new user account.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or email already registered"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AppResponse<Void>> save(@Valid @RequestBody RegisterRequest request) {
        registrationService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AppResponse.created(null));
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request login credentials
     * @return 200 OK with JWT token and role
     */
    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates the user and returns a JWT token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Wrong password"),
            @ApiResponse(responseCode = "403", description = "Email not verified"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "423", description = "Account locked"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AppResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(AppResponse.ok(loginService.loginUser(request)));
    }

    /**
     * Verifies a user's email address using the token from the verification email.
     *
     * @param token the verification token from the email link
     * @return 200 OK on success
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify email", description = "Activates a user account using the email verification token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified — account is now active"),
            @ApiResponse(responseCode = "400", description = "Invalid or already-used verification token")
    })
    public ResponseEntity<AppResponse<Void>> verifyEmail(@RequestParam String token) {
        registrationService.verifyEmail(token);
        return ResponseEntity.ok(AppResponse.ok(null));
    }

    /**
     * Sends a password reset link to the given email address.
     * Clicking the link will also unlock a locked account.
     *
     * @param request body containing the email address
     * @return 202 Accepted — response is the same whether or not the email exists
     */
    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset",
            description = "Sends a one-time reset link to the user's email. Also works to unlock a locked account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Reset link sent"),
            @ApiResponse(responseCode = "404", description = "No account found for that email"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AppResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.accepted().body(AppResponse.of(null, "Reset link sent", HttpStatus.ACCEPTED.value()));
    }

    /**
     * Resets the password using a previously issued token and unlocks the account.
     *
     * @param request body containing the token and new password
     * @return 200 OK on success
     */
    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password",
            description = "Sets a new password using the token from the reset email. Unlocks the account if it was locked."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "410", description = "Reset link has expired or already been used"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AppResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok(AppResponse.ok(null));
    }
}
