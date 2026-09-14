package za.co.urbaneye.reporthole.admin.security.dto;

import za.co.urbaneye.reporthole.admin.security.entity.AuditLogEntry;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Row shape for {@code GET /admin/security/audit-log} — the general-purpose counterpart to
 * {@link AuditEntryResponse}, for actions that don't fit the identity/accountability shape
 * (actor and target both mandatory {@code User} accounts).
 *
 * @param id         entry id
 * @param action     what happened, e.g. {@code "CONTRACTOR_INVITED"}
 * @param actorId    who did it, or {@code null} for an unauthenticated action
 * @param actorName  the actor's name, or {@code "System"} when there is no actor
 * @param entityType the kind of thing acted on, e.g. {@code "MUNICIPALITY"}
 * @param entityId   the affected row's id, when it has one
 * @param summary    human-readable one-line description
 * @param createdAt  when the action was recorded
 * @author Refentse
 * @since 1.0
 */
public record AuditLogEntryResponse(
        UUID id,
        String action,
        UUID actorId,
        String actorName,
        String entityType,
        UUID entityId,
        String summary,
        LocalDateTime createdAt
) {
    /**
     * @param e the entity
     * @return the DTO view
     */
    public static AuditLogEntryResponse from(AuditLogEntry e) {
        String actorName = e.getActor() != null
                ? (e.getActor().getFirstName() + " " + e.getActor().getLastName()).trim()
                : "System";
        return new AuditLogEntryResponse(
                e.getId(),
                e.getAction(),
                e.getActor() != null ? e.getActor().getUserId() : null,
                actorName,
                e.getEntityType(),
                e.getEntityId(),
                e.getSummary(),
                e.getCreatedAt());
    }
}
