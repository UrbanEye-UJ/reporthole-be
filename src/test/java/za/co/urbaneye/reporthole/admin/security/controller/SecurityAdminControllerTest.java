package za.co.urbaneye.reporthole.admin.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.urbaneye.reporthole.admin.security.dto.AuditEntryResponse;
import za.co.urbaneye.reporthole.admin.security.dto.GrantRoleRequest;
import za.co.urbaneye.reporthole.admin.security.dto.SecurityUserResponse;
import za.co.urbaneye.reporthole.admin.security.entity.AccessControlAction;
import za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException;
import za.co.urbaneye.reporthole.admin.security.service.interfaces.ISecurityAdminService;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.security.Jwt;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;

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

/**
 * Slice tests for {@link SecurityAdminController} — request wiring, validation, and the
 * exception-to-HTTP-status mapping supplied by the global handler.
 */
@WebMvcTest(SecurityAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class SecurityAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ISecurityAdminService securityAdminService;

    @MockitoBean
    private Jwt jwt;

    @MockitoBean
    private DashcamDeviceRepository dashcamDeviceRepository;

    @MockitoBean
    private IUserAuthRepository userAuthRepository;

    private static final UUID TARGET = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void listUsers_returnsAccounts() throws Exception {
        SecurityUserResponse row = new SecurityUserResponse(
                TARGET, "Terry Target", "terry@example.com",
                za.co.urbaneye.reporthole.user.entity.UserRole.CIVILIAN,
                za.co.urbaneye.reporthole.user.entity.UserStatus.ACTIVE,
                LocalDateTime.now());
        when(securityAdminService.listUsers()).thenReturn(List.of(row));

        mockMvc.perform(get("/admin/security/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(TARGET.toString()))
                .andExpect(jsonPath("$.data[0].email").value("terry@example.com"))
                .andExpect(jsonPath("$.data[0].role").value("CIVILIAN"));
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void grantRole_valid_returns200() throws Exception {
        doNothing().when(securityAdminService).grantRole(eq(TARGET), any(GrantRoleRequest.class));

        mockMvc.perform(post("/admin/security/users/" + TARGET + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GrantRoleRequest(UserRole.ADMIN, "Q3 rollout"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role granted."));
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void grantRole_blankReason_returns400() throws Exception {
        mockMvc.perform(post("/admin/security/users/" + TARGET + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void grantRole_callerNotSecurityAdmin_returns403() throws Exception {
        doThrow(new SecurityAdminException("Only security admins can perform this action"))
                .when(securityAdminService).grantRole(eq(TARGET), any(GrantRoleRequest.class));

        mockMvc.perform(post("/admin/security/users/" + TARGET + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GrantRoleRequest(UserRole.ADMIN, "x"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void grantRole_selfTarget_returns400() throws Exception {
        doThrow(new SecurityAdminException("A security admin cannot change your own role"))
                .when(securityAdminService).grantRole(eq(TARGET), any(GrantRoleRequest.class));

        mockMvc.perform(post("/admin/security/users/" + TARGET + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GrantRoleRequest(UserRole.ADMIN, "x"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void grantRole_alreadyHasRole_returns409() throws Exception {
        doThrow(new SecurityAdminException("Account already has role ADMIN"))
                .when(securityAdminService).grantRole(eq(TARGET), any(GrantRoleRequest.class));

        mockMvc.perform(post("/admin/security/users/" + TARGET + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GrantRoleRequest(UserRole.ADMIN, "x"))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void grantRole_targetNotFound_returns404() throws Exception {
        doThrow(new SecurityAdminException("Target user not found"))
                .when(securityAdminService).grantRole(eq(TARGET), any(GrantRoleRequest.class));

        mockMvc.perform(post("/admin/security/users/" + TARGET + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GrantRoleRequest(UserRole.ADMIN, "x"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void revokeRole_valid_returns200() throws Exception {
        mockMvc.perform(post("/admin/security/users/" + TARGET + "/revoke-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"left the municipality\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role revoked."));
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void suspend_valid_returns200() throws Exception {
        mockMvc.perform(post("/admin/security/users/" + TARGET + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"suspicious activity\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account suspended."));
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void reactivate_valid_returns200() throws Exception {
        mockMvc.perform(post("/admin/security/users/" + TARGET + "/reactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"cleared\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account reactivated."));
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void forceLogout_valid_returns200() throws Exception {
        mockMvc.perform(post("/admin/security/users/" + TARGET + "/force-logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"lost device\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sessions revoked."));
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void listAudit_returnsTrail() throws Exception {
        AuditEntryResponse entry = new AuditEntryResponse(
                UUID.randomUUID(), AccessControlAction.ROLE_GRANTED,
                UUID.randomUUID(), "Sam Secure",
                TARGET, "Terry Target",
                "CIVILIAN", "ADMIN", "Q3 rollout", LocalDateTime.now());
        when(securityAdminService.listAudit(null)).thenReturn(List.of(entry));

        mockMvc.perform(get("/admin/security/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("ROLE_GRANTED"))
                .andExpect(jsonPath("$.data[0].toValue").value("ADMIN"));
    }
}
