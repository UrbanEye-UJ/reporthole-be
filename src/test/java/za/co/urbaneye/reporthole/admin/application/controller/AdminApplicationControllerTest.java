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
import za.co.urbaneye.reporthole.admin.application.exception.AdminApplicationException;
import za.co.urbaneye.reporthole.admin.application.service.interfaces.IAdminApplicationService;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.security.Jwt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
}
