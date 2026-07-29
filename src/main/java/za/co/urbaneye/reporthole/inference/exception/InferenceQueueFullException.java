package za.co.urbaneye.reporthole.inference.exception;

/**
 * Thrown when the bounded inference executor queue is at capacity and cannot
 * accept a new frame. The caller should respond with HTTP 503.
 */
public class InferenceQueueFullException extends RuntimeException {

    public InferenceQueueFullException(String message) {
        super(message);
    }
}
