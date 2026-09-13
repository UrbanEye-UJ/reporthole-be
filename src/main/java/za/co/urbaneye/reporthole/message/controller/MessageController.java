package za.co.urbaneye.reporthole.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.urbaneye.reporthole.global.entity.AppResponse;
import za.co.urbaneye.reporthole.message.dto.ContactMessageRequest;
import za.co.urbaneye.reporthole.message.dto.MessageResponse;
import za.co.urbaneye.reporthole.message.dto.SendMessageRequest;
import za.co.urbaneye.reporthole.message.service.interfaces.IMessageService;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for the messaging feature.
 *
 * <ul>
 *     <li>{@code POST /messages/contact} — public, no JWT required (landing-page contact form)</li>
 *     <li>{@code POST /messages} — any authenticated user sends a message to the admin team</li>
 *     <li>{@code GET /messages/admin} — ADMIN or SECURITY_ADMIN reads civilian complaints</li>
 *     <li>{@code GET /messages/security-admin} — SECURITY_ADMIN only reads contact-us submissions</li>
 * </ul>
 *
 * @author Refentse
 * @since 1.0
 */
@RestController
@RequestMapping("messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "In-app messaging: civilian complaints to admins, and landing-page contact-form submissions.")
public class MessageController {

    private final IMessageService messageService;

    @PostMapping("/contact")
    @Operation(
            summary = "Submit a contact-form message",
            description = "Public endpoint — no JWT required. Stores the submission as a CONTACT_US message " +
                    "visible to security admins. Used by the landing-page contact form."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message stored"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<AppResponse<Void>> contact(@Valid @RequestBody ContactMessageRequest request) {
        messageService.submitContact(request);
        return ResponseEntity.ok(AppResponse.of(null, "Message received. Thank you for reaching out.", 200));
    }

    @PostMapping
    @Operation(
            summary = "Send a message to the admin team",
            description = "Stores a CIVILIAN_COMPLAINT message from the authenticated user. Visible to admins " +
                    "via GET /messages/admin."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message sent"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ResponseEntity<AppResponse<Void>> send(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        // JWT filter sets the principal as a UUID String; test mocks may use UserDetails
        String principalStr = authentication.getPrincipal() instanceof UserDetails ud
                ? ud.getUsername() : (String) authentication.getPrincipal();
        UUID senderUserId = UUID.fromString(principalStr);
        messageService.sendMessage(request, senderUserId);
        return ResponseEntity.ok(AppResponse.of(null, "Message sent.", 200));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('SECURITY_ADMIN')")
    @Operation(
            summary = "List civilian complaint messages",
            description = "Returns all CIVILIAN_COMPLAINT messages, newest first. SECURITY_ADMIN only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public ResponseEntity<AppResponse<List<MessageResponse>>> getAdminMessages() {
        return ResponseEntity.ok(AppResponse.ok(messageService.getCivilianComplaints()));
    }

    @GetMapping("/security-admin")
    @PreAuthorize("hasRole('SECURITY_ADMIN')")
    @Operation(
            summary = "List contact-form submissions",
            description = "Returns all CONTACT_US messages submitted via the public landing-page form, " +
                    "newest first. Security admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin")
    })
    public ResponseEntity<AppResponse<List<MessageResponse>>> getSecurityAdminMessages() {
        return ResponseEntity.ok(AppResponse.ok(messageService.getContactMessages()));
    }
}
