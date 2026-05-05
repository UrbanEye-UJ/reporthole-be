package za.co.urbaneye.reporthole.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.entity.UserRole;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRegisterAndLogin() {

        String url = "http://localhost:" + port + "/api/auth/register";

        RegisterRequest register =
                new RegisterRequest("John","Doe","john@mail.com", UserRole.CIVILIAN,"123","0711111111");

        ResponseEntity<Void> registerResponse =
                restTemplate.postForEntity(url, register, Void.class);

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        LoginRequest login = new LoginRequest("john@mail.com","123");

        ResponseEntity<String> loginResponse =
                restTemplate.postForEntity(
                        "http://localhost:" + port + "/api/auth/login",
                        login,
                        String.class
                );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    }
}