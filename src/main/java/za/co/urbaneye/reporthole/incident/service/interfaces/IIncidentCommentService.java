package za.co.urbaneye.reporthole.incident.service.interfaces;

import za.co.urbaneye.reporthole.incident.dto.IncidentCommentResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for reading and posting incident comments.
 * Any authenticated user (civilian, admin, contractor) may interact with comments.
 */
public interface IIncidentCommentService {

    /** Returns all comments for the given incident, oldest first. */
    List<IncidentCommentResponse> getComments(UUID incidentId);

    /**
     * Posts a comment on the given incident on behalf of the currently authenticated user.
     * Throws 404 if the incident does not exist.
     */
    IncidentCommentResponse addComment(UUID incidentId, String content);
}
