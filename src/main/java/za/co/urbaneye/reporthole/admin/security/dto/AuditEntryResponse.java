package za.co.urbaneye.reporthole.admin.security.dto;

import za.co.urbaneye.reporthole.admin.security.entity.AccessControlAction;
import za.co.urbaneye.reporthole.admin.security.entity.AccessControlAuditEntry;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read/export shape for one {@link AccessControlAuditEntry} row, returned by
 * {@code GET /admin/security/audit}.
 *
 * <p>Carries actor and target names for readability, but no reporter PII beyond the
 * names already visible to admins — the trail is about <em>who did what to whom</em>,
 * not about incident content.</p>
 *
 * @param auditId    the entry's id
 * @param action     what was done
 * @param actorId    id of the security admin who acted
 * @param actorName  display name of the security admin who acted
 * @param targetId   id of the affected account
 * @param targetName display name of the affected account
 * @param fromValue  previous value where applicable (old role/status), else null
 * @param toValue    new value where applicable (new role/status), else null
 * @param reason     the justification supplied at the time
 * @param createdAt  when the action was recorded
 * @author Refentse
 * @since 1.0
 */
public record AuditEntryResponse(
        UUID auditId,
        AccessControlAction action,
        UUID actorId,
        String actorName,
        UUID targetId,
        String targetName,
        String fromValue,
        String toValue,
        String reason,
        LocalDateTime createdAt
) {

    /**
     * Projects an entity into its response DTO.
     *
     * @param entry the persisted audit entry (actor and target must be loadable)
     * @return the DTO view
     */
    public static AuditEntryResponse from(AccessControlAuditEntry entry) {
        return new AuditEntryResponse(
                entry.getAuditId(),
                entry.getAction(),
                entry.getActor().getUserId(),
                entry.getActor().getFirstName() + " " + entry.getActor().getLastName(),
                entry.getTarget().getUserId(),
                entry.getTarget().getFirstName() + " " + entry.getTarget().getLastName(),
                entry.getFromValue(),
                entry.getToValue(),
                entry.getReason(),
                entry.getCreatedAt()
        );
    }
}
