package za.co.urbaneye.reporthole.admin.municipality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.urbaneye.reporthole.admin.municipality.entity.MunicipalityToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link MunicipalityToken} entities.
 *
 * @author Refentse
 * @since 1.0
 */
@Repository
public interface IMunicipalityTokenRepository extends JpaRepository<MunicipalityToken, UUID> {

    /**
     * Resolves a raw token string submitted on the registration form.
     *
     * @param token the token value
     * @return the matching token, if any (callers must still check {@link MunicipalityToken#isUsable()})
     */
    Optional<MunicipalityToken> findByToken(String token);

    /**
     * @return every issued token, newest first — the security admin's oversight list
     */
    List<MunicipalityToken> findAllByOrderByIssuedAtDesc();

    /**
     * @param municipalityId the municipality to filter by
     * @return that municipality's tokens, newest first
     */
    List<MunicipalityToken> findByMunicipality_IdOrderByIssuedAtDesc(UUID municipalityId);

    /**
     * @param municipalityId the municipality
     * @return how many tokens have ever been issued for it
     */
    long countByMunicipality_Id(UUID municipalityId);
}
