package za.co.urbaneye.reporthole.inference.dto;

import za.co.urbaneye.reporthole.inference.entity.InferenceSource;

/**
 * Nested detection payload inside {@link PredictResponseDTO}.
 *
 * <p>All fields are {@code null} when {@code detected} on the parent response is
 * {@code false}. When a road-damage class is found, {@code label} contains the
 * IssueType-compatible string (e.g. {@code "POTHOLE"}), {@code rawLabel} contains
 * the raw model class name (e.g. {@code "Pothole_FP"}), and {@code confidence} is
 * the best detection score in [0.0, 1.0] rounded to 4 decimal places.</p>
 *
 * @param label      IssueType-compatible label (custom model) or uppercased raw
 *                   COCO class name (stock model); {@code null} when not detected
 * @param confidence best detection confidence in [0.0, 1.0]; {@code null} when not detected
 * @param rawLabel   raw model class name; {@code null} when not detected
 * @param source     which model produced this detection
 * @param bboxXCenter normalised [0.0, 1.0] box centre X relative to the original image
 *                    (centre-based YOLO format); {@code null} when not detected or the
 *                    producing model doesn't decode boxes (e.g. the stock model)
 * @param bboxYCenter normalised [0.0, 1.0] box centre Y relative to the original image
 * @param bboxWidth   normalised [0.0, 1.0] box width relative to the original image
 * @param bboxHeight  normalised [0.0, 1.0] box height relative to the original image
 *
 * @author Refentse
 * @since 1.0
 */
public record DetectionDTO(
        String label,
        Double confidence,
        String rawLabel,
        InferenceSource source,
        Double bboxXCenter,
        Double bboxYCenter,
        Double bboxWidth,
        Double bboxHeight
) {}
