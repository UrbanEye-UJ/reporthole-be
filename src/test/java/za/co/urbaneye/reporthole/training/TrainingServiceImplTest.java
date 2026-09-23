package za.co.urbaneye.reporthole.training;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import za.co.urbaneye.reporthole.incident.entity.Incident;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.incident.repository.IncidentRepository;
import za.co.urbaneye.reporthole.incident.service.interfaces.ImageStorageService;
import za.co.urbaneye.reporthole.training.dto.AnnotationBoxRequest;
import za.co.urbaneye.reporthole.training.dto.AnnotationResponse;
import za.co.urbaneye.reporthole.training.dto.SaveAnnotationsRequest;
import za.co.urbaneye.reporthole.training.entity.IssueAnnotation;
import za.co.urbaneye.reporthole.training.entity.TrainingStatus;
import za.co.urbaneye.reporthole.training.exception.TrainingException;
import za.co.urbaneye.reporthole.training.repository.IssueAnnotationRepository;
import za.co.urbaneye.reporthole.training.service.impl.TrainingServiceImpl;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceImplTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private IssueAnnotationRepository annotationRepository;
    @Mock private IUserRepository userRepository;
    @Mock private ImageStorageService imageStorageService;
    @InjectMocks private TrainingServiceImpl service;

    private final UUID securityAdminId = UUID.randomUUID();
    private User securityAdmin;
    private Incident incident;

    @BeforeEach
    void setUp() {
        securityAdmin = new User();
        securityAdmin.setUserId(securityAdminId);
        securityAdmin.setRole(UserRole.SECURITY_ADMIN);
        incident = Incident.builder()
                .incidentId(UUID.randomUUID())
                .incidentType(IssueType.POTHOLE)
                .imageUrl("http://host/api/uploads/incidents/abc.jpg")
                .build();
        // clearContext() first: some other tests in this module leave a mocked SecurityContext
        // behind in the shared surefire thread, and mutating that stale object instead of a real
        // one would silently no-op.
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(securityAdminId.toString(), null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void loggedInAs(UserRole role) {
        securityAdmin.setRole(role);
        when(userRepository.findById(securityAdminId)).thenReturn(Optional.of(securityAdmin));
    }

    private void incidentExists() {
        when(incidentRepository.findById(incident.getIncidentId())).thenReturn(Optional.of(incident));
    }

    private static IssueAnnotation box(Incident incident, IssueType type) {
        return IssueAnnotation.builder().incident(incident).classLabel(type)
                .xCenter(0.5).yCenter(0.5).width(0.2).height(0.2).build();
    }

    @Test
    void saveAnnotations_normalisesPixelBoxToYoloCentreCoordinates() {
        loggedInAs(UserRole.SECURITY_ADMIN);
        incidentExists();
        when(annotationRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // 200x100 box with top-left at (100, 50) on a 1000x500 image
        var request = new SaveAnnotationsRequest(1000, 500,
                List.of(new AnnotationBoxRequest(IssueType.POTHOLE, 100.0, 50.0, 200.0, 100.0)));

        List<AnnotationResponse> saved = service.saveAnnotations(incident.getIncidentId(), request);

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).xCenter()).isCloseTo(0.2, within(1e-9));   // (100+100)/1000
        assertThat(saved.get(0).yCenter()).isCloseTo(0.2, within(1e-9));   // (50+50)/500
        assertThat(saved.get(0).width()).isCloseTo(0.2, within(1e-9));
        assertThat(saved.get(0).height()).isCloseTo(0.2, within(1e-9));
        assertThat(incident.getImageWidth()).isEqualTo(1000);
        assertThat(incident.getImageHeight()).isEqualTo(500);
    }

    @Test
    void saveAnnotations_clampsBoxOverhangingImageEdge() {
        loggedInAs(UserRole.SECURITY_ADMIN);
        incidentExists();
        when(annotationRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // Box runs 100px past the right edge of a 1000x500 image: clamped to x 900..1000
        var request = new SaveAnnotationsRequest(1000, 500,
                List.of(new AnnotationBoxRequest(IssueType.CRACK, 900.0, 0.0, 200.0, 500.0)));

        AnnotationResponse saved = service.saveAnnotations(incident.getIncidentId(), request).get(0);

        assertThat(saved.xCenter()).isCloseTo(0.95, within(1e-9));
        assertThat(saved.width()).isCloseTo(0.1, within(1e-9));
    }

    @Test
    void saveAnnotations_rejectsBoxEntirelyOutsideImage() {
        loggedInAs(UserRole.SECURITY_ADMIN);
        incidentExists();

        var request = new SaveAnnotationsRequest(1000, 500,
                List.of(new AnnotationBoxRequest(IssueType.POTHOLE, 1200.0, 10.0, 50.0, 50.0)));

        assertThatThrownBy(() -> service.saveAnnotations(incident.getIncidentId(), request))
                .isInstanceOf(TrainingException.class).hasMessageContaining("outside the image");
        verify(annotationRepository, never()).saveAll(any());
    }

    @Test
    void saveAnnotations_rejectsDimensionsThatDifferFromRecordedSize() {
        loggedInAs(UserRole.SECURITY_ADMIN);
        incidentExists();
        incident.setImageWidth(1000);
        incident.setImageHeight(500);

        var request = new SaveAnnotationsRequest(640, 480,
                List.of(new AnnotationBoxRequest(IssueType.POTHOLE, 1.0, 1.0, 50.0, 50.0)));

        assertThatThrownBy(() -> service.saveAnnotations(incident.getIncidentId(), request))
                .isInstanceOf(TrainingException.class).hasMessageContaining("do not match");
    }

    @Test
    void nonSecurityAdmin_isRejectedBeforeAnyIncidentLookup() {
        loggedInAs(UserRole.CIVILIAN);

        assertThatThrownBy(() -> service.getAnnotations(incident.getIncidentId()))
                .isInstanceOf(TrainingException.class).hasMessageContaining("Only security admins");
        verify(incidentRepository, never()).findById(any());
    }

    @Test
    void flag_requiresAtLeastOneAnnotation() {
        loggedInAs(UserRole.SECURITY_ADMIN);
        incidentExists();
        when(annotationRepository.countByIncident_IncidentId(incident.getIncidentId())).thenReturn(0L);

        assertThatThrownBy(() -> service.setFlaggedForTraining(incident.getIncidentId(), true))
                .isInstanceOf(TrainingException.class).hasMessageContaining("at least one annotation");
    }

    @Test
    void flag_withNullToggles_andReflagsExportedIncident() {
        loggedInAs(UserRole.SECURITY_ADMIN);
        incidentExists();
        when(annotationRepository.countByIncident_IncidentId(incident.getIncidentId())).thenReturn(2L);

        assertThat(service.setFlaggedForTraining(incident.getIncidentId(), null).trainingStatus())
                .isEqualTo(TrainingStatus.FLAGGED);
        assertThat(incident.getTrainingFlaggedAt()).isNotNull();

        assertThat(service.setFlaggedForTraining(incident.getIncidentId(), null).trainingStatus())
                .isEqualTo(TrainingStatus.NOT_FLAGGED);

        incident.setTrainingStatus(TrainingStatus.EXPORTED);
        assertThat(service.setFlaggedForTraining(incident.getIncidentId(), true).trainingStatus())
                .isEqualTo(TrainingStatus.FLAGGED);
    }

    @Test
    void flag_explicitFalseLeavesExportedIncidentAlone() {
        loggedInAs(UserRole.SECURITY_ADMIN);
        incidentExists();
        incident.setTrainingStatus(TrainingStatus.EXPORTED);

        assertThat(service.setFlaggedForTraining(incident.getIncidentId(), false).trainingStatus())
                .isEqualTo(TrainingStatus.EXPORTED);
    }

    @Test
    void deleteLastAnnotation_unflagsFlaggedIncident() {
        loggedInAs(UserRole.SECURITY_ADMIN);
        IssueAnnotation annotation = box(incident, IssueType.POTHOLE);
        annotation.setId(UUID.randomUUID());
        incident.setTrainingStatus(TrainingStatus.FLAGGED);
        when(annotationRepository.findById(annotation.getId())).thenReturn(Optional.of(annotation));
        when(annotationRepository.countByIncident_IncidentId(incident.getIncidentId())).thenReturn(0L);

        service.deleteAnnotation(annotation.getId());

        verify(annotationRepository).delete(annotation);
        assertThat(incident.getTrainingStatus()).isEqualTo(TrainingStatus.NOT_FLAGGED);
    }

    @Test
    void export_marksIncludedIncidentsExported_andLeavesUnreadableOnesFlagged() {
        loggedInAs(UserRole.SECURITY_ADMIN);
        Incident readable = Incident.builder().incidentId(UUID.randomUUID()).imageUrl("http://h/ok.jpg")
                .trainingStatus(TrainingStatus.FLAGGED).build();
        Incident missing = Incident.builder().incidentId(UUID.randomUUID()).imageUrl("http://h/gone.jpg")
                .trainingStatus(TrainingStatus.FLAGGED).build();
        when(incidentRepository.findByTrainingStatusAndDeletedFalseOrderByTrainingFlaggedAtAsc(TrainingStatus.FLAGGED))
                .thenReturn(List.of(readable, missing));
        when(annotationRepository.findByIncident_IncidentIdIn(any()))
                .thenReturn(List.of(box(readable, IssueType.POTHOLE), box(missing, IssueType.CRACK)));
        when(imageStorageService.readImage("http://h/ok.jpg")).thenReturn(new byte[]{1, 2, 3});
        when(imageStorageService.readImage("http://h/gone.jpg"))
                .thenThrow(new UncheckedIOException(new IOException("nope")));

        byte[] zip = service.exportYoloDataset(null);

        assertThat(zip).isNotEmpty();
        assertThat(readable.getTrainingStatus()).isEqualTo(TrainingStatus.EXPORTED);
        assertThat(missing.getTrainingStatus()).isEqualTo(TrainingStatus.FLAGGED);
    }

    @Test
    void export_failsWhenNothingIsExportable() {
        loggedInAs(UserRole.SECURITY_ADMIN);
        when(incidentRepository.findByTrainingStatusAndDeletedFalseAndTrainingFlaggedAtGreaterThanEqualOrderByTrainingFlaggedAtAsc(
                any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.exportYoloDataset(LocalDateTime.now()))
                .isInstanceOf(TrainingException.class).hasMessageContaining("were found to export");
    }
}
