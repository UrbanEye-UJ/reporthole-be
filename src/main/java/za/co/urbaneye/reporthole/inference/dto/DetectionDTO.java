package za.co.urbaneye.reporthole.inference.dto;

/**
 * Nested detection payload inside {@link PredictResponseDTO}.
 *
 * <p>All fields are {@code null} when {@code detected} on the parent response is
 * {@code false}. When a road-damage class is found, {@code label} contains the
 * IssueType-compatible string (e.g. {@code "POTHOLE"}), {@code rawLabel} contains
 * the raw model class name (e.g. {@code "Pothole_FP"}), and {@code confidence} is
 * the best detection score in [0.0, 1.0] rounded to 4 decimal places.</p>
 *
 * @param label      IssueType-compatible label; {@code null} when not detected
 * @param confidence best detection confidence in [0.0, 1.0]; {@code null} when not detected
 * @param rawLabel   raw model class name; {@code null} when not detected
 *
 * @author Refentse
 * @since 1.0
 */
public record DetectionDTO(
        String label,
        Double confidence,
        String rawLabel
) {}
