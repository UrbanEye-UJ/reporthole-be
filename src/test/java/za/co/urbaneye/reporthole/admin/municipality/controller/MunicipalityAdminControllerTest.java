package za.co.urbaneye.reporthole.admin.municipality.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.urbaneye.reporthole.admin.municipality.dto.CreateMunicipalityRequest;
import za.co.urbaneye.reporthole.admin.municipality.dto.MunicipalityResponse;
import za.co.urbaneye.reporthole.admin.municipality.dto.MunicipalityTokenResponse;
import za.co.urbaneye.reporthole.admin.municipality.exception.MunicipalityException;
import za.co.urbaneye.reporthole.admin.municipality.service.interfaces.IMunicipalityService;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.security.Jwt;
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
 * Slice tests for {@link MunicipalityAdminController} — wiring, validation, and the
 * exception-to-status mapping from the global handler.
 */
@WebMvcTest(MunicipalityAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class MunicipalityAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IMunicipalityService municipalityService;

    @MockitoBean
    private Jwt jwt;

    @MockitoBean
    private DashcamDeviceRepository dashcamDeviceRepository;

    @MockitoBean
    private IUserAuthRepository userAuthRepository;

    private static final UUID MUNI_ID = UUID.randomUUID();

    private MunicipalityResponse municipalityResponse() {
        return new MunicipalityResponse(MUNI_ID, "City of Tshwane", "Gauteng", 0L, LocalDateTime.now());
    }

    private MunicipalityTokenResponse tokenResponse() {
        return new MunicipalityTokenResponse(UUID.randomUUID(), "MUNI-ABCDEFGHJKLM", MUNI_ID,
                "City of Tshwane", "Sam Secure", LocalDateTime.now(), null, null,
                MunicipalityTokenResponse.Status.ACTIVE);
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void create_valid_returns201() throws Exception {
        when(municipalityService.createMunicipality(any())).thenReturn(municipalityResponse());

        mockMvc.perform(post("/admin/municipalities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMunicipalityRequest("City of Tshwane", "Gauteng"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("City of Tshwane"));
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/admin/municipalities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"province\":\"Gauteng\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void create_duplicateName_returns409() throws Exception {
        doThrow(new MunicipalityException("A municipality with that name already exists"))
                .when(municipalityService).createMunicipality(any());

        mockMvc.perform(post("/admin/municipalities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMunicipalityRequest("City of Tshwane", null))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void create_callerNotSecurityAdmin_returns403() throws Exception {
        doThrow(new MunicipalityException("Only security admins can perform this action"))
                .when(municipalityService).createMunicipality(any());

        mockMvc.perform(post("/admin/municipalities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMunicipalityRequest("X", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void list_returnsMunicipalities() throws Exception {
        when(municipalityService.listMunicipalities()).thenReturn(List.of(municipalityResponse()));

        mockMvc.perform(get("/admin/municipalities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("City of Tshwane"));
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void issueToken_valid_returns201() throws Exception {
        when(municipalityService.issueToken(eq(MUNI_ID), any())).thenReturn(tokenResponse());

        mockMvc.perform(post("/admin/municipalities/" + MUNI_ID + "/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expiresInDays\":7,\"note\":\"for Jane\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").value("MUNI-ABCDEFGHJKLM"));
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void issueToken_municipalityNotFound_returns404() throws Exception {
        doThrow(new MunicipalityException("Municipality not found"))
                .when(municipalityService).issueToken(eq(MUNI_ID), any());

        mockMvc.perform(post("/admin/municipalities/" + MUNI_ID + "/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void listTokens_returnsTokens() throws Exception {
        when(municipalityService.listTokens(null)).thenReturn(List.of(tokenResponse()));

        mockMvc.perform(get("/admin/municipalities/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void revokeToken_valid_returns200() throws Exception {
        UUID tokenId = UUID.randomUUID();
        doNothing().when(municipalityService).revokeToken(tokenId);

        mockMvc.perform(post("/admin/municipalities/tokens/" + tokenId + "/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token revoked."));
    }

    @Test
    @WithMockUser(roles = "SECURITY_ADMIN")
    void revokeToken_alreadyRevoked_returns409() throws Exception {
        UUID tokenId = UUID.randomUUID();
        doThrow(new MunicipalityException("Token is already revoked"))
                .when(municipalityService).revokeToken(tokenId);

        mockMvc.perform(post("/admin/municipalities/tokens/" + tokenId + "/revoke"))
                .andExpect(status().isConflict());
    }
}
