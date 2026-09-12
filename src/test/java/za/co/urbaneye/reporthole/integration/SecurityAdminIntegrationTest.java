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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end coverage for the security-admin slice: endpoint authorization, the append-only
 * audit trail being populated, and {@code credentialsValidFrom} enforcement making a role
 * change / suspension take effect on the affected account's very next request.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class SecurityAdminIntegrationTest {

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

    private void register(String email) {
        restTemplate.postForEntity(
                base("/auth/register"),
                new RegisterRequest("Test", "User", email, UserRole.CIVILIAN, "Test@Pass1", "0700000000", null),
                Void.class);
    }

    private String login(String email) {
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                base("/auth/login"), new LoginRequest(email, "Test@Pass1"), Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode(), "login failed: " + resp.getBody());
        Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
        return (String) data.get("token");
    }

    private UUID userIdOf(String email) {
        UserAuth auth = userAuthRepository.findByEmailHash(SecretUtil.hashEmail(email)).orElseThrow();
        return auth.getAuthId();
    }

    /** Bootstraps the first security admin the only way possible — directly, with no one above them. */
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

    /**
     * Waits until the wall clock has moved into a later whole second than any token just issued.
     *
     * <p>A JWT {@code iat} claim has second precision and {@code credentialsValidFrom} is compared
     * truncated to seconds, so a watermark bump in the <em>same</em> second as a token's issuance
     * deliberately does not revoke that token. Real forced-logouts are seconds-to-minutes after the
     * session started; the test just needs to cross that boundary.</p>
     */
    private void sleepPastTokenSecond() {
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void audit_endpoint_forbiddenForNonSecurityAdmin() {
        String email = "sec_it_civ_" + UUID.randomUUID() + "@mail.com";
        register(email);
        String civilianToken = login(email);

        ResponseEntity<String> resp = restTemplate.exchange(
                base("/admin/security/audit"), HttpMethod.GET,
                new HttpEntity<>(bearer(civilianToken)), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void grantRole_writesAuditRow_andRevokesTargetsExistingSessions() {
        String adminEmail = "sec_it_admin_" + UUID.randomUUID() + "@mail.com";
        String targetEmail = "sec_it_target_" + UUID.randomUUID() + "@mail.com";
        register(adminEmail);
        register(targetEmail);
        promoteToSecurityAdmin(adminEmail);

        String adminToken = login(adminEmail);
        String targetOldToken = login(targetEmail);
        UUID targetId = userIdOf(targetEmail);

        // target's pre-grant token works
        assertTrue(restTemplate.exchange(base("/incidents/my"), HttpMethod.GET,
                        new HttpEntity<>(bearer(targetOldToken)), String.class)
                .getStatusCode().is2xxSuccessful());

        sleepPastTokenSecond();

        // security admin grants ADMIN
        ResponseEntity<String> grant = restTemplate.exchange(
                base("/admin/security/users/" + targetId + "/role"), HttpMethod.POST,
                new HttpEntity<>(Map.of("role", "ADMIN", "reason", "integration test"), bearer(adminToken)),
                String.class);
        assertEquals(HttpStatus.OK, grant.getStatusCode(), grant.getBody());

        // target's old token is now rejected — credentialsValidFrom moved past it
        assertEquals(HttpStatus.UNAUTHORIZED,
                restTemplate.exchange(base("/incidents/my"), HttpMethod.GET,
                        new HttpEntity<>(bearer(targetOldToken)), String.class).getStatusCode());

        // and the audit trail has the role-granted row (login also writes a USER_LOGIN row, so size >= 1)
        ResponseEntity<Map> audit = restTemplate.exchange(
                base("/admin/security/audit?userId=" + targetId), HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), Map.class);
        assertEquals(HttpStatus.OK, audit.getStatusCode());
        List<?> rows = (List<?>) audit.getBody().get("data");
        assertNotNull(rows);
        assertTrue(rows.size() >= 1);
        // newest first — ROLE_GRANTED was written after login
        Map<?, ?> row = (Map<?, ?>) rows.getFirst();
        assertEquals("ROLE_GRANTED", row.get("action"));
        assertEquals("CIVILIAN", row.get("fromValue"));
        assertEquals("ADMIN", row.get("toValue"));
        assertEquals("integration test", row.get("reason"));
    }

    @Test
    void suspendAccount_blocksLoginAndRejectsExistingToken_untilReactivated() {
        String adminEmail = "sec_it_admin2_" + UUID.randomUUID() + "@mail.com";
        String targetEmail = "sec_it_target2_" + UUID.randomUUID() + "@mail.com";
        register(adminEmail);
        register(targetEmail);
        promoteToSecurityAdmin(adminEmail);

        String adminToken = login(adminEmail);
        String targetToken = login(targetEmail);
        UUID targetId = userIdOf(targetEmail);

        sleepPastTokenSecond();

        restTemplate.exchange(base("/admin/security/users/" + targetId + "/suspend"), HttpMethod.POST,
                new HttpEntity<>(Map.of("reason", "compromised"), bearer(adminToken)), String.class);

        // existing token rejected
        assertEquals(HttpStatus.UNAUTHORIZED,
                restTemplate.exchange(base("/incidents/my"), HttpMethod.GET,
                        new HttpEntity<>(bearer(targetToken)), String.class).getStatusCode());

        // fresh login refused
        ResponseEntity<String> reLogin = restTemplate.postForEntity(
                base("/auth/login"), new LoginRequest(targetEmail, "Test@Pass1"), String.class);
        assertEquals(HttpStatus.FORBIDDEN, reLogin.getStatusCode());

        // reactivate, then login works again
        restTemplate.exchange(base("/admin/security/users/" + targetId + "/reactivate"), HttpMethod.POST,
                new HttpEntity<>(Map.of("reason", "cleared"), bearer(adminToken)), String.class);
        assertEquals(HttpStatus.OK, restTemplate.postForEntity(
                base("/auth/login"), new LoginRequest(targetEmail, "Test@Pass1"), String.class).getStatusCode());
    }

    @Test
    void grantRole_toSecurityAdmin_letsSecondAccountActAsSecurityAdmin() {
        String firstAdminEmail = "sec_it_first_" + UUID.randomUUID() + "@mail.com";
        String secondAdminEmail = "sec_it_second_" + UUID.randomUUID() + "@mail.com";
        register(firstAdminEmail);
        register(secondAdminEmail);
        promoteToSecurityAdmin(firstAdminEmail);

        String firstAdminToken = login(firstAdminEmail);
        UUID secondAdminId = userIdOf(secondAdminEmail);

        // an existing security admin promotes a second account straight to SECURITY_ADMIN —
        // nothing restricts grantRole to lesser roles
        ResponseEntity<String> grant = restTemplate.exchange(
                base("/admin/security/users/" + secondAdminId + "/role"), HttpMethod.POST,
                new HttpEntity<>(Map.of("role", "SECURITY_ADMIN", "reason", "onboarding a second security admin"),
                        bearer(firstAdminToken)),
                String.class);
        assertEquals(HttpStatus.OK, grant.getStatusCode(), grant.getBody());

        // the promotion is on the audit trail, actor = the first security admin
        ResponseEntity<Map> audit = restTemplate.exchange(
                base("/admin/security/audit?userId=" + secondAdminId), HttpMethod.GET,
                new HttpEntity<>(bearer(firstAdminToken)), Map.class);
        List<?> rows = (List<?>) audit.getBody().get("data");
        assertNotNull(rows);
        assertEquals(1, rows.size());
        Map<?, ?> row = (Map<?, ?>) rows.getFirst();
        assertEquals("ROLE_GRANTED", row.get("action"));
        assertEquals("SECURITY_ADMIN", row.get("toValue"));

        // the promotion bumped credentialsValidFrom, so the second account re-logs in to pick it up
        String secondAdminToken = login(secondAdminEmail);

        // and can now use security-admin powers itself — e.g. list every account
        ResponseEntity<String> listUsers = restTemplate.exchange(
                base("/admin/security/users"), HttpMethod.GET,
                new HttpEntity<>(bearer(secondAdminToken)), String.class);
        assertEquals(HttpStatus.OK, listUsers.getStatusCode());
    }

    @Test
    void adminApplicationApproval_isSecurityAdminOnly_andWritesAuditRow() {
        String secAdminEmail = "sec_it_admin3_" + UUID.randomUUID() + "@mail.com";
        String applicantEmail = "sec_it_applicant_" + UUID.randomUUID() + "@mail.com";
        register(secAdminEmail);
        register(applicantEmail);
        promoteToSecurityAdmin(secAdminEmail);

        String secAdminToken = login(secAdminEmail);
        String applicantOldToken = login(applicantEmail);
        UUID applicantId = userIdOf(applicantEmail);

        // applicant submits an admin access application
        assertEquals(HttpStatus.CREATED, restTemplate.exchange(
                base("/admin/applications"), HttpMethod.POST,
                new HttpEntity<>(Map.of("municipalityToken", "GPJHB2025"), bearer(applicantOldToken)),
                String.class).getStatusCode());

        // a civilian cannot review the queue
        assertEquals(HttpStatus.FORBIDDEN, restTemplate.exchange(
                base("/admin/applications"), HttpMethod.GET,
                new HttpEntity<>(bearer(applicantOldToken)), String.class).getStatusCode());

        // the security admin can — grab this applicant's pending row
        ResponseEntity<Map> queue = restTemplate.exchange(
                base("/admin/applications?status=PENDING"), HttpMethod.GET,
                new HttpEntity<>(bearer(secAdminToken)), Map.class);
        assertEquals(HttpStatus.OK, queue.getStatusCode());
        List<?> pending = (List<?>) queue.getBody().get("data");
        assertNotNull(pending);
        String applicationId = pending.stream()
                .map(r -> (Map<?, ?>) r)
                .filter(r -> applicantId.toString().equals(r.get("userId")))
                .map(r -> (String) r.get("applicationId"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no pending application for the applicant"));

        sleepPastTokenSecond();

        // approve
        assertEquals(HttpStatus.OK, restTemplate.exchange(
                base("/admin/applications/" + applicationId + "/approve"), HttpMethod.POST,
                new HttpEntity<>(null, bearer(secAdminToken)), String.class).getStatusCode());

        // applicant's pre-approval token is now rejected — the promotion bumped credentialsValidFrom
        assertEquals(HttpStatus.UNAUTHORIZED, restTemplate.exchange(
                base("/incidents/my"), HttpMethod.GET,
                new HttpEntity<>(bearer(applicantOldToken)), String.class).getStatusCode());

        // the promotion is on the access-control audit trail (login also writes USER_LOGIN, so size >= 1)
        ResponseEntity<Map> audit = restTemplate.exchange(
                base("/admin/security/audit?userId=" + applicantId), HttpMethod.GET,
                new HttpEntity<>(bearer(secAdminToken)), Map.class);
        assertEquals(HttpStatus.OK, audit.getStatusCode());
        List<?> rows = (List<?>) audit.getBody().get("data");
        assertNotNull(rows);
        assertTrue(rows.size() >= 1);
        // newest first — ROLE_GRANTED was written after login
        Map<?, ?> row = (Map<?, ?>) rows.getFirst();
        assertEquals("ROLE_GRANTED", row.get("action"));
        assertEquals("CIVILIAN", row.get("fromValue"));
        assertEquals("ADMIN", row.get("toValue"));
        assertEquals(applicantId.toString(), row.get("targetId"));
    }
}
