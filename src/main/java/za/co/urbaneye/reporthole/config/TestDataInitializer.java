package za.co.urbaneye.reporthole.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.Set;

/**
 * Seeds pre-verified test accounts at startup when {@code app.test-data.enabled=true}.
 *
 * <p>Activate via {@code application-local.yml} or the {@code TEST_DATA_ENABLED=true}
 * environment variable. Never enable in production ({@code application-prod.yml}).</p>
 *
 * <p>The three seeded accounts are the credentials k6 regression tests use
 * (matched by {@code CIVILIAN_EMAIL}, {@code ADMIN_EMAIL}, {@code CONTRACTOR_EMAIL}
 * secrets in the GitHub workflow). Each account is created only if no user with
 * that email hash already exists, making this idempotent on every restart.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestDataInitializer implements ApplicationRunner {

    private final IUserAuthRepository authRepository;
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.test-data.enabled:false}")
    private boolean enabled;

    @Value("${app.test-data.civilian-email:civilian@reporthole-test.local}")
    private String civilianEmail;

    @Value("${app.test-data.civilian-password:Test@k6Civilian1}")
    private String civilianPassword;

    @Value("${app.test-data.admin-email:admin@reporthole-test.local}")
    private String adminEmail;

    @Value("${app.test-data.admin-password:Test@k6Admin1}")
    private String adminPassword;

    @Value("${app.test-data.contractor-email:contractor@reporthole-test.local}")
    private String contractorEmail;

    @Value("${app.test-data.contractor-password:Test@k6Contractor1}")
    private String contractorPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        log.warn("TEST DATA INITIALIZER IS ACTIVE — do not enable in production");

        seed(civilianEmail,   civilianPassword,   UserRole.CIVILIAN,   "k6-civilian",   "Tester", Set.of());
        seed(adminEmail,      adminPassword,      UserRole.ADMIN,      "k6-admin",      "Tester", Set.of());
        // Contractor needs POTHOLE specialisation so the assign endpoint accepts them
        seed(contractorEmail, contractorPassword, UserRole.CONTRACTOR, "k6-contractor", "Tester",
                Set.of(IssueType.POTHOLE, IssueType.CRACK));
    }

    private void seed(String email, String password, UserRole role,
                      String firstName, String lastName, Set<IssueType> specialisations) {
        final String hash = SecretUtil.hashEmail(email);
        if (authRepository.findByEmailHash(hash).isPresent()) {
            log.info("Test account already exists — skipping: {} ({})", role, email);
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
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber("0600000000")
                .role(role)
                .specialisations(specialisations)
                .build();
        userRepository.save(user);

        log.info("Seeded test account: {} {} ({})", role, email, savedAuth.getAuthId());
    }
}
