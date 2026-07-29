package za.co.urbaneye.reporthole.aspect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ExecutionMetricsAspect} helper methods.
 * AOP wiring is verified by the Spring Boot integration tests that exercise
 * the service and repository layers end-to-end.
 */
class ExecutionMetricsAspectTest {

    private final ExecutionMetricsAspect aspect = new ExecutionMetricsAspect();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvePrincipal_returnsAnonymous_whenNoAuthentication() {
        assertThat(aspect.resolvePrincipal()).isEqualTo("anonymous");
    }

    @Test
    void resolvePrincipal_returnsAnonymous_whenAnonymousUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThat(aspect.resolvePrincipal()).isEqualTo("anonymous");
    }

    @Test
    void resolvePrincipal_returnsUserId_whenAuthenticated() {
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));

        assertThat(aspect.resolvePrincipal()).isEqualTo(userId);
    }

    @Test
    void resolveLayer_identifiesController() {
        assertThat(aspect.resolveLayer("za.co.urbaneye.reporthole.incident.controller.IncidentController"))
                .isEqualTo("CONTROLLER");
    }

    @Test
    void resolveLayer_identifiesService() {
        assertThat(aspect.resolveLayer("za.co.urbaneye.reporthole.incident.service.impl.IncidentServiceImpl"))
                .isEqualTo("SERVICE");
    }

    @Test
    void resolveLayer_identifiesRepository() {
        assertThat(aspect.resolveLayer("za.co.urbaneye.reporthole.incident.repository.IncidentRepository"))
                .isEqualTo("REPOSITORY");
    }

    @Test
    void resolveLayer_returnsUnknown_forUnrecognisedPackage() {
        assertThat(aspect.resolveLayer("za.co.urbaneye.reporthole.security.Jwt"))
                .isEqualTo("UNKNOWN");
    }
}
