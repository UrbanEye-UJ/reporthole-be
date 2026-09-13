package za.co.urbaneye.reporthole.admin.municipality.dto;

import za.co.urbaneye.reporthole.admin.municipality.entity.Municipality;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response shape for {@code GET /admin/municipalities}.
 *
 * @param id         municipality id
 * @param name       display name
 * @param province   province
 * @param tokenCount how many tokens have ever been issued for this municipality
 * @param createdAt  when it was created
 * @author Refentse
 * @since 1.0
 */
public record MunicipalityResponse(
        UUID id,
        String name,
        String province,
        long tokenCount,
        LocalDateTime createdAt
) {
    /**
     * @param m          the entity
     * @param tokenCount issued-token count for {@code m}
     * @return the DTO view
     */
    public static MunicipalityResponse from(Municipality m, long tokenCount) {
        return new MunicipalityResponse(m.getId(), m.getName(), m.getProvince(), tokenCount, m.getCreatedAt());
    }
}
