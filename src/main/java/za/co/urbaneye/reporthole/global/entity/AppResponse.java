package za.co.urbaneye.reporthole.global.entity;

import java.time.LocalDateTime;

/**
 * Generic wrapper for all successful API responses.
 *
 * @param data      response payload
 * @param message   human-readable status message
 * @param status    HTTP status code
 * @param timestamp time the response was generated
 *
 * @author Refentse
 * @since 1.0
 */
public record AppResponse<T>(
        T data,
        String message,
        int status,
        LocalDateTime timestamp
) {

    public static <T> AppResponse<T> of(T data, String message, int status) {
        return new AppResponse<>(data, message, status, LocalDateTime.now());
    }

    public static <T> AppResponse<T> ok(T data) {
        return of(data, "Success", 200);
    }

    public static <T> AppResponse<T> created(T data) {
        return of(data, "Created", 201);
    }
}
