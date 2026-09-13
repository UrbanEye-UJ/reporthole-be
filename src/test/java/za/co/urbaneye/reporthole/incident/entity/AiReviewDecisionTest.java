package za.co.urbaneye.reporthole.incident.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AiReviewDecision#from(double, double)}.
 */
class AiReviewDecisionTest {

    @Test
    void exactThreshold_isAutoApproved() {
        assertThat(AiReviewDecision.from(0.80, 0.80)).isEqualTo(AiReviewDecision.AUTO_APPROVED);
    }

    @Test
    void justBelowThreshold_isPendingReview() {
        assertThat(AiReviewDecision.from(0.7999, 0.80)).isEqualTo(AiReviewDecision.PENDING_REVIEW);
    }

    @Test
    void highConfidence_isAutoApproved() {
        assertThat(AiReviewDecision.from(0.95, 0.80)).isEqualTo(AiReviewDecision.AUTO_APPROVED);
    }

    @Test
    void lowConfidence_isPendingReview() {
        assertThat(AiReviewDecision.from(0.10, 0.80)).isEqualTo(AiReviewDecision.PENDING_REVIEW);
    }

    @ParameterizedTest(name = "confidence={0}, threshold={1} -> {2}")
    @CsvSource({
            "0.90, 0.80, AUTO_APPROVED",
            "0.80, 0.80, AUTO_APPROVED",
            "0.79, 0.80, PENDING_REVIEW",
            "0.50, 0.90, PENDING_REVIEW",
            "1.00, 1.00, AUTO_APPROVED",
            "0.00, 0.00, AUTO_APPROVED"
    })
    void customThresholds(double confidence, double threshold, AiReviewDecision expected) {
        assertThat(AiReviewDecision.from(confidence, threshold)).isEqualTo(expected);
    }
}
