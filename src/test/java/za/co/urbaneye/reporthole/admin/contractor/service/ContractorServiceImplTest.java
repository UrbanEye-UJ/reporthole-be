package za.co.urbaneye.reporthole.admin.contractor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.urbaneye.reporthole.admin.contractor.dto.CompleteContractorRegistrationRequest;
import za.co.urbaneye.reporthole.admin.contractor.dto.ContractorResponse;
import za.co.urbaneye.reporthole.admin.contractor.dto.InviteContractorRequest;
import za.co.urbaneye.reporthole.admin.contractor.dto.RevealEmailResponse;
import za.co.urbaneye.reporthole.admin.contractor.entity.ContractorInvite;
import za.co.urbaneye.reporthole.admin.contractor.exception.ContractorException;
import za.co.urbaneye.reporthole.admin.contractor.repository.ContractorInviteRepository;
import za.co.urbaneye.reporthole.admin.contractor.service.impl.ContractorServiceImpl;
import za.co.urbaneye.reporthole.incident.entity.AssignmentStatus;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.incident.repository.AssignmentRepository;
import za.co.urbaneye.reporthole.notification.service.interfaces.IMailService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractorServiceImplTest {

    @Mock private IUserRepository userRepository;
    @Mock private IUserAuthRepository userAuthRepository;
    @Mock private ContractorInviteRepository inviteRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private PasswordEncoder encoder;
    @Mock private IMailService mailService;

    @InjectMocks
    private ContractorServiceImpl service;

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID CONTRACTOR_ID = UUID.randomUUID();
    private static final String CONTRACTOR_EMAIL = "con.tractor@example.com";
    private static final String ADMIN_PASSWORD_HASH = "hashed-admin-password";

    @BeforeEach
    void mockSecurityContext() {
        Authentication auth = mock(Authentication.class);
        // lenient — not all tests call requireAdmin(), so these stubs aren't always consumed
        lenient().when(auth.getPrincipal()).thenReturn(ADMIN_ID.toString());
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private User stubAdmin() {
        return User.builder().userId(ADMIN_ID).role(UserRole.ADMIN).build();
    }

    private User stubContractor() {
        return User.builder().userId(CONTRACTOR_ID).role(UserRole.CONTRACTOR)
                .firstName("Con").lastName("Tractor").phoneNumber("0123456789")
                .specialisations(Set.of(IssueType.POTHOLE))
                .build();
    }

    // --- inviteContractor ---

    @Test
    void inviteContractor_sendsEmail_whenEmailNotTaken() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(stubAdmin()));
        when(userAuthRepository.findByEmailHash(any())).thenReturn(Optional.empty());
        when(inviteRepository.existsByEmailHash(any())).thenReturn(false);
        when(inviteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.inviteContractor(new InviteContractorRequest(CONTRACTOR_EMAIL, List.of(IssueType.POTHOLE)));

        verify(mailService).sendContractorInviteEmail(any(), any());
    }

    @Test
    void inviteContractor_throws_whenEmailAlreadyRegistered() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(stubAdmin()));
        when(userAuthRepository.findByEmailHash(any()))
                .thenReturn(Optional.of(UserAuth.builder().build()));

        assertThatThrownBy(() -> service.inviteContractor(
                new InviteContractorRequest(CONTRACTOR_EMAIL, List.of(IssueType.POTHOLE))))
                .isInstanceOf(ContractorException.class)
                .hasMessageContaining("already exists");

        verify(inviteRepository, never()).save(any());
    }

    @Test
    void inviteContractor_throws_whenInviteAlreadySent() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(stubAdmin()));
        when(userAuthRepository.findByEmailHash(any())).thenReturn(Optional.empty());
        when(inviteRepository.existsByEmailHash(any())).thenReturn(true);

        assertThatThrownBy(() -> service.inviteContractor(
                new InviteContractorRequest(CONTRACTOR_EMAIL, List.of(IssueType.POTHOLE))))
                .isInstanceOf(ContractorException.class)
                .hasMessageContaining("already been sent");

        verify(inviteRepository, never()).save(any());
    }

    @Test
    void inviteContractor_throws_whenCallerIsNotAdmin() {
        when(userRepository.findById(ADMIN_ID))
                .thenReturn(Optional.of(User.builder().userId(ADMIN_ID).role(UserRole.CIVILIAN).build()));

        assertThatThrownBy(() -> service.inviteContractor(
                new InviteContractorRequest(CONTRACTOR_EMAIL, List.of(IssueType.POTHOLE))))
                .isInstanceOf(ContractorException.class)
                .hasMessageContaining("Only admins");
    }

    // --- completeContractorRegistration ---

    @Test
    void completeRegistration_createsAccount_whenTokenValid() {
        UUID token = UUID.randomUUID();
        ContractorInvite invite = ContractorInvite.builder()
                .token(token)
                .email(CONTRACTOR_EMAIL)
                .emailHash("hash")
                .specialisations(Set.of(IssueType.POTHOLE))
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        when(inviteRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(userAuthRepository.findByEmailHash("hash")).thenReturn(Optional.empty());
        when(encoder.encode(any())).thenReturn("hashed");
        when(userAuthRepository.save(any())).thenAnswer(inv -> {
            UserAuth a = inv.getArgument(0);
            a.setAuthId(CONTRACTOR_ID);
            return a;
        });
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inviteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContractorResponse response = service.completeContractorRegistration(
                new CompleteContractorRegistrationRequest(token, "Con", "Tractor", "0123456789", "Passw0rd!"));

        assertThat(response.email()).isEqualTo(CONTRACTOR_EMAIL);
        assertThat(invite.isUsed()).isTrue();
    }

    @Test
    void completeRegistration_throws_whenTokenNotFound() {
        UUID token = UUID.randomUUID();
        when(inviteRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeContractorRegistration(
                new CompleteContractorRegistrationRequest(token, "Con", "Tractor", "0123456789", "Passw0rd!")))
                .isInstanceOf(ContractorException.class)
                .hasMessageContaining("Invalid invite token");
    }

    @Test
    void completeRegistration_throws_whenTokenAlreadyUsed() {
        UUID token = UUID.randomUUID();
        ContractorInvite invite = ContractorInvite.builder()
                .token(token).email(CONTRACTOR_EMAIL).emailHash("hash")
                .specialisations(Set.of()).expiresAt(LocalDateTime.now().plusHours(24)).used(true)
                .build();
        when(inviteRepository.findByToken(token)).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> service.completeContractorRegistration(
                new CompleteContractorRegistrationRequest(token, "Con", "Tractor", "0123456789", "Passw0rd!")))
                .isInstanceOf(ContractorException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void completeRegistration_throws_whenTokenExpired() {
        UUID token = UUID.randomUUID();
        ContractorInvite invite = ContractorInvite.builder()
                .token(token).email(CONTRACTOR_EMAIL).emailHash("hash")
                .specialisations(Set.of()).expiresAt(LocalDateTime.now().minusHours(1)).used(false)
                .build();
        when(inviteRepository.findByToken(token)).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> service.completeContractorRegistration(
                new CompleteContractorRegistrationRequest(token, "Con", "Tractor", "0123456789", "Passw0rd!")))
                .isInstanceOf(ContractorException.class)
                .hasMessageContaining("expired");
    }

    // --- getContractors ---

    @Test
    void getContractors_returnsMaskedEmail_whenCallerIsAdmin() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(stubAdmin()));
        when(userRepository.findByRole(UserRole.CONTRACTOR)).thenReturn(List.of(stubContractor()));
        when(userAuthRepository.findById(CONTRACTOR_ID))
                .thenReturn(Optional.of(UserAuth.builder().authId(CONTRACTOR_ID).email(CONTRACTOR_EMAIL).build()));
        when(assignmentRepository.countByContractor_UserIdAndStatusNot(CONTRACTOR_ID, AssignmentStatus.RESOLVED))
                .thenReturn(2);
        when(assignmentRepository.countByContractor_UserIdAndStatus(CONTRACTOR_ID, AssignmentStatus.RESOLVED))
                .thenReturn(5);

        List<ContractorResponse> result = service.getContractors();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().email()).isEqualTo("c***@example.com");
        assertThat(result.getFirst().activeJobs()).isEqualTo(2);
        assertThat(result.getFirst().completedJobs()).isEqualTo(5);
        assertThat(result.getFirst().specialisations()).containsExactly(IssueType.POTHOLE);
    }

    @Test
    void getContractors_throws_whenCallerIsNotAdmin() {
        when(userRepository.findById(ADMIN_ID))
                .thenReturn(Optional.of(User.builder().userId(ADMIN_ID).role(UserRole.CIVILIAN).build()));

        assertThatThrownBy(() -> service.getContractors())
                .isInstanceOf(ContractorException.class)
                .hasMessageContaining("Only admins");
    }

    // --- revealEmail ---

    @Test
    void revealEmail_returnsDecryptedEmail_whenPasswordCorrect() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(stubAdmin()));
        when(userAuthRepository.findById(ADMIN_ID))
                .thenReturn(Optional.of(UserAuth.builder().authId(ADMIN_ID).password(ADMIN_PASSWORD_HASH).build()));
        when(encoder.matches("correct-password", ADMIN_PASSWORD_HASH)).thenReturn(true);
        when(userRepository.findById(CONTRACTOR_ID)).thenReturn(Optional.of(stubContractor()));
        when(userAuthRepository.findById(CONTRACTOR_ID))
                .thenReturn(Optional.of(UserAuth.builder().authId(CONTRACTOR_ID).email(CONTRACTOR_EMAIL).build()));

        RevealEmailResponse response = service.revealEmail(CONTRACTOR_ID, "correct-password");

        assertThat(response.email()).isEqualTo(CONTRACTOR_EMAIL);
    }

    @Test
    void revealEmail_throws_whenPasswordIncorrect() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(stubAdmin()));
        when(userAuthRepository.findById(ADMIN_ID))
                .thenReturn(Optional.of(UserAuth.builder().authId(ADMIN_ID).password(ADMIN_PASSWORD_HASH).build()));
        when(encoder.matches("wrong-password", ADMIN_PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> service.revealEmail(CONTRACTOR_ID, "wrong-password"))
                .isInstanceOf(ContractorException.class)
                .hasMessageContaining("Incorrect password");
    }

    @Test
    void revealEmail_throws_whenCallerIsNotAdmin() {
        when(userRepository.findById(ADMIN_ID))
                .thenReturn(Optional.of(User.builder().userId(ADMIN_ID).role(UserRole.CIVILIAN).build()));

        assertThatThrownBy(() -> service.revealEmail(CONTRACTOR_ID, "whatever"))
                .isInstanceOf(ContractorException.class)
                .hasMessageContaining("Only admins");
    }

    @Test
    void revealEmail_throws_whenTargetUserIsNotContractor() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(stubAdmin()));
        when(userAuthRepository.findById(ADMIN_ID))
                .thenReturn(Optional.of(UserAuth.builder().authId(ADMIN_ID).password(ADMIN_PASSWORD_HASH).build()));
        when(encoder.matches("correct-password", ADMIN_PASSWORD_HASH)).thenReturn(true);

        UUID civilianId = UUID.randomUUID();
        when(userRepository.findById(civilianId))
                .thenReturn(Optional.of(User.builder().userId(civilianId).role(UserRole.CIVILIAN).build()));

        assertThatThrownBy(() -> service.revealEmail(civilianId, "correct-password"))
                .isInstanceOf(ContractorException.class)
                .hasMessageContaining("not found");
    }
}
