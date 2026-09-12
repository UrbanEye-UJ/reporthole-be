package za.co.urbaneye.reporthole.admin.security.repository;

import org.springframework.data.repository.Repository;
import za.co.urbaneye.reporthole.admin.security.entity.AccessControlAuditEntry;

import java.util.List;
import java.util.UUID;

/**
 * Persistence gateway for {@link AccessControlAuditEntry}.
 *
 * <p>This interface deliberately extends the bare Spring Data {@link Repository} marker rather
 * than {@code JpaRepository}/{@code CrudRepository}: it exposes <em>only</em> a create
 * ({@link #save}) and read operations. There is no {@code delete}, no {@code deleteAll}, no
 * {@code saveAll} for bulk rewrites — from anywhere in the application code the audit trail can
 * be appended to and inspected, never edited or pruned.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public interface IAccessControlAuditRepository extends Repository<AccessControlAuditEntry, UUID> {

    /**
     * Appends one entry to the trail.
     *
     * @param entry the entry to persist (must be new — entries are never updated)
     * @param <S>   the concrete entry type
     * @return the persisted entry, with its generated id and timestamp populated
     */
    <S extends AccessControlAuditEntry> S save(S entry);

    /**
     * Returns the whole trail, newest first — the security admin's oversight view.
     *
     * @return all audit entries ordered by {@code createdAt} descending
     */
    List<AccessControlAuditEntry> findAllByOrderByCreatedAtDesc();

    /**
     * Returns the trail for a single account, newest first.
     *
     * @param targetUserId the id of the account the entries were performed on
     * @return matching audit entries ordered by {@code createdAt} descending
     */
    List<AccessControlAuditEntry> findByTarget_UserIdOrderByCreatedAtDesc(UUID targetUserId);

    /**
     * @return the total number of entries in the trail
     */
    long count();
}
