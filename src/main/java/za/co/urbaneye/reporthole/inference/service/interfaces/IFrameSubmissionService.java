package za.co.urbaneye.reporthole.inference.service.interfaces;

import za.co.urbaneye.reporthole.inference.dto.FrameAcceptedResponse;

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
}
