package za.co.urbaneye.reporthole.inference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.urbaneye.reporthole.incident.service.interfaces.IncidentService;
import za.co.urbaneye.reporthole.inference.config.InferenceProperties;
import za.co.urbaneye.reporthole.inference.dto.EscalatedFrameDTO;
import za.co.urbaneye.reporthole.inference.dto.FrameAcceptedResponse;
import za.co.urbaneye.reporthole.inference.entity.FrameJob;
import za.co.urbaneye.reporthole.inference.entity.FrameStatus;
import za.co.urbaneye.reporthole.inference.entity.RoutingDecision;
import za.co.urbaneye.reporthole.inference.service.OnnxInferenceService;
import za.co.urbaneye.reporthole.inference.service.impl.AsyncFrameSubmissionService;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the escalated-frame review methods on {@link AsyncFrameSubmissionService}.
 *
 * <p>The in-memory job map is populated directly via reflection to decouple these tests
 * from the full async submission path (which involves the ONNX model).</p>
 */
@ExtendWith(MockitoExtension.class)
class EscalatedFrameServiceTest {

    @Mock private OnnxInferenceService inferenceService;
    @Mock private IncidentService incidentService;
    @Mock private InferenceProperties inferenceProperties;

    private AsyncFrameSubmissionService service;
    private ConcurrentHashMap<UUID, FrameJob> jobs;

    @BeforeEach
    void setUp() throws Exception {
        service = new AsyncFrameSubmissionService(inferenceService, incidentService, inferenceProperties);

        // Access the private jobs map via reflection
        Field jobsField = AsyncFrameSubmissionService.class.getDeclaredField("jobs");
        jobsField.setAccessible(true);
        //noinspection unchecked
        jobs = (ConcurrentHashMap<UUID, FrameJob>) jobsField.get(service);
    }

    private FrameJob makeJob(RoutingDecision decision, FrameStatus status) {
        return FrameJob.builder()
                .frameId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .status(status)
                .imageBytes(new byte[]{1, 2, 3})
                .label("POTHOLE")
                .confidence(0.70)
                .routingDecision(decision.name())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getEscalatedFrames_returnsOnlyEscalatedDoneJobs() {
        FrameJob escalated = makeJob(RoutingDecision.ESCALATE, FrameStatus.DONE);
        FrameJob autoLog = makeJob(RoutingDecision.AUTO_LOG, FrameStatus.DONE);
        FrameJob pending = makeJob(RoutingDecision.ESCALATE, FrameStatus.PENDING);

        jobs.put(escalated.getFrameId(), escalated);
        jobs.put(autoLog.getFrameId(), autoLog);
        jobs.put(pending.getFrameId(), pending);

        List<EscalatedFrameDTO> result = service.getEscalatedFrames();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).frameId()).isEqualTo(escalated.getFrameId());
    }

    @Test
    void approveFrame_createsIncidentAndRemovesJob() {
        FrameJob job = makeJob(RoutingDecision.ESCALATE, FrameStatus.DONE);
        jobs.put(job.getFrameId(), job);

        service.approveFrame(job.getFrameId());

        verify(incidentService).createIncident(any());
        assertThat(jobs).doesNotContainKey(job.getFrameId());
    }

    @Test
    void approveFrame_throws_whenFrameNotFound() {
        assertThatThrownBy(() -> service.approveFrame(UUID.randomUUID()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void approveFrame_throws_whenFrameIsNotEscalated() {
        FrameJob job = makeJob(RoutingDecision.AUTO_LOG, FrameStatus.DONE);
        jobs.put(job.getFrameId(), job);

        assertThatThrownBy(() -> service.approveFrame(job.getFrameId()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void discardFrame_removesJobWithoutCreatingIncident() {
        FrameJob job = makeJob(RoutingDecision.ESCALATE, FrameStatus.DONE);
        jobs.put(job.getFrameId(), job);

        service.discardFrame(job.getFrameId());

        assertThat(jobs).doesNotContainKey(job.getFrameId());
        verify(incidentService, org.mockito.Mockito.never()).createIncident(any());
    }

    @Test
    void discardFrame_throws_whenFrameNotFound() {
        assertThatThrownBy(() -> service.discardFrame(UUID.randomUUID()))
                .isInstanceOf(NoSuchElementException.class);
    }
}
