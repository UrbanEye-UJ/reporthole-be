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
import za.co.urbaneye.reporthole.admin.security.dto.AuditEntryResponse;
import za.co.urbaneye.reporthole.admin.security.dto.GrantRoleRequest;
import za.co.urbaneye.reporthole.admin.security.entity.AccessControlAction;
import za.co.urbaneye.reporthole.admin.security.entity.AccessControlAuditEntry;
import za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException;
import za.co.urbaneye.reporthole.admin.security.repository.IAccessControlAuditRepository;
import za.co.urbaneye.reporthole.admin.security.service.impl.SecurityAdminServiceImpl;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SecurityAdminServiceImpl} — the separation-of-duties guard, the
 * append-only audit writes, and the credentialsValidFrom bumps that make revocation immediate.
 */
@ExtendWith(MockitoExtension.class)
class SecurityAdminServiceImplTest {

    @Mock private IUserRepository userRepository;
    @Mock private IUserAuthRepository userAuthRepository;
    @Mock private IAccessControlAuditRepository auditRepository;

    @InjectMocks
    private SecurityAdminServiceImpl service;

    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();

    @BeforeEach
    void mockSecurityContext() {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(CALLER_ID.toString());
        SecurityContext ctx = org.mockito.Mockito.mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private User securityAdminCaller() {
        return User.builder().userId(CALLER_ID).role(UserRole.SECURITY_ADMIN)
                .firstName("Sam").lastName("Secure").build();
    }

    private User targetUser(UserRole role) {
        return User.builder().userId(TARGET_ID).role(role)
                .firstName("Terry").lastName("Target").build();
    }

    private UserAuth targetAuth(UserStatus status) {
        return UserAuth.builder().authId(TARGET_ID).status(status).build();
    }

    // ---------------- grantRole ----------------

    @Test
    void grantRole_happyPath_changesRole_bumpsCredentials_writesAudit() {
        User target = targetUser(UserRole.CIVILIAN);
        UserAuth auth = targetAuth(UserStatus.ACTIVE);
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
        when(userAuthRepository.findById(TARGET_ID)).thenReturn(Optional.of(auth));

        service.grantRole(TARGET_ID, new GrantRoleRequest(UserRole.ADMIN, "promoting for Q3 rollout"));

        assertThat(target.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).save(target);
        assertThat(auth.getCredentialsValidFrom()).isNotNull();
        verify(userAuthRepository).save(auth);

        ArgumentCaptor<AccessControlAuditEntry> captor = ArgumentCaptor.forClass(AccessControlAuditEntry.class);
        verify(auditRepository).save(captor.capture());
        AccessControlAuditEntry entry = captor.getValue();
        assertThat(entry.getAction()).isEqualTo(AccessControlAction.ROLE_GRANTED);
        assertThat(entry.getFromValue()).isEqualTo("CIVILIAN");
        assertThat(entry.getToValue()).isEqualTo("ADMIN");
        assertThat(entry.getReason()).isEqualTo("promoting for Q3 rollout");
        assertThat(entry.getActor().getUserId()).isEqualTo(CALLER_ID);
        assertThat(entry.getTarget().getUserId()).isEqualTo(TARGET_ID);
    }

    @Test
    void grantRole_toSecurityAdmin_succeeds_soASecurityAdminCanPromoteAnotherSecurityAdmin() {
        // Nothing in grantRole whitelists which roles may be granted — SECURITY_ADMIN is a
        // UserRole like any other, so an existing security admin can onboard a second one this
        // way instead of needing the scripts/promote-to-security-admin.sh bootstrap path.
        User target = targetUser(UserRole.ADMIN);
        UserAuth auth = targetAuth(UserStatus.ACTIVE);
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
        when(userAuthRepository.findById(TARGET_ID)).thenReturn(Optional.of(auth));

        service.grantRole(TARGET_ID, new GrantRoleRequest(UserRole.SECURITY_ADMIN, "onboarding a second security admin"));

        assertThat(target.getRole()).isEqualTo(UserRole.SECURITY_ADMIN);
        assertThat(auth.getCredentialsValidFrom()).isNotNull();

        ArgumentCaptor<AccessControlAuditEntry> captor = ArgumentCaptor.forClass(AccessControlAuditEntry.class);
        verify(auditRepository).save(captor.capture());
        AccessControlAuditEntry entry = captor.getValue();
        assertThat(entry.getAction()).isEqualTo(AccessControlAction.ROLE_GRANTED);
        assertThat(entry.getFromValue()).isEqualTo("ADMIN");
        assertThat(entry.getToValue()).isEqualTo("SECURITY_ADMIN");
    }

    @Test
    void grantRole_callerNotSecurityAdmin_throwsForbidden_noWrites() {
        when(userRepository.findById(CALLER_ID))
                .thenReturn(Optional.of(User.builder().userId(CALLER_ID).role(UserRole.ADMIN).build()));

        assertThatThrownBy(() -> service.grantRole(TARGET_ID, new GrantRoleRequest(UserRole.ADMIN, "why")))
                .isInstanceOf(SecurityAdminException.class)
                .hasMessageContaining("Only security admins");

        verify(userRepository, never()).save(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    void grantRole_selfTarget_throwsBadRequest() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));

        assertThatThrownBy(() -> service.grantRole(CALLER_ID, new GrantRoleRequest(UserRole.ADMIN, "why")))
                .isInstanceOf(SecurityAdminException.class)
                .hasMessageContaining("cannot change your own role");

        verify(auditRepository, never()).save(any());
    }

