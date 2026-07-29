package za.co.urbaneye.reporthole.inference.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.incident.dto.IncidentRequestDTO;
import za.co.urbaneye.reporthole.incident.entity.IncidentSource;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.incident.service.interfaces.IncidentService;
import za.co.urbaneye.reporthole.inference.config.InferenceProperties;
import za.co.urbaneye.reporthole.inference.dto.FrameAcceptedResponse;
import za.co.urbaneye.reporthole.inference.entity.FrameJob;
import za.co.urbaneye.reporthole.inference.entity.FrameStatus;
import za.co.urbaneye.reporthole.inference.entity.InferenceResult;
import za.co.urbaneye.reporthole.inference.entity.RoutingDecision;
import za.co.urbaneye.reporthole.inference.exception.InferenceQueueFullException;
import za.co.urbaneye.reporthole.inference.service.OnnxInferenceService;
import za.co.urbaneye.reporthole.inference.service.interfaces.IFrameSubmissionService;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

/**
 * In-process implementation of {@link IFrameSubmissionService}.
 *
 * <p>Accepts a dashcam frame, registers a {@link FrameJob} in memory, and hands
 * it to the bounded {@code inferenceExecutor} thread pool via {@link #processAsync}.
 * The HTTP thread returns as soon as the job is enqueued — inference runs
 * independently on a background thread.</p>
 *
 * <p><b>Backpressure:</b> {@code inferenceExecutor} is configured with a bounded
 * queue and {@code AbortPolicy}. When the queue is full, Spring throws
 * {@link RejectedExecutionException}, which is caught here and re-thrown as
 * {@link InferenceQueueFullException} for the controller to map to HTTP 503.</p>
 *
 * <p><b>SSE hook point:</b> After updating a job's status to {@code DONE} or
 * {@code FAILED}, add a call to {@code IncidentSseService.pushInferenceUpdate()}
 * to notify the connected dashcam operator in real time.</p>
 *
 * <p><b>Future microservice boundary:</b> Replace this bean with a
 * {@code RemoteFrameSubmissionService} that POSTs to a separate inference
 * service. The controller depends only on {@link IFrameSubmissionService} and
 * requires no changes.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncFrameSubmissionService implements IFrameSubmissionService {

    private final OnnxInferenceService inferenceService;
    private final IncidentService incidentService;
    private final InferenceProperties inferenceProperties;

    /**
     * In-memory job registry. Keys are frame IDs assigned at submission time.
     * Future persistence: convert to a JPA repository backed by a
     * {@code pending_frame} table with a {@code BYTEA image_bytes} column.
     */
    private final ConcurrentHashMap<UUID, FrameJob> jobs = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     *
     * <p>The frame is stored in {@link #jobs} with status {@code PENDING}, then
     * submitted to the bounded inference executor. If the executor's queue is full,
     * the job is removed and {@link InferenceQueueFullException} is thrown so the
     * controller can respond with HTTP 503.</p>
     */
    @Override
    public FrameAcceptedResponse submit(byte[] imageBytes, UUID deviceUserId) {
        UUID frameId = UUID.randomUUID();

        FrameJob job = FrameJob.builder()
                .frameId(frameId)
                .userId(deviceUserId)
                .status(FrameStatus.PENDING)
                .imageBytes(imageBytes)
                .createdAt(LocalDateTime.now())
                .build();

        jobs.put(frameId, job);
        log.info("[Frame {}] Accepted from user {} ({} bytes)", frameId, deviceUserId, imageBytes.length);

        try {
            processAsync(frameId, deviceUserId);
        } catch (RejectedExecutionException ex) {
            jobs.remove(frameId);
            log.warn("[Frame {}] Rejected — inference queue is full", frameId);
            throw new InferenceQueueFullException("Inference queue full — slow down frame submission");
        }

        return new FrameAcceptedResponse(frameId, FrameStatus.PENDING);
    }

    /**
     * Runs ONNX inference on the frame identified by {@code frameId}, applies
     * threshold routing, and (for {@code AUTO_LOG}) creates an incident.
     *
     * <p>Executes on the {@code inferenceExecutor} thread pool — never on the
     * HTTP request thread. All exceptions are caught, logged, and recorded on
     * the job so they are visible without crashing the worker.</p>
     *
     * @param frameId      the job to process
     * @param deviceUserId the owning user's ID — set into the security context
     *                     so {@link IncidentService#createIncident} can resolve
     *                     the current user
     */
    @Async("inferenceExecutor")
    public void processAsync(UUID frameId, UUID deviceUserId) {
        FrameJob job = jobs.get(frameId);
        if (job == null) {
            log.warn("[Frame {}] Job not found in registry — skipping", frameId);
            return;
        }

        job.setStatus(FrameStatus.PROCESSING);
        log.info("[Frame {}] Starting inference on thread {}", frameId, Thread.currentThread().getName());

        // Seed the security context so IncidentService can read the current user.
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                deviceUserId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_CIVILIAN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            InferenceResult result = inferenceService.predict(job.getImageBytes());
            RoutingDecision decision = RoutingDecision.from(result.confidence(), inferenceProperties);

            log.info("[Frame {}] Inference done — label={}, confidence={}, decision={}",
                    frameId, result.label(), result.confidence(), decision);

            job.setLabel(result.label());
            job.setConfidence(result.confidence());
            job.setRoutingDecision(decision.name());

            if (decision == RoutingDecision.AUTO_LOG && result.detected()) {
                createIncidentForFrame(frameId, job, result);
            }

            job.setStatus(FrameStatus.DONE);
            // Future SSE hook: sseService.pushInferenceUpdate(deviceUserId, frameId, result, decision);

        } catch (Exception ex) {
            log.error("[Frame {}] Inference failed: {}", frameId, ex.getMessage(), ex);
            job.setErrorMessage(ex.getMessage());
            job.setStatus(FrameStatus.FAILED);
            // Future SSE hook: sseService.pushInferenceUpdate(deviceUserId, frameId, null, null);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Calls {@link IncidentService#createIncident} for a frame that exceeded the
     * AUTO_LOG confidence threshold. The image is encoded to Base64 as required
     * by {@link IncidentRequestDTO}. GPS coordinates are not available from the
     * frame bytes — they default to 0.0 (the dashcam's GPS must be attached to
     * the frame submission in a future enhancement).
     */
    private void createIncidentForFrame(UUID frameId, FrameJob job, InferenceResult result) {
        try {
            String imageBase64 = Base64.getEncoder().encodeToString(job.getImageBytes());
            IssueType issueType = parseIssueType(result.label());

            IncidentRequestDTO request = new IncidentRequestDTO(
                    issueType,
                    String.format("Dashcam AUTO_LOG: %s detected at %.0f%% confidence.",
                            result.label(), result.confidence() * 100),
                    IncidentSource.DASHCAM,
                    0.0,   // latitude — GPS not yet attached to async frame submission
                    0.0,   // longitude
                    imageBase64,
                    true,  // forceCreate — bypass duplicate check for dashcam frames
                    null
            );

            incidentService.createIncident(request);
            log.info("[Frame {}] Incident created (AUTO_LOG)", frameId);
        } catch (Exception ex) {
            log.error("[Frame {}] Failed to create incident after AUTO_LOG: {}", frameId, ex.getMessage(), ex);
            job.setStatus(FrameStatus.FAILED);
            job.setErrorMessage("Inference succeeded but incident creation failed: " + ex.getMessage());
        }
    }

    /**
     * Maps an inference label string to {@link IssueType}, falling back to
     * {@link IssueType#POTHOLE} for unknown labels.
     */
    private IssueType parseIssueType(String label) {
        if (label == null) return IssueType.POTHOLE;
        try {
            return IssueType.valueOf(label);
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown inference label '{}' — defaulting to POTHOLE", label);
            return IssueType.POTHOLE;
        }
    }
}
