package za.co.urbaneye.reporthole.incident.exception;

/**
 * Thrown when an incident cannot be assigned to a contractor
 * (incident/contractor not found, contractor isn't a CONTRACTOR, caller isn't an admin).
 *
 * <p>Handled by {@link za.co.urbaneye.reporthole.global.exception.GlobalExceptionHandler}
 * which maps the message text to the appropriate HTTP status.</p>
 */
public class AssignmentException extends RuntimeException {

    public AssignmentException(String message) {
        super(message);
    }
}
