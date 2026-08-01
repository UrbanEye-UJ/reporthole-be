package za.co.urbaneye.reporthole.admin.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationRequest;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplication;
import za.co.urbaneye.reporthole.admin.application.exception.AdminApplicationException;
import za.co.urbaneye.reporthole.admin.application.repository.IAdminApplicationRepository;
import za.co.urbaneye.reporthole.admin.application.service.impl.AdminApplicationServiceImpl;
import za.co.urbaneye.reporthole.notification.service.interfaces.IMailService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
