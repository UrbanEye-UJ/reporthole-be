package za.co.urbaneye.reporthole.device.service.interfaces;

import za.co.urbaneye.reporthole.device.dto.DeviceTokenResponse;

import java.util.UUID;

/**
 * Service interface for dashcam device management.
 *
 * <p>Defines the contract for operations that create and manage
 * long-lived device tokens used to authenticate dashcam devices
 * without a full user login flow.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public interface IDeviceService {

    /**
     * Generates a new long-lived device token linked to the given user.
     *
     * <p>Each call creates a new {@code DashcamDevice} row so that a
     * single user can register multiple physical devices. Tokens do not
     * expire; revocation requires deleting the row from the database.</p>
     *
     * @param userId the UUID string of the authenticated user
     * @return a response containing the newly generated token
     */
    DeviceTokenResponse generateToken(String userId);

    /**
     * Revokes (permanently deletes) a device token, so a lost or leaked
     * dashcam device can no longer authenticate.
     *
     * @param userId  the UUID string of the authenticated user making the request
     * @param tokenId the id of the {@code DashcamDevice} row to revoke
     * @throws za.co.urbaneye.reporthole.device.exception.DeviceServiceException
     *         if the token doesn't exist or doesn't belong to {@code userId}
     */
    void revokeToken(String userId, UUID tokenId);
}
