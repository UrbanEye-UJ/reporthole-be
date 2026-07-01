package za.co.urbaneye.reporthole.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.entity.IncidentSource;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.entity.UserRole;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class IncidentIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String jwtToken;

    private String base(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    @BeforeEach
    void authenticate() {
        String email = "incident_test_" + UUID.randomUUID() + "@mail.com";
        restTemplate.postForEntity(
                base("/auth/register"),
                new RegisterRequest("Test", "User", email, UserRole.CIVILIAN, "pass", "0700000000"),
                Void.class
        );

        ResponseEntity<Map> loginResp = restTemplate.postForEntity(
                base("/auth/login"),
                new LoginRequest(email, "pass"),
                Map.class
        );
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());

        Map<?, ?> body = loginResp.getBody();
        assertNotNull(body);
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        jwtToken = (String) data.get("token");
        assertNotNull(jwtToken);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        return headers;
    }

    private String createIncidentAndGetId() {
        // forceCreate=true skips the ST_DWithin duplicate check (PostGIS function, not available in H2)
        // Minimal valid 1x1 white PNG, plain base64 (no data URI prefix)
        IncidentRequestDTO req = new IncidentRequestDTO(
                IssueType.POTHOLE, "Integration test pothole", IncidentSource.MANUAL,
                -26.2041, 28.0473,
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwADhQGAWjR9awAAAABJRU5ErkJggg==",
                true, null
        );

        ResponseEntity<Map> resp = restTemplate.exchange(
                base("/incidents/create"),
                HttpMethod.POST,
                new HttpEntity<>(req, authHeaders()),
                Map.class
        );
        assertTrue(resp.getStatusCode().is2xxSuccessful(),
                "Expected 2xx but got " + resp.getStatusCode() + " body: " + resp.getBody());
        Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
        return (String) data.get("incidentId");
    }

    @Test
    void getIncidentById_returnsIncident_whenAuthenticated() {
        String incidentId = createIncidentAndGetId();

        ResponseEntity<Map> resp = restTemplate.exchange(
                base("/incidents/" + incidentId),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
        assertEquals(incidentId, data.get("incidentId"));
        assertEquals("POTHOLE", data.get("incidentType"));
        assertEquals("Integration test pothole", data.get("description"));
    }

    @Test
    void getIncidentById_returnsUnauthorizedOrForbidden_whenUnauthenticated() {
        String incidentId = createIncidentAndGetId();

        ResponseEntity<String> resp = restTemplate.getForEntity(
                base("/incidents/" + incidentId),
                String.class
        );

        // Spring Security returns 403 when no credentials are supplied (no WWW-Authenticate header configured)
        assertTrue(
                resp.getStatusCode() == HttpStatus.UNAUTHORIZED || resp.getStatusCode() == HttpStatus.FORBIDDEN,
                "Expected 401 or 403 but got: " + resp.getStatusCode()
        );
    }

    @Test
    void getIncidentById_returns500_whenIdDoesNotExist() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                base("/incidents/" + UUID.randomUUID()),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }
}
