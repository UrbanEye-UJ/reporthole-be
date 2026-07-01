package za.co.urbaneye.reporthole.device.exception;

/**
 * Custom runtime exception for device management errors.
 *
 * <p>Thrown by the device service layer when an operation cannot
 * complete — for example when the requesting user account cannot
 * be found during token generation.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public class DeviceServiceException extends RuntimeException {

    /**
     * Creates a new device service exception with the given message.
     *
     * @param message descriptive error message
     */
    public DeviceServiceException(String message) {
        super(message);
    }
}
