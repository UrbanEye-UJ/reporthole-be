package za.co.urbaneye.reporthole.admin.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.urbaneye.reporthole.admin.user.dto.CivilianSummaryResponse;
import za.co.urbaneye.reporthole.admin.user.service.interfaces.IAdminUserService;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.security.Jwt;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for {@link AdminUserController}: response shape and service delegation.
 * Security filters are disabled to focus on controller behaviour; role access is covered
 * in the integration tests.
 */
@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private IAdminUserService adminUserService;
    @MockitoBean private Jwt jwt;
    @MockitoBean private DashcamDeviceRepository dashcamDeviceRepository;
    @MockitoBean private IUserAuthRepository userAuthRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCivilians_returnsListWithMaskedFields() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserService.getCivilians()).thenReturn(List.of(
                new CivilianSummaryResponse(id, "Alice K.", "a***@example.com", 5L, UserStatus.ACTIVE, LocalDateTime.now())
        ));

        mockMvc.perform(get("/admin/users/civilians").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].maskedName").value("Alice K."))
                .andExpect(jsonPath("$.data[0].maskedEmail").value("a***@example.com"))
                .andExpect(jsonPath("$.data[0].incidentCount").value(5));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCivilians_emptyList_returns200WithEmptyData() throws Exception {
        when(adminUserService.getCivilians()).thenReturn(List.of());

        mockMvc.perform(get("/admin/users/civilians").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
