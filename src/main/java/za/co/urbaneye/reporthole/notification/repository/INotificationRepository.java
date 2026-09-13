package za.co.urbaneye.reporthole.notification.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.urbaneye.reporthole.notification.entity.Notification;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Notification} entities.
 *
 * @author Refentse
 * @since 1.0
 */
@Repository
public interface INotificationRepository extends JpaRepository<Notification, UUID> {

    /** Most recent notifications for a user (read and unread), newest first. */
    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** Count of unread notifications for a user — used for the badge. */
    long countByUser_UserIdAndReadFalse(UUID userId);

    /** Bulk-mark every unread notification for a user as read. */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.userId = :userId AND n.read = false")
    void markAllReadByUserId(@Param("userId") UUID userId);
}
