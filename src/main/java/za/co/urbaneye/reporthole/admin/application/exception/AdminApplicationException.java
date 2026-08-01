package za.co.urbaneye.reporthole.admin.application.exception;

/**
 * Thrown when an admin application request cannot be fulfilled due to a
 * business rule violation (duplicate submission, wrong role, user not found).
 *
 * <p>Handled by {@link za.co.urbaneye.reporthole.global.exception.GlobalExceptionHandler}
 * which maps the message text to the appropriate HTTP status.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public class AdminApplicationException extends RuntimeException {

    /**
     * Creates an exception with the given message.
     *
     * @param message descriptive error message used by the global handler
     *                to determine the HTTP status code
     */
    public AdminApplicationException(String message) {
        super(message);
    }
}
