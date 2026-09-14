package za.co.urbaneye.reporthole.admin.municipality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.urbaneye.reporthole.admin.municipality.entity.Municipality;

import java.util.List;
import java.util.Optional;
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
     * @param name candidate municipality name
     * @return the matching municipality, case-insensitively, if one exists
     */
    Optional<Municipality> findByNameIgnoreCase(String name);

    /**
     * @return all municipalities ordered by name
     */
    List<Municipality> findAllByOrderByNameAsc();

    /**
     * @return the municipality whose real boundary polygon contains the given point, if any —
     *         used to auto-tag a newly reported incident to its municipality on creation
     */
    @Query(value = """
            SELECT *
            FROM municipalities
            WHERE municipality_boundary IS NOT NULL
              AND ST_Contains(
                  municipality_boundary,
                  ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)
              )
            LIMIT 1
            """, nativeQuery = true)
    Optional<Municipality> findContainingPoint(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude);
}
