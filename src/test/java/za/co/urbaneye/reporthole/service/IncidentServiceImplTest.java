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
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
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
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;

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
    private IncidentSseService incidentSseService;

    @Mock
    private IUserAuthRepository userRepository;

    @Mock
    private ImageStorageService imageStorageService;

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
        IncidentRequestDTO req = new IncidentRequestDTO();
        req.setIncidentType(IssueType.POTHOLE);
        req.setDescription("Big pothole on Main Road");
        req.setSource(IncidentSource.MANUAL);
        req.setLatitude(-26.2041);
        req.setLongitude(28.0473);
        req.setImageBase64("base64data");
        return req;
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

        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getExistingIncidentId()).isNull();
        assertThat(result.getIncidentType()).isEqualTo(IssueType.POTHOLE);
        assertThat(result.getReporterCount()).isEqualTo(1);
        verify(incidentRepository).save(any());
        verify(incidentReporterRepository).save(any());
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

        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.isAlreadyConfirmed()).isFalse();
        assertThat(result.getExistingIncidentId()).isEqualTo(existingId);
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
        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.getExistingIncidentId()).isEqualTo(id);
        assertThat(result.getReporterCount()).isEqualTo(2);
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
        assertThat(result.getFirst().getIncidentType()).isEqualTo(IssueType.POTHOLE);
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

        assertThat(result.getIncidentId()).isEqualTo(id);
        assertThat(result.getIncidentType()).isEqualTo(IssueType.POTHOLE);
        assertThat(result.getDescription()).isEqualTo("Deep pothole");
        assertThat(result.getReportCount()).isEqualTo(3);
        assertThat(result.getReporterCount()).isEqualTo(2);
        assertThat(result.getImageUrl()).isEqualTo("http://img/test.jpg");
        assertThat(result.isDuplicate()).isFalse();
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
}
