package za.co.urbaneye.reporthole.training.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

/** One box in natural-image pixels: {@code (x, y)} is the top-left corner. */
public record AnnotationBoxRequest(
        @NotNull(message = "Each box needs a class label") IssueType classLabel,
        @NotNull(message = "Box x is required") @PositiveOrZero(message = "Box x must not be negative") Double x,
        @NotNull(message = "Box y is required") @PositiveOrZero(message = "Box y must not be negative") Double y,
        @NotNull(message = "Box width is required") @Positive(message = "Box width must be positive") Double width,
        @NotNull(message = "Box height is required") @Positive(message = "Box height must be positive") Double height
) {}
