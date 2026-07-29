package za.co.urbaneye.reporthole.device.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.urbaneye.reporthole.device.dto.DeviceTokenResponse;
import za.co.urbaneye.reporthole.device.service.interfaces.IDeviceService;
import za.co.urbaneye.reporthole.global.entity.AppResponse;

/**
 * REST controller for dashcam device management.
 *
 * <p>Exposes an endpoint for authenticated users to generate a long-lived
 * device token that a dashcam device can use instead of a full JWT login.
 * The token is entered once on the device and stored locally.</p>
 *
 * <p>Base URL: <b>/devices</b></p>
 *
 * @author Refentse
 * @since 1.0
 */
@RestController
@RequestMapping("devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "Endpoints for dashcam device registration and token management.")
public class DeviceController {

    private final IDeviceService deviceService;

    /**
     * Generates a new long-lived device token for the authenticated user.
     *
     * <p>Requires a valid user JWT in the {@code Authorization} header.
     * Device tokens (which carry no dots) are not accepted here — a dashcam
     * cannot mint its own tokens.</p>
     *
     * <p>Each call creates a new token, allowing one account to register
     * multiple physical dashcam devices.</p>
     *
     * @return the newly generated device token wrapped in {@link AppResponse}
     */
    @PostMapping("/token/generate")
    @Operation(
            summary = "Generate device token",
            description = "Generates a long-lived device token linked to the authenticated user. "
                    + "Enter this token once on the dashcam device — it replaces the normal login flow."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token generated successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated or device token used instead of JWT"),
            @ApiResponse(responseCode = "404", description = "Authenticated user account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AppResponse<DeviceTokenResponse>> generateToken() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DeviceTokenResponse response = deviceService.generateToken(userId);
        return ResponseEntity.ok(AppResponse.ok(response));
    }
}
