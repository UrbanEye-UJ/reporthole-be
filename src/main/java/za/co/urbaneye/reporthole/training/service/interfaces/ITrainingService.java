package za.co.urbaneye.reporthole.training.service.interfaces;

import za.co.urbaneye.reporthole.training.dto.AnnotationResponse;
import za.co.urbaneye.reporthole.training.dto.SaveAnnotationsRequest;
import za.co.urbaneye.reporthole.training.dto.TrainingStatusResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Admin-only YOLO annotation, training-flag and dataset-export operations. */
public interface ITrainingService {

    List<AnnotationResponse> getAnnotations(UUID incidentId);

    /** Appends the boxes to the incident's existing annotations; returns the ones just created. */
    List<AnnotationResponse> saveAnnotations(UUID incidentId, SaveAnnotationsRequest request);

    void deleteAnnotation(UUID annotationId);

    /**
     * Sets the incident's training flag. {@code flagged == null} toggles: FLAGGED becomes NOT_FLAGGED,
     * anything else becomes FLAGGED. Flagging requires at least one annotation.
     */
    TrainingStatusResponse setFlaggedForTraining(UUID incidentId, Boolean flagged);

    /**
     * Builds a YOLO dataset zip (images/, labels/, data.yaml) of every FLAGGED incident — optionally
     * only those flagged at or after {@code since} — and marks the included incidents EXPORTED.
     *
     * @throws za.co.urbaneye.reporthole.training.exception.TrainingException if nothing is exportable
     */
    byte[] exportYoloDataset(LocalDateTime since);
}