    @Test
    void grantRole_targetAlreadyHasRole_throwsConflict() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser(UserRole.ADMIN)));

        assertThatThrownBy(() -> service.grantRole(TARGET_ID, new GrantRoleRequest(UserRole.ADMIN, "why")))
                .isInstanceOf(SecurityAdminException.class)
                .hasMessageContaining("already has role");

        verify(auditRepository, never()).save(any());
    }

    @Test
    void grantRole_targetNotFound_throwsNotFound() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.grantRole(TARGET_ID, new GrantRoleRequest(UserRole.ADMIN, "why")))
                .isInstanceOf(SecurityAdminException.class)
                .hasMessageContaining("not found");
    }

    // ---------------- revokeRole ----------------

    @Test
    void revokeRole_happyPath_demotesToCivilian_writesAudit() {
        User target = targetUser(UserRole.ADMIN);
        UserAuth auth = targetAuth(UserStatus.ACTIVE);
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
        when(userAuthRepository.findById(TARGET_ID)).thenReturn(Optional.of(auth));

        service.revokeRole(TARGET_ID, "left the municipality");

        assertThat(target.getRole()).isEqualTo(UserRole.CIVILIAN);
        assertThat(auth.getCredentialsValidFrom()).isNotNull();

        ArgumentCaptor<AccessControlAuditEntry> captor = ArgumentCaptor.forClass(AccessControlAuditEntry.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AccessControlAction.ROLE_REVOKED);
        assertThat(captor.getValue().getFromValue()).isEqualTo("ADMIN");
        assertThat(captor.getValue().getToValue()).isEqualTo("CIVILIAN");
    }

    @Test
    void revokeRole_targetIsCivilian_throwsConflict() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser(UserRole.CIVILIAN)));

        assertThatThrownBy(() -> service.revokeRole(TARGET_ID, "why"))
                .isInstanceOf(SecurityAdminException.class)
                .hasMessageContaining("no elevated role");

        verify(auditRepository, never()).save(any());
    }

    // ---------------- suspend / reactivate ----------------

    @Test
    void suspendAccount_happyPath_setsSuspended_bumpsCredentials_writesAudit() {
        UserAuth auth = targetAuth(UserStatus.ACTIVE);
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser(UserRole.CIVILIAN)));
        when(userAuthRepository.findById(TARGET_ID)).thenReturn(Optional.of(auth));

        service.suspendAccount(TARGET_ID, "credential stuffing from this account");

        assertThat(auth.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(auth.getCredentialsValidFrom()).isNotNull();
        verify(userAuthRepository).save(auth);

        ArgumentCaptor<AccessControlAuditEntry> captor = ArgumentCaptor.forClass(AccessControlAuditEntry.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AccessControlAction.ACCOUNT_SUSPENDED);
        assertThat(captor.getValue().getFromValue()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().getToValue()).isEqualTo("SUSPENDED");
    }

    @Test
    void suspendAccount_alreadySuspended_throwsConflict() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser(UserRole.CIVILIAN)));
        when(userAuthRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetAuth(UserStatus.SUSPENDED)));

        assertThatThrownBy(() -> service.suspendAccount(TARGET_ID, "why"))
                .isInstanceOf(SecurityAdminException.class)
                .hasMessageContaining("already suspended");

        verify(auditRepository, never()).save(any());
    }

    @Test
    void suspendAccount_selfTarget_throwsBadRequest() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));

        assertThatThrownBy(() -> service.suspendAccount(CALLER_ID, "why"))
                .isInstanceOf(SecurityAdminException.class)
                .hasMessageContaining("cannot suspend your own account");
    }

    @Test
    void reactivateAccount_happyPath_setsActive_writesAudit_doesNotBumpCredentials() {
        UserAuth auth = targetAuth(UserStatus.SUSPENDED);
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser(UserRole.CIVILIAN)));
        when(userAuthRepository.findById(TARGET_ID)).thenReturn(Optional.of(auth));

        service.reactivateAccount(TARGET_ID, "investigation cleared the account");

        assertThat(auth.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(auth.getCredentialsValidFrom()).isNull();

        ArgumentCaptor<AccessControlAuditEntry> captor = ArgumentCaptor.forClass(AccessControlAuditEntry.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AccessControlAction.ACCOUNT_REACTIVATED);
    }

    @Test
    void reactivateAccount_notSuspended_throwsConflict() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser(UserRole.CIVILIAN)));
        when(userAuthRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetAuth(UserStatus.ACTIVE)));

        assertThatThrownBy(() -> service.reactivateAccount(TARGET_ID, "why"))
                .isInstanceOf(SecurityAdminException.class)
                .hasMessageContaining("not suspended");

        verify(auditRepository, never()).save(any());
    }

    // ---------------- forceLogout ----------------

    @Test
    void forceLogout_happyPath_bumpsCredentials_writesAudit_withNoFromTo() {
        UserAuth auth = targetAuth(UserStatus.ACTIVE);
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser(UserRole.ADMIN)));
        when(userAuthRepository.findById(TARGET_ID)).thenReturn(Optional.of(auth));

        service.forceLogout(TARGET_ID, "lost laptop");

        assertThat(auth.getCredentialsValidFrom()).isNotNull();

        ArgumentCaptor<AccessControlAuditEntry> captor = ArgumentCaptor.forClass(AccessControlAuditEntry.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AccessControlAction.SESSIONS_REVOKED);
        assertThat(captor.getValue().getFromValue()).isNull();
        assertThat(captor.getValue().getToValue()).isNull();
    }

    // ---------------- listUsers ----------------

    @Test
    void listUsers_returnsEveryAccount_withEmailAndStatus() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));

        User u1 = User.builder().userId(TARGET_ID).role(UserRole.CIVILIAN)
                .firstName("Terry").lastName("Target")
                .createdAt(java.time.LocalDateTime.now()).build();
        UserAuth a1 = UserAuth.builder().authId(TARGET_ID).email("terry@example.com")
                .status(UserStatus.ACTIVE).build();
        when(userRepository.findAll()).thenReturn(List.of(u1));
        when(userAuthRepository.findAll()).thenReturn(List.of(a1));

        List<za.co.urbaneye.reporthole.admin.security.dto.SecurityUserResponse> result = service.listUsers();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().userId()).isEqualTo(TARGET_ID);
        assertThat(result.getFirst().name()).isEqualTo("Terry Target");
        assertThat(result.getFirst().email()).isEqualTo("terry@example.com");
        assertThat(result.getFirst().role()).isEqualTo(UserRole.CIVILIAN);
        assertThat(result.getFirst().status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void listUsers_callerNotSecurityAdmin_throwsForbidden() {
        when(userRepository.findById(CALLER_ID))
                .thenReturn(Optional.of(User.builder().userId(CALLER_ID).role(UserRole.ADMIN).build()));

        assertThatThrownBy(() -> service.listUsers())
                .isInstanceOf(SecurityAdminException.class)
                .hasMessageContaining("Only security admins");
    }

    // ---------------- listAudit ----------------

    @Test
    void listAudit_noFilter_returnsWholeTrailMapped() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        AccessControlAuditEntry entry = AccessControlAuditEntry.builder()
                .auditId(UUID.randomUUID())
                .action(AccessControlAction.ROLE_GRANTED)
                .actor(securityAdminCaller())
                .target(targetUser(UserRole.ADMIN))
                .fromValue("CIVILIAN").toValue("ADMIN").reason("r")
                .build();
        when(auditRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(entry));

        List<AuditEntryResponse> result = service.listAudit(null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().action()).isEqualTo(AccessControlAction.ROLE_GRANTED);
        assertThat(result.getFirst().actorName()).isEqualTo("Sam Secure");
        assertThat(result.getFirst().targetName()).isEqualTo("Terry Target");
    }

    @Test
    void listAudit_withFilter_queriesByTarget() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(securityAdminCaller()));
        when(auditRepository.findByTarget_UserIdOrderByCreatedAtDesc(TARGET_ID)).thenReturn(List.of());

        List<AuditEntryResponse> result = service.listAudit(TARGET_ID);

        assertThat(result).isEmpty();
        verify(auditRepository).findByTarget_UserIdOrderByCreatedAtDesc(TARGET_ID);
        verify(auditRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void listAudit_callerNotSecurityAdmin_throwsForbidden() {
        when(userRepository.findById(CALLER_ID))
                .thenReturn(Optional.of(User.builder().userId(CALLER_ID).role(UserRole.ADMIN).build()));

        assertThatThrownBy(() -> service.listAudit(null))
                .isInstanceOf(SecurityAdminException.class)
                .hasMessageContaining("Only security admins");
    }
}
