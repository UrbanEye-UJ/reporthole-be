package za.co.urbaneye.reporthole.message.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.urbaneye.reporthole.device.repository.DashcamDeviceRepository;
import za.co.urbaneye.reporthole.message.dto.ContactMessageRequest;
import za.co.urbaneye.reporthole.message.dto.MessageResponse;
import za.co.urbaneye.reporthole.message.dto.SendMessageRequest;
import za.co.urbaneye.reporthole.message.entity.MessageCategory;
import za.co.urbaneye.reporthole.message.service.interfaces.IMessageService;
import za.co.urbaneye.reporthole.security.Jwt;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for {@link MessageController}: request validation and response shape.
 * Security filters are disabled; {@code POST /messages} is tested with {@code @WithMockUser}
 * whose username is a UUID string matching what the JWT filter sets in production.
 * Role-access enforcement is covered by the integration tests.
 */
@WebMvcTest(MessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private IMessageService messageService;
    @MockitoBean private Jwt jwt;
    @MockitoBean private DashcamDeviceRepository dashcamDeviceRepository;
    @MockitoBean private IUserAuthRepository userAuthRepository;

    // ── POST /messages/contact ────────────────────────────────────────────────

    @Test
    void contact_validRequest_returns200() throws Exception {
        doNothing().when(messageService).submitContact(any());

        ContactMessageRequest req = new ContactMessageRequest(
                "John Smith", "john@example.com", "Enquiry", "Please contact me.");

        mockMvc.perform(post("/messages/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Message received. Thank you for reaching out."));
    }

    @Test
    void contact_missingName_returns400() throws Exception {
        ContactMessageRequest req = new ContactMessageRequest(
                "", "john@example.com", null, "Please contact me.");

        mockMvc.perform(post("/messages/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contact_invalidEmail_returns400() throws Exception {
        ContactMessageRequest req = new ContactMessageRequest(
                "John", "not-an-email", null, "Hello.");

        mockMvc.perform(post("/messages/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contact_missingContent_returns400() throws Exception {
        ContactMessageRequest req = new ContactMessageRequest(
                "John", "john@example.com", "Subject", "");

        mockMvc.perform(post("/messages/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /messages ────────────────────────────────────────────────────────

    /**
     * Validation failure on POST /messages — returns 400 before Authentication is resolved,
     * so this test works without a live security context. The authenticated happy path is
     * covered by the integration tests.
     */
    @Test
    void send_missingContent_returns400() throws Exception {
        SendMessageRequest req = new SendMessageRequest(null, "");

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /messages/admin ───────────────────────────────────────────────────

    @Test
    void getAdminMessages_returns200WithList() throws Exception {
        UUID id = UUID.randomUUID();
        when(messageService.getUserMessages()).thenReturn(List.of(
                new MessageResponse(id, null, "Alice K.", "a***@example.com", UserRole.CIVILIAN,
                        "Complaint", "Road is broken.", MessageCategory.USER_MESSAGE, false, LocalDateTime.now())
        ));

        mockMvc.perform(get("/messages/admin").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].senderName").value("Alice K."))
                .andExpect(jsonPath("$.data[0].senderRole").value("CIVILIAN"))
                .andExpect(jsonPath("$.data[0].category").value("USER_MESSAGE"));
    }

    // ── GET /messages/security-admin ──────────────────────────────────────────

    @Test
    void getSecurityAdminMessages_returns200WithList() throws Exception {
        UUID id = UUID.randomUUID();
        when(messageService.getContactMessages()).thenReturn(List.of(
                new MessageResponse(id, null, "John Smith", "john@example.com", null,
                        "Hello", "Interested in your platform.", MessageCategory.CONTACT_US, false, LocalDateTime.now())
        ));

        mockMvc.perform(get("/messages/security-admin").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("CONTACT_US"));
    }
}
