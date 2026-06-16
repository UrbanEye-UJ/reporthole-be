package za.co.urbaneye.reporthole.incident.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.urbaneye.reporthole.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "incident_reporter",
    uniqueConstraints = @UniqueConstraint(columnNames = {"IR_INCIDENT_ID", "IR_USER_ID"})
)
@Getter
@Setter
@NoArgsConstructor
public class IncidentReporter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "IR_ID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IR_INCIDENT_ID", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IR_USER_ID", nullable = false)
    private User user;

    @Column(name = "IR_REPORTED_AT", nullable = false, updatable = false)
    private LocalDateTime reportedAt;

    @PrePersist
    protected void onCreate() {
        this.reportedAt = LocalDateTime.now();
    }

    public IncidentReporter(Incident incident, User user) {
        this.incident = incident;
        this.user = user;
    }
}
