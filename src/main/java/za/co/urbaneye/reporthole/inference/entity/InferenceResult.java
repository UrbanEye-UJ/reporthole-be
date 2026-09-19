package za.co.urbaneye.reporthole.inference.entity;

import za.co.urbaneye.reporthole.inference.service.OnnxInferenceService;

/**
 * Internal result produced by {@link OnnxInferenceService} for a single image.
 *
 * <p>This is a service-layer model, not a DTO. It is mapped to
 * {@link za.co.urbaneye.reporthole.inference.dto.PredictResponseDTO} by the
 * controller before being serialised to JSON.</p>
 *
 * <p>When {@code detected} is {@code false}, all other fields are {@code null} or
 * zero — the caller must check {@code detected} before using the detection fields.</p>
 *
 * @param detected   {@code true} if at least one road-damage class was detected above zero confidence
 * @param label      IssueType-compatible label (e.g. {@code "POTHOLE"}, {@code "CRACK"});
 *                   {@code null} when {@code detected} is {@code false}
 * @param rawLabel   raw class name from the model (e.g. {@code "Pothole_FP"});
 *                   {@code null} when {@code detected} is {@code false}
 * @param confidence best detection confidence in the range [0.0, 1.0], rounded to 4 decimal places;
 *                   {@code 0.0} when {@code detected} is {@code false}
 * @param source     which model produced this result
 *
 * @author Refentse
 * @since 1.0
 */
public record InferenceResult(
        boolean detected,
        String label,
        String rawLabel,
        double confidence,
        InferenceSource source
) {

    /**
     * Convenience factory for a no-detection {@link InferenceSource#CUSTOM} result.
     *
     * @return {@link InferenceResult} with {@code detected=false} and all other fields null/zero
     */
    public static InferenceResult empty() {
        return empty(InferenceSource.CUSTOM);
    }

    /**
     * Convenience factory for a no-detection result from a specific model.
     *
     * @param source which model found nothing
     * @return {@link InferenceResult} with {@code detected=false} and all other fields null/zero
     */
    public static InferenceResult empty(InferenceSource source) {
        return new InferenceResult(false, null, null, 0.0, source);
    }
}
