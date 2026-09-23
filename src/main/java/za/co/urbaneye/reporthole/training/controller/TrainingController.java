package za.co.urbaneye.reporthole.training.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.urbaneye.reporthole.global.entity.AppResponse;
import za.co.urbaneye.reporthole.training.dto.AnnotationResponse;
import za.co.urbaneye.reporthole.training.dto.SaveAnnotationsRequest;
import za.co.urbaneye.reporthole.training.dto.TrainingStatusResponse;
import za.co.urbaneye.reporthole.training.service.interfaces.ITrainingService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Security-admin-only endpoints for marking up incident images with YOLO bounding boxes and
 * exporting them as a retraining dataset. Requires an authenticated JWT; the service layer
 * enforces the SECURITY_ADMIN role (see {@link za.co.urbaneye.reporthole.user.entity.UserRole}).
 */
@RestController
@RequestMapping("admin/training")
@RequiredArgsConstructor
@Tag(name = "Training", description = "Security-admin: annotate incident images and export YOLO training data.")
public class TrainingController {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final ITrainingService trainingService;

    @GetMapping("/incidents/{id}/annotations")
    @Operation(summary = "List annotations", description = "Returns every bounding box on the incident's image, oldest first. Security admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Annotations returned (may be empty)"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<AppResponse<List<AnnotationResponse>>> getAnnotations(@PathVariable UUID id) {
        return ResponseEntity.ok(AppResponse.ok(trainingService.getAnnotations(id)));
    }

    @PostMapping("/incidents/{id}/annotations")
    @Operation(
            summary = "Save annotations",
            description = "Adds one or more bounding boxes (natural-image pixels, top-left origin) to the incident's " +
                    "image; they are stored as normalised YOLO boxes. Does not change the training flag. Security admin only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Annotations created"),
            @ApiResponse(responseCode = "400", description = "Invalid box, or image size differs from the one on record"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<AppResponse<List<AnnotationResponse>>> saveAnnotations(
            @PathVariable UUID id, @Valid @RequestBody SaveAnnotationsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponse.created(trainingService.saveAnnotations(id, request)));
    }

    @DeleteMapping("/annotations/{annotationId}")
    @Operation(
            summary = "Delete annotation",
            description = "Removes one bounding box. If it was the incident's last box, a FLAGGED incident is un-flagged. Security admin only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Annotation deleted"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Annotation not found")
    })
    public ResponseEntity<Void> deleteAnnotation(@PathVariable UUID annotationId) {
        trainingService.deleteAnnotation(annotationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/incidents/{id}/flag-for-training")
    @Operation(
            summary = "Flag incident for training",
            description = "Sets the incident's training flag independently of its workflow status. With ?flagged=true|false " +
                    "the flag is set explicitly (idempotent); without it the flag toggles. Flagging needs at least one " +
                    "annotation. Security admin only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training status updated"),
            @ApiResponse(responseCode = "400", description = "No annotations to flag"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<AppResponse<TrainingStatusResponse>> flagForTraining(
            @PathVariable UUID id, @RequestParam(required = false) Boolean flagged) {
        return ResponseEntity.ok(AppResponse.ok(trainingService.setFlaggedForTraining(id, flagged)));
    }

    @GetMapping(value = "/export/yolo", produces = "application/zip")
    @Operation(
            summary = "Export YOLO dataset",
            description = "Downloads a zip (images/, labels/, data.yaml) of every FLAGGED incident, optionally only those " +
                    "flagged at or after ?since= (ISO-8601 local date-time). Included incidents are marked EXPORTED so " +
                    "they are not exported again. Security admin only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zip returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not a security admin"),
            @ApiResponse(responseCode = "404", description = "Nothing flagged to export")
    })
    public ResponseEntity<byte[]> exportYolo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        byte[] zip = trainingService.exportYoloDataset(since);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("reporthole-yolo-" + FILE_STAMP.format(LocalDateTime.now()) + ".zip").build().toString())
                .body(zip);
    }
}
