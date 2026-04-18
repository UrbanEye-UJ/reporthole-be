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
@Table(name = "assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ASSIGNMENT_ID", updatable = false, nullable = false)
    private UUID assignmentId;

    @Column(name = "ASSIGNMENT_DATE", nullable = false, updatable = false)
    private LocalDateTime assignmentDate;

    @Column(name = "ASSIGNMENT_COMPLETION_DATE")
    private LocalDateTime completionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "ASSIGNMENT_STATUS", nullable = false)
    private AssignmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ASSIGNMENT_INCIDENT_ID", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ASSIGNMENT_CONTRACTOR_USER_ID", nullable = false)
    private User contractor;

    @PrePersist
    protected void onCreate() {
        this.assignmentDate = LocalDateTime.now();
    }
}
