package za.co.urbaneye.reporthole.incident.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.urbaneye.reporthole.incident.entity.Incident;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByUser_UserId(UUID userId);

    /** Most recently logged, non-deleted incidents across all users, newest first. */
    List<Incident> findByDeletedFalseOrderByIncidentDateDesc(Pageable pageable);

    /** All non-deleted incidents, unordered — used as the input set for location clustering. */
    List<Incident> findByDeletedFalse();

    /** AI-generated, non-deleted incidents, newest first — candidates for the human-review queue. */
    List<Incident> findByAiGeneratedTrueAndDeletedFalseOrderByIncidentDateDesc();

    long countByDeletedFalse();

    @Query("SELECT DISTINCT i FROM Incident i LEFT JOIN IncidentReporter ir ON ir.incident = i WHERE (i.user.userId = :userId OR ir.user.userId = :userId) AND i.deleted = false")
    List<Incident> findAllReportedByUser(@Param("userId") UUID userId);

    /**
     * Returns the nearest existing incident of the same type within radiusMeters, if any.
     * Ordered by distance ascending so the closest match is returned first.
     */
    @Query(value = """
            SELECT *
            FROM incident
            WHERE ST_DWithin(
                ST_SetSRID(INCIDENT_LOCATION, 4326)::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                :radiusMeters
            )
            AND INCIDENT_TYPE = :#{#issueType.name()}
            AND INCIDENT_DELETED = false
            ORDER BY ST_Distance(
                ST_SetSRID(INCIDENT_LOCATION, 4326)::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
            )
            LIMIT 1
            """, nativeQuery = true)
    Optional<Incident> findNearestDuplicate(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("issueType") IssueType issueType
    );

    /**
     * Filters the authenticated user's incidents by a free-text keyword (matched against
     * description and location address) and/or by issue type. Null parameters are treated
     * as "no filter" so the query works when either or both params are omitted.
     */
    @Query("""
            SELECT DISTINCT i FROM Incident i
            LEFT JOIN IncidentReporter ir ON ir.incident = i
            WHERE (i.user.userId = :userId OR ir.user.userId = :userId)
              AND i.deleted = false
              AND (:keyword IS NULL
                   OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(i.locationAddress) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:issueType IS NULL OR i.incidentType = :issueType)
            """)
    List<Incident> searchByUser(
            @Param("userId") UUID userId,
            @Param("keyword") String keyword,
            @Param("issueType") IssueType issueType);

    @Modifying
    @Query("UPDATE Incident i SET i.reportCount = i.reportCount + 1 WHERE i.incidentId = :id")
    void incrementReportCount(@Param("id") UUID id);
}

