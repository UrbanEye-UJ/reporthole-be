package za.co.urbaneye.reporthole.idempotency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records that a client-generated request key has already been processed, so a mutation
 * replayed after a network failure (see the offline-queue sync flow) doesn't re-run its
 * side effects a second time. The key itself is client-supplied (not server-generated) —
 * callers send the same UUID on every retry of the same logical action.
 */
@Entity
@Table(name = "idempotency_key")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @Column(name = "IDEMPOTENCY_KEY_ID", nullable = false, updatable = false)
    private UUID key;

    @Column(name = "IDEMPOTENCY_KEY_CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
