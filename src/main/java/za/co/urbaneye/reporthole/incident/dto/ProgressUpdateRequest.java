package za.co.urbaneye.reporthole.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body sent by a contractor when posting a progress update on an in-progress incident.
 *
 * @param note free-text description of what has been done so far
 */
public record ProgressUpdateRequest(
        @NotBlank(message = "Progress note must not be blank")
        @Size(max = 255, message = "Progress note must not exceed 255 characters")
        String note
) {}
