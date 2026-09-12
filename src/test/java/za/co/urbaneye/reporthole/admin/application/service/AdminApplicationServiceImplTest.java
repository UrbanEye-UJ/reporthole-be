package za.co.urbaneye.reporthole.admin.application.service;

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
import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationRequest;
import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationResponse;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplication;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplicationStatus;
import za.co.urbaneye.reporthole.admin.application.exception.AdminApplicationException;
import za.co.urbaneye.reporthole.admin.application.repository.IAdminApplicationRepository;
import za.co.urbaneye.reporthole.admin.application.service.impl.AdminApplicationServiceImpl;
import za.co.urbaneye.reporthole.admin.security.entity.AccessControlAction;
import za.co.urbaneye.reporthole.admin.security.entity.AccessControlAuditEntry;
import za.co.urbaneye.reporthole.admin.security.repository.IAccessControlAuditRepository;
import za.co.urbaneye.reporthole.notification.service.interfaces.IMailService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminApplicationServiceImplTest {

    @Mock private IAdminApplicationRepository applicationRepository;
    @Mock private IUserRepository userRepository;
    @Mock private IUserAuthRepository userAuthRepository;
    @Mock private IAccessControlAuditRepository auditRepository;
    @Mock private IMailService mailService;

    @InjectMocks
    private AdminApplicationServiceImpl service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN = "GPJHB2025";
    private static final String EMAIL_HASH = "abc123hash==";
    private static final String EMAIL = "test@example.com";

    @BeforeEach
    void mockSecurityContext() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(USER_ID.toString());
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void apply_happyPath_savesAndSendsEmail() {
        User user = User.builder().userId(USER_ID).role(UserRole.CIVILIAN).firstName("Alice").build();
        UserAuth auth = UserAuth.builder().authId(USER_ID).emailHash(EMAIL_HASH).email(EMAIL).build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userAuthRepository.findById(USER_ID)).thenReturn(Optional.of(auth));
        when(applicationRepository.existsByUser_UserId(USER_ID)).thenReturn(false);
        when(applicationRepository.save(any())).thenReturn(mock(AdminApplication.class));

        service.apply(new AdminApplicationRequest(TOKEN));

        verify(applicationRepository).save(any(AdminApplication.class));
        verify(mailService).sendAdminApplicationEmail(
                eq("Alice"), eq(USER_ID.toString()), eq(EMAIL_HASH), eq(TOKEN), eq(EMAIL));
    }

    @Test
    void apply_duplicateGuard_throwsConflict() {
        User user = User.builder().userId(USER_ID).role(UserRole.CIVILIAN).firstName("Alice").build();
        UserAuth auth = UserAuth.builder().authId(USER_ID).emailHash(EMAIL_HASH).email(EMAIL).build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userAuthRepository.findById(USER_ID)).thenReturn(Optional.of(auth));
        when(applicationRepository.existsByUser_UserId(USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.apply(new AdminApplicationRequest(TOKEN)))
                .isInstanceOf(AdminApplicationException.class)
                .hasMessageContaining("already submitted");

        verify(applicationRepository, never()).save(any());
        verify(mailService, never()).sendAdminApplicationEmail(any(), any(), any(), any(), any());
    }

    @Test
    void apply_roleGuard_adminThrowsBadRequest() {
        User user = User.builder().userId(USER_ID).role(UserRole.ADMIN).firstName("Bob").build();
        UserAuth auth = UserAuth.builder().authId(USER_ID).emailHash(EMAIL_HASH).email(EMAIL).build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userAuthRepository.findById(USER_ID)).thenReturn(Optional.of(auth));

        assertThatThrownBy(() -> service.apply(new AdminApplicationRequest(TOKEN)))
                .isInstanceOf(AdminApplicationException.class)
                .hasMessageContaining("already");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void apply_userNotFound_throwsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(new AdminApplicationRequest(TOKEN)))
                .isInstanceOf(AdminApplicationException.class)
                .hasMessageContaining("not found");
    }

    private static final UUID APPLICANT_ID = UUID.randomUUID();

    private User stubSecurityAdminCaller() {
        return User.builder().userId(USER_ID).role(UserRole.SECURITY_ADMIN)
                .firstName("Ada").lastName("Admin").build();
    }

    private AdminApplication stubPendingApplication() {
        User applicant = User.builder().userId(APPLICANT_ID).role(UserRole.CIVILIAN).firstName("Bob").lastName("Builder").build();
        return AdminApplication.builder()
                .applicationId(UUID.randomUUID())
                .user(applicant)
                .municipalityToken(TOKEN)
                .status(AdminApplicationStatus.PENDING)
                .build();
    }

    @Test
    void listApplications_noFilter_returnsAllMapped_whenCallerIsSecurityAdmin() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubSecurityAdminCaller()));

        AdminApplication application = stubPendingApplication();
        when(applicationRepository.findAllByOrderBySubmittedAtDesc())
                .thenReturn(List.of(application));
        when(userAuthRepository.findById(APPLICANT_ID))
                .thenReturn(Optional.of(UserAuth.builder().authId(APPLICANT_ID).email(EMAIL).build()));

        List<AdminApplicationResponse> result = service.listApplications(null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().applicationId()).isEqualTo(application.getApplicationId());
        assertThat(result.getFirst().applicantEmail()).isEqualTo(EMAIL);
        assertThat(result.getFirst().municipalityName()).isNull();
    }

    @Test
    void listApplications_withStatus_filtersByThatStatus() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubSecurityAdminCaller()));
        when(applicationRepository.findByStatusOrderBySubmittedAtDesc(AdminApplicationStatus.REJECTED))
                .thenReturn(List.of());

        List<AdminApplicationResponse> result = service.listApplications(AdminApplicationStatus.REJECTED);

        assertThat(result).isEmpty();
        verify(applicationRepository).findByStatusOrderBySubmittedAtDesc(AdminApplicationStatus.REJECTED);
        verify(applicationRepository, never()).findAllByOrderBySubmittedAtDesc();
    }

    @Test
    void listApplications_throws_whenCallerIsNotSecurityAdmin() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().userId(USER_ID).role(UserRole.ADMIN).build()));

        assertThatThrownBy(() -> service.listApplications(null))
                .isInstanceOf(AdminApplicationException.class)
                .hasMessageContaining("Only security admins");
    }

    @Test
    void approve_promotesApplicant_bumpsCredentials_writesAudit_sendsEmail_whenPending() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubSecurityAdminCaller()));

        AdminApplication application = stubPendingApplication();
        UserAuth applicantAuth = UserAuth.builder().authId(APPLICANT_ID).email(EMAIL).build();
        when(applicationRepository.findById(application.getApplicationId())).thenReturn(Optional.of(application));
        when(userAuthRepository.findById(APPLICANT_ID)).thenReturn(Optional.of(applicantAuth));

        service.approve(application.getApplicationId());

        assertThat(application.getUser().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(application.getStatus()).isEqualTo(AdminApplicationStatus.APPROVED);
        assertThat(applicantAuth.getCredentialsValidFrom()).isNotNull();
        verify(userRepository).save(application.getUser());
        verify(applicationRepository).save(application);
        verify(userAuthRepository).save(applicantAuth);
        verify(mailService).sendAdminApplicationDecisionEmail(EMAIL, "Bob", true);

        ArgumentCaptor<AccessControlAuditEntry> captor = ArgumentCaptor.forClass(AccessControlAuditEntry.class);
        verify(auditRepository).save(captor.capture());
        AccessControlAuditEntry entry = captor.getValue();
        assertThat(entry.getAction()).isEqualTo(AccessControlAction.ROLE_GRANTED);
        assertThat(entry.getFromValue()).isEqualTo("CIVILIAN");
        assertThat(entry.getToValue()).isEqualTo("ADMIN");
        assertThat(entry.getActor().getUserId()).isEqualTo(USER_ID);
        assertThat(entry.getTarget().getUserId()).isEqualTo(APPLICANT_ID);
        assertThat(entry.getReason()).contains(application.getApplicationId().toString());
    }

    @Test
    void approve_throws_whenApplicationAlreadyProcessed() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubSecurityAdminCaller()));

        AdminApplication application = stubPendingApplication();
        application.setStatus(AdminApplicationStatus.APPROVED);
        when(applicationRepository.findById(application.getApplicationId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.approve(application.getApplicationId()))
                .isInstanceOf(AdminApplicationException.class)
                .hasMessageContaining("already been processed");

        verify(userRepository, never()).save(any());
        verify(auditRepository, never()).save(any());
        verify(mailService, never()).sendAdminApplicationDecisionEmail(any(), any(), anyBoolean());
    }

    @Test
    void approve_throws_whenCallerIsNotSecurityAdmin() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(User.builder().userId(USER_ID).role(UserRole.ADMIN).build()));

        UUID applicationId = UUID.randomUUID();

        assertThatThrownBy(() -> service.approve(applicationId))
                .isInstanceOf(AdminApplicationException.class)
                .hasMessageContaining("Only security admins");

        verify(applicationRepository, never()).findById(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    void reject_marksRejectedAndSendsEmail_withoutChangingRoleOrAuditing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubSecurityAdminCaller()));

        AdminApplication application = stubPendingApplication();
        when(applicationRepository.findById(application.getApplicationId())).thenReturn(Optional.of(application));
        when(userAuthRepository.findById(APPLICANT_ID))
                .thenReturn(Optional.of(UserAuth.builder().authId(APPLICANT_ID).email(EMAIL).build()));

        service.reject(application.getApplicationId());

        assertThat(application.getStatus()).isEqualTo(AdminApplicationStatus.REJECTED);
        assertThat(application.getUser().getRole()).isEqualTo(UserRole.CIVILIAN);
        verify(userRepository, never()).save(any());
        verify(auditRepository, never()).save(any());
        verify(applicationRepository).save(application);
        verify(mailService).sendAdminApplicationDecisionEmail(EMAIL, "Bob", false);
    }
}
