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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.urbaneye.reporthole.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A comment posted on an incident by any authenticated user (civilian, admin, or contractor).
 * Comments are append-only and ordered by creation time.
 */
@Entity
@Table(name = "incident_comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "COMMENT_ID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INCIDENT_ID", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AUTHOR_ID", nullable = false)
    private User author;

    @Column(name = "CONTENT", nullable = false, length = 500)
    private String content;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
