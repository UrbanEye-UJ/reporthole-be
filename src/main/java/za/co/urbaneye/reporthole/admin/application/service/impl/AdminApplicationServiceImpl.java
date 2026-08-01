package za.co.urbaneye.reporthole.admin.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationRequest;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplication;
import za.co.urbaneye.reporthole.admin.application.exception.AdminApplicationException;
import za.co.urbaneye.reporthole.admin.application.repository.IAdminApplicationRepository;
import za.co.urbaneye.reporthole.admin.application.service.interfaces.IAdminApplicationService;
import za.co.urbaneye.reporthole.notification.service.interfaces.IMailService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.UUID;

/**
 * Handles submission of admin access applications.
 *
 * <p>On success, the application is persisted with {@code PENDING} status
 * and an async notification email is sent to the project inbox.
 * A developer then reviews the municipality token and promotes the user
 * via {@code scripts/promote-to-admin.sql}.</p>
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

    private UUID currentUserId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        );
    }
}
