package za.co.urbaneye.reporthole.admin.municipality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.urbaneye.reporthole.admin.municipality.entity.Municipality;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Municipality} entities.
 *
 * @author Refentse
 * @since 1.0
 */
@Repository
public interface IMunicipalityRepository extends JpaRepository<Municipality, UUID> {

    /**
     * @param name candidate municipality name
     * @return {@code true} if a municipality with this name already exists (case-insensitive)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * @return all municipalities ordered by name
     */
    List<Municipality> findAllByOrderByNameAsc();
}
