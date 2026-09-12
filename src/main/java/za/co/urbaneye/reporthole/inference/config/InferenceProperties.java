package za.co.urbaneye.reporthole.inference.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for the ONNX inference module.
 *
 * <p>All properties are prefixed with {@code inference} in application configuration files.
 * Example {@code application.yaml} block:</p>
 * <pre>
 * inference:
 *   model-path: classpath:models/Reporthole-v1.onnx
 *   discard-threshold: 0.65
 *   auto-log-threshold: 0.75
 * </pre>
 *
 * @author Refentse
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "inference")
@Getter
@Setter
public class InferenceProperties {

    /**
     * Path to the exported ONNX model file.
     * Accepts a {@code classpath:} prefix for resources bundled in the JAR,
     * or a plain filesystem path for externally mounted models (e.g. Docker volumes).
     */
    private String modelPath = "classpath:models/Reporthole-v1.onnx";

    /**
     * Minimum confidence required to log an event; detections below this value are discarded.
     * Mirrors {@code DISCARD_THRESHOLD} on the frontend dashcam page.
     */
    private double discardThreshold = 0.65;

    /**
     * Confidence at or above which an incident is created immediately without user confirmation.
     * Mirrors {@code AUTO_LOG_THRESHOLD} on the frontend dashcam page.
     */
    private double autoLogThreshold = 0.75;
}
