package za.co.urbaneye.reporthole.training.dto;

import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.training.entity.IssueAnnotation;

import java.time.LocalDateTime;
import java.util.UUID;

/** A stored box in normalised YOLO coordinates (0–1, centre-based). */
public record AnnotationResponse(
        UUID id,
        UUID incidentId,
        IssueType classLabel,
        double xCenter,
        double yCenter,
        double width,
        double height,
        UUID annotatedBy,
        LocalDateTime createdAt
) {
    public static AnnotationResponse from(IssueAnnotation a) {
        return new AnnotationResponse(
                a.getId(), a.getIncident().getIncidentId(), a.getClassLabel(),
                a.getXCenter(), a.getYCenter(), a.getWidth(), a.getHeight(),
                a.getAnnotatedBy().getUserId(), a.getCreatedAt());
    }
}
