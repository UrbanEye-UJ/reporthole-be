package za.co.urbaneye.reporthole.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import za.co.urbaneye.reporthole.admin.security.service.interfaces.IAuditLogService;
import za.co.urbaneye.reporthole.incident.dto.IncidentCommentResponse;
import za.co.urbaneye.reporthole.incident.entity.Incident;
import za.co.urbaneye.reporthole.incident.entity.IncidentComment;
import za.co.urbaneye.reporthole.incident.repository.IncidentCommentRepository;
import za.co.urbaneye.reporthole.incident.repository.IncidentRepository;
import za.co.urbaneye.reporthole.incident.service.impl.IncidentCommentServiceImpl;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentCommentServiceTest {

    @Mock private IncidentCommentRepository commentRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private IUserRepository userRepository;
    @Mock private IAuditLogService auditLogService;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private IncidentCommentServiceImpl commentService;

    private final UUID incidentId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private Incident incident;
    private User author;

    @BeforeEach
    void setUp() {
        incident = new Incident();

        author = new User();
        author.setFirstName("Jane");
        author.setLastName("Doe");
        author.setRole(UserRole.CIVILIAN);

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(userId.toString());
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getComments_returnsMappedList() {
        IncidentComment comment = IncidentComment.builder()
                .id(UUID.randomUUID())
                .incident(incident)
                .author(author)
                .content("Pothole is very deep")
                .createdAt(LocalDateTime.now())
                .build();

        when(commentRepository.findByIncident_IncidentIdOrderByCreatedAtAsc(incidentId))
                .thenReturn(List.of(comment));

        List<IncidentCommentResponse> result = commentService.getComments(incidentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("Pothole is very deep");
        // Comments are visible to any authenticated user, so a civilian author's name is masked.
        assertThat(result.get(0).authorName()).isEqualTo("Jane D.");
        assertThat(result.get(0).authorRole()).isEqualTo("CIVILIAN");
    }

    @Test
    void addComment_persistsAndReturnsResponse() {
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));

        IncidentComment saved = IncidentComment.builder()
                .id(UUID.randomUUID())
                .incident(incident)
                .author(author)
                .content("Fixed now")
                .createdAt(LocalDateTime.now())
                .build();
        when(commentRepository.save(any())).thenReturn(saved);

        IncidentCommentResponse result = commentService.addComment(incidentId, "Fixed now");

        assertThat(result.content()).isEqualTo("Fixed now");
        // Comments are visible to any authenticated user, so a civilian author's name is masked.
        assertThat(result.authorName()).isEqualTo("Jane D.");
        verify(commentRepository).save(any(IncidentComment.class));
    }

    @Test
    void addComment_adminRole_authorRoleIsAdminAndNameUnmasked() {
        author.setRole(UserRole.ADMIN);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));

        IncidentComment saved = IncidentComment.builder()
                .id(UUID.randomUUID()).incident(incident).author(author)
                .content("Assigned to contractor").createdAt(LocalDateTime.now()).build();
        when(commentRepository.save(any())).thenReturn(saved);

        IncidentCommentResponse result = commentService.addComment(incidentId, "Assigned to contractor");

        assertThat(result.authorRole()).isEqualTo("ADMIN");
        // Staff names are shown in full — only civilian PII is masked.
        assertThat(result.authorName()).isEqualTo("Jane Doe");
    }
}
