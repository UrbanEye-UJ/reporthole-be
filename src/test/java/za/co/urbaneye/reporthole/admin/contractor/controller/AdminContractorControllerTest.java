package za.co.urbaneye.reporthole.admin.contractor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.urbaneye.reporthole.admin.contractor.dto.ContractorResponse;
import za.co.urbaneye.reporthole.admin.contractor.dto.InviteContractorRequest;
import za.co.urbaneye.reporthole.admin.contractor.dto.RevealEmailRequest;
import za.co.urbaneye.reporthole.admin.contractor.dto.RevealEmailResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminContractorController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminContractorControllerTest {

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

    @Test
    @WithMockUser
    void inviteContractor_validRequest_returns200() throws Exception {
        doNothing().when(contractorService).inviteContractor(any());

        mockMvc.perform(post("/admin/contractors/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InviteContractorRequest("con.tractor@example.com", List.of(IssueType.POTHOLE)))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void inviteContractor_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/admin/contractors/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InviteContractorRequest("", List.of(IssueType.POTHOLE)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void inviteContractor_callerNotAdmin_returns403() throws Exception {
        doThrow(new ContractorException("Only admins can manage contractors"))
                .when(contractorService).inviteContractor(any());

        mockMvc.perform(post("/admin/contractors/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InviteContractorRequest("con.tractor@example.com", List.of(IssueType.POTHOLE)))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void inviteContractor_emailAlreadyInvited_returns409() throws Exception {
        doThrow(new ContractorException("An invite has already been sent to this email"))
                .when(contractorService).inviteContractor(any());

        mockMvc.perform(post("/admin/contractors/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InviteContractorRequest("con.tractor@example.com", List.of(IssueType.POTHOLE)))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void getContractors_returnsMaskedList() throws Exception {
        ContractorResponse response = new ContractorResponse(
                UUID.randomUUID(), "Con", "Tractor", "c***@example.com", "0123456789", 1, 2, LocalDateTime.now(),
                List.of(IssueType.POTHOLE));
        when(contractorService.getContractors()).thenReturn(List.of(response));

        mockMvc.perform(get("/admin/contractors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email").value("c***@example.com"));
    }

    @Test
    @WithMockUser
    void getContractors_callerNotAdmin_returns403() throws Exception {
        when(contractorService.getContractors())
                .thenThrow(new ContractorException("Only admins can manage contractors"));

        mockMvc.perform(get("/admin/contractors"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void revealEmail_correctPassword_returns200WithFullEmail() throws Exception {
        UUID id = UUID.randomUUID();
        when(contractorService.revealEmail(eq(id), any()))
                .thenReturn(new RevealEmailResponse("con.tractor@example.com"));

        mockMvc.perform(post("/admin/contractors/" + id + "/reveal-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RevealEmailRequest("correct-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("con.tractor@example.com"));
    }

    @Test
    @WithMockUser
    void revealEmail_incorrectPassword_returns401() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ContractorException("Incorrect password"))
                .when(contractorService).revealEmail(eq(id), any());

        mockMvc.perform(post("/admin/contractors/" + id + "/reveal-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RevealEmailRequest("wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void revealEmail_missingPassword_returns400() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/admin/contractors/" + id + "/reveal-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RevealEmailRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void revealEmail_contractorNotFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ContractorException("Contractor not found: " + id))
                .when(contractorService).revealEmail(eq(id), any());

        mockMvc.perform(post("/admin/contractors/" + id + "/reveal-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RevealEmailRequest("correct-password"))))
                .andExpect(status().isNotFound());
    }
}
