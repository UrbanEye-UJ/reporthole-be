package za.co.urbaneye.reporthole.admin.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.urbaneye.reporthole.admin.application.entity.AdminApplication;

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
}
