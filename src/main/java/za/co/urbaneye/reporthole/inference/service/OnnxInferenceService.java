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

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * Spring-managed service that loads the Reporthole YOLOv8 ONNX model once at
 * startup and exposes a {@link #predict(byte[])} method for confidence-only inference.
 *
 * <p><b>Session lifecycle:</b> The {@link OrtSession} is created in
 * {@link #init()} (annotated {@link PostConstruct}) and closed in
 * {@link #close()} (annotated {@link PreDestroy}). Both the session and the
 * {@link OrtEnvironment} are held as fields and shared across all calls.
 * {@code OrtSession} is thread-safe for concurrent inference.</p>
 *
 * <p><b>Model expectations (verified against Reporthole-v1.onnx):</b></p>
 * <ul>
 *   <li>Input name: {@code images}, shape {@code [1, 3, 640, 640]}, float32 NCHW,
 *       pixel values normalised to [0.0, 1.0].</li>
 *   <li>Output name: {@code output0}, shape {@code [1, 28, 8400]} — first 4 values
 *       per anchor are bounding-box coordinates (ignored here); values 4–27 are
 *       raw class scores for the 24 model classes.</li>
 * </ul>
 *
 * <p><b>Confidence extraction:</b> No bounding-box decoding or NMS is performed.
 * For each of the 8400 anchors the maximum raw class score across the 24 classes
 * is found. The highest score across all anchors (excluding non-damage classes) is
 * returned as the detection confidence.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnnxInferenceService {

    private static final int NUM_CLASSES  = 24;
    private static final int BBOX_OFFSET  = 4;   // first 4 values per anchor are bbox coords
    private static final int NUM_ANCHORS  = 8400;

    /**
     * Class names in the exact index order produced by Reporthole-v1.pt / Reporthole-v1.onnx.
     * Verified via {@code YOLO('models/Reporthole-v1.pt').names}.
     */
    private static final String[] CLASS_NAMES = {
            "Alligator_Crack_FP",    // 0
            "Block_Crack_FP",        // 1
            "Bumps_Sags_FP",         // 2
            "Corner_Break_RP",       // 3
            "Divided_Slab_Crack_RP", // 4
            "Durability_Crack_RP",   // 5
            "Edge_Crack_FP",         // 6
            "Joint_RP",              // 7
            "Joint_Spalling_RP",     // 8
            "Linear_Crack_RP",       // 9
            "Longitudinal_Crack_FP", // 10
            "Manhole_Cover",         // 11
            "Miscellanious_Crack_FP",// 12
            "Patch_Utility_Cut_FP",  // 13
            "Pothole_FP",            // 14
            "Rutting_FP",            // 15
            "Slippage_Crack_FP",     // 16
            "Transverse_Crack_FP",   // 17
            "Weather_Raveling_FP",   // 18
            "multiple_accident",     // 19
            "normal",                // 20
            "object",                // 21
            "pothole",               // 22
            "single accident"        // 23
    };

    /** Maps raw model class name → IssueType string used by the backend. Mirrors model.py::LABEL_MAP. */
    private static final Map<String, String> LABEL_MAP = Map.ofEntries(
            Map.entry("Pothole_FP",             "POTHOLE"),
            Map.entry("pothole",                "POTHOLE"),
            Map.entry("Alligator_Crack_FP",     "CRACK"),
            Map.entry("Block_Crack_FP",         "CRACK"),
            Map.entry("Bumps_Sags_FP",          "CRACK"),
            Map.entry("Corner_Break_RP",        "CRACK"),
            Map.entry("Divided_Slab_Crack_RP",  "CRACK"),
            Map.entry("Durability_Crack_RP",    "CRACK"),
            Map.entry("Edge_Crack_FP",          "CRACK"),
            Map.entry("Joint_RP",               "CRACK"),
            Map.entry("Joint_Spalling_RP",      "CRACK"),
            Map.entry("Linear_Crack_RP",        "CRACK"),
            Map.entry("Longitudinal_Crack_FP",  "CRACK"),
            Map.entry("Miscellanious_Crack_FP", "CRACK"),
            Map.entry("Patch_Utility_Cut_FP",   "CRACK"),
            Map.entry("Rutting_FP",             "CRACK"),
            Map.entry("Slippage_Crack_FP",      "CRACK"),
            Map.entry("Transverse_Crack_FP",    "CRACK"),
            Map.entry("Weather_Raveling_FP",    "CRACK"),
            Map.entry("Manhole_Cover",          "BLOCKED_DRAIN"),
            Map.entry("multiple_accident",      "ACCIDENT"),
            Map.entry("single accident",        "ACCIDENT")
    );

    /**
     * Classes that represent no road damage.
     * Detections matching these class names are ignored even if they have the highest confidence.
     */
    private static final Set<String> NON_DAMAGE_CLASSES = Set.of("normal", "object");

    private final InferenceProperties properties;

    private OrtEnvironment env;
    private OrtSession session;

    /**
     * Loads the ONNX model and creates the inference session.
     * Called automatically by Spring after dependency injection.
     *
     * <p>Supports two path formats in {@link InferenceProperties#getModelPath()}:</p>
     * <ul>
     *   <li>{@code classpath:models/Reporthole-v1.onnx} — loaded from the JAR classpath</li>
     *   <li>Any other value — treated as a plain filesystem path</li>
     * </ul>
     *
     * @throws IOException  if the model file cannot be read
     * @throws OrtException if the ONNX Runtime session cannot be created
     */
    @PostConstruct
    public void init() throws IOException, OrtException {
        String modelPath = properties.getModelPath();
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

        log.info("ONNX inference session loaded from '{}' — input: {}, output: {}",
                modelPath,
                session.getInputNames(),
                session.getOutputNames());
    }

    /**
     * Runs confidence-only inference on the supplied image bytes.
     *
     * <p>The image is preprocessed (letterbox resize to 640×640, normalised to [0, 1])
     * and passed through the model. The highest raw class score across all 8400 anchor
     * positions is returned as the detection confidence. Non-damage classes
     * ({@code "normal"}, {@code "object"}) are excluded.</p>
     *
     * @param imageBytes raw bytes of a JPEG or PNG image
     * @return {@link InferenceResult} with the best detection label and confidence,
     *         or {@link InferenceResult#empty()} if no damage class scored above 0
     * @throws IOException  if the image bytes cannot be decoded
     * @throws OrtException if the ONNX Runtime inference call fails
     */
    public InferenceResult predict(byte[] imageBytes) throws IOException, OrtException {
        log.debug("Running inference on {} byte image", imageBytes.length);
        long start = System.currentTimeMillis();

        try (OnnxTensor input = ImagePreprocessor.preprocess(imageBytes, env);
             OrtSession.Result results = session.run(Map.of("images", input))) {

            OnnxTensor output = (OnnxTensor) results.get("output0").orElseThrow(
                    () -> new OrtException("Model did not produce 'output0'"));

            InferenceResult result = extractBestDetection(output.getFloatBuffer());
            long elapsed = System.currentTimeMillis() - start;

            if (result.detected()) {
                log.info("Inference complete in {}ms — detected: {} (raw: {}, confidence: {})",
                        elapsed, result.label(), result.rawLabel(), result.confidence());
            } else {
                log.info("Inference complete in {}ms — no damage detected", elapsed);
            }

            return result;
        }
    }

    /**
     * Extracts the highest-confidence non-damage detection from the raw model output buffer.
     *
     * <p>Buffer layout for shape {@code [1, 28, 8400]} in row-major order:
     * {@code buffer[channel * 8400 + anchor]}. Channels 0–3 are bbox coordinates
     * (skipped); channels 4–27 are raw class scores for the 24 model classes.</p>
     *
     * @param buffer flattened float buffer of the {@code output0} tensor
     * @return best detection, or {@link InferenceResult#empty()} if nothing found
     */
    public InferenceResult extractBestDetection(FloatBuffer buffer) {
        // Anchors with all-zero class scores are not detections; initialise at 0 so they are excluded.
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

            String rawLabel = CLASS_NAMES[anchorBestClass];
            if (NON_DAMAGE_CLASSES.contains(rawLabel)) continue;

            if (anchorBest > bestConf) {
                bestConf = anchorBest;
                bestRawLabel = rawLabel;
            }
        }

        if (bestRawLabel == null) {
            log.debug("extractBestDetection — all anchors scored zero for damage classes");
            return InferenceResult.empty();
        }

        String mappedLabel = LABEL_MAP.getOrDefault(bestRawLabel,
                bestRawLabel.toUpperCase().replace(" ", "_"));
        double confidence = Math.round(bestConf * 10_000.0) / 10_000.0;

        log.debug("extractBestDetection — best anchor: rawLabel={}, mappedLabel={}, confidence={}",
                bestRawLabel, mappedLabel, confidence);

        return new InferenceResult(true, mappedLabel, bestRawLabel, confidence);
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
            log.info("ONNX inference session closed");
        } catch (OrtException e) {
            log.warn("Error closing ONNX session: {}", e.getMessage());
        }
    }
}
