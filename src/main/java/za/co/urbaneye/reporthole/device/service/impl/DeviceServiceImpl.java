package za.co.urbaneye.reporthole.device.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.admin.security.service.interfaces.IAuditLogService;
import za.co.urbaneye.reporthole.device.dto.DeviceTokenResponse;
import za.co.urbaneye.reporthole.device.entity.DashcamDevice;
import za.co.urbaneye.reporthole.device.exception.DeviceServiceException;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.device.service.interfaces.IDeviceService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.UUID;

/**
 * Implementation of {@link IDeviceService} that manages dashcam device tokens.
 *
 * <p>Tokens are randomly generated UUIDs stored in the {@code dashcam_device}
 * table. A UUID contains no dots, which lets {@code JwtAuthenticationFilter}
 * distinguish them from JWTs (which always contain exactly two dots) without
 * parsing the value.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements IDeviceService {

    private final DashcamDeviceRepository deviceRepository;
    private final IUserRepository userAuthRepository;
    private final IAuditLogService auditLogService;

    /**
     * Generates a new device token for the currently authenticated user.
     *
     * <p>Reads the user ID from the Spring Security context — the same
     * mechanism used by {@code IncidentServiceImpl}. Each call produces a
     * new token and a new {@code DashcamDevice} row, allowing one user to
     * register multiple physical dashcam devices.</p>
     *
     * @param userId UUID string of the authenticated user, extracted from
     *               the security context by the controller
     * @return response containing the newly created device token
     * @throws DeviceServiceException if the user account cannot be found
     */
    @Override
    public DeviceTokenResponse generateToken(String userId) {
        User user = userAuthRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new DeviceServiceException("User not found: " + userId));

        // UUID.randomUUID() produces a token with no dots, distinguishing it
        // from JWTs in the authentication filter without any extra parsing.
        String token = UUID.randomUUID().toString();

        DashcamDevice device = DashcamDevice.builder()
                .deviceToken(token)
                .user(user)
                .build();

        deviceRepository.save(device);

        auditLogService.record(user, "DEVICE_TOKEN_GENERATED", "DEVICE_TOKEN", device.getDeviceId(),
                "Generated a dashcam device token");

        return new DeviceTokenResponse(token);
    }

    /**
     * Revokes a device token by deleting its {@code DashcamDevice} row.
     *
     * <p>Verifies ownership first — a user may only revoke their own
     * device tokens — before deleting.</p>
     *
     * @param userId  UUID string of the authenticated user making the request
     * @param tokenId the {@code DashcamDevice} row id to revoke
     * @throws DeviceServiceException if the token doesn't exist or belongs to another user
     */
    @Override
    public void revokeToken(String userId, UUID tokenId) {
        DashcamDevice device = deviceRepository.findById(tokenId)
                .orElseThrow(() -> new DeviceServiceException("Device token not found: " + tokenId));

        if (!device.getUser().getUserId().equals(UUID.fromString(userId))) {
            throw new DeviceServiceException("Device token does not belong to the authenticated user");
        }

        User owner = device.getUser();
        deviceRepository.delete(device);

        auditLogService.record(owner, "DEVICE_TOKEN_REVOKED", "DEVICE_TOKEN", tokenId,
                "Revoked a dashcam device token");
    }
}
