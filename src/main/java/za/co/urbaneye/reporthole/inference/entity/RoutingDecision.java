package za.co.urbaneye.reporthole.inference.entity;

import za.co.urbaneye.reporthole.inference.config.InferenceProperties;

/**
 * Three-tier routing decision based on detection confidence.
 *
 * <p>Mirrors the thresholds defined on the frontend dashcam and manual-report
 * flows ({@code DISCARD_THRESHOLD = 0.65}, {@code AUTO_LOG_THRESHOLD = 0.75}).
 * Thresholds are read from {@link InferenceProperties} so they can be
 * overridden without recompiling.</p>
 *
 * <ul>
 *   <li>{@code AUTO_LOG}  — confidence ≥ autoLogThreshold (default 0.75);
 *       incident created immediately without user intervention</li>
 *   <li>{@code ESCALATE}  — confidence ≥ discardThreshold (default 0.65) and below autoLogThreshold;
 *       event held in the log; user must confirm before incident is created</li>
 *   <li>{@code DISCARD}   — confidence below discardThreshold; event logged silently,
 *       no incident created</li>
 * </ul>
 *
 * @author Refentse
 * @since 1.0
 */
public enum RoutingDecision {
    AUTO_LOG,
    ESCALATE,
    DISCARD;

    /**
     * Derives the routing decision for a given confidence score.
     *
     * @param confidence detection confidence in [0.0, 1.0]
     * @param props      threshold configuration
     * @return {@code AUTO_LOG}, {@code ESCALATE}, or {@code DISCARD}
     */
    public static RoutingDecision from(double confidence, InferenceProperties props) {
        if (confidence >= props.getAutoLogThreshold()) {
            return AUTO_LOG;
        }
        if (confidence >= props.getDiscardThreshold()) {
            return ESCALATE;
        }
        return DISCARD;
    }
}
