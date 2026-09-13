package za.co.urbaneye.reporthole.incident.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for AI-generated incident handling.
 *
 * <p>All properties are prefixed with {@code incident} in application configuration files.
 * Example {@code application.yaml} block:</p>
 * <pre>
 * incident:
 *   ai-approval-threshold: 0.80
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "incident")
@Getter
@Setter
public class IncidentProperties {

    /**
     * Minimum AI detection confidence, in [0.0, 1.0], at or above which an
     * AI-generated incident is automatically verified instead of being queued
     * for manual admin review. See {@link za.co.urbaneye.reporthole.incident.entity.AiReviewDecision}.
     */
    private double aiApprovalThreshold = 0.80;
}
