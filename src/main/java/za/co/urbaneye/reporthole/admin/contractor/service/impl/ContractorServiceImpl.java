package za.co.urbaneye.reporthole.admin.contractor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.admin.contractor.dto.ContractorResponse;
import za.co.urbaneye.reporthole.admin.contractor.dto.CreateContractorRequest;
import za.co.urbaneye.reporthole.admin.contractor.exception.ContractorException;
import za.co.urbaneye.reporthole.admin.contractor.service.interfaces.IContractorService;
import za.co.urbaneye.reporthole.incident.entity.AssignmentStatus;
import za.co.urbaneye.reporthole.incident.repository.AssignmentRepository;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractorServiceImpl implements IContractorService {

    private final IUserRepository userRepository;
    private final IUserAuthRepository userAuthRepository;
    private final AssignmentRepository assignmentRepository;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public ContractorResponse createContractor(CreateContractorRequest request) {
        requireAdmin();

        final String emailHash = SecretUtil.hashEmail(request.email());
        if (userAuthRepository.findByEmailHash(emailHash).isPresent()) {
            throw new ContractorException("A user with this email already exists");
        }

        UserAuth auth = UserAuth.builder()
                .email(request.email())
                .emailHash(emailHash)
                .password(encoder.encode(request.password()))
                .status(UserStatus.ACTIVE)
                .build();
        final UserAuth savedAuth = userAuthRepository.save(auth);

        User user = User.builder()
                .userId(savedAuth.getAuthId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .role(UserRole.CONTRACTOR)
                .build();
        final User savedUser = userRepository.save(user);

        log.info("Contractor account created: {}", savedUser.getUserId());
        return toResponse(savedUser, savedAuth, 0, 0);
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
                    return toResponse(user, auth, activeJobs, completedJobs);
                })
                .toList();
    }

    private ContractorResponse toResponse(User user, UserAuth auth, int activeJobs, int completedJobs) {
        return new ContractorResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                auth.getEmail(),
                user.getPhoneNumber(),
                activeJobs,
                completedJobs,
                user.getCreatedAt()
        );
    }

    private void requireAdmin() {
        UUID currentUserId = UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ContractorException("User not found"));
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ContractorException("Only admins can manage contractors");
        }
    }
}
