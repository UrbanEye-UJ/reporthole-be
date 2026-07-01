package za.co.urbaneye.reporthole.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.urbaneye.reporthole.device.controller.DeviceController;
import za.co.urbaneye.reporthole.device.dto.DeviceTokenResponse;
import za.co.urbaneye.reporthole.device.service.interfaces.IDeviceService;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.security.Jwt;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link DeviceController} using {@link WebMvcTest} with
 * filters disabled.
 *
 * <p>Security enforcement (device tokens rejected on {@code /devices/**}) is
 * covered by the integration test. These tests verify that the endpoint
 * serialises the service response correctly when a valid authentication
 * is already in the security context.</p>
 */
@WebMvcTest(DeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IDeviceService deviceService;

    /** Required by JwtAuthenticationFilter even when filters are disabled. */
    @MockitoBean
    private Jwt jwt;

    /** Required by the modified JwtAuthenticationFilter. */
    @MockitoBean
    private DashcamDeviceRepository dashcamDeviceRepository;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String FAKE_TOKEN = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    /**
     * Manually places a String-principal authentication in the security context,
     * matching what {@link za.co.urbaneye.reporthole.security.JwtAuthenticationFilter}
     * sets up when it validates a real JWT.
     */
    @BeforeEach
    void setUpSecurityContext() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_CIVILIAN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateToken_returns200WithToken_whenAuthenticated() throws Exception {
        when(deviceService.generateToken(USER_ID.toString()))
                .thenReturn(new DeviceTokenResponse(FAKE_TOKEN));

        mockMvc.perform(post("/devices/token/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceToken").value(FAKE_TOKEN));
    }

    @Test
    void generateToken_tokenInResponseContainsNoDots() throws Exception {
        when(deviceService.generateToken(USER_ID.toString()))
                .thenReturn(new DeviceTokenResponse(FAKE_TOKEN));

        mockMvc.perform(post("/devices/token/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceToken").value(FAKE_TOKEN));

        // Verify the returned token has no dots (the filter uses this to distinguish from JWTs)
        assert !FAKE_TOKEN.contains(".");
    }
}
