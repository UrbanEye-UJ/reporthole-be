package za.co.urbaneye.reporthole.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.admin.municipality.entity.Municipality;
import za.co.urbaneye.reporthole.admin.security.service.interfaces.IAuditLogService;
import za.co.urbaneye.reporthole.admin.municipality.repository.IMunicipalityRepository;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * <p>Safe to leave enabled permanently — uses {@link IMunicipalityRepository#findByNameIgnoreCase}
 * to skip any name that is already present (beyond backfilling its boundary if missing),
 * making every run idempotent.</p>
 *
 * <p>If the platform admin has not been seeded yet (e.g. bootstrap-admin is disabled),
 * this runner logs a warning and exits without failing startup.</p>
 *
 * <p>Also loads real Municipal Demarcation Board boundary polygons bundled as
 * {@code data/gauteng-municipality-boundaries.geojson} (sourced from the MDB's public 2018
 * local- and district-municipality boundary datasets, reprojected from UTM Zone 35S and
 * simplified) and attaches one to each municipality by name — including backfilling any
 * existing row whose {@code boundary} is still null, so this is safe to re-run after the
 * data file changes without needing a manual migration.</p>
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

    private static final String BOUNDARY_DATA_PATH = "data/gauteng-municipality-boundaries.geojson";

    private final IMunicipalityRepository municipalityRepository;
    private final IUserAuthRepository userAuthRepository;
    private final IUserRepository userRepository;
    private final IAuditLogService auditLogService;
    private final ObjectMapper objectMapper;

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
        final Map<String, MultiPolygon> boundaries = loadBoundaries();
        int seeded = 0;
        int backfilled = 0;

        for (String name : GAUTENG_MUNICIPALITIES) {
            final Optional<Municipality> existing = municipalityRepository.findByNameIgnoreCase(name);

            if (existing.isPresent()) {
                final Municipality municipality = existing.get();
                if (municipality.getBoundary() == null && boundaries.containsKey(name)) {
                    municipality.setBoundary(boundaries.get(name));
                    municipalityRepository.save(municipality);
                    backfilled++;
                    log.info("[BOOTSTRAP] Backfilled boundary for existing municipality: {}", name);
                } else {
                    log.debug("[BOOTSTRAP] Municipality already exists — skipping: {}", name);
                }
                continue;
            }

            final Municipality municipality = Municipality.builder()
                    .name(name)
                    .province("Gauteng")
                    .createdBy(admin)
                    .boundary(boundaries.get(name))
                    .build();

            municipalityRepository.save(municipality);
            auditLogService.record(admin, "MUNICIPALITY_SEEDED", "MUNICIPALITY", municipality.getId(),
                    "Seeded municipality " + name + " on startup");
            seeded++;
            log.info("[BOOTSTRAP] Seeded municipality: {}", name);
        }

        if (seeded > 0 || backfilled > 0) {
            log.info("[BOOTSTRAP] Seeded {} and backfilled boundaries for {} Gauteng municipalities.",
                    seeded, backfilled);
        } else {
            log.info("[BOOTSTRAP] All Gauteng municipalities already present — nothing to seed.");
        }
    }

    /**
     * Parses the bundled boundary GeoJSON into a name-keyed map of JTS {@link MultiPolygon}s.
     *
     * <p>The bundled file's geometry is deliberately simple — every feature is a
     * {@code MultiPolygon} containing exactly one polygon part with a single outer ring and no
     * holes — so this reads the coordinate arrays directly rather than pulling in a general
     * GeoJSON parsing library. Coordinates are {@code [lon, lat]} pairs, matching JTS's
     * {@code (x, y)} convention.</p>
     *
     * @return boundaries keyed by municipality name, or an empty map if the data file is
     *         missing or malformed (seeding still proceeds — municipalities are simply left
     *         without an overlay rather than failing startup)
     */
    private Map<String, MultiPolygon> loadBoundaries() {
        final Map<String, MultiPolygon> boundaries = new HashMap<>();
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        try (InputStream in = new ClassPathResource(BOUNDARY_DATA_PATH).getInputStream()) {
            final JsonNode root = objectMapper.readTree(in);
            for (JsonNode feature : root.get("features")) {
                final String name = feature.get("properties").get("name").asText();
                final JsonNode polygonsNode = feature.get("geometry").get("coordinates");

                final Polygon[] polygons = new Polygon[polygonsNode.size()];
                for (int i = 0; i < polygonsNode.size(); i++) {
                    final JsonNode outerRing = polygonsNode.get(i).get(0);
                    final Coordinate[] coordinates = new Coordinate[outerRing.size()];
                    for (int j = 0; j < outerRing.size(); j++) {
                        final JsonNode point = outerRing.get(j);
                        coordinates[j] = new Coordinate(point.get(0).asDouble(), point.get(1).asDouble());
                    }
                    final LinearRing shell = geometryFactory.createLinearRing(coordinates);
                    polygons[i] = geometryFactory.createPolygon(shell);
                }
                boundaries.put(name, geometryFactory.createMultiPolygon(polygons));
            }
        } catch (IOException | NullPointerException e) {
            log.error("[BOOTSTRAP] Failed to load municipality boundary data from {} — "
                    + "map overlays will be unavailable until this is fixed", BOUNDARY_DATA_PATH, e);
        }

        return boundaries;
    }
}
