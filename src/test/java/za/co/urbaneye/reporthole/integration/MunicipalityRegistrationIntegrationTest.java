package za.co.urbaneye.reporthole.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import za.co.urbaneye.reporthole.security.SecretUtil;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end coverage for the municipality-token onboarding flow: a SECURITY_ADMIN creates a
 * municipality and issues a token, a new user registers with that token and lands as ADMIN,
 * and the record shows up (APPROVED) in {@code GET /admin/applications}. Also covers rejection
 * of bogus and revoked tokens.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class MunicipalityRegistrationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IUserAuthRepository userAuthRepository;

    private String base(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private void register(String email, String municipalityToken) {
        restTemplate.postForEntity(
                base("/auth/register"),
                new RegisterRequest("Test", "User", email, UserRole.CIVILIAN, "Test@Pass1", "0700000000", municipalityToken),
                String.class);
    }

    private ResponseEntity<String> registerRaw(String email, String municipalityToken) {
        return restTemplate.postForEntity(
                base("/auth/register"),
                new RegisterRequest("Test", "User", email, UserRole.CIVILIAN, "Test@Pass1", "0700000000", municipalityToken),
                String.class);
    }

    private String login(String email) {
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                base("/auth/login"), new LoginRequest(email, "Test@Pass1"), Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode(), "login failed: " + resp.getBody());
        return (String) ((Map<?, ?>) resp.getBody().get("data")).get("token");
    }

    private UUID userIdOf(String email) {
        UserAuth auth = userAuthRepository.findByEmailHash(SecretUtil.hashEmail(email)).orElseThrow();
        return auth.getAuthId();
    }

    private void promoteToSecurityAdmin(String email) {
        User user = userRepository.findById(userIdOf(email)).orElseThrow();
        user.setRole(UserRole.SECURITY_ADMIN);
        userRepository.save(user);
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void registerWithMunicipalityToken_landsAsAdmin_andShowsApprovedInApplications() {
        String secAdminEmail = "muni_it_admin_" + UUID.randomUUID() + "@mail.com";
        register(secAdminEmail, null);
        promoteToSecurityAdmin(secAdminEmail);
        String secAdminToken = login(secAdminEmail);

        // create municipality
        ResponseEntity<Map> muni = restTemplate.exchange(
                base("/admin/municipalities"), HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "City of Ekurhuleni " + UUID.randomUUID(), "province", "Gauteng"),
                        bearer(secAdminToken)),
                Map.class);
        assertEquals(HttpStatus.CREATED, muni.getStatusCode(), String.valueOf(muni.getBody()));
        String municipalityId = (String) ((Map<?, ?>) muni.getBody().get("data")).get("id");
        String municipalityName = (String) ((Map<?, ?>) muni.getBody().get("data")).get("name");

        // issue a token
        ResponseEntity<Map> tok = restTemplate.exchange(
                base("/admin/municipalities/" + municipalityId + "/tokens"), HttpMethod.POST,
                new HttpEntity<>(Map.of(), bearer(secAdminToken)), Map.class);
        assertEquals(HttpStatus.CREATED, tok.getStatusCode());
        String tokenValue = (String) ((Map<?, ?>) tok.getBody().get("data")).get("token");
        assertNotNull(tokenValue);

        // a new user registers WITH the token
        String adminEmail = "muni_it_newadmin_" + UUID.randomUUID() + "@mail.com";
        ResponseEntity<String> reg = registerRaw(adminEmail, tokenValue);
        assertEquals(HttpStatus.CREATED, reg.getStatusCode(), reg.getBody());

        // they can log in and the token registration made them ADMIN
        User newAdmin = userRepository.findById(userIdOf(adminEmail)).orElseThrow();
        assertEquals(UserRole.ADMIN, newAdmin.getRole());

        // and there's an APPROVED row in the applications list, tagged with the municipality
        ResponseEntity<Map> apps = restTemplate.exchange(
                base("/admin/applications?status=APPROVED"), HttpMethod.GET,
                new HttpEntity<>(bearer(secAdminToken)), Map.class);
        assertEquals(HttpStatus.OK, apps.getStatusCode());
        List<?> rows = (List<?>) apps.getBody().get("data");
        assertNotNull(rows);
        boolean found = rows.stream().anyMatch(r -> {
            Map<?, ?> row = (Map<?, ?>) r;
            return newAdmin.getUserId().toString().equals(row.get("userId"))
                    && "APPROVED".equals(row.get("status"))
                    && municipalityName.equals(row.get("municipalityName"));
        });
        org.junit.jupiter.api.Assertions.assertTrue(found, "expected an APPROVED application row for the new admin");
    }

    @Test
    void registerWithUnknownToken_isRejected() {
        String email = "muni_it_bogus_" + UUID.randomUUID() + "@mail.com";
        ResponseEntity<String> reg = registerRaw(email, "MUNI-DOESNOTEXIST99");
        assertEquals(HttpStatus.BAD_REQUEST, reg.getStatusCode());
        // nothing was created
        org.junit.jupiter.api.Assertions.assertTrue(
                userAuthRepository.findByEmailHash(SecretUtil.hashEmail(email)).isEmpty());
    }

    @Test
    void registerWithRevokedToken_isRejected() {
        String secAdminEmail = "muni_it_admin2_" + UUID.randomUUID() + "@mail.com";
        register(secAdminEmail, null);
        promoteToSecurityAdmin(secAdminEmail);
        String secAdminToken = login(secAdminEmail);

        ResponseEntity<Map> muni = restTemplate.exchange(
                base("/admin/municipalities"), HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Revoked Test " + UUID.randomUUID(), "province", "Gauteng"),
                        bearer(secAdminToken)),
                Map.class);
        String municipalityId = (String) ((Map<?, ?>) muni.getBody().get("data")).get("id");

        ResponseEntity<Map> tok = restTemplate.exchange(
                base("/admin/municipalities/" + municipalityId + "/tokens"), HttpMethod.POST,
                new HttpEntity<>(Map.of(), bearer(secAdminToken)), Map.class);
        String tokenId = (String) ((Map<?, ?>) tok.getBody().get("data")).get("id");
        String tokenValue = (String) ((Map<?, ?>) tok.getBody().get("data")).get("token");

        restTemplate.exchange(
                base("/admin/municipalities/tokens/" + tokenId + "/revoke"), HttpMethod.POST,
                new HttpEntity<>(bearer(secAdminToken)), String.class);

        String email = "muni_it_afterrevoke_" + UUID.randomUUID() + "@mail.com";
        ResponseEntity<String> reg = registerRaw(email, tokenValue);
        assertEquals(HttpStatus.BAD_REQUEST, reg.getStatusCode());
    }
}
