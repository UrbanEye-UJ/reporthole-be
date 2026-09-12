package za.co.urbaneye.reporthole.admin.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationRequest;
import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationResponse;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplication;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplicationStatus;
import za.co.urbaneye.reporthole.admin.application.exception.AdminApplicationException;
import za.co.urbaneye.reporthole.admin.application.repository.IAdminApplicationRepository;
import za.co.urbaneye.reporthole.admin.application.service.interfaces.IAdminApplicationService;
import za.co.urbaneye.reporthole.admin.security.entity.AccessControlAction;
import za.co.urbaneye.reporthole.admin.security.entity.AccessControlAuditEntry;
import za.co.urbaneye.reporthole.admin.security.repository.IAccessControlAuditRepository;
import za.co.urbaneye.reporthole.notification.service.interfaces.IMailService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Handles submission and review of admin access applications.
 *
 * <p>On submission, the application is persisted with {@code PENDING} status
 * and an async notification email is sent to the project inbox. A
 * {@code SECURITY_ADMIN} — not an operational {@code ADMIN} — then reviews the
 * queue via {@code GET /admin/applications} and approves or rejects it.
 * Approval is a {@code CIVILIAN} → {@code ADMIN} privilege grant, so it is
 * treated exactly like any other role grant: it writes an append-only
 * {@link AccessControlAuditEntry} and bumps the applicant's
 * {@code credentialsValidFrom} so the new role only takes effect on their next
 * sign-in.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminApplicationServiceImpl implements IAdminApplicationService {

    private final IAdminApplicationRepository applicationRepository;
    private final IUserRepository userRepository;
    private final IUserAuthRepository userAuthRepository;
    private final IAccessControlAuditRepository accessControlAuditRepository;
    private final IMailService mailService;

    @Override
    @Transactional
    public void apply(AdminApplicationRequest request) {
        UUID userId = currentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AdminApplicationException("User not found"));

        UserAuth auth = userAuthRepository.findById(userId)
                .orElseThrow(() -> new AdminApplicationException("User not found"));

        if (user.getRole() != UserRole.CIVILIAN) {
            throw new AdminApplicationException("Account is already " + user.getRole());
        }

        if (applicationRepository.existsByUser_UserId(userId)) {
            throw new AdminApplicationException("Application already submitted");
        }

        applicationRepository.save(
                AdminApplication.builder()
                        .user(user)
                        .municipalityToken(request.municipalityToken())
                        .build()
        );

        log.info("Admin application saved for user {}", userId);

        mailService.sendAdminApplicationEmail(
                user.getFirstName(),
                userId.toString(),
                auth.getEmailHash(),
                request.municipalityToken(),
                auth.getEmail()
        );
    }

    @Override
    public List<AdminApplicationResponse> listApplications(AdminApplicationStatus status) {
        requireSecurityAdmin();

        List<AdminApplication> applications = status == null
                ? applicationRepository.findAllByOrderBySubmittedAtDesc()
                : applicationRepository.findByStatusOrderBySubmittedAtDesc(status);

        return applications.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void approve(UUID applicationId) {
        User approver = requireSecurityAdmin();

        AdminApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AdminApplicationException("Application not found"));
        if (application.getStatus() != AdminApplicationStatus.PENDING) {
            throw new AdminApplicationException("Application has already been processed");
        }

        User applicant = application.getUser();
        UserRole previousRole = applicant.getRole();
        applicant.setRole(UserRole.ADMIN);
        userRepository.save(applicant);

        application.setStatus(AdminApplicationStatus.APPROVED);
        applicationRepository.save(application);

        UserAuth auth = userAuthRepository.findById(applicant.getUserId())
                .orElseThrow(() -> new AdminApplicationException("User not found"));
        // A role change must take effect immediately: invalidate the applicant's existing
        // sessions so they re-authenticate and pick up ROLE_ADMIN. Whole-second precision to
        // match the JWT iat claim (see JwtAuthenticationFilter).
        auth.setCredentialsValidFrom(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        userAuthRepository.save(auth);

        // Approval is a privilege grant — record it on the same append-only trail as every
        // other role grant, with the reviewing security admin as the actor.
        accessControlAuditRepository.save(AccessControlAuditEntry.builder()
                .action(AccessControlAction.ROLE_GRANTED)
                .actor(approver)
                .target(applicant)
                .fromValue(previousRole.name())
                .toValue(UserRole.ADMIN.name())
                .reason("Admin application " + applicationId + " approved")
                .build());

        log.info("Admin application {} approved by security admin {} — user {} promoted to ADMIN",
                applicationId, approver.getUserId(), applicant.getUserId());

        mailService.sendAdminApplicationDecisionEmail(auth.getEmail(), applicant.getFirstName(), true);
    }

    @Override
    @Transactional
    public void reject(UUID applicationId) {
        requireSecurityAdmin();

        AdminApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AdminApplicationException("Application not found"));
        if (application.getStatus() != AdminApplicationStatus.PENDING) {
            throw new AdminApplicationException("Application has already been processed");
        }

        application.setStatus(AdminApplicationStatus.REJECTED);
        applicationRepository.save(application);

        User applicant = application.getUser();
        log.info("Admin application {} rejected for user {}", applicationId, applicant.getUserId());

        UserAuth auth = userAuthRepository.findById(applicant.getUserId())
                .orElseThrow(() -> new AdminApplicationException("User not found"));
        mailService.sendAdminApplicationDecisionEmail(auth.getEmail(), applicant.getFirstName(), false);
    }

    private AdminApplicationResponse toResponse(AdminApplication application) {
        User applicant = application.getUser();
        UserAuth auth = userAuthRepository.findById(applicant.getUserId())
                .orElseThrow(() -> new AdminApplicationException("User not found"));
        return new AdminApplicationResponse(
                application.getApplicationId(),
                applicant.getUserId(),
                applicant.getFirstName(),
                applicant.getLastName(),
                auth.getEmail(),
                application.getMunicipalityToken(),
                application.getMunicipality() != null ? application.getMunicipality().getName() : null,
                application.getSubmittedAt(),
                application.getStatus()
        );
    }

    /**
     * Loads the calling account and asserts it holds {@code SECURITY_ADMIN}.
     *
     * <p>Reviewing admin access applications is an identity decision — granting or withholding a
     * role — so it belongs to the security admin, not to operational {@code ADMIN}s who would
     * otherwise be approving their own peers.</p>
     *
     * @return the caller's {@link User}
     * @throws AdminApplicationException if the caller is unknown or not a security admin
     */
    private User requireSecurityAdmin() {
        User currentUser = userRepository.findById(currentUserId())
                .orElseThrow(() -> new AdminApplicationException("User not found"));
        if (currentUser.getRole() != UserRole.SECURITY_ADMIN) {
            throw new AdminApplicationException("Only security admins can perform this action");
        }
        return currentUser;
    }

    private UUID currentUserId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        );
    }
}
