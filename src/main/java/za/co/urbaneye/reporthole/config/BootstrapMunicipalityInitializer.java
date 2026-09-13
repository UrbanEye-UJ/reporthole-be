package za.co.urbaneye.reporthole.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.admin.municipality.entity.Municipality;
import za.co.urbaneye.reporthole.admin.municipality.repository.IMunicipalityRepository;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Seeds the 11 Gauteng municipalities at startup when
 * {@code app.bootstrap-municipalities.enabled=true}.
 *
 * <p>Runs after {@link BootstrapAdminInitializer} (Order 2) so the platform admin
 * account is guaranteed to exist by the time municipalities are created.
 * Each municipality is attributed to the platform admin as its creator.</p>
 *
 * <p>Safe to leave enabled permanently — uses {@link IMunicipalityRepository#existsByNameIgnoreCase}
 * to skip any name that is already present, making every run idempotent.</p>
 *
 * <p>If the platform admin has not been seeded yet (e.g. bootstrap-admin is disabled),
 * this runner logs a warning and exits without failing startup.</p>
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class BootstrapMunicipalityInitializer implements ApplicationRunner {

    private static final List<String> GAUTENG_MUNICIPALITIES = List.of(
            "City of Ekurhuleni Metropolitan",
            "City of Johannesburg Metropolitan",
            "City of Tshwane Metropolitan",
            "Sedibeng District",
            "Emfuleni Local",
            "Lesedi Local",
            "Midvaal Local",
            "West Rand District",
            "Merafong City Local",
            "Mogale City Local",
            "Rand West City Local"
    );

    private final IMunicipalityRepository municipalityRepository;
    private final IUserAuthRepository userAuthRepository;
    private final IUserRepository userRepository;

    @Value("${app.bootstrap-municipalities.enabled:false}")
    private boolean enabled;

    /** Must match the email used by {@link BootstrapAdminInitializer}. */
    @Value("${app.bootstrap-admin.email:reporthole.team@gmail.com}")
    private String adminEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        final String hash = SecretUtil.hashEmail(adminEmail);
        final Optional<User> adminOpt = userAuthRepository.findByEmailHash(hash)
                .map(auth -> userRepository.findById(auth.getAuthId()).orElse(null));

        if (adminOpt.isEmpty()) {
            log.warn("[BOOTSTRAP] Platform admin not found — skipping Gauteng municipality seeding. "
                    + "Ensure bootstrap-admin is also enabled and has run.");
            return;
        }

        final User admin = adminOpt.get();
        int seeded = 0;

        for (String name : GAUTENG_MUNICIPALITIES) {
            if (municipalityRepository.existsByNameIgnoreCase(name)) {
                log.debug("[BOOTSTRAP] Municipality already exists — skipping: {}", name);
                continue;
            }

            final Municipality municipality = Municipality.builder()
                    .name(name)
                    .province("Gauteng")
                    .createdBy(admin)
                    .build();

            municipalityRepository.save(municipality);
            seeded++;
            log.info("[BOOTSTRAP] Seeded municipality: {}", name);
        }

        if (seeded > 0) {
            log.info("[BOOTSTRAP] Seeded {} Gauteng municipalities.", seeded);
        } else {
            log.info("[BOOTSTRAP] All Gauteng municipalities already present — nothing to seed.");
        }
    }
}
