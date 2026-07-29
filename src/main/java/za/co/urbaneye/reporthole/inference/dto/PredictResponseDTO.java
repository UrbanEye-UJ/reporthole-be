package za.co.urbaneye.reporthole.inference.dto;

/**
 * Response body returned by {@code POST /inference/predict}.
 *
 * <p>The shape intentionally mirrors the FastAPI {@code /predict} response so the
 * Next.js proxy at {@code /api/ml/predict} can switch backends by changing only the
 * {@code ML_SERVICE_URL} environment variable, with no frontend code change.</p>
 *
 * <p>When {@code detected} is {@code false} all fields inside {@code detection} are
 * {@code null}. Callers must check {@code detected} before accessing {@code detection}
 * fields.</p>
 *
 * <p>This class is included in the OpenAPI specification so that running
 * {@code npx orval} in {@code reporthole-fe/} generates a matching TypeScript type.
 * The frontend calls the Next.js proxy ({@code /api/ml/predict}) rather than the
 * Spring Boot endpoint directly; use the generated TypeScript type for type-safety
 * but keep the raw {@code fetch} to the proxy.</p>
 *
 * @param detected  {@code true} if at least one damage class scored above zero
 * @param detection nested detection payload; all fields {@code null} when not detected
 *
 * @author Refentse
 * @since 1.0
 */
public record PredictResponseDTO(
        boolean detected,
        DetectionDTO detection
) {

    /**
     * Convenience factory for a no-detection result.
     *
     * @return response with {@code detected=false} and all detection fields {@code null}
     */
    public static PredictResponseDTO empty() {
        return new PredictResponseDTO(false, new DetectionDTO(null, null, null));
    }
}
