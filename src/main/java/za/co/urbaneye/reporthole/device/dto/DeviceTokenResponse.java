package za.co.urbaneye.reporthole.device.dto;

/**
 * Response payload returned by the device token generation endpoint.
 *
 * <p>The token is a long-lived, randomly generated UUID string that
 * the dashcam device stores locally and sends in the
 * {@code Authorization: Bearer} header on every subsequent request.</p>
 *
 * @param deviceToken the generated device token
 * @author Refentse
 * @since 1.0
 */
public record DeviceTokenResponse(String deviceToken) {}
