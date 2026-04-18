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


import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "IMAGE_ID", updatable = false, nullable = false)
    private UUID imageId;

    @Column(name = "IMAGE_URL", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "IMAGE_DATE", nullable = false, updatable = false)
    private LocalDateTime imageDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IMAGE_INCIDENT_ID", nullable = false)
    private Incident incident;

    @PrePersist
    protected void onCreate() {
        this.imageDate = LocalDateTime.now();
    }
}
