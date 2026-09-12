package za.co.urbaneye.reporthole.admin.municipality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /admin/municipalities}.
 *
 * @param name     display name, e.g. "City of Tshwane" (must be unique)
 * @param province province the municipality falls under; blank defaults to "Gauteng" in the service
 * @author Refentse
 * @since 1.0
 */
public record CreateMunicipalityRequest(
        @NotBlank(message = "Municipality name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @Size(max = 60, message = "Province must be at most 60 characters")
        String province
) {}
