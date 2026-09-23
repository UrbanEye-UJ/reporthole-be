package za.co.urbaneye.reporthole.training.entity;

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
import za.co.urbaneye.reporthole.incident.entity.Incident;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One YOLO bounding box drawn by an admin on an incident's image. An incident can have many
 * (several defects in one photo). Coordinates are normalised 0–1 and centre-based, exactly as
 * they appear in a YOLO label file.
 */
@Entity
@Table(name = "issue_annotation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ANNOTATION_ID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ANNOTATION_INCIDENT_ID", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "ANNOTATION_CLASS_LABEL", nullable = false)
    private IssueType classLabel;

    @Column(name = "ANNOTATION_X_CENTER", nullable = false)
    private double xCenter;

    @Column(name = "ANNOTATION_Y_CENTER", nullable = false)
    private double yCenter;

    @Column(name = "ANNOTATION_WIDTH", nullable = false)
    private double width;

    @Column(name = "ANNOTATION_HEIGHT", nullable = false)
    private double height;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ANNOTATION_ANNOTATED_BY", nullable = false)
    private User annotatedBy;

    @Column(name = "ANNOTATION_CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
