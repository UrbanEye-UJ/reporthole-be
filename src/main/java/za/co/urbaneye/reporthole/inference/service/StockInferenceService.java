package za.co.urbaneye.reporthole.inference.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.inference.config.InferenceProperties;
import za.co.urbaneye.reporthole.inference.entity.InferenceResult;
import za.co.urbaneye.reporthole.inference.entity.InferenceSource;
import za.co.urbaneye.reporthole.inference.service.interfaces.IInferenceService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Spring-managed service that loads a stock, COCO-pretrained YOLOv8n ONNX model
 * ({@code yolov8n-coco.onnx}, exported from the standard ultralytics
 * {@code yolov8n.pt} checkpoint) and exposes generic object detection.
 *
 * <p>Unlike {@link OnnxInferenceService}, this model was never trained on
 * road-damage imagery — it has no concept of potholes, cracks, or blocked
 * drains. It only recognises the 80 standard COCO classes (car, person, stop
 * sign, etc.). Its detections are supplementary context, never authoritative
 * for {@code IssueType}/routing — see {@code ChainedInferenceService}.</p>
 *
 * <p>Session lifecycle mirrors {@link OnnxInferenceService}: its own
 * {@link OrtSession}/{@link OrtEnvironment}, loaded in {@link #init()} and
 * closed in {@link #close()}.</p>
 *
 * <p><b>Model I/O:</b> input {@code images} shape {@code [1, 3, 640, 640]}
 * float32 NCHW normalised to [0, 1] (same convention as the custom model,
 * reuses {@link ImagePreprocessor}); output {@code output0} shape
 * {@code [1, 84, 8400]} — 4 bbox values (ignored, confidence-only, no NMS)
 * + 80 raw COCO class scores per anchor.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockInferenceService implements IInferenceService {

    private static final int NUM_CLASSES = 80;
    private static final int BBOX_OFFSET = 4;
    private static final int NUM_ANCHORS = 8400;

    /** COCO class names in the exact index order produced by yolov8n.pt / yolov8n-coco.onnx. */
    private static final String[] CLASS_NAMES = {
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck",
            "boat", "traffic light", "fire hydrant", "stop sign", "parking meter", "bench",
            "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra",
            "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
            "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove",
            "skateboard", "surfboard", "tennis racket", "bottle", "wine glass", "cup",
            "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
            "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse",
            "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
            "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
            "toothbrush"
    };

    private final InferenceProperties properties;

    private OrtEnvironment env;
    private OrtSession session;

    /**
     * Loads the stock ONNX model and creates the inference session.
     * Called automatically by Spring after dependency injection.
     *
     * @throws IOException  if the model file cannot be read
     * @throws OrtException if the ONNX Runtime session cannot be created
     */
    @PostConstruct
    public void init() throws IOException, OrtException {
        String modelPath = properties.getStockModelPath();
        byte[] modelBytes;

        if (modelPath.startsWith("classpath:")) {
            String resource = modelPath.substring("classpath:".length());
            try (InputStream is = new ClassPathResource(resource).getInputStream()) {
                modelBytes = is.readAllBytes();
            }
        } else {
            modelBytes = Files.readAllBytes(Paths.get(modelPath));
        }

        env = OrtEnvironment.getEnvironment();
        session = env.createSession(modelBytes, new OrtSession.SessionOptions());

        log.info("Stock ONNX inference session loaded from '{}' — input: {}, output: {}",
                modelPath,
                session.getInputNames(),
                session.getOutputNames());
    }

    /**
     * Runs confidence-only inference on the supplied image bytes against the
     * stock COCO model.
     *
     * @param imageBytes raw bytes of a JPEG or PNG image
     * @return single-element list — {@link InferenceResult#empty(InferenceSource)}
     *         tagged {@link InferenceSource#STOCK} if nothing scored above zero
     * @throws IOException  if the image bytes cannot be decoded
     * @throws OrtException if the ONNX Runtime inference call fails
     */
    @Override
    public List<InferenceResult> predict(byte[] imageBytes) throws IOException, OrtException {
        log.debug("Running stock inference on {} byte image", imageBytes.length);
        long start = System.currentTimeMillis();

        try (OnnxTensor input = ImagePreprocessor.preprocess(imageBytes, env).tensor();
             OrtSession.Result results = session.run(Map.of("images", input))) {

            OnnxTensor output = (OnnxTensor) results.get("output0").orElseThrow(
                    () -> new OrtException("Stock model did not produce 'output0'"));

            InferenceResult result = extractBestDetection(output.getFloatBuffer());
            long elapsed = System.currentTimeMillis() - start;

            if (result.detected()) {
                log.info("Stock inference complete in {}ms — detected: {} (confidence: {})",
                        elapsed, result.rawLabel(), result.confidence());
            } else {
                log.info("Stock inference complete in {}ms — nothing detected", elapsed);
            }

            return List.of(result);
        }
    }

    /**
     * Extracts the highest-confidence detection from the raw model output buffer.
     *
     * <p>Buffer layout for shape {@code [1, 84, 8400]} in row-major order:
     * {@code buffer[channel * 8400 + anchor]}. Channels 0–3 are bbox coordinates
     * (skipped); channels 4–83 are raw class scores for the 80 COCO classes.</p>
     *
     * <p>Unlike {@link OnnxInferenceService}, there is no {@code IssueType} label
     * mapping and no non-damage exclusion set — every COCO class is a candidate,
     * and {@code label()} on the returned result is simply the raw COCO class
     * name uppercased (there is no road-damage {@code IssueType} equivalent).</p>
     *
     * @param buffer flattened float buffer of the {@code output0} tensor
     * @return best detection, or {@link InferenceResult#empty(InferenceSource)} if nothing found
     */
    public InferenceResult extractBestDetection(FloatBuffer buffer) {
        float bestConf = 0.0f;
        String bestRawLabel = null;

        for (int anchor = 0; anchor < NUM_ANCHORS; anchor++) {
            float anchorBest = 0.0f;
            int anchorBestClass = -1;

            for (int cls = 0; cls < NUM_CLASSES; cls++) {
                int idx = (BBOX_OFFSET + cls) * NUM_ANCHORS + anchor;
                float score = buffer.get(idx);
                if (score > anchorBest) {
                    anchorBest = score;
                    anchorBestClass = cls;
                }
            }

            if (anchorBestClass < 0) continue;

            if (anchorBest > bestConf) {
                bestConf = anchorBest;
                bestRawLabel = CLASS_NAMES[anchorBestClass];
            }
        }

        if (bestRawLabel == null) {
            return InferenceResult.empty(InferenceSource.STOCK);
        }

        double confidence = Math.round(bestConf * 10_000.0) / 10_000.0;
        String label = bestRawLabel.toUpperCase().replace(" ", "_");

        return new InferenceResult(true, label, bestRawLabel, confidence, InferenceSource.STOCK, null, null, null, null);
    }

    /**
     * Closes the ONNX Runtime session and environment.
     * Called automatically by Spring before the application context is destroyed.
     */
    @PreDestroy
    public void close() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
            log.info("Stock ONNX inference session closed");
        } catch (OrtException e) {
            log.warn("Error closing stock ONNX session: {}", e.getMessage());
        }
    }
}
