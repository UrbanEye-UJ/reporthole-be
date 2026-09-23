package za.co.urbaneye.reporthole.idempotency.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.urbaneye.reporthole.idempotency.entity.IdempotencyKey;

import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
}
