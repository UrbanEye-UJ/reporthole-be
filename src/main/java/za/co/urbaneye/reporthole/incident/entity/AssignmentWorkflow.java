package za.co.urbaneye.reporthole.incident.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.urbaneye.reporthole.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assignment_workflow")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ASSIGNMENT_WORKFLOW_ID", updatable = false, nullable = false)
    private UUID workflowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ASSIGNMENT_WORKFLOW_INCIDENT", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ASSIGNMENT_WORKFLOW_UPDATEDBY", nullable = true)
    private User updatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "ASSIGNMENT_WORKFLOW_STATUS", nullable = false)
    private AssignmentStatus status;

    @Column(name = "ASSIGNMENT_WORKFLOW_UPDATED_DATE", nullable = false)
    private LocalDateTime updatedDate;

    @Column(name = "ASSIGNMENT_WORKFLOW_NOTES", length = 255)
    private String notes;

    @PrePersist
    protected void onCreate() {
        this.updatedDate = LocalDateTime.now();
    }
}
