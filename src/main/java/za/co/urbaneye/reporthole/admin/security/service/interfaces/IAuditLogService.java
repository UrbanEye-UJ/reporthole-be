package za.co.urbaneye.reporthole.admin.security.service.interfaces;

import za.co.urbaneye.reporthole.admin.security.dto.AuditLogEntryResponse;
import za.co.urbaneye.reporthole.user.entity.User;

import java.util.List;
import java.util.UUID;

/**
 * Shared writer/reader for the general-purpose {@code audit_log} table — the counterpart to
 * {@code ISecurityAdminService}'s identity-action writes, for state-changing actions that don't
 * fit "actor and target are both User accounts" (municipality management, comments, messages,
 * contractor invites, and similar). Injected directly into whichever service performs the
 * write; there is deliberately no role check on {@link #record} itself since it is called from
 * many different contexts (some with no authenticated actor at all, e.g. the public contact
 * form) — each caller is responsible for its own authorization before recording.
 *
 * @author Refentse
 * @since 1.0
 */
public interface IAuditLogService {

    /**
     * Appends one entry to the log.
     *
     * @param actor      who did it, or {@code null} when there is no authenticated caller
     * @param action     what happened, e.g. {@code "CONTRACTOR_INVITED"} (SCREAMING_SNAKE_CASE
     *                   by convention)
     * @param entityType the kind of thing acted on, e.g. {@code "MUNICIPALITY"}
     * @param entityId   the affected row's id, or {@code null} when it doesn't have one yet
     * @param summary    human-readable one-line description shown directly in the audit UI
     */
    void record(User actor, String action, String entityType, UUID entityId, String summary);

    /**
     * Returns the whole log, newest first. Security admin only.
     *
     * @return every entry, newest first
     * @throws za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException
     *         if the caller is not a security admin
     */
    List<AuditLogEntryResponse> listAll();
}
