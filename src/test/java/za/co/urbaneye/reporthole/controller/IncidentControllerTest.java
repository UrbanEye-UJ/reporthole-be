package za.co.urbaneye.reporthole.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.incident.controller.IncidentController;
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
import za.co.urbaneye.reporthole.incident.entity.IncidentSource;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.incident.service.impl.IncidentSseService;
import za.co.urbaneye.reporthole.incident.service.interfaces.IncidentService;
import za.co.urbaneye.reporthole.security.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentController.class)
@AutoConfigureMockMvc(addFilters = false)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncidentService incidentService;

    @MockitoBean
    private IncidentSseService incidentSseService;

    @MockitoBean
    private Jwt jwt;

    /** Required by the updated JwtAuthenticationFilter which now also handles device tokens. */
    @MockitoBean
    private DashcamDeviceRepository dashcamDeviceRepository;

    private IncidentRequestDTO buildRequest() {
        return new IncidentRequestDTO(IssueType.POTHOLE, "Big pothole", IncidentSource.MANUAL, -26.2041, 28.0473, "base64data", false, null);
    }

    @Test
    void createIncident_returnsCreated_whenNoDuplicate() throws Exception {
        IncidentResponseDTO response = IncidentResponseDTO.builder()
                .incidentId(UUID.randomUUID())
                .incidentType(IssueType.POTHOLE)
                .description("Big pothole")
                .source(IncidentSource.MANUAL)
                .incidentDate(LocalDateTime.now())
                .latitude(-26.2041)
                .longitude(28.0473)
                .userId(UUID.randomUUID())
                .reportCount(1)
                .duplicate(false)
                .build();

        when(incidentService.createIncident(any())).thenReturn(response);

        mockMvc.perform(post("/incidents/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.duplicate").value(false))
                .andExpect(jsonPath("$.data.incidentType").value("POTHOLE"));
    }

    @Test
    void createIncident_returnsOkWithDuplicateFlag_whenDuplicateDetected() throws Exception {
        UUID existingId = UUID.randomUUID();
        IncidentResponseDTO response = IncidentResponseDTO.builder()
                .incidentId(existingId)
                .incidentType(IssueType.POTHOLE)
                .description("Existing pothole")
                .source(IncidentSource.MANUAL)
                .incidentDate(LocalDateTime.now())
                .latitude(-26.2041)
                .longitude(28.0473)
                .userId(UUID.randomUUID())
                .reportCount(1)
                .duplicate(true)
                .existingIncidentId(existingId)
                .build();

        when(incidentService.createIncident(any())).thenReturn(response);

        mockMvc.perform(post("/incidents/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(true))
                .andExpect(jsonPath("$.data.existingIncidentId").value(existingId.toString()));
    }

    @Test
    void confirmDuplicate_returnsOkWithUpdatedCount() throws Exception {
        UUID id = UUID.randomUUID();
        IncidentResponseDTO response = IncidentResponseDTO.builder()
                .incidentId(id)
                .incidentType(IssueType.POTHOLE)
                .reportCount(2)
                .duplicate(true)
                .existingIncidentId(id)
                .build();

        when(incidentService.confirmDuplicate(id)).thenReturn(response);

        mockMvc.perform(post("/incidents/" + id + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportCount").value(2));
    }

    @Test
    void getMyIncidents_returnsOk() throws Exception {
        IncidentResponseDTO response = IncidentResponseDTO.builder()
                .incidentId(UUID.randomUUID())
                .incidentType(IssueType.POTHOLE)
                .description("Crack")
                .source(IncidentSource.MANUAL)
                .incidentDate(LocalDateTime.now())
                .latitude(-26.2041)
                .longitude(28.0473)
                .userId(UUID.randomUUID())
                .reportCount(1)
                .build();

        when(incidentService.getMyIncidents()).thenReturn(List.of(response));

        mockMvc.perform(get("/incidents/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].incidentType").value("POTHOLE"));
    }

    @Test
    void getIncidentById_returnsOk_whenFound() throws Exception {
        UUID id = UUID.randomUUID();
        IncidentResponseDTO response = IncidentResponseDTO.builder()
                .incidentId(id)
                .incidentType(IssueType.POTHOLE)
                .description("Deep pothole")
                .source(IncidentSource.MANUAL)
                .incidentDate(LocalDateTime.now())
                .latitude(-26.2041)
                .longitude(28.0473)
                .imageUrl("http://img/test.jpg")
                .userId(UUID.randomUUID())
                .reportCount(3)
                .duplicate(false)
                .build();

        when(incidentService.getIncidentById(id)).thenReturn(response);

        mockMvc.perform(get("/incidents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentId").value(id.toString()))
                .andExpect(jsonPath("$.data.incidentType").value("POTHOLE"))
                .andExpect(jsonPath("$.data.description").value("Deep pothole"))
                .andExpect(jsonPath("$.data.reportCount").value(3));
    }

    @Test
    void getIncidentById_returns500_whenServiceThrows() throws Exception {
        UUID id = UUID.randomUUID();
        when(incidentService.getIncidentById(id)).thenThrow(new RuntimeException("Incident not found: " + id));

        mockMvc.perform(get("/incidents/" + id))
                .andExpect(status().isInternalServerError());
    }
}
