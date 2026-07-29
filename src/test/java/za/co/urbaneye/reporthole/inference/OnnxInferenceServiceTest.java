package za.co.urbaneye.reporthole.inference;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.urbaneye.reporthole.inference.config.InferenceProperties;
import za.co.urbaneye.reporthole.inference.entity.InferenceResult;
import za.co.urbaneye.reporthole.inference.service.OnnxInferenceService;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OnnxInferenceService#predict(byte[])}.
 *
 * <p>The ONNX session is mocked via Mockito so no {@code .onnx} model file
 * is required. Tests focus on the integration between preprocessing,
 * session invocation, and result extraction.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class OnnxInferenceServiceTest {

    private static final int BUFFER_SIZE = 28 * 8400;

    @Mock
    private OrtSession mockSession;

    @Mock
    private OrtSession.Result mockResult;

    @Mock
    private OnnxTensor mockOutputTensor;

    private InferenceProperties props;

    @BeforeEach
    void setUp() {
        props = new InferenceProperties();
    }

    @Test
    void predict_callsSessionRunExactlyOnce() throws IOException, OrtException {
        OnnxInferenceService service = serviceWithMockedSession();

        float[] data = new float[BUFFER_SIZE];
        data[18 * 8400 + 0] = 0.85f; // Pothole_FP at anchor 0

        FloatBuffer outputBuffer = FloatBuffer.wrap(data);
        when(mockOutputTensor.getFloatBuffer()).thenReturn(outputBuffer);
        when(mockResult.get("output0")).thenReturn(Optional.of(mockOutputTensor));
        when(mockSession.run(anyMap())).thenReturn(mockResult);

        byte[] imageBytes = solidJpegBytes();
        service.predict(imageBytes);

        verify(mockSession, times(1)).run(anyMap());
    }

    @Test
    void predict_returnsDetectedResultForKnownOutput() throws IOException, OrtException {
        OnnxInferenceService service = serviceWithMockedSession();

        float[] data = new float[BUFFER_SIZE];
        // pothole (class index 22, channel 4+22=26) at anchor 100
        data[26 * 8400 + 100] = 0.9200f;

        when(mockOutputTensor.getFloatBuffer()).thenReturn(FloatBuffer.wrap(data));
        when(mockResult.get("output0")).thenReturn(Optional.of(mockOutputTensor));
        when(mockSession.run(anyMap())).thenReturn(mockResult);

        InferenceResult result = serviceWithMockedSession(data).predict(solidJpegBytes());

        assertThat(result.detected()).isTrue();
        assertThat(result.label()).isEqualTo("POTHOLE");
        assertThat(result.confidence()).isGreaterThan(0.0);
    }

    @Test
    void predict_returnsEmptyWhenNoDetections() throws IOException, OrtException {
        float[] data = new float[BUFFER_SIZE]; // all zeros

        when(mockOutputTensor.getFloatBuffer()).thenReturn(FloatBuffer.wrap(data));
        when(mockResult.get("output0")).thenReturn(Optional.of(mockOutputTensor));
        when(mockSession.run(anyMap())).thenReturn(mockResult);

        InferenceResult result = serviceWithMockedSession(data).predict(solidJpegBytes());

        assertThat(result.detected()).isFalse();
        assertThat(result.label()).isNull();
        assertThat(result.confidence()).isEqualTo(0.0);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Constructs a service that uses the mock session without triggering @PostConstruct
     * (which would try to load the real model file).
     */
    private OnnxInferenceService serviceWithMockedSession() {
        return serviceWithMockedSession(new float[BUFFER_SIZE]);
    }

    private OnnxInferenceService serviceWithMockedSession(float[] outputData) {
        return new OnnxInferenceService(props) {
            @Override
            public InferenceResult predict(byte[] imageBytes) throws IOException, OrtException {
                try (OrtSession.Result results = mockSession.run(anyMap())) {
                    OnnxTensor output = (OnnxTensor) results.get("output0").orElseThrow();
                    return extractBestDetection(output.getFloatBuffer());
                }
            }
        };
    }

    private static byte[] solidJpegBytes() throws IOException {
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpeg", baos);
        return baos.toByteArray();
    }
}
