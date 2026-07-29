package za.co.urbaneye.reporthole.inference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import za.co.urbaneye.reporthole.inference.config.InferenceProperties;
import za.co.urbaneye.reporthole.inference.entity.RoutingDecision;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RoutingDecision#from(double, InferenceProperties)}.
 *
 * <p>Verifies boundary behaviour at and around the default thresholds
 * (discard=0.65, auto-log=0.80) using the same logic as the frontend
 * dashcam page.</p>
 *
 * @author Refentse
 * @since 1.0
 */
class RoutingDecisionTest {

    private InferenceProperties props;

    @BeforeEach
    void setUp() {
        props = new InferenceProperties(); // defaults: discard=0.65, autoLog=0.80
    }

    // ── exact boundary values ────────────────────────────────────────────────

    @Test
    void exactAutoLogThreshold_isAutoLog() {
        assertThat(RoutingDecision.from(0.80, props)).isEqualTo(RoutingDecision.AUTO_LOG);
    }

    @Test
    void justBelowAutoLogThreshold_isEscalate() {
        assertThat(RoutingDecision.from(0.7999, props)).isEqualTo(RoutingDecision.ESCALATE);
    }

    @Test
    void exactDiscardThreshold_isEscalate() {
        assertThat(RoutingDecision.from(0.65, props)).isEqualTo(RoutingDecision.ESCALATE);
    }

    @Test
    void justBelowDiscardThreshold_isDiscard() {
        assertThat(RoutingDecision.from(0.6499, props)).isEqualTo(RoutingDecision.DISCARD);
    }

    // ── clear cases ──────────────────────────────────────────────────────────

    @Test
    void highConfidence_isAutoLog() {
        assertThat(RoutingDecision.from(0.95, props)).isEqualTo(RoutingDecision.AUTO_LOG);
    }

    @Test
    void midRangeConfidence_isEscalate() {
        assertThat(RoutingDecision.from(0.72, props)).isEqualTo(RoutingDecision.ESCALATE);
    }

    @Test
    void zeroConfidence_isDiscard() {
        assertThat(RoutingDecision.from(0.0, props)).isEqualTo(RoutingDecision.DISCARD);
    }

    // ── custom thresholds ────────────────────────────────────────────────────

    @ParameterizedTest(name = "confidence={0} → {1}")
    @CsvSource({
            "0.90, AUTO_LOG",
            "0.85, ESCALATE",
            "0.80, ESCALATE",
            "0.79, DISCARD",
            "0.50, DISCARD"
    })
    void customThresholds(double confidence, RoutingDecision expected) {
        InferenceProperties custom = new InferenceProperties();
        custom.setDiscardThreshold(0.80);
        custom.setAutoLogThreshold(0.90);
        assertThat(RoutingDecision.from(confidence, custom)).isEqualTo(expected);
    }
}
