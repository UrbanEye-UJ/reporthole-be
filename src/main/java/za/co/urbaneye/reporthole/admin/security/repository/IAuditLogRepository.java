package za.co.urbaneye.reporthole.admin.security.repository;

import org.springframework.data.repository.Repository;
import za.co.urbaneye.reporthole.admin.security.entity.AuditLogEntry;

import java.util.List;
import java.util.UUID;

/**
 * Persistence gateway for {@link AuditLogEntry}.
 *
 * <p>Like {@link IAccessControlAuditRepository}, this deliberately extends the bare Spring
 * Data {@link Repository} marker rather than {@code JpaRepository}/{@code CrudRepository}: only
 * {@code save} and read operations are exposed — no update, no delete.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public interface IAuditLogRepository extends Repository<AuditLogEntry, UUID> {

    /**
     * Appends one entry to the log.
     *
     * @param entry the entry to persist (must be new — entries are never updated)
     * @param <S>   the concrete entry type
     * @return the persisted entry, with its generated id and timestamp populated
     */
    <S extends AuditLogEntry> S save(S entry);

    /**
     * Returns the whole log, newest first.
     *
     * @return all entries ordered by {@code createdAt} descending
     */
    List<AuditLogEntry> findAllByOrderByCreatedAtDesc();
}
