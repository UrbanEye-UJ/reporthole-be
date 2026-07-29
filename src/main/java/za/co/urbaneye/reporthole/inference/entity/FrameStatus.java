package za.co.urbaneye.reporthole.inference.entity;

/**
 * Lifecycle states for an in-memory dashcam frame job.
 *
 * <p>Transitions: {@code PENDING} → {@code PROCESSING} → {@code DONE} or {@code FAILED}.</p>
 */
public enum FrameStatus {
    /** Frame accepted; waiting for an inference worker thread. */
    PENDING,
    /** Inference is actively running on a background thread. */
    PROCESSING,
    /** Inference completed and routing action (AUTO_LOG / ESCALATE / DISCARD) was applied. */
    DONE,
    /** Inference or post-processing threw an exception; see {@code FrameJob#errorMessage}. */
    FAILED
}
