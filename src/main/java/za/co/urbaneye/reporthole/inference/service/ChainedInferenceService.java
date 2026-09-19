package za.co.urbaneye.reporthole.inference.service;

import ai.onnxruntime.OrtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.inference.entity.InferenceResult;
import za.co.urbaneye.reporthole.inference.service.interfaces.IInferenceService;

import java.io.IOException;
import java.util.List;

/**
 * Composes {@link OnnxInferenceService} (the custom, road-damage-trained model)
 * and {@link StockInferenceService} (a generic COCO-pretrained model) into a
 * single {@link IInferenceService}.
 *
 * <p>Both models always run on every call — there is no gating/fallback between
 * them. The stock model was trained on far more data than the custom model can
 * realistically match, but its 80 COCO classes have no road-damage equivalent
 * (no "pothole", "crack", etc.), so it cannot itself decide whether an image
 * contains road damage. Its detections are shown as supplementary context
 * alongside the custom model's, never used to skip or gate the custom model.</p>
 *
 * <p>The custom model's result is always {@code predict()}'s first (primary)
 * list element — it remains authoritative for {@code IssueType}/auto-creation
 * routing, per the {@link IInferenceService} contract. The stock model's result
 * is appended as a second, supplementary element.</p>
 *
 * <p>{@code @Primary} so that {@code IInferenceService} injection (in
 * {@code InferenceController} and {@code AsyncFrameSubmissionService}) resolves
 * to this bean unambiguously, since {@link OnnxInferenceService} and
 * {@link StockInferenceService} both also implement the interface.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class ChainedInferenceService implements IInferenceService {

    private final OnnxInferenceService customInferenceService;
    private final StockInferenceService stockInferenceService;

    /**
     * Runs both the custom and stock models on the supplied image bytes.
     *
     * @param imageBytes raw bytes of a JPEG or PNG image
     * @return two-element list: {@code [0]} the custom model's result (primary,
     *         authoritative), {@code [1]} the stock model's result (supplementary)
     * @throws IOException  if the image bytes cannot be decoded
     * @throws OrtException if either ONNX Runtime inference call fails
     */
    @Override
    public List<InferenceResult> predict(byte[] imageBytes) throws IOException, OrtException {
        InferenceResult custom = customInferenceService.predict(imageBytes).get(0);
        InferenceResult stock = stockInferenceService.predict(imageBytes).get(0);

        log.debug("Chained inference — custom: detected={} label={} confidence={}; stock: detected={} label={} confidence={}",
                custom.detected(), custom.label(), custom.confidence(),
                stock.detected(), stock.label(), stock.confidence());

        return List.of(custom, stock);
    }
}
