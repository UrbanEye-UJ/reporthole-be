package za.co.urbaneye.reporthole.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import za.co.urbaneye.reporthole.incident.config.IncidentProperties;
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
import za.co.urbaneye.reporthole.incident.entity.AssignmentStatus;
import za.co.urbaneye.reporthole.incident.entity.AssignmentWorkflow;
import za.co.urbaneye.reporthole.incident.entity.Incident;
import za.co.urbaneye.reporthole.incident.entity.IncidentSource;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.incident.repository.IncidentRepository;
import za.co.urbaneye.reporthole.incident.service.impl.IncidentServiceImpl;
import za.co.urbaneye.reporthole.incident.service.impl.IncidentSseService;

import java.util.Set;
import za.co.urbaneye.reporthole.incident.service.interfaces.ImageStorageService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class IncidentServiceImplTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private za.co.urbaneye.reporthole.incident.repository.IncidentReporterRepository incidentReporterRepository;

    @Mock
    private za.co.urbaneye.reporthole.incident.repository.AssignmentWorkflowRepository assignmentWorkflowRepository;

    @Mock
    private za.co.urbaneye.reporthole.incident.repository.AssignmentRepository assignmentRepository;

    @Mock
    private IncidentSseService incidentSseService;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private za.co.urbaneye.reporthole.user.repository.IUserAuthRepository userAuthRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private za.co.urbaneye.reporthole.notification.service.interfaces.IMailService mailService;

    @Mock
    private za.co.urbaneye.reporthole.notification.service.interfaces.INotificationService notificationService;

    @Mock
    private IncidentProperties incidentProperties;

    @Mock
    private za.co.urbaneye.reporthole.admin.municipality.repository.IMunicipalityRepository municipalityRepository;

    @Mock
    private za.co.urbaneye.reporthole.training.repository.IssueAnnotationRepository issueAnnotationRepository;

    @InjectMocks
    private IncidentServiceImpl incidentService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private void mockSecurityContext() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(USER_ID.toString());
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private User stubUser() {
        User user = new User();
        user.setUserId(USER_ID);
        user.setRole(UserRole.CIVILIAN);
        return user;
    }

    private IncidentRequestDTO buildRequest() {
        return new IncidentRequestDTO(IssueType.POTHOLE, "Big pothole on Main Road", IncidentSource.MANUAL, -26.2041, 28.0473, "base64data", false, null, null, null, null, null, null, null);
    }

    private IncidentRequestDTO buildRequest(Double confidence) {
        return new IncidentRequestDTO(IssueType.POTHOLE, "Big pothole on Main Road", IncidentSource.MANUAL, -26.2041, 28.0473, "base64data", false, null, confidence, null, null, null, null, null);
    }

    private IncidentRequestDTO buildRequest(LocalDateTime occurredAt) {
        return new IncidentRequestDTO(IssueType.POTHOLE, "Big pothole on Main Road", IncidentSource.MANUAL, -26.2041, 28.0473, "base64data", false, null, null, occurredAt, null, null, null, null);
    }

    private IncidentRequestDTO buildRequestWithBbox(Double confidence, Double x, Double y, Double w, Double h) {
        return new IncidentRequestDTO(IssueType.POTHOLE, "Big pothole on Main Road", IncidentSource.MANUAL, -26.2041, 28.0473, "base64data", false, null, confidence, null, x, y, w, h);
    }

    @Test
    void createIncident_savesAndReturnsDTOWithDuplicateFalse_whenNoDuplicate() {
        mockSecurityContext();
        User user = stubUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(incidentRepository.findNearestDuplicate(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(Optional.empty());
        when(imageStorageService.saveBase64Image(any())).thenReturn("http://img/path.jpg");

        Incident saved = new Incident();
        saved.setIncidentId(UUID.randomUUID());
        saved.setIncidentType(IssueType.POTHOLE);
        saved.setDescription("Big pothole on Main Road");
        saved.setSource(IncidentSource.MANUAL);
        saved.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        saved.setImageUrl("http://img/path.jpg");
        saved.setUser(user);
        when(incidentRepository.save(any())).thenReturn(saved);
        when(incidentReporterRepository.save(any())).thenReturn(null);

        IncidentResponseDTO result = incidentService.createIncident(buildRequest());

        assertThat(result.duplicate()).isFalse();
        assertThat(result.existingIncidentId()).isNull();
        assertThat(result.incidentType()).isEqualTo(IssueType.POTHOLE);
        assertThat(result.reporterCount()).isEqualTo(1);
        verify(incidentRepository).save(any());
        verify(incidentReporterRepository).save(any());
    }

    @Test
    void createIncident_usesClientSuppliedOccurredAt_whenPresent() {
        mockSecurityContext();
        User user = stubUser();
        LocalDateTime occurredAt = LocalDateTime.now().minusHours(2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(incidentRepository.findNearestDuplicate(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(Optional.empty());
        when(imageStorageService.saveBase64Image(any())).thenReturn("http://img/path.jpg");
        when(incidentRepository.save(any())).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            incident.setIncidentId(UUID.randomUUID());
            incident.setUser(user);
            return incident;
        });
        when(incidentReporterRepository.save(any())).thenReturn(null);

        IncidentResponseDTO result = incidentService.createIncident(buildRequest(occurredAt));

        assertThat(result.incidentDate()).isEqualTo(occurredAt);
    }

    @Test
    void createIncident_defaultsIncidentDateToNow_whenOccurredAtAbsent() {
        mockSecurityContext();
        User user = stubUser();
        LocalDateTime before = LocalDateTime.now();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(incidentRepository.findNearestDuplicate(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(Optional.empty());
        when(imageStorageService.saveBase64Image(any())).thenReturn("http://img/path.jpg");
        when(incidentRepository.save(any())).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            incident.setIncidentId(UUID.randomUUID());
            incident.setUser(user);
            return incident;
        });
        when(incidentReporterRepository.save(any())).thenReturn(null);

        IncidentResponseDTO result = incidentService.createIncident(buildRequest());
        LocalDateTime after = LocalDateTime.now();

        assertThat(result.incidentDate()).isBetween(before, after);
    }

    @Test
    void createIncident_autoApprovesAndVerifies_whenAiConfidenceAtOrAboveThreshold() {
        mockSecurityContext();
        User user = stubUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(incidentRepository.findNearestDuplicate(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(Optional.empty());
        when(imageStorageService.saveBase64Image(any())).thenReturn("http://img/path.jpg");
        when(incidentRepository.save(any())).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            incident.setIncidentId(UUID.randomUUID());
            incident.setUser(user);
            return incident;
        });
        when(incidentReporterRepository.save(any())).thenReturn(null);
        when(incidentProperties.getAiApprovalThreshold()).thenReturn(0.80);

        IncidentResponseDTO result = incidentService.createIncident(buildRequest(0.92));

        assertThat(result.aiGenerated()).isTrue();
        assertThat(result.aiConfidence()).isEqualTo(0.92);
        assertThat(result.status()).isEqualTo(AssignmentStatus.VERIFIED);
        verify(assignmentWorkflowRepository).save(argThat(workflow ->
                workflow.getStatus() == AssignmentStatus.VERIFIED));
    }

    @Test
    void createIncident_leavesAsReported_whenAiConfidenceBelowThreshold() {
        mockSecurityContext();
        User user = stubUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(incidentRepository.findNearestDuplicate(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(Optional.empty());
        when(imageStorageService.saveBase64Image(any())).thenReturn("http://img/path.jpg");
        when(incidentRepository.save(any())).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            incident.setIncidentId(UUID.randomUUID());
            incident.setUser(user);
            return incident;
        });
        when(incidentReporterRepository.save(any())).thenReturn(null);
        when(incidentProperties.getAiApprovalThreshold()).thenReturn(0.80);

        IncidentResponseDTO result = incidentService.createIncident(buildRequest(0.68));

        assertThat(result.aiGenerated()).isTrue();
        assertThat(result.aiConfidence()).isEqualTo(0.68);
        assertThat(result.status()).isEqualTo(AssignmentStatus.REPORTED);
        verify(assignmentWorkflowRepository, never()).save(any());
    }

    @Test
    void createIncident_autoCreatesTrainingAnnotation_whenAiGeneratedWithBbox() {
        mockSecurityContext();
        User user = stubUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(incidentRepository.findNearestDuplicate(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(Optional.empty());
        when(imageStorageService.saveBase64Image(any())).thenReturn("http://img/path.jpg");
        when(incidentRepository.save(any())).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            if (incident.getIncidentId() == null) incident.setIncidentId(UUID.randomUUID());
            incident.setUser(user);
            return incident;
        });
        when(incidentReporterRepository.save(any())).thenReturn(null);
        when(incidentProperties.getAiApprovalThreshold()).thenReturn(0.80);

        incidentService.createIncident(buildRequestWithBbox(0.92, 0.5, 0.4, 0.1, 0.2));

        verify(issueAnnotationRepository).save(argThat(annotation ->
                annotation.getClassLabel() == IssueType.POTHOLE
                        && annotation.getXCenter() == 0.5
                        && annotation.getYCenter() == 0.4
                        && annotation.getWidth() == 0.1
                        && annotation.getHeight() == 0.2
                        && annotation.isAutoGenerated()
                        && annotation.getAnnotatedBy() == null));
        // incidentRepository.save is called twice: once to persist the new incident, once more
        // to persist the FLAGGED training status set by the auto-annotation step.
        verify(incidentRepository, times(2)).save(argThat(i -> true));
    }

    @Test
    void createIncident_doesNotCreateTrainingAnnotation_whenBboxAbsent() {
        mockSecurityContext();
        User user = stubUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(incidentRepository.findNearestDuplicate(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(Optional.empty());
        when(imageStorageService.saveBase64Image(any())).thenReturn("http://img/path.jpg");
        when(incidentRepository.save(any())).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            incident.setIncidentId(UUID.randomUUID());
            incident.setUser(user);
            return incident;
        });
        when(incidentReporterRepository.save(any())).thenReturn(null);
        when(incidentProperties.getAiApprovalThreshold()).thenReturn(0.80);

        incidentService.createIncident(buildRequest(0.92));

        verify(issueAnnotationRepository, never()).save(any());
    }

    @Test
    void createIncident_doesNotSetAiFields_whenConfidenceIsNull() {
        mockSecurityContext();
        User user = stubUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(incidentRepository.findNearestDuplicate(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(Optional.empty());
        when(imageStorageService.saveBase64Image(any())).thenReturn("http://img/path.jpg");
        when(incidentRepository.save(any())).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            incident.setIncidentId(UUID.randomUUID());
            incident.setUser(user);
            return incident;
        });
        when(incidentReporterRepository.save(any())).thenReturn(null);

        IncidentResponseDTO result = incidentService.createIncident(buildRequest());

        assertThat(result.aiGenerated()).isFalse();
        assertThat(result.aiConfidence()).isNull();
        assertThat(result.status()).isEqualTo(AssignmentStatus.REPORTED);
        verify(assignmentWorkflowRepository, never()).save(any());
    }

    @Test
    void createIncident_returnsDuplicateDTOWithoutSaving_whenDuplicateFound() {
        mockSecurityContext();
        User user = stubUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UUID existingId = UUID.randomUUID();
        Incident existing = new Incident();
        existing.setIncidentId(existingId);
        existing.setIncidentType(IssueType.POTHOLE);
        existing.setDescription("Existing pothole");
        existing.setSource(IncidentSource.MANUAL);
        existing.setIncidentDate(LocalDateTime.now().minusDays(1));
        existing.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        existing.setUser(user);

        when(incidentRepository.findNearestDuplicate(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(Optional.of(existing));
        when(incidentReporterRepository.existsByIncident_IncidentIdAndUser_UserId(existingId, USER_ID))
                .thenReturn(false);

        IncidentResponseDTO result = incidentService.createIncident(buildRequest());

        assertThat(result.duplicate()).isTrue();
        assertThat(result.alreadyConfirmed()).isFalse();
        assertThat(result.existingIncidentId()).isEqualTo(existingId);
        verify(incidentRepository, never()).save(any());
        verify(imageStorageService, never()).saveBase64Image(any());
    }

    @Test
    void confirmDuplicate_incrementsCountAndLinksReporter() {
        mockSecurityContext();
        UUID id = UUID.randomUUID();
        User user = stubUser();

        Incident incident = new Incident();
        incident.setIncidentId(id);
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setDescription("Pothole");
        incident.setSource(IncidentSource.MANUAL);
        incident.setIncidentDate(LocalDateTime.now());
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(user);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(incidentReporterRepository.existsByIncident_IncidentIdAndUser_UserId(id, USER_ID)).thenReturn(false);
        when(incidentReporterRepository.countByIncident_IncidentId(id)).thenReturn(2);
        when(incidentReporterRepository.findUserIdsByIncidentId(id)).thenReturn(List.of(USER_ID));

        IncidentResponseDTO result = incidentService.confirmDuplicate(id);

        verify(incidentRepository).incrementReportCount(id);
        verify(incidentReporterRepository).save(any());
        verify(incidentSseService).pushIncidentUpdate(eq(id), eq(Set.of(USER_ID)));
        assertThat(result.duplicate()).isTrue();
        assertThat(result.existingIncidentId()).isEqualTo(id);
        assertThat(result.reporterCount()).isEqualTo(2);
    }

    @Test
    void confirmDuplicate_doesNotAddDuplicateReporter_whenUserAlreadyLinked() {
        mockSecurityContext();
        UUID id = UUID.randomUUID();
        User user = stubUser();

        Incident incident = new Incident();
        incident.setIncidentId(id);
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(user);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(incidentReporterRepository.existsByIncident_IncidentIdAndUser_UserId(id, USER_ID)).thenReturn(true);
        when(incidentReporterRepository.countByIncident_IncidentId(id)).thenReturn(1);
        when(incidentReporterRepository.findUserIdsByIncidentId(id)).thenReturn(List.of(USER_ID));

        incidentService.confirmDuplicate(id);

        verify(incidentReporterRepository, never()).save(any());
        verify(incidentRepository, never()).incrementReportCount(any());
    }

    @Test
    void getMyIncidents_returnsListForAuthenticatedUser() {
        mockSecurityContext();

        Incident incident = new Incident();
        incident.setIncidentId(UUID.randomUUID());
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setDescription("Crack");
        incident.setSource(IncidentSource.MANUAL);
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(stubUser());

        when(incidentRepository.findAllReportedByUser(USER_ID)).thenReturn(List.of(incident));

        List<IncidentResponseDTO> result = incidentService.getMyIncidents();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().incidentType()).isEqualTo(IssueType.POTHOLE);
    }

    @Test
    void getRecentIncidents_filtersByMunicipality_whenMunicipalityIdProvided() {
        // SECURITY_ADMIN map view: an explicit municipalityId bypasses the caller-role-based
        // resolveAdminMunicipality() scoping entirely and goes straight to the dedicated query.
        UUID municipalityId = UUID.randomUUID();
        Incident incident = new Incident();
        incident.setIncidentId(UUID.randomUUID());
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(stubUser());

        when(incidentRepository.findByDeletedFalseAndMunicipality_IdOrderByIncidentDateDesc(eq(municipalityId), any()))
                .thenReturn(List.of(incident));

        List<IncidentResponseDTO> result = incidentService.getRecentIncidents(10, municipalityId);

        assertThat(result).hasSize(1);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getIncidentById_returnsDTO_whenIncidentExists() {
        UUID id = UUID.randomUUID();
        User user = stubUser();

        Incident incident = new Incident();
        incident.setIncidentId(id);
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setDescription("Deep pothole");
        incident.setSource(IncidentSource.MANUAL);
        incident.setIncidentDate(LocalDateTime.now());
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setImageUrl("http://img/test.jpg");
        incident.setUser(user);
        incident.setReportCount(3);

        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(incidentReporterRepository.countByIncident_IncidentId(id)).thenReturn(2);

        IncidentResponseDTO result = incidentService.getIncidentById(id);

        assertThat(result.incidentId()).isEqualTo(id);
        assertThat(result.incidentType()).isEqualTo(IssueType.POTHOLE);
        assertThat(result.description()).isEqualTo("Deep pothole");
        assertThat(result.reportCount()).isEqualTo(3);
        assertThat(result.reporterCount()).isEqualTo(2);
        assertThat(result.imageUrl()).isEqualTo("http://img/test.jpg");
        assertThat(result.duplicate()).isFalse();
    }

    @Test
    void getIncidentById_includesWorkflowHistory() {
        // Regression test: getIncidentById used to build its DTO by hand and skip workflow
        // history entirely, so rejection reasons and status notes never reached GET /incidents/{id}.
        UUID id = UUID.randomUUID();
        User user = stubUser();

        Incident incident = new Incident();
        incident.setIncidentId(id);
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setSource(IncidentSource.MANUAL);
        incident.setIncidentDate(LocalDateTime.now());
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(user);

        AssignmentWorkflow rejection = new AssignmentWorkflow();
        rejection.setIncident(incident);
        rejection.setStatus(AssignmentStatus.VERIFIED);
        rejection.setNotes("Rejected by Jane Doe — needs reassignment. Reason: Wrong address");

        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(incidentReporterRepository.countByIncident_IncidentId(id)).thenReturn(1);
        when(assignmentWorkflowRepository.findAllByIncident_IncidentIdOrderByUpdatedDateAsc(id))
                .thenReturn(List.of(rejection));

        IncidentResponseDTO result = incidentService.getIncidentById(id);

        assertThat(result.workflowHistory()).hasSize(1);
        assertThat(result.workflowHistory().getFirst().notes()).contains("Reason: Wrong address");
    }

    @Test
    void getIncidentById_throwsRuntimeException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(incidentRepository.findById(id)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> incidentService.getIncidentById(id)
        );
    }

    @Test
    void verifyIncident_marksReportedIncidentAsVerified_whenCallerIsAdmin() {
        mockSecurityContext();
        UUID id = UUID.randomUUID();

        User admin = stubUser();
        admin.setRole(UserRole.ADMIN);
        admin.setFirstName("Ada");
        admin.setLastName("Min");

        Incident incident = new Incident();
        incident.setIncidentId(id);
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(admin);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(admin));
        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(assignmentWorkflowRepository.findFirstByIncident_IncidentIdOrderByUpdatedDateDesc(id))
                .thenReturn(Optional.empty());
        when(incidentReporterRepository.findUserIdsByIncidentId(id)).thenReturn(List.of(USER_ID));

        incidentService.verifyIncident(id);

        verify(assignmentWorkflowRepository).save(any());
        verify(incidentSseService).pushIncidentUpdate(eq(id), eq(Set.of(USER_ID)));
    }

    @Test
    void verifyIncident_throws_whenCallerIsNotAdmin() {
        mockSecurityContext();
        UUID id = UUID.randomUUID();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubUser()));

        org.junit.jupiter.api.Assertions.assertThrows(
                za.co.urbaneye.reporthole.incident.exception.AssignmentException.class,
                () -> incidentService.verifyIncident(id)
        );
        verify(assignmentWorkflowRepository, never()).save(any());
    }

    @Test
    void verifyIncident_throws_whenIncidentAlreadyVerified() {
        mockSecurityContext();
        UUID id = UUID.randomUUID();

        User admin = stubUser();
        admin.setRole(UserRole.ADMIN);

        Incident incident = new Incident();
        incident.setIncidentId(id);
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(admin);

        za.co.urbaneye.reporthole.incident.entity.AssignmentWorkflow existingWorkflow =
                new za.co.urbaneye.reporthole.incident.entity.AssignmentWorkflow();
        existingWorkflow.setStatus(za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.VERIFIED);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(admin));
        when(incidentRepository.findById(id)).thenReturn(Optional.of(incident));
        when(assignmentWorkflowRepository.findFirstByIncident_IncidentIdOrderByUpdatedDateDesc(id))
                .thenReturn(Optional.of(existingWorkflow));

        org.junit.jupiter.api.Assertions.assertThrows(
                za.co.urbaneye.reporthole.incident.exception.AssignmentException.class,
                () -> incidentService.verifyIncident(id)
        );
        verify(assignmentWorkflowRepository, never()).save(any());
    }

    @Test
    void assignIncident_savesAssignmentAndEmailsContractor_whenCallerIsAdmin() {
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();
        UUID contractorId = UUID.randomUUID();

        User admin = stubUser();
        admin.setRole(UserRole.ADMIN);

        User contractor = new User();
        contractor.setUserId(contractorId);
        contractor.setRole(UserRole.CONTRACTOR);
        contractor.setFirstName("Con");
        contractor.setLastName("Tractor");
        contractor.setSpecialisations(java.util.Set.of(IssueType.POTHOLE));

        Incident incident = new Incident();
        incident.setIncidentId(incidentId);
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setLocationAddress("Main Road, Johannesburg");
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(admin);

        za.co.urbaneye.reporthole.user.entity.UserAuth contractorAuth =
                za.co.urbaneye.reporthole.user.entity.UserAuth.builder()
                        .authId(contractorId)
                        .email("con.tractor@example.com")
                        .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(admin));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(userRepository.findById(contractorId)).thenReturn(Optional.of(contractor));
        when(userAuthRepository.findById(contractorId)).thenReturn(Optional.of(contractorAuth));
        when(incidentReporterRepository.countByIncident_IncidentId(incidentId)).thenReturn(1);
        when(assignmentWorkflowRepository.findFirstByIncident_IncidentIdOrderByUpdatedDateDesc(incidentId))
                .thenReturn(Optional.empty());

        incidentService.assignIncident(incidentId, contractorId);

        verify(assignmentRepository).save(argThat(a ->
                a.getStatus() == za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.ASSIGNED
                        && a.getContractor() == contractor));
        verify(mailService).sendJobAssignedEmail(
                eq("con.tractor@example.com"), eq("Con"), eq("POTHOLE"), eq("Main Road, Johannesburg"), eq(incidentId.toString()));
    }

    @Test
    void assignIncident_succeeds_whenAdminIncidentAndContractorShareSameMunicipality() {
        // Regression test: Municipality had no equals()/hashCode(), so two separately loaded
        // JPA instances of the *same* municipality row compared unequal, and the boundary
        // check in assignIncident rejected every same-municipality assignment.
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();
        UUID contractorId = UUID.randomUUID();
        UUID municipalityId = UUID.randomUUID();

        za.co.urbaneye.reporthole.admin.municipality.entity.Municipality adminMunicipality =
                za.co.urbaneye.reporthole.admin.municipality.entity.Municipality.builder().id(municipalityId).build();
        za.co.urbaneye.reporthole.admin.municipality.entity.Municipality incidentMunicipality =
                za.co.urbaneye.reporthole.admin.municipality.entity.Municipality.builder().id(municipalityId).build();
        za.co.urbaneye.reporthole.admin.municipality.entity.Municipality contractorMunicipality =
                za.co.urbaneye.reporthole.admin.municipality.entity.Municipality.builder().id(municipalityId).build();

        User admin = stubUser();
        admin.setRole(UserRole.ADMIN);
        admin.setMunicipality(adminMunicipality);

        User contractor = new User();
        contractor.setUserId(contractorId);
        contractor.setRole(UserRole.CONTRACTOR);
        contractor.setFirstName("Con");
        contractor.setLastName("Tractor");
        contractor.setSpecialisations(java.util.Set.of(IssueType.POTHOLE));
        contractor.setMunicipality(contractorMunicipality);

        Incident incident = new Incident();
        incident.setIncidentId(incidentId);
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setLocationAddress("Main Road, Johannesburg");
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(admin);
        incident.setMunicipality(incidentMunicipality);

        za.co.urbaneye.reporthole.user.entity.UserAuth contractorAuth =
                za.co.urbaneye.reporthole.user.entity.UserAuth.builder()
                        .authId(contractorId)
                        .email("con.tractor@example.com")
                        .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(admin));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(userRepository.findById(contractorId)).thenReturn(Optional.of(contractor));
        when(userAuthRepository.findById(contractorId)).thenReturn(Optional.of(contractorAuth));
        when(incidentReporterRepository.countByIncident_IncidentId(incidentId)).thenReturn(1);
        when(assignmentWorkflowRepository.findFirstByIncident_IncidentIdOrderByUpdatedDateDesc(incidentId))
                .thenReturn(Optional.empty());

        incidentService.assignIncident(incidentId, contractorId);

        verify(assignmentRepository).save(argThat(a ->
                a.getStatus() == za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.ASSIGNED
                        && a.getContractor() == contractor));
    }

    @Test
    void assignIncident_throws_whenCallerIsNotAdmin() {
        mockSecurityContext();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubUser()));

        org.junit.jupiter.api.Assertions.assertThrows(
                za.co.urbaneye.reporthole.incident.exception.AssignmentException.class,
                () -> incidentService.assignIncident(UUID.randomUUID(), UUID.randomUUID())
        );
        verify(assignmentRepository, never()).save(any());
        verify(mailService, never()).sendJobAssignedEmail(any(), any(), any(), any(), any());
    }

    @Test
    void assignIncident_throws_whenSelectedUserIsNotContractor() {
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        User admin = stubUser();
        admin.setRole(UserRole.ADMIN);

        User civilian = new User();
        civilian.setUserId(otherUserId);
        civilian.setRole(UserRole.CIVILIAN);

        Incident incident = new Incident();
        incident.setIncidentId(incidentId);
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(admin);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(admin));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(civilian));

        org.junit.jupiter.api.Assertions.assertThrows(
                za.co.urbaneye.reporthole.incident.exception.AssignmentException.class,
                () -> incidentService.assignIncident(incidentId, otherUserId)
        );
        verify(assignmentRepository, never()).save(any());
        verify(mailService, never()).sendJobAssignedEmail(any(), any(), any(), any(), any());
    }

    @Test
    void assignIncident_throws_whenContractorNotSpecialisedInIssueType() {
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();
        UUID contractorId = UUID.randomUUID();

        User admin = stubUser();
        admin.setRole(UserRole.ADMIN);

        User contractor = new User();
        contractor.setUserId(contractorId);
        contractor.setRole(UserRole.CONTRACTOR);
        contractor.setSpecialisations(java.util.Set.of(IssueType.CRACK));

        Incident incident = new Incident();
        incident.setIncidentId(incidentId);
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(admin);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(admin));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(userRepository.findById(contractorId)).thenReturn(Optional.of(contractor));

        org.junit.jupiter.api.Assertions.assertThrows(
                za.co.urbaneye.reporthole.incident.exception.AssignmentException.class,
                () -> incidentService.assignIncident(incidentId, contractorId)
        );
        verify(assignmentRepository, never()).save(any());
        verify(mailService, never()).sendJobAssignedEmail(any(), any(), any(), any(), any());
    }

    @Test
    void reportStillUnresolved_revertsToVerifiedAndLinksReporter_whenIncidentIsResolved() {
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();
        User user = stubUser();

        Incident incident = new Incident();
        incident.setIncidentId(incidentId);
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(user);

        za.co.urbaneye.reporthole.incident.entity.AssignmentWorkflow resolvedWorkflow =
                new za.co.urbaneye.reporthole.incident.entity.AssignmentWorkflow();
        resolvedWorkflow.setStatus(za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.RESOLVED);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(assignmentWorkflowRepository.findFirstByIncident_IncidentIdOrderByUpdatedDateDesc(incidentId))
                .thenReturn(Optional.of(resolvedWorkflow));
        when(incidentReporterRepository.existsByIncident_IncidentIdAndUser_UserId(incidentId, USER_ID)).thenReturn(false);
        when(incidentReporterRepository.findUserIdsByIncidentId(incidentId)).thenReturn(List.of(USER_ID));

        incidentService.reportStillUnresolved(incidentId);

        verify(assignmentWorkflowRepository).save(argThat(w ->
                w.getStatus() == za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.VERIFIED));
        verify(incidentReporterRepository).save(any());
        verify(incidentSseService).pushIncidentUpdate(eq(incidentId), eq(Set.of(USER_ID)));
    }

    @Test
    void reportStillUnresolved_throws_whenIncidentIsNotResolved() {
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();
        User user = stubUser();

        Incident incident = new Incident();
        incident.setIncidentId(incidentId);
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(user);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(assignmentWorkflowRepository.findFirstByIncident_IncidentIdOrderByUpdatedDateDesc(incidentId))
                .thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                za.co.urbaneye.reporthole.incident.exception.AssignmentException.class,
                () -> incidentService.reportStillUnresolved(incidentId)
        );
        verify(assignmentWorkflowRepository, never()).save(any());
    }

    @Test
    void getNearbyIncidents_mapsRepositoryResultsToDTOs() {
        Incident incident = new Incident();
        incident.setIncidentId(UUID.randomUUID());
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(stubUser());

        when(incidentRepository.findNearby(-26.2041, 28.0473, 1000.0)).thenReturn(List.of(incident));

        List<IncidentResponseDTO> result = incidentService.getNearbyIncidents(-26.2041, 28.0473, 1000.0);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().incidentType()).isEqualTo(IssueType.POTHOLE);
    }

    @Test
    void searchIncidents_mapsPageToResponse() {
        UUID municipalityId = UUID.randomUUID();
        Incident incident = new Incident();
        incident.setIncidentId(UUID.randomUUID());
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(stubUser());

        // PageImpl offset+pageSize > total (which is almost always true for a partial page)
        // silently recomputes total to offset+content.size() — page 0 keeps the given total
        // as-is since offset is 0, so use page 0 here to assert the pass-through total exactly.
        org.springframework.data.domain.Page<Incident> page =
                new org.springframework.data.domain.PageImpl<>(
                        List.of(incident), org.springframework.data.domain.PageRequest.of(0, 25), 30);
        when(incidentRepository.search(eq(municipalityId), eq(IssueType.POTHOLE), any()))
                .thenReturn(page);

        za.co.urbaneye.reporthole.incident.dto.IncidentPageResponse result =
                incidentService.searchIncidents(municipalityId, IssueType.POTHOLE, 0, 25);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().incidentType()).isEqualTo(IssueType.POTHOLE);
        assertThat(result.totalElements()).isEqualTo(30);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(25);
    }

    private za.co.urbaneye.reporthole.incident.entity.Assignment buildPendingAssignment(UUID incidentId, User contractor) {
        Incident incident = new Incident();
        incident.setIncidentId(incidentId);
        incident.setIncidentType(IssueType.POTHOLE);
        incident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        incident.setUser(contractor);

        return za.co.urbaneye.reporthole.incident.entity.Assignment.builder()
                .status(za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.ASSIGNED)
                .incident(incident)
                .contractor(contractor)
                .build();
    }

    @Test
    void acceptAssignment_marksInProgress_whenAssignmentIsPending() {
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();
        User contractor = stubUser();
        contractor.setRole(UserRole.CONTRACTOR);
        contractor.setFirstName("Con");
        contractor.setLastName("Tractor");

        za.co.urbaneye.reporthole.incident.entity.Assignment assignment = buildPendingAssignment(incidentId, contractor);

        when(assignmentRepository.findByIncident_IncidentIdAndContractor_UserId(incidentId, USER_ID))
                .thenReturn(Optional.of(assignment));
        when(incidentReporterRepository.findUserIdsByIncidentId(incidentId)).thenReturn(List.of(USER_ID));

        incidentService.acceptAssignment(incidentId);

        assertThat(assignment.getStatus()).isEqualTo(za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.IN_PROGRESS);
        verify(assignmentRepository).save(assignment);
        verify(assignmentWorkflowRepository).save(any());
        verify(incidentSseService).pushIncidentUpdate(eq(incidentId), eq(Set.of(USER_ID)));
    }

    @Test
    void acceptAssignment_throws_whenAssignmentNotPending() {
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();
        User contractor = stubUser();

        za.co.urbaneye.reporthole.incident.entity.Assignment assignment = buildPendingAssignment(incidentId, contractor);
        assignment.setStatus(za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.IN_PROGRESS);

        when(assignmentRepository.findByIncident_IncidentIdAndContractor_UserId(incidentId, USER_ID))
                .thenReturn(Optional.of(assignment));

        org.junit.jupiter.api.Assertions.assertThrows(
                za.co.urbaneye.reporthole.incident.exception.AssignmentException.class,
                () -> incidentService.acceptAssignment(incidentId)
        );
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void rejectAssignment_deletesAssignmentAndRevertsToVerified_whenAssignmentIsPending() {
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();
        User contractor = stubUser();
        contractor.setRole(UserRole.CONTRACTOR);
        contractor.setFirstName("Con");
        contractor.setLastName("Tractor");

        za.co.urbaneye.reporthole.incident.entity.Assignment assignment = buildPendingAssignment(incidentId, contractor);

        when(assignmentRepository.findByIncident_IncidentIdAndContractor_UserId(incidentId, USER_ID))
                .thenReturn(Optional.of(assignment));
        when(incidentReporterRepository.findUserIdsByIncidentId(incidentId)).thenReturn(List.of(USER_ID));

        incidentService.rejectAssignment(incidentId, new za.co.urbaneye.reporthole.incident.dto.RejectAssignmentRequest("Wrong contractor for this issue type"));

        verify(assignmentRepository).delete(assignment);
        verify(assignmentWorkflowRepository).save(argThat(workflow ->
                workflow.getStatus() == za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.VERIFIED
                        && workflow.getNotes().contains("Wrong contractor for this issue type")));
        verify(incidentSseService).pushIncidentUpdate(eq(incidentId), eq(Set.of(USER_ID)));
    }

    @Test
    void addProgressUpdate_savesWorkflowEntryAndPushesSSE_whenInProgress() {
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();
        User contractor = stubUser();
        contractor.setRole(UserRole.CONTRACTOR);
        contractor.setFirstName("Con");
        contractor.setLastName("Tractor");

        za.co.urbaneye.reporthole.incident.entity.Assignment assignment = buildPendingAssignment(incidentId, contractor);
        assignment.setStatus(za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.IN_PROGRESS);

        when(assignmentRepository.findByIncident_IncidentIdAndContractor_UserId(incidentId, USER_ID))
                .thenReturn(Optional.of(assignment));
        when(incidentReporterRepository.findUserIdsByIncidentId(incidentId)).thenReturn(List.of(USER_ID));
        when(assignmentWorkflowRepository.findAllByIncident_IncidentIdOrderByUpdatedDateAsc(incidentId))
                .thenReturn(List.of());

        IncidentResponseDTO result = incidentService.addProgressUpdate(incidentId, "Pothole filled halfway");

        verify(assignmentWorkflowRepository).save(argThat(w ->
                w.getStatus() == za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.IN_PROGRESS
                && "Pothole filled halfway".equals(w.getNotes())
        ));
        verify(incidentSseService).pushIncidentUpdate(eq(incidentId), eq(Set.of(USER_ID)));
        assertThat(result).isNotNull();
    }

    @Test
    void addProgressUpdate_throws_whenAssignmentNotInProgress() {
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();
        User contractor = stubUser();

        za.co.urbaneye.reporthole.incident.entity.Assignment assignment = buildPendingAssignment(incidentId, contractor);
        // status is still ASSIGNED, not IN_PROGRESS

        when(assignmentRepository.findByIncident_IncidentIdAndContractor_UserId(incidentId, USER_ID))
                .thenReturn(Optional.of(assignment));

        org.junit.jupiter.api.Assertions.assertThrows(
                za.co.urbaneye.reporthole.incident.exception.AssignmentException.class,
                () -> incidentService.addProgressUpdate(incidentId, "Some note")
        );
        verify(assignmentWorkflowRepository, never()).save(any());
    }

    @Test
    void addProgressUpdate_throws_whenAssignmentNotFound() {
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();

        when(assignmentRepository.findByIncident_IncidentIdAndContractor_UserId(incidentId, USER_ID))
                .thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                za.co.urbaneye.reporthole.incident.exception.AssignmentException.class,
                () -> incidentService.addProgressUpdate(incidentId, "Some note")
        );
    }

    @Test
    void rejectAssignment_throws_whenAssignmentNotPending() {
        mockSecurityContext();
        UUID incidentId = UUID.randomUUID();
        User contractor = stubUser();

        za.co.urbaneye.reporthole.incident.entity.Assignment assignment = buildPendingAssignment(incidentId, contractor);
        assignment.setStatus(za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.RESOLVED);

        when(assignmentRepository.findByIncident_IncidentIdAndContractor_UserId(incidentId, USER_ID))
                .thenReturn(Optional.of(assignment));

        org.junit.jupiter.api.Assertions.assertThrows(
                za.co.urbaneye.reporthole.incident.exception.AssignmentException.class,
                () -> incidentService.rejectAssignment(incidentId, new za.co.urbaneye.reporthole.incident.dto.RejectAssignmentRequest("Too busy"))
        );
        verify(assignmentRepository, never()).delete(any());
    }

    @Test
    void getIncidentsPendingAiReview_returnsOnlyReportedAiIncidents_whenCallerIsAdmin() {
        mockSecurityContext();
        User admin = stubUser();
        admin.setRole(UserRole.ADMIN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(admin));

        Incident aiIncident = new Incident();
        aiIncident.setIncidentId(UUID.randomUUID());
        aiIncident.setIncidentType(IssueType.POTHOLE);
        aiIncident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        aiIncident.setUser(admin);
        aiIncident.setAiGenerated(true);
        aiIncident.setAiConfidence(0.7);

        when(incidentRepository.findByAiGeneratedTrueAndDeletedFalseOrderByIncidentDateDesc())
                .thenReturn(List.of(aiIncident));
        when(assignmentWorkflowRepository.findFirstByIncident_IncidentIdOrderByUpdatedDateDesc(aiIncident.getIncidentId()))
                .thenReturn(Optional.empty());
        when(incidentReporterRepository.countByIncident_IncidentId(aiIncident.getIncidentId())).thenReturn(0);

        List<IncidentResponseDTO> result = incidentService.getIncidentsPendingAiReview();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().aiConfidence()).isEqualTo(0.7);
        assertThat(result.getFirst().status()).isEqualTo(AssignmentStatus.REPORTED);
    }

    @Test
    void getIncidentsPendingAiReview_excludesAlreadyVerifiedAiIncidents() {
        mockSecurityContext();
        User admin = stubUser();
        admin.setRole(UserRole.ADMIN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(admin));

        Incident verifiedAiIncident = new Incident();
        verifiedAiIncident.setIncidentId(UUID.randomUUID());
        verifiedAiIncident.setLocation(GF.createPoint(new Coordinate(28.0473, -26.2041)));
        verifiedAiIncident.setUser(admin);
        verifiedAiIncident.setAiGenerated(true);
        verifiedAiIncident.setAiConfidence(0.95);

        za.co.urbaneye.reporthole.incident.entity.AssignmentWorkflow verifiedWorkflow =
                new za.co.urbaneye.reporthole.incident.entity.AssignmentWorkflow();
        verifiedWorkflow.setStatus(AssignmentStatus.VERIFIED);

        when(incidentRepository.findByAiGeneratedTrueAndDeletedFalseOrderByIncidentDateDesc())
                .thenReturn(List.of(verifiedAiIncident));
        when(assignmentWorkflowRepository.findFirstByIncident_IncidentIdOrderByUpdatedDateDesc(verifiedAiIncident.getIncidentId()))
                .thenReturn(Optional.of(verifiedWorkflow));

        List<IncidentResponseDTO> result = incidentService.getIncidentsPendingAiReview();

        assertThat(result).isEmpty();
    }

    @Test
    void getIncidentsPendingAiReview_throws_whenCallerIsNotAdmin() {
        mockSecurityContext();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubUser()));

        org.junit.jupiter.api.Assertions.assertThrows(
                za.co.urbaneye.reporthole.incident.exception.AssignmentException.class,
                () -> incidentService.getIncidentsPendingAiReview()
        );
    }
}
