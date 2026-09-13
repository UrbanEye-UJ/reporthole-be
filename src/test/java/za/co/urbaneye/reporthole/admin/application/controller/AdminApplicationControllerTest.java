package za.co.urbaneye.reporthole.admin.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationRequest;
import za.co.urbaneye.reporthole.admin.application.dto.AdminApplicationResponse;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplicationStatus;
import za.co.urbaneye.reporthole.admin.application.exception.AdminApplicationException;
import za.co.urbaneye.reporthole.admin.application.service.interfaces.IAdminApplicationService;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.security.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IAdminApplicationService adminApplicationService;

    @MockitoBean
    private Jwt jwt;

    @MockitoBean
    private DashcamDeviceRepository dashcamDeviceRepository;

    /** Required by JwtAuthenticationFilter, which now does a per-request account-status lookup. */
    @MockitoBean
    private IUserAuthRepository userAuthRepository;

    @Test
    @WithMockUser
    void apply_validRequest_returns201() throws Exception {
        doNothing().when(adminApplicationService).apply(any());

        mockMvc.perform(post("/admin/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminApplicationRequest("GPJHB2025"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Application submitted. We'll be in touch."));
    }

    @Test
    @WithMockUser
    void apply_duplicateSubmission_returns409() throws Exception {
        doThrow(new AdminApplicationException("Application already submitted"))
                .when(adminApplicationService).apply(any());

        mockMvc.perform(post("/admin/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminApplicationRequest("GPJHB2025"))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void apply_alreadyAdmin_returns400() throws Exception {
        doThrow(new AdminApplicationException("Account is already ADMIN"))
                .when(adminApplicationService).apply(any());

        mockMvc.perform(post("/admin/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminApplicationRequest("GPJHB2025"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void apply_missingToken_returns400() throws Exception {
        mockMvc.perform(post("/admin/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminApplicationRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void listApplications_returnsAll_whenNoStatusFilter() throws Exception {
        AdminApplicationResponse response = new AdminApplicationResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Bob", "Builder", "bob@example.com",
                "GPJHB2025", "City of Johannesburg", LocalDateTime.now(), AdminApplicationStatus.APPROVED);
        when(adminApplicationService.listApplications(null)).thenReturn(List.of(response));

        mockMvc.perform(get("/admin/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].applicantFirstName").value("Bob"))
                .andExpect(jsonPath("$.data[0].municipalityName").value("City of Johannesburg"))
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
    }

    @Test
    @WithMockUser
    void listApplications_passesStatusFilterThrough() throws Exception {
        when(adminApplicationService.listApplications(AdminApplicationStatus.PENDING))
                .thenReturn(List.of());

        mockMvc.perform(get("/admin/applications").param("status", "PENDING"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void listApplications_callerNotSecurityAdmin_returns403() throws Exception {
        when(adminApplicationService.listApplications(null))
                .thenThrow(new AdminApplicationException("Only security admins can perform this action"));

        mockMvc.perform(get("/admin/applications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void approve_validApplication_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(adminApplicationService).approve(id);

        mockMvc.perform(post("/admin/applications/" + id + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Application approved."));
    }

    @Test
    @WithMockUser
    void approve_alreadyProcessed_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new AdminApplicationException("Application has already been processed"))
                .when(adminApplicationService).approve(id);

        mockMvc.perform(post("/admin/applications/" + id + "/approve"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void reject_validApplication_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(adminApplicationService).reject(id);

        mockMvc.perform(post("/admin/applications/" + id + "/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Application rejected."));
    }

    @Test
    @WithMockUser
    void reject_applicationNotFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new AdminApplicationException("Application not found"))
                .when(adminApplicationService).reject(id);

        mockMvc.perform(post("/admin/applications/" + id + "/reject"))
                .andExpect(status().isNotFound());
    }
}
