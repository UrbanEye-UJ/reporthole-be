package za.co.urbaneye.reporthole.device.entity;

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
@Table(name = "dashcam_device")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashcamDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "DEVICE_ID", updatable = false, nullable = false)
    private UUID deviceId;

    @Column(name = "DEVICE_TOKEN", nullable = false, length = 255)
    private String deviceToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEVICE_USER_ID", nullable = false)
    private User user;

    @Column(name = "DEVICE_CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
