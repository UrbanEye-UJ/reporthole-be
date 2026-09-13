# Module: inference

On-device ONNX inference pipeline for dashcam-submitted frames. When a dashcam device captures a frame with medium confidence (65–79%), the Spring Boot backend runs a second-pass inference using a bundled ONNX model. High-confidence detections (≥80%) are accepted directly without re-inference.

---

## Package structure

```
inference/
├── config/
│   └── InferenceProperties.java      — configurable confidence thresholds, queue capacity
├── controller/
│   └── InferenceController.java      — POST /inference/frame
├── dto/
│   ├── DetectionDTO.java             — label, confidence, bounding box
│   ├── FrameAcceptedResponse.java    — jobId returned on async submission
│   └── PredictResponseDTO.java       — detected: bool, detection: DetectionDTO
├── entity/
│   ├── FrameJob.java                 — pending/in-progress inference job
│   ├── FrameStatus.java              — PENDING | PROCESSING | DONE | FAILED
│   ├── InferenceResult.java          — final classification for a frame
│   └── RoutingDecision.java          — ACCEPT_DIRECT | RUN_INFERENCE | REJECT
├── exception/
│   └── InferenceQueueFullException.java
└── service/
    ├── ImagePreprocessor.java         — resizes and normalises frame bytes for ONNX input
    ├── OnnxInferenceService.java      — loads ONNX model, runs inference session
    └── impl/
        └── AsyncFrameSubmissionService.java — queues frames, runs ONNX async, persists result
```

---

## Confidence routing

`RoutingDecision` determines what happens to each incoming frame based on the on-device confidence score sent in the request:

| Score | `RoutingDecision` | Action |
|-------|----------|--------|
| ≥ 80% | `AUTO_LOG` | Incident created and auto-verified (see `incident` module's `AiReviewDecision`) |
| 65–79% | `ESCALATE` | Incident created but left `REPORTED` for human (admin) review |
| < 65% | `DISCARD` | Frame discarded, no incident created |

Thresholds are configured in `InferenceProperties` (bound from `inference.*` in `application.yml`).
Both `AUTO_LOG` and `ESCALATE` incidents are created via `IncidentService.createIncident`, passing
the detection confidence through so the `incident` module's own AI approval threshold
(`incident.ai-approval-threshold`) decides whether it's auto-verified or queued for review.

---

## ONNX model

The model file (`Reporthole-v1.onnx`) lives in `src/main/resources/models/` but is **gitignored** — it must be placed there manually on each machine. Ask a teammate for the model binary or re-export from the `reporthole-ml` service.

`OnnxInferenceService` loads the model via the ONNX Runtime Java API at startup. The service fails fast if the model file is missing.

`ImagePreprocessor` resizes each frame to the model's expected input dimensions and normalises pixel values before creating the ONNX tensor.

---

## Async processing

`AsyncFrameSubmissionService` uses Spring's `@Async` executor (configured in `config/AsyncConfig.java`) to avoid blocking the HTTP request thread during inference. On submission:

1. `RoutingDecision` is evaluated immediately.
2. For `RUN_INFERENCE`: frame is queued as a `FrameJob`; the response returns a `jobId` immediately.
3. The async executor picks up the job, runs `OnnxInferenceService`, and persists the `InferenceResult`.
4. If the queue is full, `InferenceQueueFullException` is thrown and the frame is rejected with 503.

---

## Tests

| Test class | Type |
|-----------|------|
| `OnnxInferenceServiceTest` | Unit (Mockito) |
| `ImagePreprocessorTest` | Unit |
| `RoutingDecisionTest` | Unit |
| `ConfidenceExtractorTest` | Unit |
