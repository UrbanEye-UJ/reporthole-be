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
import org.locationtech.jts.geom.Point;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "incident")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "INCIDENT_ID", updatable = false, nullable = false)
    private UUID incidentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "INCIDENT_TYPE", nullable = false)
    private IssueType incidentType;

    @Column(name = "INCIDENT_DESCRIPTION", nullable = false, length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "INCIDENT_SOURCE", nullable = false)
    private IncidentSource source;

    @Column(name = "INCIDENT_DATE", nullable = false, updatable = false)
    private LocalDateTime incidentDate;

    @Column(name = "INCIDENT_LOCATION", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "INCIDENT_IMAGE_URL", length = 255)
    private String imageUrl;

    @Column(name = "INCIDENT_LOCATION_ADDRESS", length = 300)
    private String locationAddress;

    @Column(name = "INCIDENT_REPORT_COUNT", nullable = false)
    private int reportCount = 1;

    @Column(name = "INCIDENT_DELETED", nullable = false)
    private boolean deleted = false;

    @Column(name = "INCIDENT_AI_GENERATED", nullable = false)
    private boolean aiGenerated = false;

    @Column(name = "INCIDENT_AI_CONFIDENCE")
    private Double aiConfidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INCIDENT_USER_ID", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        this.incidentDate = LocalDateTime.now();
    }
}
