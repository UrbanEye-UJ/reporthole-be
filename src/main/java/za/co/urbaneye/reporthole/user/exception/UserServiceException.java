package za.co.urbaneye.reporthole.user.exception;

/**
 * Custom runtime exception used for user-related
 * business logic and service layer errors.
 *
 * <p>This exception is typically thrown when user
 * operations cannot be completed successfully, such as:</p>
 *
 * <ul>
 *     <li>User not found</li>
 *     <li>Duplicate registration attempts</li>
 *     <li>Invalid login credentials</li>
 *     <li>Validation or processing failures</li>
 * </ul>
 *
 * <p>Exceptions of this type are commonly handled by the
 * global exception handler to return structured API
 * error responses.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public class UserServiceException extends RuntimeException {

    /**
     * Creates a new user service exception
     * with the provided message.
     *
     * @param message descriptive error message
     */
    public UserServiceException(String message) {
        super(message);
    }
}