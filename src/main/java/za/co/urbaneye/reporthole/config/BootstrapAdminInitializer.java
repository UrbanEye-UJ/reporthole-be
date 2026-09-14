package za.co.urbaneye.reporthole.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.admin.security.service.interfaces.IAuditLogService;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.Set;

/**
 * Seeds the platform's first SECURITY_ADMIN account at startup when
 * {@code app.bootstrap-admin.enabled=true}.
 *
 * <p>This runner fires after the Spring context is fully initialised (DB connection
 * live, all beans ready) and is therefore safe to leave enabled on every restart —
 * it checks for the account first and skips creation if it already exists.</p>
 *
 * <p>Intended for both local development and production first-deploy. Override the
 * password in production via the {@code APP_BOOTSTRAP_ADMIN_PASSWORD} environment
 * variable — never ship the default password to a live environment.</p>
 *
 * <p>Runs before {@link TestDataInitializer} (Order 1 vs 2) so the admin account
 * is always present even when test data seeding is also active.</p>
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final IUserAuthRepository authRepository;
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IAuditLogService auditLogService;

    @Value("${app.bootstrap-admin.enabled:false}")
    private boolean enabled;

    /** The email address of the platform admin account. */
    @Value("${app.bootstrap-admin.email:reporthole.team@gmail.com}")
    private String email;

    /**
     * Password for the bootstrap account. Override via {@code APP_BOOTSTRAP_ADMIN_PASSWORD}
     * in production — the default here is for local development only.
     */
    @Value("${app.bootstrap-admin.password:1@qwerty}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        final String hash = SecretUtil.hashEmail(email);

        if (authRepository.findByEmailHash(hash).isPresent()) {
            log.info("[BOOTSTRAP] Platform admin already exists — skipping: {}", email);
            return;
        }

        final UserAuth auth = UserAuth.builder()
                .email(email)
                .emailHash(hash)
                .password(passwordEncoder.encode(password))
                .status(UserStatus.ACTIVE)
                .build();
        final UserAuth savedAuth = authRepository.save(auth);

        final User user = User.builder()
                .userId(savedAuth.getAuthId())
                .firstName("Reporthole")
                .lastName("Team")
                .phoneNumber("0600000000")
                .role(UserRole.SECURITY_ADMIN)
                .specialisations(Set.of())
                .build();
        // User's id is manually assigned (not @GeneratedValue), so Spring Data treats this as an
        // update and merges rather than persists — capture the returned managed instance rather
        // than reusing the transient `user` reference, or the audit call below fails with
        // TransientObjectException when it tries to associate it as the entry's actor.
        final User savedUser = userRepository.save(user);

        auditLogService.record(savedUser, "SECURITY_ADMIN_BOOTSTRAPPED", "USER", savedUser.getUserId(),
                "Initial platform SECURITY_ADMIN account created on startup");

        log.warn("[BOOTSTRAP] Seeded platform SECURITY_ADMIN: {} (id={}). "
                + "Change the password immediately if this is a production environment.",
                email, savedAuth.getAuthId());
    }
}
