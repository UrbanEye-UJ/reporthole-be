package za.co.urbaneye.reporthole.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import za.co.urbaneye.reporthole.global.entity.AppResponse;
import za.co.urbaneye.reporthole.incident.service.impl.IncidentSseService;
import za.co.urbaneye.reporthole.notification.dto.NotificationResponse;
import za.co.urbaneye.reporthole.notification.service.interfaces.INotificationService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the authenticated user's in-app notification inbox.
 *
 * <p>All endpoints are authenticated — the caller's identity is resolved from the JWT.
 * Live delivery is handled separately via SSE ({@code GET /incidents/events}); these endpoints
 * are for initial load and marking-read operations.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@RestController
@RequestMapping("notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification inbox for the authenticated user")
public class NotificationController {

    private final INotificationService notificationService;
    private final IncidentSseService incidentSseService;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Notification SSE stream",
            description = "Opens a persistent SSE connection that delivers a 'notification' event whenever " +
                    "a new in-app notification is created for the authenticated user. Auth via ?token=<jwt> " +
                    "query param (EventSource cannot set custom headers). The FE proxies this via Next.js rewrites."
    )
    public SseEmitter subscribeToNotificationEvents() {
        UUID userId = UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return incidentSseService.subscribe(userId);
    }

    @GetMapping
    @Operation(summary = "List notifications", description = "Returns the 50 most recent notifications for the authenticated user, newest first.")
    public ResponseEntity<AppResponse<List<NotificationResponse>>> getMyNotifications() {
        return ResponseEntity.ok(AppResponse.ok(notificationService.getMyNotifications()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread notification count", description = "Returns the number of unread notifications — used to drive the bell badge.")
    public ResponseEntity<AppResponse<Long>> getUnreadCount() {
        return ResponseEntity.ok(AppResponse.ok(notificationService.getUnreadCount()));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark one notification read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications read", description = "Marks every unread notification for the caller as read in a single operation.")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.noContent().build();
    }
}
