package za.co.urbaneye.reporthole.admin.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import za.co.urbaneye.reporthole.admin.security.dto.AuditLogEntryResponse;
import za.co.urbaneye.reporthole.admin.security.entity.AuditLogEntry;
import za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException;
import za.co.urbaneye.reporthole.admin.security.repository.IAuditLogRepository;
import za.co.urbaneye.reporthole.admin.security.service.impl.AuditLogServiceImpl;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AuditLogServiceImpl} — the general-purpose counterpart to
 * {@code SecurityAdminServiceImpl}'s identity-action audit writes.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock private IAuditLogRepository repository;
    @Mock private IUserRepository userRepository;

    @InjectMocks
    private AuditLogServiceImpl service;

    private static final UUID CALLER_ID = UUID.randomUUID();

    @BeforeEach
    void mockSecurityContext() {
        // lenient: record() never touches the security context, only listAll() does.
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.lenient().when(auth.getPrincipal()).thenReturn(CALLER_ID.toString());
        SecurityContext ctx = org.mockito.Mockito.mock(SecurityContext.class);
        org.mockito.Mockito.lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void record_savesEntryWithGivenFields() {
        User actor = User.builder().userId(UUID.randomUUID()).firstName("Sam").lastName("Secure").build();
        UUID entityId = UUID.randomUUID();

        service.record(actor, "MUNICIPALITY_CREATED", "MUNICIPALITY", entityId, "Created municipality \"X\"");

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(repository).save(captor.capture());
        AuditLogEntry saved = captor.getValue();

        assertThat(saved.getActor()).isEqualTo(actor);
        assertThat(saved.getAction()).isEqualTo("MUNICIPALITY_CREATED");
        assertThat(saved.getEntityType()).isEqualTo("MUNICIPALITY");
        assertThat(saved.getEntityId()).isEqualTo(entityId);
        assertThat(saved.getSummary()).isEqualTo("Created municipality \"X\"");
    }

    @Test
    void record_withNullActor_savesEntryWithNoActor() {
        // The public contact form has no authenticated caller — record() must accept that.
        service.record(null, "CONTACT_FORM_SUBMITTED", "MESSAGE", null, "Public contact-form submission");

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getActor()).isNull();
    }

    @Test
    void listAll_callerIsSecurityAdmin_returnsMappedEntries() {
        when(userRepository.findById(CALLER_ID)).thenReturn(
                Optional.of(User.builder().userId(CALLER_ID).role(UserRole.SECURITY_ADMIN).build()));

        User actor = User.builder().userId(UUID.randomUUID()).firstName("Con").lastName("Tractor").build();
        AuditLogEntry entry = AuditLogEntry.builder()
                .id(UUID.randomUUID())
                .actor(actor)
                .action("CONTRACTOR_INVITED")
                .entityType("CONTRACTOR_INVITE")
                .entityId(UUID.randomUUID())
                .summary("Invited a contractor")
                .createdAt(LocalDateTime.now())
                .build();
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(entry));

        List<AuditLogEntryResponse> result = service.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().action()).isEqualTo("CONTRACTOR_INVITED");
        assertThat(result.getFirst().actorName()).isEqualTo("Con Tractor");
    }

    @Test
    void listAll_entryWithNoActor_actorNameIsSystem() {
        when(userRepository.findById(CALLER_ID)).thenReturn(
                Optional.of(User.builder().userId(CALLER_ID).role(UserRole.SECURITY_ADMIN).build()));

        AuditLogEntry entry = AuditLogEntry.builder()
                .id(UUID.randomUUID())
                .actor(null)
                .action("CONTACT_FORM_SUBMITTED")
                .entityType("MESSAGE")
                .summary("Public contact-form submission")
                .createdAt(LocalDateTime.now())
                .build();
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(entry));

        List<AuditLogEntryResponse> result = service.listAll();

        assertThat(result.getFirst().actorId()).isNull();
        assertThat(result.getFirst().actorName()).isEqualTo("System");
    }

    @Test
    void listAll_callerNotSecurityAdmin_throwsForbidden() {
        when(userRepository.findById(CALLER_ID)).thenReturn(
                Optional.of(User.builder().userId(CALLER_ID).role(UserRole.ADMIN).build()));

        assertThatThrownBy(() -> service.listAll())
                .isInstanceOf(SecurityAdminException.class)
                .hasMessageContaining("Only security admins");
    }
}
