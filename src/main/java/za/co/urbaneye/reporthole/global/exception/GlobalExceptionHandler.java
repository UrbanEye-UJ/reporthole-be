package za.co.urbaneye.reporthole.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import za.co.urbaneye.reporthole.global.entity.ErrorObject;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;

import java.time.LocalDateTime;

/**
 * Global exception handler for the application.
 *
 * <p>This class centralizes exception handling across all REST controllers
 * using {@code @RestControllerAdvice}. It ensures that API clients receive
 * consistent and structured error responses.</p>
 *
 * <p>Handled exceptions include:</p>
 * <ul>
 *     <li>{@link MethodArgumentNotValidException} for {@code @Valid} constraint violations</li>
 *     <li>{@link UserServiceException} for user-related business errors</li>
 *     <li>Generic {@link Exception} for unexpected system failures</li>
 * </ul>
 *
 * <p>All responses are returned using the {@link ErrorObject} structure.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@code @Valid} constraint violations on request bodies.
     * Returns the first failing field's message so the client can surface it directly.
     *
     * @param ex the validation exception thrown by Spring MVC
     * @return 400 Bad Request with the first constraint violation message
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorObject> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("Invalid request");

        ErrorObject response = new ErrorObject(message, HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles user service related exceptions.
     *
     * <p>Returns:</p>
     * <ul>
     *     <li>400 Bad Request for general validation/business errors</li>
     *     <li>404 Not Found when the message contains "not found"</li>
     * </ul>
     *
     * @param ex the thrown {@link UserServiceException}
     * @return structured error response with relevant HTTP status
     */
    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<ErrorObject> handleUserServiceException(UserServiceException ex) {
        final String msg = ex.getMessage() != null ? ex.getMessage() : "";

        HttpStatus status;
        if (msg.contains("not found")) {
            status = HttpStatus.NOT_FOUND;                    // 404
        } else if (msg.contains("locked") || msg.contains("Too many")) {
            status = HttpStatus.LOCKED;                       // 423
        } else if (msg.contains("not verified")) {
            status = HttpStatus.FORBIDDEN;                    // 403
        } else if (msg.contains("Invalid or expired reset link")) {
            status = HttpStatus.GONE;                         // 410
        } else if (msg.contains("Invalid or already-used verification link")) {
            status = HttpStatus.GONE;                         // 410
        } else if (msg.contains("Verification link has expired")) {
            status = HttpStatus.GONE;                         // 410
        } else {
            status = HttpStatus.BAD_REQUEST;                  // 400
        }

        ErrorObject response = new ErrorObject(ex.getMessage(), status.value(), LocalDateTime.now());
        return new ResponseEntity<>(response, status);
    }

    /**
     * Handles all unanticipated exceptions not explicitly mapped.
     *
     * <p>The full exception stack trace is logged for troubleshooting,
     * while a generic internal server error message is returned
     * to the client.</p>
     *
     * @param ex the unexpected exception
     * @return HTTP 500 Internal Server Error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorObject> handleGenericException(Exception ex) {
        log.error(ex.getMessage(), ex);

        ErrorObject response = new ErrorObject(
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}