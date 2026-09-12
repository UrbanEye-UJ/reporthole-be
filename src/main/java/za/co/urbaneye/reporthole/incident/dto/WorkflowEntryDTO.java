package za.co.urbaneye.reporthole.incident.dto;

import za.co.urbaneye.reporthole.incident.entity.AssignmentStatus;
import za.co.urbaneye.reporthole.incident.entity.AssignmentWorkflow;

import java.time.LocalDateTime;

/**
 * A single entry in an incident's status/progress history, surfaced to admins.
 *
 * @param status      the status recorded at this point in the workflow
 * @param notes       optional free-text note added by the contractor
 * @param updatedDate when this entry was written
 * @param updatedBy   display name of the user who wrote it, or {@code null} for system entries
 */
public record WorkflowEntryDTO(
        AssignmentStatus status,
        String notes,
        LocalDateTime updatedDate,
        String updatedBy
) {
    /** Maps an {@link AssignmentWorkflow} entity to this DTO. */
    public static WorkflowEntryDTO from(AssignmentWorkflow w) {
        String name = null;
        if (w.getUpdatedBy() != null) {
            name = (w.getUpdatedBy().getFirstName() + " " + w.getUpdatedBy().getLastName()).trim();
        }
        return new WorkflowEntryDTO(w.getStatus(), w.getNotes(), w.getUpdatedDate(), name);
    }
}
