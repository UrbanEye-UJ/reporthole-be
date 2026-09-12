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
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.security.Jwt;
import za.co.urbaneye.reporthole.user.controller.AuthServiceController;
import za.co.urbaneye.reporthole.user.dto.AuthResponse;
import za.co.urbaneye.reporthole.user.dto.ForgotPasswordRequest;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.dto.ResetPasswordRequest;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.exception.UserServiceException;
import za.co.urbaneye.reporthole.user.service.interfaces.ILoginService;
import za.co.urbaneye.reporthole.user.service.interfaces.IPasswordResetService;
import za.co.urbaneye.reporthole.user.service.interfaces.IRegistrationService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ILoginService service;

    @MockitoBean
    private IRegistrationService registrationService;

    @MockitoBean
    private IPasswordResetService passwordResetService;

    @MockitoBean
    private Jwt jwt;

    @MockitoBean
    private DashcamDeviceRepository dashcamDeviceRepository;

    /** Required by JwtAuthenticationFilter, which now does a per-request account-status lookup. */
    @MockitoBean
    private IUserAuthRepository userAuthRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterUser() throws Exception {
        RegisterRequest request =
                new RegisterRequest("John", "Doe", "john@mail.com", UserRole.CIVILIAN, "Test@Pass1", "0711111111", null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldLoginUser() throws Exception {
        LoginRequest request = new LoginRequest("john@mail.com", "123");

        Mockito.when(service.loginUser(any()))
                .thenReturn(new AuthResponse("jwt-token", UserRole.CIVILIAN, UUID.randomUUID()));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.role").value("CIVILIAN"));
    }

    @Test
    void forgotPassword_returns202_whenEmailFound() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("john@mail.com");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    void forgotPassword_returns404_whenEmailNotFound() throws Exception {
        Mockito.doThrow(new UserServiceException("User not found"))
                .when(passwordResetService).requestReset(anyString());

        ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@mail.com");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void resetPassword_returns200_onSuccess() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "NewPass@1");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_returns410_whenTokenExpiredOrUsed() throws Exception {
        Mockito.doThrow(new UserServiceException("Invalid or expired reset link"))
                .when(passwordResetService).resetPassword(anyString(), anyString());

        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "NewPass@1");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone());
    }
}
