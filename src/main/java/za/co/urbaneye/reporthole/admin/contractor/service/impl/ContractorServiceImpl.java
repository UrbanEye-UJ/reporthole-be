package za.co.urbaneye.reporthole.admin.contractor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.admin.contractor.dto.CompleteContractorRegistrationRequest;
import za.co.urbaneye.reporthole.admin.contractor.dto.ContractorResponse;
import za.co.urbaneye.reporthole.admin.contractor.dto.InviteContractorRequest;
import za.co.urbaneye.reporthole.admin.contractor.dto.RevealEmailResponse;
import za.co.urbaneye.reporthole.admin.contractor.entity.ContractorInvite;
import za.co.urbaneye.reporthole.admin.contractor.exception.ContractorException;
import za.co.urbaneye.reporthole.admin.contractor.repository.ContractorInviteRepository;
import za.co.urbaneye.reporthole.admin.contractor.service.interfaces.IContractorService;
import za.co.urbaneye.reporthole.incident.entity.AssignmentStatus;
import za.co.urbaneye.reporthole.incident.repository.AssignmentRepository;
import za.co.urbaneye.reporthole.notification.service.interfaces.IMailService;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractorServiceImpl implements IContractorService {

    private final IUserRepository userRepository;
    private final IUserAuthRepository userAuthRepository;
    private final ContractorInviteRepository inviteRepository;
    private final AssignmentRepository assignmentRepository;
    private final PasswordEncoder encoder;
    private final IMailService mailService;

    @Value("${mail.contractor-invite-url}")
    private String inviteBaseUrl;

    @Override
    @Transactional
    public void inviteContractor(InviteContractorRequest request) {
        requireAdmin();

        final String emailHash = SecretUtil.hashEmail(request.email());

        if (userAuthRepository.findByEmailHash(emailHash).isPresent()) {
            throw new ContractorException("A user with this email already exists");
        }
        if (inviteRepository.existsByEmailHash(emailHash)) {
            throw new ContractorException("An invite has already been sent to this email");
        }

        UUID token = UUID.randomUUID();
        ContractorInvite invite = ContractorInvite.builder()
                .email(request.email())
                .emailHash(emailHash)
                .token(token)
                .specialisations(new HashSet<>(request.specialisations()))
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();
        inviteRepository.save(invite);

        String inviteUrl = inviteBaseUrl + token;
        mailService.sendContractorInviteEmail(request.email(), inviteUrl);
        log.info("Contractor invite sent to email hash={}", emailHash);
    }

    @Override
    @Transactional
    public ContractorResponse completeContractorRegistration(CompleteContractorRegistrationRequest request) {
        ContractorInvite invite = inviteRepository.findByToken(request.token())
                .orElseThrow(() -> new ContractorException("Invalid invite token"));

        if (invite.isUsed()) {
            throw new ContractorException("This invite has already been used");
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ContractorException("This invite has expired");
        }

        // Defensive check in case the email was registered between invite creation and completion
        if (userAuthRepository.findByEmailHash(invite.getEmailHash()).isPresent()) {
            throw new ContractorException("An account with this email already exists");
        }

        UserAuth auth = UserAuth.builder()
                .email(invite.getEmail())
                .emailHash(invite.getEmailHash())
                .password(encoder.encode(request.password()))
                .status(UserStatus.ACTIVE)
                .build();
        UserAuth savedAuth = userAuthRepository.save(auth);

        User user = User.builder()
                .userId(savedAuth.getAuthId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .role(UserRole.CONTRACTOR)
                .specialisations(new HashSet<>(invite.getSpecialisations()))
                .build();
        User savedUser = userRepository.save(user);

        invite.setUsed(true);
        inviteRepository.save(invite);

        log.info("Contractor registration completed for invite token={}", request.token());
        return toResponse(savedUser, savedAuth, 0, 0, false);
    }

    @Override
    public List<ContractorResponse> getContractors() {
        requireAdmin();

        return userRepository.findByRole(UserRole.CONTRACTOR).stream()
                .map(user -> {
                    UserAuth auth = userAuthRepository.findById(user.getUserId())
                            .orElseThrow(() -> new ContractorException("Contractor auth record not found"));
                    int activeJobs = assignmentRepository.countByContractor_UserIdAndStatusNot(
                            user.getUserId(), AssignmentStatus.RESOLVED);
                    int completedJobs = assignmentRepository.countByContractor_UserIdAndStatus(
                            user.getUserId(), AssignmentStatus.RESOLVED);
                    return toResponse(user, auth, activeJobs, completedJobs, true);
                })
                .toList();
    }

    @Override
    public RevealEmailResponse revealEmail(UUID contractorId, String password) {
        User admin = requireAdmin();

        UserAuth adminAuth = userAuthRepository.findById(admin.getUserId())
                .orElseThrow(() -> new ContractorException("Admin auth record not found"));
        if (!encoder.matches(password, adminAuth.getPassword())) {
            throw new ContractorException("Incorrect password");
        }

        User contractor = userRepository.findById(contractorId)
                .filter(u -> u.getRole() == UserRole.CONTRACTOR)
                .orElseThrow(() -> new ContractorException("Contractor not found: " + contractorId));
        UserAuth contractorAuth = userAuthRepository.findById(contractor.getUserId())
                .orElseThrow(() -> new ContractorException("Contractor auth record not found"));

        log.info("Admin {} revealed email for contractor {}", admin.getUserId(), contractorId);
        return new RevealEmailResponse(contractorAuth.getEmail());
    }

    /**
     * Builds a {@link ContractorResponse}, masking the email (e.g. {@code "jo***@example.com"})
     * unless {@code maskEmail} is {@code false}.
     */
    private ContractorResponse toResponse(User user, UserAuth auth, int activeJobs, int completedJobs, boolean maskEmail) {
        return new ContractorResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                maskEmail ? SecretUtil.maskEmail(auth.getEmail()) : auth.getEmail(),
                user.getPhoneNumber(),
                activeJobs,
                completedJobs,
                user.getCreatedAt(),
                List.copyOf(user.getSpecialisations())
        );
    }

    private User requireAdmin() {
        UUID currentUserId = UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ContractorException("User not found"));
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ContractorException("Only admins can manage contractors");
        }
        return currentUser;
    }
}
