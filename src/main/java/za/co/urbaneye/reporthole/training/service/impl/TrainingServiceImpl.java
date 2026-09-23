package za.co.urbaneye.reporthole.training.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.incident.entity.Incident;
import za.co.urbaneye.reporthole.incident.repository.IncidentRepository;
import za.co.urbaneye.reporthole.incident.service.interfaces.ImageStorageService;
import za.co.urbaneye.reporthole.training.dto.AnnotationBoxRequest;
import za.co.urbaneye.reporthole.training.dto.AnnotationResponse;
import za.co.urbaneye.reporthole.training.dto.SaveAnnotationsRequest;
import za.co.urbaneye.reporthole.training.dto.TrainingStatusResponse;
import za.co.urbaneye.reporthole.training.entity.IssueAnnotation;
import za.co.urbaneye.reporthole.training.entity.TrainingStatus;
import za.co.urbaneye.reporthole.training.exception.TrainingException;
import za.co.urbaneye.reporthole.training.repository.IssueAnnotationRepository;
import za.co.urbaneye.reporthole.training.service.interfaces.ITrainingService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingServiceImpl implements ITrainingService {

    /** Smallest box edge kept after clamping to the image, in pixels. */
    private static final double MIN_BOX_EDGE_PX = 1.0;

    private final IncidentRepository incidentRepository;
    private final IssueAnnotationRepository annotationRepository;
    private final IUserRepository userRepository;
    private final ImageStorageService imageStorageService;

    @Override
    public List<AnnotationResponse> getAnnotations(UUID incidentId) {
        User securityAdmin = requireSecurityAdmin();
        requireIncident(incidentId, securityAdmin);
        return annotationRepository.findByIncident_IncidentIdOrderByCreatedAtAsc(incidentId).stream()
                .map(AnnotationResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public List<AnnotationResponse> saveAnnotations(UUID incidentId, SaveAnnotationsRequest request) {
        User securityAdmin = requireSecurityAdmin();
        Incident incident = requireIncident(incidentId, securityAdmin);

        if (incident.getImageUrl() == null || incident.getImageUrl().isBlank()) {
            throw new TrainingException("Incident has no image to annotate");
        }
        int width = request.imageWidth();
        int height = request.imageHeight();
        if (incident.getImageWidth() == null || incident.getImageHeight() == null) {
            incident.setImageWidth(width);
            incident.setImageHeight(height);
            incidentRepository.save(incident);
        } else if (incident.getImageWidth() != width || incident.getImageHeight() != height) {
            throw new TrainingException("Image dimensions do not match the size recorded for this incident ("
                    + incident.getImageWidth() + "x" + incident.getImageHeight() + ")");
        }

        List<IssueAnnotation> created = new ArrayList<>();
        for (AnnotationBoxRequest box : request.boxes()) {
            created.add(toAnnotation(box, incident, securityAdmin, width, height));
        }
        return annotationRepository.saveAll(created).stream().map(AnnotationResponse::from).toList();
    }

    @Override
    @Transactional
    public void deleteAnnotation(UUID annotationId) {
        User securityAdmin = requireSecurityAdmin();
        IssueAnnotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new TrainingException("Annotation not found: " + annotationId));
        Incident incident = annotation.getIncident();
        requireSameMunicipality(securityAdmin, incident);

        annotationRepository.delete(annotation);
        annotationRepository.flush();

        // A flagged incident with no boxes left would export as an empty label file (a background
        // image) — almost certainly not what the security admin meant, so drop the flag with the
        // last box.
        if (incident.getTrainingStatus() == TrainingStatus.FLAGGED
                && annotationRepository.countByIncident_IncidentId(incident.getIncidentId()) == 0) {
            incident.setTrainingStatus(TrainingStatus.NOT_FLAGGED);
            incident.setTrainingFlaggedAt(null);
            incidentRepository.save(incident);
        }
    }

    @Override
    @Transactional
    public TrainingStatusResponse setFlaggedForTraining(UUID incidentId, Boolean flagged) {
        User securityAdmin = requireSecurityAdmin();
        Incident incident = requireIncident(incidentId, securityAdmin);

        boolean flag = flagged != null ? flagged : incident.getTrainingStatus() != TrainingStatus.FLAGGED;
        if (flag) {
            if (annotationRepository.countByIncident_IncidentId(incidentId) == 0) {
                throw new TrainingException("Draw at least one annotation before flagging for training");
            }
            if (incident.getTrainingStatus() != TrainingStatus.FLAGGED) {
                incident.setTrainingStatus(TrainingStatus.FLAGGED);
                incident.setTrainingFlaggedAt(LocalDateTime.now());
            }
        } else if (incident.getTrainingStatus() == TrainingStatus.FLAGGED) {
            incident.setTrainingStatus(TrainingStatus.NOT_FLAGGED);
            incident.setTrainingFlaggedAt(null);
        }
        incidentRepository.save(incident);
        return new TrainingStatusResponse(incidentId, incident.getTrainingStatus());
    }

    @Override
    @Transactional
    public byte[] exportYoloDataset(LocalDateTime since) {
        requireSecurityAdmin();

        List<Incident> flagged = since == null
                ? incidentRepository.findByTrainingStatusAndDeletedFalseOrderByTrainingFlaggedAtAsc(TrainingStatus.FLAGGED)
                : incidentRepository.findByTrainingStatusAndDeletedFalseAndTrainingFlaggedAtGreaterThanEqualOrderByTrainingFlaggedAtAsc(
                        TrainingStatus.FLAGGED, since);

        Map<UUID, List<IssueAnnotation>> boxesByIncident = flagged.isEmpty() ? Map.of()
                : annotationRepository.findByIncident_IncidentIdIn(
                                flagged.stream().map(Incident::getIncidentId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(a -> a.getIncident().getIncidentId()));

        List<YoloDatasetZipBuilder.Sample> samples = new ArrayList<>();
        List<Incident> included = new ArrayList<>();
        for (Incident incident : flagged) {
            List<IssueAnnotation> boxes = boxesByIncident.getOrDefault(incident.getIncidentId(), List.of());
            if (boxes.isEmpty() || incident.getImageUrl() == null) {
                log.warn("Skipping incident {} in YOLO export: no annotations or no image", incident.getIncidentId());
                continue;
            }
            try {
                samples.add(new YoloDatasetZipBuilder.Sample(
                        incident.getIncidentId().toString(),
                        imageStorageService.readImage(incident.getImageUrl()),
                        boxes));
                included.add(incident);
            } catch (UncheckedIOException e) {
                // Stays FLAGGED so it is picked up by the next export once the file is restored.
                log.warn("Skipping incident {} in YOLO export: {}", incident.getIncidentId(), e.getMessage());
            }
        }

        if (samples.isEmpty()) {
            throw new TrainingException("No flagged incidents with readable images were found to export");
        }

        byte[] zip = YoloDatasetZipBuilder.build(samples);
        included.forEach(i -> i.setTrainingStatus(TrainingStatus.EXPORTED));
        incidentRepository.saveAll(included);
        log.info("YOLO export: {} incident(s) exported, {} skipped", included.size(), flagged.size() - included.size());
        return zip;
    }

    /** Pixel box (top-left origin) to normalised YOLO box (centre origin), clamped to the image. */
    private IssueAnnotation toAnnotation(AnnotationBoxRequest box, Incident incident, User securityAdmin, int imgW, int imgH) {
        double x0 = Math.max(0, box.x());
        double y0 = Math.max(0, box.y());
        double x1 = Math.min(imgW, box.x() + box.width());
        double y1 = Math.min(imgH, box.y() + box.height());
        if (x1 - x0 < MIN_BOX_EDGE_PX || y1 - y0 < MIN_BOX_EDGE_PX) {
            throw new TrainingException("Box is outside the image or too small");
        }
        return IssueAnnotation.builder()
                .incident(incident)
                .classLabel(box.classLabel())
                .xCenter(((x0 + x1) / 2) / imgW)
                .yCenter(((y0 + y1) / 2) / imgH)
                .width((x1 - x0) / imgW)
                .height((y1 - y0) / imgH)
                .annotatedBy(securityAdmin)
                .build();
    }

    private Incident requireIncident(UUID incidentId, User securityAdmin) {
        Incident incident = incidentRepository.findById(incidentId)
                .filter(i -> !i.isDeleted())
                .orElseThrow(() -> new TrainingException("Incident not found: " + incidentId));
        requireSameMunicipality(securityAdmin, incident);
        return incident;
    }

    /**
     * Same rule as assignment: skipped when either side has no municipality. A security admin has
     * no municipality of their own, so this is effectively a no-op for them today — kept as a guard
     * in case training is ever scoped to a municipality-bound caller too.
     */
    private void requireSameMunicipality(User securityAdmin, Incident incident) {
        if (securityAdmin.getMunicipality() != null && incident.getMunicipality() != null
                && !securityAdmin.getMunicipality().equals(incident.getMunicipality())) {
            throw new TrainingException("Incident belongs to a different municipality");
        }
    }

    private User requireSecurityAdmin() {
        UUID userId = UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TrainingException("User not found"));
        if (user.getRole() != UserRole.SECURITY_ADMIN) {
            throw new TrainingException("Only security admins can perform this action");
        }
        return user;
    }
}
