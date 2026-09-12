package za.co.urbaneye.reporthole.admin.municipality.dto;

import za.co.urbaneye.reporthole.admin.municipality.entity.MunicipalityToken;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response shape for the token listings under {@code /admin/municipalities/**}.
 *
 * @param id               token id (used to revoke)
 * @param token            the token string to hand to a prospective admin
 * @param municipalityId   owning municipality id
 * @param municipalityName owning municipality name
 * @param issuedByName     display name of the security admin who issued it
 * @param issuedAt         when it was issued
 * @param expiresAt        expiry, or null if it never expires
 * @param revokedAt        revocation time, or null if still live
 * @param status           derived lifecycle: {@code ACTIVE}, {@code EXPIRED} or {@code REVOKED}
 * @author Refentse
 * @since 1.0
 */
public record MunicipalityTokenResponse(
        UUID id,
        String token,
        UUID municipalityId,
        String municipalityName,
        String issuedByName,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        Status status
) {

    /** Derived display status for a token. */
    public enum Status { ACTIVE, EXPIRED, REVOKED }

    /**
     * @param t the entity (municipality and issuedBy must be loadable)
     * @return the DTO view, with {@link #status} derived from revoke/expiry state
     */
    public static MunicipalityTokenResponse from(MunicipalityToken t) {
        Status status;
        if (t.getRevokedAt() != null) {
            status = Status.REVOKED;
        } else if (t.getExpiresAt() != null && !t.getExpiresAt().isAfter(LocalDateTime.now())) {
            status = Status.EXPIRED;
        } else {
            status = Status.ACTIVE;
        }
        return new MunicipalityTokenResponse(
                t.getId(),
                t.getToken(),
                t.getMunicipality().getId(),
                t.getMunicipality().getName(),
                t.getIssuedBy().getFirstName() + " " + t.getIssuedBy().getLastName(),
                t.getIssuedAt(),
                t.getExpiresAt(),
                t.getRevokedAt(),
                status
        );
    }
}
