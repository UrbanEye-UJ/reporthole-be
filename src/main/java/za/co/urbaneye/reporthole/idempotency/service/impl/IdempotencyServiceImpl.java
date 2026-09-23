package za.co.urbaneye.reporthole.idempotency.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.idempotency.entity.IdempotencyKey;
import za.co.urbaneye.reporthole.idempotency.repository.IdempotencyKeyRepository;
import za.co.urbaneye.reporthole.idempotency.service.interfaces.IIdempotencyService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IIdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Override
    public boolean tryRecord(UUID key) {
        if (idempotencyKeyRepository.existsById(key)) {
            return false;
        }
        idempotencyKeyRepository.save(IdempotencyKey.builder().key(key).build());
        return true;
    }
}
