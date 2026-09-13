package za.co.urbaneye.reporthole.admin.contractor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.urbaneye.reporthole.admin.contractor.dto.CompleteContractorRegistrationRequest;
import za.co.urbaneye.reporthole.admin.contractor.dto.ContractorResponse;
import za.co.urbaneye.reporthole.admin.contractor.exception.ContractorException;
import za.co.urbaneye.reporthole.admin.contractor.service.interfaces.IContractorService;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.security.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractorRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContractorRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IContractorService contractorService;

    @MockitoBean
    private Jwt jwt;

    @MockitoBean
    private DashcamDeviceRepository dashcamDeviceRepository;

    /** Required by JwtAuthenticationFilter, which now does a per-request account-status lookup. */
    @MockitoBean
    private IUserAuthRepository userAuthRepository;

    private CompleteContractorRegistrationRequest validRequest() {
        return new CompleteContractorRegistrationRequest(
                UUID.randomUUID(), "Con", "Tractor", "0123456789", "Passw0rd!");
    }

    @Test
    void completeRegistration_validToken_returns200WithProfile() throws Exception {
        UUID contractorId = UUID.randomUUID();
        ContractorResponse response = new ContractorResponse(
                contractorId, "Con", "Tractor", "con.tractor@example.com",
                "0123456789", 0, 0, LocalDateTime.now(), List.of(IssueType.POTHOLE));
        when(contractorService.completeContractorRegistration(any())).thenReturn(response);

        mockMvc.perform(post("/contractors/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Con"))
                .andExpect(jsonPath("$.data.email").value("con.tractor@example.com"));
    }

    @Test
    void completeRegistration_invalidToken_returns400() throws Exception {
        when(contractorService.completeContractorRegistration(any()))
                .thenThrow(new ContractorException("Invalid invite token"));

        mockMvc.perform(post("/contractors/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeRegistration_expiredToken_returns400() throws Exception {
        when(contractorService.completeContractorRegistration(any()))
                .thenThrow(new ContractorException("This invite has expired"));

        mockMvc.perform(post("/contractors/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeRegistration_missingToken_returns400() throws Exception {
        CompleteContractorRegistrationRequest badRequest = new CompleteContractorRegistrationRequest(
                null, "Con", "Tractor", "0123456789", "Passw0rd!");

        mockMvc.perform(post("/contractors/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeRegistration_weakPassword_returns400() throws Exception {
        CompleteContractorRegistrationRequest badRequest = new CompleteContractorRegistrationRequest(
                UUID.randomUUID(), "Con", "Tractor", "0123456789", "weakpassword");

        mockMvc.perform(post("/contractors/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }
}
