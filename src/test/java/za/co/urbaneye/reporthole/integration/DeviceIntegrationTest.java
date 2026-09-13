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

/**
 * Integration tests for the dashcam device token system.
 *
 * <p>Verifies the full authentication path end-to-end:
 * JWT login → token generation → device token used for incident creation
 * → device token rejected on non-incident endpoints.</p>
 *
 * <p>Runs against H2 in-memory under the {@code local} profile (Hibernate
 * auto-creates the {@code dashcam_device} table; no PostGIS needed for
 * device operations). Incident creation uses {@code forceCreate=true} to
 * skip the {@code ST_DWithin} duplicate check which is not available in H2.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class DeviceIntegrationTest {

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
        String email = "device_test_" + UUID.randomUUID() + "@mail.com";
        restTemplate.postForEntity(
                base("/auth/register"),
                new RegisterRequest("Device", "Tester", email, UserRole.CIVILIAN, "Test@Pass1", "0700000000"),
                Void.class
        );

        ResponseEntity<Map> loginResp = restTemplate.postForEntity(
                base("/auth/login"),
                new LoginRequest(email, "Test@Pass1"),
                Map.class
        );
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());

        Map<?, ?> data = (Map<?, ?>) loginResp.getBody().get("data");
        jwtToken = (String) data.get("token");
        assertNotNull(jwtToken);
    }

    private HttpHeaders jwtHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        return headers;
    }

    private HttpHeaders deviceTokenHeaders(String deviceToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deviceToken);
        return headers;
    }

    /** Calls POST /devices/token/generate with the current JWT and returns the device token. */
    private String generateDeviceToken() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                base("/devices/token/generate"),
                HttpMethod.POST,
                new HttpEntity<>(jwtHeaders()),
                Map.class
        );
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
        String token = (String) data.get("deviceToken");
        assertNotNull(token);
        return token;
    }

    @Test
    void generateToken_returns200AndToken_whenAuthenticatedWithJwt() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                base("/devices/token/generate"),
                HttpMethod.POST,
                new HttpEntity<>(jwtHeaders()),
                Map.class
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
        assertNotNull(data.get("deviceToken"));
        // Device tokens are plain UUIDs — they never contain dots
        assertFalse(data.get("deviceToken").toString().contains("."));
    }

    @Test
    void deviceToken_allowsIncidentCreation_onIncidentsEndpoint() {
        String deviceToken = generateDeviceToken();

        // Minimal valid 1×1 white PNG, plain base64 (no data-URI prefix)
        IncidentRequestDTO req = new IncidentRequestDTO(
                IssueType.POTHOLE, "Dashcam detected pothole", IncidentSource.DASHCAM,
                -26.2041, 28.0473,
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwADhQGAWjR9awAAAABJRU5ErkJggg==",
                true, null, null
        );

        ResponseEntity<Map> resp = restTemplate.exchange(
                base("/incidents/create"),
                HttpMethod.POST,
                new HttpEntity<>(req, deviceTokenHeaders(deviceToken)),
                Map.class
        );

        assertTrue(resp.getStatusCode().is2xxSuccessful(),
                "Expected 2xx but got " + resp.getStatusCode());
        Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
        assertEquals("DASHCAM", data.get("source"));
    }

    @Test
    void deviceToken_isRejected_onTokenGenerateEndpoint() {
        String deviceToken = generateDeviceToken();

        // A device token must NOT be able to generate more tokens
        ResponseEntity<String> resp = restTemplate.exchange(
                base("/devices/token/generate"),
                HttpMethod.POST,
                new HttpEntity<>(deviceTokenHeaders(deviceToken)),
                String.class
        );

        assertTrue(
                resp.getStatusCode() == HttpStatus.UNAUTHORIZED || resp.getStatusCode() == HttpStatus.FORBIDDEN,
                "Expected 401 or 403 but got: " + resp.getStatusCode()
        );
    }

    @Test
    void generateToken_returnsUnauthorizedOrForbidden_whenUnauthenticated() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resp = restTemplate.exchange(
                base("/devices/token/generate"),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        assertTrue(
                resp.getStatusCode() == HttpStatus.UNAUTHORIZED || resp.getStatusCode() == HttpStatus.FORBIDDEN,
                "Expected 401 or 403 but got: " + resp.getStatusCode()
        );
    }
}
