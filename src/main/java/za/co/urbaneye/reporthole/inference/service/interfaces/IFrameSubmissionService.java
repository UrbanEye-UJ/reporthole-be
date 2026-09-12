package za.co.urbaneye.reporthole.inference.service.interfaces;

import za.co.urbaneye.reporthole.inference.dto.EscalatedFrameDTO;
import za.co.urbaneye.reporthole.inference.dto.FrameAcceptedResponse;

import java.util.List;
import java.util.UUID;

/**
 * Contract for accepting a dashcam frame for asynchronous inference.
 *
 * <p>Implementations must return immediately after accepting the frame — inference
 * must not block the calling thread. The {@link FrameAcceptedResponse} carries a
 * {@code frameId} the client can use to correlate future status notifications.</p>
 *
 * <p>This interface is the microservice boundary: the current in-process
 * {@link za.co.urbaneye.reporthole.inference.service.impl.AsyncFrameSubmissionService}
 * can be replaced by a {@code RemoteFrameSubmissionService} that POSTs to a
 * dedicated inference service without changing any controller code.</p>
 *
 * @throws za.co.urbaneye.reporthole.inference.exception.InferenceQueueFullException
 *         if the backing queue cannot accept further frames right now
 */
public interface IFrameSubmissionService {

    /**
     * Accepts raw image bytes from a dashcam and queues them for inference.
     *
     * @param imageBytes   JPEG or PNG bytes of the captured frame
     * @param deviceUserId the user ID linked to the submitting device token
     * @return a response containing a unique {@code frameId} and initial status {@code PENDING}
     */
    FrameAcceptedResponse submit(byte[] imageBytes, UUID deviceUserId);

    /**
     * Returns all completed frames whose routing decision was {@code ESCALATE},
     * ordered oldest-first so the operator works through the backlog in arrival order.
     */
    List<EscalatedFrameDTO> getEscalatedFrames();

    /**
     * Promotes an escalated frame to an incident and removes it from the review queue.
     *
     * @param frameId the frame to approve
     * @throws java.util.NoSuchElementException if no escalated frame with that ID exists
     */
    void approveFrame(UUID frameId);

    /**
     * Discards an escalated frame without creating an incident.
     *
     * @param frameId the frame to discard
     * @throws java.util.NoSuchElementException if no escalated frame with that ID exists
     */
    void discardFrame(UUID frameId);
}
