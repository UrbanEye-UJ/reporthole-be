package za.co.urbaneye.reporthole.global.entity;

import java.time.LocalDateTime;

/**
 * Represents a standardized error response object returned by the API
 * when an exception or request failure occurs.
 *
 * <p>This record is typically used in global exception handling to provide
 * consistent error details to clients.</p>
 *
 * <p>Contains:</p>
 * <ul>
 *     <li>Error message describing the issue</li>
 *     <li>HTTP status code</li>
 *     <li>Timestamp of when the error occurred</li>
 * </ul>
 *
 * @param message   descriptive error message
 * @param status    HTTP status code associated with the error
 * @param timestamp date and time when the error occurred
 *
 * @author Refentse
 * @since 1.0
 */
public record ErrorObject(
        String message,
        int status,
        LocalDateTime timestamp
) {}