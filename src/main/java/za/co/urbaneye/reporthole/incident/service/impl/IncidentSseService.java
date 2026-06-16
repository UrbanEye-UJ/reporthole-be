package za.co.urbaneye.reporthole.incident.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages Server-Sent Event (SSE) connections for real-time incident update notifications.
 *
 * <p>Each authenticated user can have multiple active connections (e.g. multiple browser tabs).
 * Emitters are stored per user ID so that pushes can be targeted to only those users
 * who are linked to a given incident, rather than broadcasting to every connected client.</p>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>The client calls {@code GET /incidents/events?token=<jwt>}, which invokes {@link #subscribe(UUID)}.</li>
 *   <li>The returned {@link SseEmitter} is held open for the lifetime of the connection.</li>
 *   <li>When a relevant incident changes, the caller invokes {@link #pushIncidentUpdate(UUID, Set)}
 *       with the incident ID and the set of user IDs who should be notified.</li>
 *   <li>On disconnect, completion, timeout, or error the emitter removes itself from the registry.</li>
 * </ol>
 *
 * <h2>Thread safety</h2>
 * <p>The outer map is a {@link ConcurrentHashMap} and each per-user list is a
 * {@link CopyOnWriteArrayList}, making reads and iteration safe under concurrent modification.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Service
@Slf4j
public class IncidentSseService {

    /**
     * Registry of active emitters keyed by authenticated user ID.
     * A single user may hold several emitters simultaneously (multiple tabs / devices).
     */
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> userEmitters =
            new ConcurrentHashMap<>();

    /**
     * Registers a new SSE connection for the given user.
     *
     * <p>The emitter is configured with an infinite timeout ({@code 0L}) so the connection
     * stays open until the client disconnects or the server explicitly closes it.
     * Cleanup callbacks ensure the emitter is removed from the registry on any terminal event.</p>
     *
     * @param userId the authenticated user subscribing to the stream
     * @return a live {@link SseEmitter} whose output is written to the HTTP response
     */
    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(0L);
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("[SSE] User {} connected — active connections for this user: {}, total across all users: {}",
                userId, userEmitters.get(userId).size(), activeCount());
        emitter.onCompletion(() -> {
            removeEmitter(userId, emitter);
            log.info("[SSE] User {} disconnected (completion) — total active: {}", userId, activeCount());
        });
        emitter.onTimeout(() -> {
            removeEmitter(userId, emitter);
            log.info("[SSE] User {} disconnected (timeout) — total active: {}", userId, activeCount());
        });
        emitter.onError(e -> {
            removeEmitter(userId, emitter);
            log.warn("[SSE] User {} disconnected (error: {}) — total active: {}", userId, e.getMessage(), activeCount());
        });
        return emitter;
    }

    /**
     * Sends an {@code incident-updated} SSE event to every active connection belonging
     * to any user in {@code targetUserIds}.
     *
     * <p>Only users who are linked to the changed incident (original reporter + all confirmed
     * reporters) should be included in {@code targetUserIds}. Emitters that fail to send
     * (because the client already disconnected) are silently removed from the registry.</p>
     *
     * @param incidentId    the UUID of the incident that changed; sent as the event payload
     * @param targetUserIds the set of user IDs that should receive the notification
     */
    public void pushIncidentUpdate(UUID incidentId, Set<UUID> targetUserIds) {
        long connectedTargets = targetUserIds.stream()
                .filter(userEmitters::containsKey)
                .count();
        log.info("[SSE] Pushing incident-updated for incident {} to {} target user(s) ({} currently connected)",
                incidentId, targetUserIds.size(), connectedTargets);

        for (UUID userId : targetUserIds) {
            List<SseEmitter> emitters = userEmitters.getOrDefault(userId, new CopyOnWriteArrayList<>());
            if (emitters.isEmpty()) {
                log.debug("[SSE] User {} is a target but has no active connections — skipping", userId);
                continue;
            }
            List<SseEmitter> dead = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("incident-updated")
                            .data(incidentId.toString()));
                    log.debug("[SSE] Event sent to user {}", userId);
                } catch (Exception e) {
                    log.warn("[SSE] Failed to send to user {} ({}), marking emitter as dead", userId, e.getMessage());
                    dead.add(emitter);
                }
            }
            emitters.removeAll(dead);
        }

        log.info("[SSE] Push complete for incident {}", incidentId);
    }

    /**
     * Returns the total number of active SSE connections across all users.
     * Intended for monitoring and testing purposes.
     *
     * @return total active emitter count
     */
    public int activeCount() {
        return userEmitters.values().stream().mapToInt(List::size).sum();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Removes a specific emitter from a user's connection list.
     * If the list becomes empty after removal, the user entry is also removed from the map.
     *
     * @param userId  the owner of the emitter
     * @param emitter the emitter to remove
     */
    private void removeEmitter(UUID userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = userEmitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                userEmitters.remove(userId, list);
            }
        }
    }
}
