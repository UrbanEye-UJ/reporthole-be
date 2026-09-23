package za.co.urbaneye.reporthole.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import za.co.urbaneye.reporthole.incident.entity.IncidentSource;
import za.co.urbaneye.reporthole.incident.entity.IssueType;

import java.time.LocalDateTime;

public record IncidentRequestDTO(
        @NotNull(message = "Issue type is required")
        IssueType incidentType,

        String description,

        @NotNull(message = "Source is required")
        IncidentSource source,

        double latitude,
        double longitude,

        @NotBlank(message = "Image is required")
        String imageBase64,

        boolean forceCreate,
        String locationAddress,

        /**
         * AI detection confidence in [0.0, 1.0], set only for incidents originating
         * from an automated detector (e.g. the dashcam inference pipeline). Null for
         * manually reported incidents.
         */
        Double confidence,

        /**
         * When the report actually happened, as captured client-side. Optional — defaults to the
         * server's receipt time (today's behaviour) when absent. Exists for offline-queued
         * submissions, where the request may only reach the server well after the fact (e.g. a
         * dashcam detection captured in a signal dead zone and replayed once reconnected) — without
         * this, such a report would be timestamped at sync time instead of when it actually occurred.
         */
        LocalDateTime occurredAt,

        /**
         * The AI detector's own bounding box for this detection, normalised [0.0, 1.0] and
         * centre-based (YOLO format) relative to the submitted image — as returned by
         * {@code POST /inference/predict}'s {@code detection} payload. Set only alongside
         * {@link #confidence}; used to automatically create an {@code IssueAnnotation} training
         * label for the incident. Null for manual reports, or if the frontend simply doesn't
         * forward it (creation still proceeds normally either way).
         */
        Double bboxXCenter,
        Double bboxYCenter,
        Double bboxWidth,
        Double bboxHeight
) {}
