package za.co.urbaneye.reporthole.inference.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import za.co.urbaneye.reporthole.global.entity.AppResponse;
import za.co.urbaneye.reporthole.inference.dto.DetectionDTO;
import za.co.urbaneye.reporthole.inference.dto.FrameAcceptedResponse;
import za.co.urbaneye.reporthole.inference.dto.PredictResponseDTO;
import za.co.urbaneye.reporthole.inference.entity.InferenceResult;
import za.co.urbaneye.reporthole.inference.exception.InferenceQueueFullException;
import za.co.urbaneye.reporthole.inference.service.OnnxInferenceService;
import za.co.urbaneye.reporthole.inference.service.interfaces.IFrameSubmissionService;

import java.io.IOException;
import java.util.UUID;

/**
 * REST controller exposing the ONNX road-damage inference endpoint.
 *
 * <p>Accepts a single image file and runs it through {@link OnnxInferenceService}.
 * The response shape intentionally mirrors the FastAPI {@code /predict} endpoint
 * so the Next.js proxy at {@code /api/ml/predict} can switch backends by changing
 * only the {@code ML_SERVICE_URL} environment variable.</p>
 *
 * <p>Authentication: {@code /inference/**} is {@code permitAll} in
 * {@code SecurityConfig} — the endpoint is called from the Next.js server
 * (not the browser directly) and performs read-only inference with no
 * side effects on application data.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@RestController
@RequestMapping("/inference")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inference", description = "Road-damage inference via ONNX Runtime")
public class InferenceController {

    private final OnnxInferenceService inferenceService;
    private final IFrameSubmissionService frameSubmissionService;

    /**
     * Accepts an image file (JPEG or PNG) and returns the highest-confidence
     * road-damage prediction from the Reporthole YOLOv8 model.
     *
     * <p>When no damage class exceeds zero confidence the response body has
     * {@code "detected": false} and all detection fields are {@code null}.</p>
     *
     * @param image multipart image file to analyse
     * @return {@link PredictResponseDTO} with detection result, or
     *         {@link PredictResponseDTO#empty()} when nothing is detected;
     *         HTTP 400 if the file cannot be read; HTTP 500 on model error
     */
    @PostMapping(value = "/predict", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Run road-damage inference on an image",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = PredictResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Unreadable image file"),
                    @ApiResponse(responseCode = "500", description = "Model inference error")
            }
    )
    public ResponseEntity<PredictResponseDTO> predict(
            @RequestParam("image") MultipartFile image) {

        log.info("Predict request received — filename: {}, size: {} bytes",
                image.getOriginalFilename(), image.getSize());

        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException e) {
            log.warn("Failed to read uploaded image: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        InferenceResult result;
        try {
            result = inferenceService.predict(bytes);
        } catch (Exception e) {
            log.error("Inference failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }

        if (!result.detected()) {
            return ResponseEntity.ok(PredictResponseDTO.empty());
        }

        DetectionDTO detection = new DetectionDTO(
                result.label(),
                result.confidence(),
                result.rawLabel()
        );
        return ResponseEntity.ok(new PredictResponseDTO(true, detection));
    }

    /**
     * Accepts a dashcam frame for asynchronous inference and returns immediately.
     *
     * <p>The frame is handed off to a bounded background thread pool; inference
     * runs independently of this request. The {@code frameId} in the response
     * can be used to correlate future SSE notifications (not yet wired) when
     * inference completes.</p>
     *
     * <p>Authentication: requires a device token (plain UUID Bearer token) issued by
     * {@code POST /devices/token/generate}. The token scope has been extended to
     * include this path in {@code JwtAuthenticationFilter}.</p>
     *
     * @param image multipart image file captured by the dashcam
     * @return {@code 202 Accepted} with a {@link FrameAcceptedResponse}; or
     *         {@code 400} if the file cannot be read; or
     *         {@code 503} if the inference queue is currently full
     */
    @PostMapping(value = "/frames", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Submit a dashcam frame for asynchronous inference",
            description = "Accepts the frame immediately (202) and runs ONNX inference on a background thread. Requires a device token.",
            responses = {
                    @ApiResponse(responseCode = "202",
                            content = @Content(schema = @Schema(implementation = FrameAcceptedResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Unreadable image file"),
                    @ApiResponse(responseCode = "503", description = "Inference queue full — reduce frame rate")
            }
    )
    public ResponseEntity<?> submitFrame(@RequestParam("image") MultipartFile image) {
        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException e) {
            log.warn("Failed to read dashcam frame: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        String principal = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        UUID deviceUserId = UUID.fromString(principal);

        try {
            FrameAcceptedResponse response = frameSubmissionService.submit(bytes, deviceUserId);
            log.info("Frame {} accepted for user {}", response.frameId(), deviceUserId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (InferenceQueueFullException ex) {
            log.warn("Inference queue full — rejecting frame from user {}", deviceUserId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(AppResponse.of(null, ex.getMessage(), 503));
        }
    }
}
