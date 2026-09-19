package za.co.urbaneye.reporthole.inference.service.interfaces;

import ai.onnxruntime.OrtException;
import za.co.urbaneye.reporthole.inference.entity.InferenceResult;

import java.io.IOException;
import java.util.List;

/**
 * Contract for running road-damage inference on a single image.
 *
 * <p>Callers that only need one authoritative result for routing decisions
 * (e.g. {@code AsyncFrameSubmissionService}) should use {@code .get(0)} on the
 * returned list — by contract, the first element is always the primary result.
 * Implementations that chain multiple models (see {@code ChainedInferenceService})
 * may append supplementary results afterward.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public interface IInferenceService {

    /**
     * Runs inference on the supplied image bytes.
     *
     * @param imageBytes raw bytes of a JPEG or PNG image
     * @return a non-empty list of {@link InferenceResult}; the first element is
     *         always the primary/authoritative result, any further elements are
     *         supplementary (e.g. a secondary model's detection)
     * @throws IOException  if the image bytes cannot be decoded
     * @throws OrtException if the ONNX Runtime inference call fails
     */
    List<InferenceResult> predict(byte[] imageBytes) throws IOException, OrtException;
}
