package za.co.urbaneye.reporthole.admin.contractor.exception;

/**
 * Thrown when a contractor-management request cannot be fulfilled due to a
 * business rule violation (duplicate email, caller is not an admin, not found).
 *
 * <p>Handled by {@link za.co.urbaneye.reporthole.global.exception.GlobalExceptionHandler}
 * which maps the message text to the appropriate HTTP status.</p>
 */
public class ContractorException extends RuntimeException {

    public ContractorException(String message) {
        super(message);
    }
}
