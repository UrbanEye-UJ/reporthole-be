package za.co.urbaneye.reporthole.admin.municipality.service.interfaces;

import za.co.urbaneye.reporthole.admin.municipality.dto.CreateMunicipalityRequest;
import za.co.urbaneye.reporthole.admin.municipality.dto.IssueTokenRequest;
import za.co.urbaneye.reporthole.admin.municipality.dto.MunicipalityResponse;
import za.co.urbaneye.reporthole.admin.municipality.dto.MunicipalityTokenResponse;

import java.util.List;
import java.util.UUID;

/**
 * Management of municipalities and the registration tokens issued against them.
 *
 * <p>Every method is restricted to {@code SECURITY_ADMIN} — issuing a municipality token is a
 * pre-approval of admin access, so it belongs to the same responsibility that owns role grants.
 * All methods throw
 * {@link za.co.urbaneye.reporthole.admin.municipality.exception.MunicipalityException} on a
 * business-rule violation.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public interface IMunicipalityService {

    /**
     * Creates a municipality.
     *
     * @param request name and (optional) province
     * @return the created municipality
     * @throws za.co.urbaneye.reporthole.admin.municipality.exception.MunicipalityException
     *         if the caller is not a security admin or the name is already taken
     */
    MunicipalityResponse createMunicipality(CreateMunicipalityRequest request);

    /**
     * @return all municipalities, ordered by name, each with its issued-token count
     * @throws za.co.urbaneye.reporthole.admin.municipality.exception.MunicipalityException
     *         if the caller is not a security admin
     */
    List<MunicipalityResponse> listMunicipalities();

    /**
     * Issues a new multi-use registration token for a municipality.
     *
     * @param municipalityId the municipality to issue against
     * @param request        optional expiry (days) and note
     * @return the issued token, including its string value
     * @throws za.co.urbaneye.reporthole.admin.municipality.exception.MunicipalityException
     *         if the caller is not a security admin or the municipality does not exist
     */
    MunicipalityTokenResponse issueToken(UUID municipalityId, IssueTokenRequest request);

    /**
     * Lists issued tokens, newest first.
     *
     * @param municipalityId optional filter — when non-null, only tokens for that municipality
     * @return the matching tokens
     * @throws za.co.urbaneye.reporthole.admin.municipality.exception.MunicipalityException
     *         if the caller is not a security admin
     */
    List<MunicipalityTokenResponse> listTokens(UUID municipalityId);

    /**
     * Revokes a token so it can no longer register new admins. Already-registered admins are
     * unaffected. Idempotent-ish: revoking an already-revoked token is an error.
     *
     * @param tokenId the token to revoke
     * @throws za.co.urbaneye.reporthole.admin.municipality.exception.MunicipalityException
     *         if the caller is not a security admin, the token does not exist, or it is already revoked
     */
    void revokeToken(UUID tokenId);
}
