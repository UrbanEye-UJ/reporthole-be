package za.co.urbaneye.reporthole.training.exception;

/**
 * Thrown for annotation / training-flag / YOLO-export rule violations (incident or annotation
 * not found, caller isn't an admin, invalid box, nothing to flag).
 *
 * <p>Handled by {@link za.co.urbaneye.reporthole.global.exception.GlobalExceptionHandler}
 * which maps the message text to the appropriate HTTP status.</p>
 */
public class TrainingException extends RuntimeException {

    public TrainingException(String message) {
        super(message);
    }
}
