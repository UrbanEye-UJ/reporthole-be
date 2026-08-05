package za.co.urbaneye.reporthole.incident.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveIncidentRequest(
        @NotBlank(message = "A note describing the repair is required")
        String note,

        @NotBlank(message = "A photo of the completed repair is required")
        String photoBase64
) {
}
