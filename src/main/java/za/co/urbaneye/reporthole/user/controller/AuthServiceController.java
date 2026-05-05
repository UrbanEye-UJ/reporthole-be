package za.co.urbaneye.reporthole.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.urbaneye.reporthole.global.entity.AppResponse;
import za.co.urbaneye.reporthole.user.dto.AuthResponse;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.service.interfaces.IUserAuthService;

/**
 * REST controller responsible for handling user authentication
 * and registration operations.
 *
 * <p>This controller exposes endpoints for:</p>
 * <ul>
 *     <li>User registration</li>
 *     <li>User login and token generation</li>
 * </ul>
 *
 * <p>Base URL: <b>/auth</b></p>
 *
 * @author Refentse
 * @since 1.0
 */
@RestController
@RequestMapping("auth")
@Slf4j
@Tag(
        name = "Authentication",
        description = "Endpoints for user registration and authentication."
)
public class AuthServiceController {

    /**
     * Service used to process authentication and user management logic.
     */
    private IUserAuthService service;

    /**
     * Constructs the authentication controller.
     *
     * @param service implementation of authentication service
     */
    @Autowired
    public AuthServiceController(IUserAuthService service) {
        this.service = service;
    }

    /**
     * Registers a new user account.
     *
     * @param request registration details
     * @return HTTP 201 Created when successful
     */
    @PostMapping("/register")
    @Operation(
            summary = "Register user",
            description = "Creates a new user account and stores the user in the database."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or user already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AppResponse<Void>> save(@RequestBody RegisterRequest request) {
        service.registerUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AppResponse.created(null));
    }

    /**
     * Authenticates a user.
     *
     * @param request login credentials
     * @return JWT token when login is successful
     */
    @PostMapping("/login")
    @Operation(
            summary = "Login user",
            description = "Authenticates the user using email and password and returns a JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Incorrect password"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AppResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(AppResponse.ok(service.loginUser(request)));
    }
}