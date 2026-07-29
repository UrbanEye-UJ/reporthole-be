package za.co.urbaneye.reporthole.inference.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * In-memory representation of a dashcam frame submitted for async inference.
 *
 * <p>Not a JPA entity — stored in a {@code ConcurrentHashMap} inside
 * {@link za.co.urbaneye.reporthole.inference.service.impl.AsyncFrameSubmissionService}.
 * If persistence is required in future, add a {@code pending_frame} table and
 * convert this to a {@code @Entity} with a {@code BYTEA imageBytes} column.</p>
 *
 * <p>{@code status}, {@code label}, {@code confidence}, {@code routingDecision},
 * and {@code errorMessage} are mutable — updated by the background inference
 * thread as it progresses through {@link FrameStatus} states.</p>
 */
@Getter
@Setter
@Builder
public class FrameJob {

    /** Unique identifier assigned at submission time. */
    private final UUID frameId;

    /** The authenticated user (or device owner) who submitted this frame. */
    private final UUID userId;

    /** Current lifecycle state. Starts as {@link FrameStatus#PENDING}. */
    private volatile FrameStatus status;

    /** Raw image bytes — held in memory until inference completes. */
    private final byte[] imageBytes;

    /** IssueType-compatible label (e.g. {@code "POTHOLE"}) — set on {@link FrameStatus#DONE}. */
    private String label;

    /** Detection confidence in [0.0, 1.0] — set on {@link FrameStatus#DONE}. */
    private double confidence;

    /** Routing tier applied after inference — set on {@link FrameStatus#DONE}. */
    private String routingDecision;

    /** Human-readable error message — set on {@link FrameStatus#FAILED}. */
    private String errorMessage;

    /** Wall-clock time when the frame was accepted by the API. */
    private final LocalDateTime createdAt;
}
