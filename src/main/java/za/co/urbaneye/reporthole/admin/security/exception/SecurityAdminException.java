package za.co.urbaneye.reporthole.admin.security.exception;

/**
 * Thrown when a security-admin identity action cannot be fulfilled due to a business-rule
 * violation (caller is not a security admin, target account missing, self-targeting, or an
 * account already in the requested state).
 *
 * <p>Handled by {@link za.co.urbaneye.reporthole.global.exception.GlobalExceptionHandler},
 * which maps the message text to the appropriate HTTP status.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public class SecurityAdminException extends RuntimeException {

    /**
     * Creates an exception with the given message.
     *
     * @param message descriptive error message used by the global handler to pick an HTTP status
     */
    public SecurityAdminException(String message) {
        super(message);
    }
}
