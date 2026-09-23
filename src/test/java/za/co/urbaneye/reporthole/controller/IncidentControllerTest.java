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
import za.co.urbaneye.reporthole.idempotency.service.interfaces.IIdempotencyService;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.incident.clustering.IncidentClusteringService;
import za.co.urbaneye.reporthole.incident.controller.IncidentController;
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.dto.IncidentResponseDTO;
import za.co.urbaneye.reporthole.incident.dto.RejectAssignmentRequest;
import za.co.urbaneye.reporthole.incident.entity.IncidentSource;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.incident.service.impl.IncidentSseService;
import za.co.urbaneye.reporthole.incident.dto.IncidentCommentResponse;
import za.co.urbaneye.reporthole.incident.service.interfaces.IIncidentCommentService;
import za.co.urbaneye.reporthole.incident.service.interfaces.IncidentService;
import za.co.urbaneye.reporthole.security.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
    private IncidentClusteringService incidentClusteringService;

    @MockitoBean
    private Jwt jwt;

    @MockitoBean
    private IIncidentCommentService commentService;

    /** Required by the updated JwtAuthenticationFilter which now also handles device tokens. */
    @MockitoBean
    private DashcamDeviceRepository dashcamDeviceRepository;

    /** Required by JwtAuthenticationFilter, which now does a per-request account-status lookup. */
    @MockitoBean
    private IUserAuthRepository userAuthRepository;

    @MockitoBean
    private IIdempotencyService idempotencyService;

    private IncidentRequestDTO buildRequest() {
        return new IncidentRequestDTO(IssueType.POTHOLE, "Big pothole", IncidentSource.MANUAL, -26.2041, 28.0473, "base64data", false, null, null, null);
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

    @Test
    void reportStillUnresolved_returnsOk_whenIncidentWasResolved() throws Exception {
        UUID id = UUID.randomUUID();
        IncidentResponseDTO response = IncidentResponseDTO.builder()
                .incidentId(id)
                .incidentType(IssueType.POTHOLE)
                .status(za.co.urbaneye.reporthole.incident.entity.AssignmentStatus.VERIFIED)
                .build();

        when(incidentService.reportStillUnresolved(id)).thenReturn(response);

        mockMvc.perform(post("/incidents/" + id + "/still-unresolved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }

    @Test
    void reportStillUnresolved_returns400_whenIncidentNotResolved() throws Exception {
        UUID id = UUID.randomUUID();
        when(incidentService.reportStillUnresolved(id))
                .thenThrow(new za.co.urbaneye.reporthole.incident.exception.AssignmentException(
                        "Only resolved incidents can be reported as still unresolved"));

        mockMvc.perform(post("/incidents/" + id + "/still-unresolved"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addProgressUpdate_returnsOk_whenNoteIsValid() throws Exception {
        UUID id = UUID.randomUUID();
        IncidentResponseDTO response = IncidentResponseDTO.builder()
                .incidentId(id)
                .incidentType(IssueType.POTHOLE)
                .build();

        when(incidentService.addProgressUpdate(eq(id), eq("Pothole filled halfway"))).thenReturn(response);

        mockMvc.perform(post("/incidents/" + id + "/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Pothole filled halfway\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentId").value(id.toString()));
    }

    @Test
    void addProgressUpdate_returnsBadRequest_whenNoteIsBlank() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/incidents/" + id + "/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getNearbyIncidents_returnsOk() throws Exception {
        IncidentResponseDTO response = IncidentResponseDTO.builder()
                .incidentId(UUID.randomUUID())
                .incidentType(IssueType.POTHOLE)
                .latitude(-26.2041)
                .longitude(28.0473)
                .build();

        when(incidentService.getNearbyIncidents(-26.2041, 28.0473, 1000.0)).thenReturn(List.of(response));

        mockMvc.perform(get("/incidents/nearby?latitude=-26.2041&longitude=28.0473"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].incidentType").value("POTHOLE"));
    }

    @Test
    void rejectAssignment_returnsOk_whenReasonProvided() throws Exception {
        UUID id = UUID.randomUUID();
        IncidentResponseDTO response = IncidentResponseDTO.builder()
                .incidentId(id)
                .incidentType(IssueType.POTHOLE)
                .build();

        when(incidentService.rejectAssignment(eq(id), any())).thenReturn(response);

        mockMvc.perform(post("/incidents/" + id + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectAssignmentRequest("Not qualified for this repair type"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentId").value(id.toString()));
    }

    @Test
    void rejectAssignment_returnsBadRequest_whenReasonIsBlank() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/incidents/" + id + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectAssignmentRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getIncidentsPendingAiReview_returnsOk() throws Exception {
        IncidentResponseDTO response = IncidentResponseDTO.builder()
                .incidentId(UUID.randomUUID())
                .incidentType(IssueType.POTHOLE)
                .aiGenerated(true)
                .aiConfidence(0.7)
                .build();

        when(incidentService.getIncidentsPendingAiReview()).thenReturn(List.of(response));

        mockMvc.perform(get("/incidents/pending-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].aiGenerated").value(true))
                .andExpect(jsonPath("$.data[0].aiConfidence").value(0.7));
    }

    @Test
    void getIncidentClusters_returnsOk() throws Exception {
        za.co.urbaneye.reporthole.incident.clustering.IncidentClusterDTO cluster =
                za.co.urbaneye.reporthole.incident.clustering.IncidentClusterDTO.builder()
                        .clusterIndex(0)
                        .centroidLatitude(-26.2041)
                        .centroidLongitude(28.0473)
                        .size(2)
                        .incidentIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                        .build();

        when(incidentClusteringService.clusterIncidents(5, null, null)).thenReturn(List.of(cluster));

        mockMvc.perform(get("/incidents/clusters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].size").value(2))
                .andExpect(jsonPath("$.data[0].clusterIndex").value(0));
    }

    @Test
    void getIncidentClusters_passesMunicipalityIdThrough_whenProvided() throws Exception {
        UUID municipalityId = UUID.randomUUID();
        when(incidentClusteringService.clusterIncidents(5, null, municipalityId)).thenReturn(List.of());

        mockMvc.perform(get("/incidents/clusters").param("municipalityId", municipalityId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void getRecentIncidents_passesMunicipalityIdThrough_whenProvided() throws Exception {
        UUID municipalityId = UUID.randomUUID();
        IncidentResponseDTO response = IncidentResponseDTO.builder()
                .incidentId(UUID.randomUUID())
                .incidentType(IssueType.POTHOLE)
                .build();
        when(incidentService.getRecentIncidents(10, municipalityId)).thenReturn(List.of(response));

        mockMvc.perform(get("/incidents/recent").param("municipalityId", municipalityId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].incidentType").value("POTHOLE"));
    }

    @Test
    void getRecentIncidents_omitsMunicipalityId_whenNotProvided() throws Exception {
        when(incidentService.getRecentIncidents(10, null)).thenReturn(List.of());

        mockMvc.perform(get("/incidents/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void searchIncidents_defaultsPageZeroAndSize50() throws Exception {
        za.co.urbaneye.reporthole.incident.dto.IncidentPageResponse page =
                new za.co.urbaneye.reporthole.incident.dto.IncidentPageResponse(List.of(), 0L, 0, 50);
        when(incidentService.searchIncidents(null, null, 0, 50)).thenReturn(page);

        mockMvc.perform(get("/incidents/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(50));
    }

    @Test
    void searchIncidents_passesFiltersAndPagingThrough() throws Exception {
        UUID municipalityId = UUID.randomUUID();
        IncidentResponseDTO response = IncidentResponseDTO.builder()
                .incidentId(UUID.randomUUID())
                .incidentType(IssueType.POTHOLE)
                .build();
        za.co.urbaneye.reporthole.incident.dto.IncidentPageResponse page =
                new za.co.urbaneye.reporthole.incident.dto.IncidentPageResponse(List.of(response), 1L, 2, 25);
        when(incidentService.searchIncidents(municipalityId, IssueType.POTHOLE, 2, 25)).thenReturn(page);

        mockMvc.perform(get("/incidents/search")
                        .param("municipalityId", municipalityId.toString())
                        .param("type", "POTHOLE")
                        .param("page", "2")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].incidentType").value("POTHOLE"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void searchIncidents_clampsRequestedSizeTo50() throws Exception {
        za.co.urbaneye.reporthole.incident.dto.IncidentPageResponse page =
                new za.co.urbaneye.reporthole.incident.dto.IncidentPageResponse(List.of(), 0L, 0, 50);
        when(incidentService.searchIncidents(null, null, 0, 50)).thenReturn(page);

        mockMvc.perform(get("/incidents/search").param("size", "500"))
                .andExpect(status().isOk());

        verify(incidentService).searchIncidents(null, null, 0, 50);
    }

    @Test
    void getComments_returnsOk() throws Exception {
        UUID incidentId = UUID.randomUUID();
        IncidentCommentResponse comment = new IncidentCommentResponse(
                UUID.randomUUID(), "Pothole is still there", java.time.LocalDateTime.now(), "Jane Doe", "CIVILIAN");

        when(commentService.getComments(incidentId)).thenReturn(List.of(comment));

        mockMvc.perform(get("/incidents/{id}/comments", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("Pothole is still there"))
                .andExpect(jsonPath("$.data[0].authorName").value("Jane Doe"));
    }

    @Test
    void addComment_returnsCreated() throws Exception {
        UUID incidentId = UUID.randomUUID();
        IncidentCommentResponse comment = new IncidentCommentResponse(
                UUID.randomUUID(), "Will be fixed soon", java.time.LocalDateTime.now(), "John Admin", "ADMIN");

        when(commentService.addComment(eq(incidentId), any())).thenReturn(comment);

        mockMvc.perform(post("/incidents/{id}/comments", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Will be fixed soon\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.authorName").value("John Admin"))
                .andExpect(jsonPath("$.data.authorRole").value("ADMIN"));
    }

    @Test
    void addComment_emptyContent_returnsBadRequest() throws Exception {
        UUID incidentId = UUID.randomUUID();

        mockMvc.perform(post("/incidents/{id}/comments", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
