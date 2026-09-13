package za.co.urbaneye.reporthole.admin.municipality.exception;

/**
 * Thrown when a municipality or municipality-token operation cannot be fulfilled due to a
 * business-rule violation (caller is not a security admin, record not found, duplicate name,
 * token already revoked).
 *
 * <p>Handled by {@link za.co.urbaneye.reporthole.global.exception.GlobalExceptionHandler},
 * which maps the message text to the appropriate HTTP status.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public class MunicipalityException extends RuntimeException {

    /**
     * @param message descriptive error used by the global handler to pick an HTTP status
     */
    public MunicipalityException(String message) {
        super(message);
    }
}
