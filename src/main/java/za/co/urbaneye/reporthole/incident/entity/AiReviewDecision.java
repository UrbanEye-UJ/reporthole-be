package za.co.urbaneye.reporthole.incident.entity;

/**
 * Two-tier routing decision for AI-generated incidents, based on detection confidence.
 *
 * <ul>
 *   <li>{@code AUTO_APPROVED} — confidence at or above the approval threshold;
 *       the incident is verified immediately, skipping manual admin review.</li>
 *   <li>{@code PENDING_REVIEW} — confidence below the approval threshold;
 *       the incident is created as {@code REPORTED} and left in the normal
 *       admin verification queue.</li>
 * </ul>
 */
public enum AiReviewDecision {
    AUTO_APPROVED,
    PENDING_REVIEW;

    /**
     * Derives the review decision for a given AI detection confidence.
     *
     * @param confidence detection confidence in [0.0, 1.0]
     * @param threshold  minimum confidence required for automatic approval
     * @return {@code AUTO_APPROVED} when {@code confidence >= threshold}, otherwise {@code PENDING_REVIEW}
     */
    public static AiReviewDecision from(double confidence, double threshold) {
        return confidence >= threshold ? AUTO_APPROVED : PENDING_REVIEW;
    }
}
