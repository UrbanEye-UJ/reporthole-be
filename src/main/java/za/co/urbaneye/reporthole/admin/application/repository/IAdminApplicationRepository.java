package za.co.urbaneye.reporthole.admin.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplication;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplicationStatus;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AdminApplication} entities.
 *
 * @author Refentse
 * @since 1.0
 */
@Repository
public interface IAdminApplicationRepository extends JpaRepository<AdminApplication, UUID> {

    /**
     * Returns {@code true} if the given user has already submitted an application,
     * regardless of its current status. Used to prevent duplicate submissions.
     *
     * @param userId the applicant's user ID
     * @return {@code true} if an application exists for this user
     */
    boolean existsByUser_UserId(UUID userId);

    /**
     * Returns all applications with the given status, most recently submitted first.
     * Used to build the reviewing admin's queue at {@code GET /admin/applications}.
     *
     * @param status the lifecycle status to filter by
     * @return matching applications ordered by submission date descending
     */
    List<AdminApplication> findByStatusOrderBySubmittedAtDesc(AdminApplicationStatus status);

    /**
     * Returns every application regardless of status, most recently submitted first.
     * Used by {@code GET /admin/applications} when no {@code status} filter is supplied.
     *
     * @return all applications ordered by submission date descending
     */
    List<AdminApplication> findAllByOrderBySubmittedAtDesc();
}
