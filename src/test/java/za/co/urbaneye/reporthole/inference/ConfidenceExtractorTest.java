package za.co.urbaneye.reporthole.inference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.urbaneye.reporthole.inference.config.InferenceProperties;
import za.co.urbaneye.reporthole.inference.entity.InferenceResult;
import za.co.urbaneye.reporthole.inference.service.OnnxInferenceService;

import java.nio.FloatBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Unit tests for {@link OnnxInferenceService#extractBestDetection(FloatBuffer)}.
 *
 * <p>Uses hand-crafted output buffers that mimic the raw {@code output0} tensor
 * from Reporthole-v1.onnx (shape {@code [1, 28, 8400]}) so no actual ONNX model
 * file is required.</p>
 *
 * <p>Buffer layout: {@code buffer[channel * 8400 + anchor]}.
 * Channels 0–3 are bbox coords (ignored); channels 4–27 are class scores for
 * the 24 model classes in the order defined by {@code CLASS_NAMES} in
 * {@link OnnxInferenceService}.</p>
 *
 * @author Refentse
 * @since 1.0
 */
class ConfidenceExtractorTest {

    private static final int NUM_CHANNELS = 28;   // 4 bbox + 24 class
    private static final int NUM_ANCHORS  = 8400;
    private static final int BUFFER_SIZE  = NUM_CHANNELS * NUM_ANCHORS;

    private InferenceProperties props;
    private OnnxInferenceService service;

    @BeforeEach
    void setUp() {
        props = new InferenceProperties();
        service = new OnnxInferenceService(props);
    }

    @Test
    void extractBestDetection_returnsEmptyWhenAllScoresZero() {
        FloatBuffer buffer = FloatBuffer.wrap(new float[BUFFER_SIZE]);
        InferenceResult result = service.extractBestDetection(buffer);

        assertThat(result.detected()).isFalse();
        assertThat(result.label()).isNull();
        assertThat(result.confidence()).isEqualTo(0.0);
    }

    @Test
    void extractBestDetection_detectsPothole() {
        // Pothole_FP is class index 14 — channel 4+14 = 18
        float[] data = new float[BUFFER_SIZE];
        int anchor = 42;
        float expectedConf = 0.9123f;
        data[18 * NUM_ANCHORS + anchor] = expectedConf;

        InferenceResult result = service.extractBestDetection(FloatBuffer.wrap(data));

        assertThat(result.detected()).isTrue();
        assertThat(result.label()).isEqualTo("POTHOLE");
        assertThat(result.rawLabel()).isEqualTo("Pothole_FP");
        assertThat(result.confidence()).isCloseTo(0.9123, offset(1e-4));
    }

    @Test
    void extractBestDetection_detectsAccident() {
        // multiple_accident is class index 19 — channel 4+19 = 23
        float[] data = new float[BUFFER_SIZE];
        data[23 * NUM_ANCHORS + 100] = 0.75f;

        InferenceResult result = service.extractBestDetection(FloatBuffer.wrap(data));

        assertThat(result.detected()).isTrue();
        assertThat(result.label()).isEqualTo("ACCIDENT");
        assertThat(result.rawLabel()).isEqualTo("multiple_accident");
    }

    @Test
    void extractBestDetection_ignoresNonDamageClasses() {
        float[] data = new float[BUFFER_SIZE];
        // "normal" is class index 20 — channel 4+20 = 24
        data[24 * NUM_ANCHORS + 0] = 0.99f;
        // "object" is class index 21 — channel 4+21 = 25
        data[25 * NUM_ANCHORS + 1] = 0.95f;

        InferenceResult result = service.extractBestDetection(FloatBuffer.wrap(data));

        assertThat(result.detected()).isFalse();
    }

    @Test
    void extractBestDetection_returnsBestAcrossAllAnchors() {
        float[] data = new float[BUFFER_SIZE];
        // Pothole_FP (index 14, channel 18) at anchor 0 with low confidence
        data[18 * NUM_ANCHORS + 0] = 0.50f;
        // Pothole_FP (index 14, channel 18) at anchor 5000 with higher confidence
        data[18 * NUM_ANCHORS + 5000] = 0.8765f;

        InferenceResult result = service.extractBestDetection(FloatBuffer.wrap(data));

        assertThat(result.detected()).isTrue();
        assertThat(result.confidence()).isCloseTo(0.8765, offset(1e-4));
    }

    @Test
    void extractBestDetection_picksDamageClassOverHigherNonDamage() {
        float[] data = new float[BUFFER_SIZE];
        // "normal" (index 20, channel 24) — very high but must be ignored
        data[24 * NUM_ANCHORS + 0] = 0.95f;
        // Crack class: Alligator_Crack_FP (index 0, channel 4) — lower but valid
        data[4 * NUM_ANCHORS + 1] = 0.70f;

        InferenceResult result = service.extractBestDetection(FloatBuffer.wrap(data));

        assertThat(result.detected()).isTrue();
        assertThat(result.label()).isEqualTo("CRACK");
        assertThat(result.confidence()).isCloseTo(0.70, offset(1e-4));
    }

    @Test
    void extractBestDetection_confidenceRoundedToFourDecimalPlaces() {
        float[] data = new float[BUFFER_SIZE];
        // pothole (index 22, channel 26) — confidence with many decimal places
        data[26 * NUM_ANCHORS + 0] = 0.873456789f;

        InferenceResult result = service.extractBestDetection(FloatBuffer.wrap(data));

        // Should be rounded to 4 decimal places
        assertThat(result.confidence()).isCloseTo(0.8735, offset(1e-4));
    }
}
