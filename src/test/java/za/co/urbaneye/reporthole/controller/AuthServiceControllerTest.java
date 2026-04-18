package za.co.urbaneye.reporthole.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.urbaneye.reporthole.security.Jwt;
import za.co.urbaneye.reporthole.user.controller.AuthServiceController;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.service.interfaces.IUserAuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUserAuthService service;

    @MockitoBean
    private Jwt jwt; // satisfies JwtAuthenticationFilter's constructor dependency


    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterUser() throws Exception {

        RegisterRequest request =
                new RegisterRequest("John","Doe","john@mail.com","","CIVILIAN","123","0711111111");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldLoginUser() throws Exception {

        LoginRequest request = new LoginRequest("john@mail.com","123");

        Mockito.when(service.loginUser(any())).thenReturn("jwt-token");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("jwt-token"));
    }
}