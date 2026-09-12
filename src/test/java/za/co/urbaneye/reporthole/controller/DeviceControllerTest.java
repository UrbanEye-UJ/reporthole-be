package za.co.urbaneye.reporthole.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import za.co.urbaneye.reporthole.device.controller.DeviceController;
import za.co.urbaneye.reporthole.device.dto.DeviceTokenResponse;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.device.service.interfaces.IDeviceService;
import za.co.urbaneye.reporthole.security.Jwt;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link DeviceController} using {@link WebMvcTest} with filters disabled.
 *
 * <p>Authentication is injected via a custom {@link RequestPostProcessor} that sets
 * {@code SecurityContextHolder} at request-dispatch time rather than in {@code @BeforeEach}.
 * This is reliable on CI because the post-processor runs on the same thread that
 * MockMvc uses to process the request, avoiding the {@code ThreadLocal} inheritance
 * issue that causes {@code @BeforeEach}-based setup to fail on pooled-thread environments.</p>
 *
 * <p>Security enforcement (device tokens rejected on {@code /devices/**}) is
 * covered by {@link za.co.urbaneye.reporthole.integration.DeviceIntegrationTest}.</p>
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

    /** Required by JwtAuthenticationFilter, which now does a per-request account-status lookup. */
    @MockitoBean
    private IUserAuthRepository userAuthRepository;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String FAKE_TOKEN = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    /**
     * Returns a {@link RequestPostProcessor} that sets a CIVILIAN-role authentication
     * on the dispatch thread's {@code SecurityContextHolder}, matching what
     * {@link za.co.urbaneye.reporthole.security.JwtAuthenticationFilter} sets for a valid JWT.
     */
    private RequestPostProcessor civilianAuth() {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            USER_ID.toString(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_CIVILIAN"))
                    )
            );
            return request;
        };
    }

    @Test
    void generateToken_returns200WithToken_whenAuthenticated() throws Exception {
        when(deviceService.generateToken(USER_ID.toString()))
                .thenReturn(new DeviceTokenResponse(FAKE_TOKEN));

        mockMvc.perform(post("/devices/token/generate")
                        .with(civilianAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceToken").value(FAKE_TOKEN));
    }
}
