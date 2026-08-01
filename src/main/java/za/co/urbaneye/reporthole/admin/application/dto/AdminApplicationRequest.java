package za.co.urbaneye.reporthole.admin.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /admin/applications}.
 *
 * @param municipalityToken opaque token issued by the municipality,
 *                          verifying the applicant's affiliation
 * @author Refentse
 * @since 1.0
 */
public record AdminApplicationRequest(
        @NotBlank(message = "Municipality token is required") String municipalityToken
) {}
