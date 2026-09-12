package za.co.urbaneye.reporthole.inference.dto;

import za.co.urbaneye.reporthole.inference.entity.FrameJob;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Projection of a {@link FrameJob} whose routing decision is {@code ESCALATE}.
 * Carries enough information for an admin operator to judge whether the detection
 * is credible and approve or discard it.
 *
 * <p>{@code imageBase64} is included so the UI can render a preview without a
 * separate image-serving endpoint; it is safe for a capstone demo but should be
 * replaced with a presigned URL in production.</p>
 *
 * @param frameId      unique frame identifier — used to target approve/discard calls
 * @param label        inferred issue-type label (e.g. {@code "POTHOLE"})
 * @param confidence   detection confidence in [0.0, 1.0]
 * @param createdAt    wall-clock time the frame was accepted
 * @param imageBase64  Base64-encoded JPEG bytes for in-browser preview
 */
public record EscalatedFrameDTO(
        UUID frameId,
        String label,
        double confidence,
        LocalDateTime createdAt,
        String imageBase64
) {
    /** Converts a {@link FrameJob} to its DTO representation. */
    public static EscalatedFrameDTO from(FrameJob job) {
        String b64 = job.getImageBytes() != null
                ? Base64.getEncoder().encodeToString(job.getImageBytes())
                : null;
        return new EscalatedFrameDTO(
                job.getFrameId(),
                job.getLabel(),
                job.getConfidence(),
                job.getCreatedAt(),
                b64
        );
    }
}
