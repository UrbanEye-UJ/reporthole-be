package za.co.urbaneye.reporthole.inference.dto;

import za.co.urbaneye.reporthole.inference.entity.FrameStatus;

import java.util.UUID;

/**
 * Response body returned immediately when a dashcam frame is accepted for async inference.
 *
 * @param frameId unique identifier for tracking this frame's processing state
 * @param status  always {@link FrameStatus#PENDING} at submission time
 */
public record FrameAcceptedResponse(UUID frameId, FrameStatus status) {}
