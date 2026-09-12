package za.co.urbaneye.reporthole.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.inference.controller.InferenceController;
import za.co.urbaneye.reporthole.inference.dto.EscalatedFrameDTO;
import za.co.urbaneye.reporthole.inference.service.OnnxInferenceService;
import za.co.urbaneye.reporthole.inference.service.interfaces.IFrameSubmissionService;
import za.co.urbaneye.reporthole.security.Jwt;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for the human-review endpoints on {@link InferenceController}.
 *
 * <p>Uses {@link AutoConfigureMockMvc} with filters disabled and injects auth via
 * a {@link RequestPostProcessor} to avoid thread-local issues in CI environments.</p>
 */
@WebMvcTest(InferenceController.class)
@AutoConfigureMockMvc(addFilters = false)
class InferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IFrameSubmissionService frameSubmissionService;

    @MockitoBean
    private OnnxInferenceService inferenceService;

    @MockitoBean
    private Jwt jwt;

    @MockitoBean
    private DashcamDeviceRepository dashcamDeviceRepository;

    @MockitoBean
    private IUserAuthRepository userAuthRepository;

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID FRAME_ID = UUID.randomUUID();

    private RequestPostProcessor adminAuth() {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            ADMIN_ID.toString(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    )
            );
            return request;
        };
    }

    @Test
    void getEscalatedFrames_returnsListOfFrames_whenAdmin() throws Exception {
        EscalatedFrameDTO dto = new EscalatedFrameDTO(
                FRAME_ID, "POTHOLE", 0.70, LocalDateTime.now(), null
        );
        when(frameSubmissionService.getEscalatedFrames()).thenReturn(List.of(dto));

        mockMvc.perform(get("/inference/escalated").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].frameId").value(FRAME_ID.toString()))
                .andExpect(jsonPath("$.data[0].label").value("POTHOLE"));
    }

    @Test
    void getEscalatedFrames_returnsEmptyList_whenNoEscalatedFrames() throws Exception {
        when(frameSubmissionService.getEscalatedFrames()).thenReturn(List.of());

        mockMvc.perform(get("/inference/escalated").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void approveFrame_returns204_whenFrameExists() throws Exception {
        doNothing().when(frameSubmissionService).approveFrame(FRAME_ID);

        mockMvc.perform(post("/inference/escalated/{frameId}/approve", FRAME_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(frameSubmissionService).approveFrame(FRAME_ID);
    }

    @Test
    void approveFrame_returns404_whenFrameNotFound() throws Exception {
        doThrow(new NoSuchElementException("not found"))
                .when(frameSubmissionService).approveFrame(FRAME_ID);

        mockMvc.perform(post("/inference/escalated/{frameId}/approve", FRAME_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void discardFrame_returns204_whenFrameExists() throws Exception {
        doNothing().when(frameSubmissionService).discardFrame(FRAME_ID);

        mockMvc.perform(post("/inference/escalated/{frameId}/discard", FRAME_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(frameSubmissionService).discardFrame(FRAME_ID);
    }

    @Test
    void discardFrame_returns404_whenFrameNotFound() throws Exception {
        doThrow(new NoSuchElementException("not found"))
                .when(frameSubmissionService).discardFrame(FRAME_ID);

        mockMvc.perform(post("/inference/escalated/{frameId}/discard", FRAME_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
