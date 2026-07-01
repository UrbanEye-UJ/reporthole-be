package za.co.urbaneye.reporthole.device.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.urbaneye.reporthole.device.entity.DashcamDevice;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DashcamDevice} entities.
 *
 * <p>Provides standard CRUD operations and a custom lookup method
 * used by the authentication filter to resolve an incoming device
 * token to the linked user account.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Repository
public interface DashcamDeviceRepository extends JpaRepository<DashcamDevice, UUID> {

    /**
     * Finds a registered dashcam device by its token string.
     *
     * <p>Used in {@code JwtAuthenticationFilter} to authenticate
     * requests that carry a device token in the Authorization header
     * instead of a JWT.</p>
     *
     * @param deviceToken the raw token string stored on the device
     * @return the matching {@link DashcamDevice}, or empty if not found
     */
    Optional<DashcamDevice> findByDeviceToken(String deviceToken);
}
