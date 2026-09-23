package za.co.urbaneye.reporthole.training.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Boxes drawn in the browser, in natural-image pixels, plus the natural size of the image they
 * were drawn on. The server normalises them to YOLO coordinates and records the image size on
 * the incident.
 */
public record SaveAnnotationsRequest(
        @NotNull(message = "imageWidth is required") @Positive(message = "imageWidth must be positive") Integer imageWidth,
        @NotNull(message = "imageHeight is required") @Positive(message = "imageHeight must be positive") Integer imageHeight,
        @NotEmpty(message = "At least one box is required")
        @Size(max = 100, message = "At most 100 boxes per request")
        List<@Valid AnnotationBoxRequest> boxes
) {}
