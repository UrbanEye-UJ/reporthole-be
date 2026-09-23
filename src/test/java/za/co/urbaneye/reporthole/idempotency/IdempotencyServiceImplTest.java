package za.co.urbaneye.reporthole.idempotency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.urbaneye.reporthole.idempotency.repository.IdempotencyKeyRepository;
import za.co.urbaneye.reporthole.idempotency.service.impl.IdempotencyServiceImpl;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceImplTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private IdempotencyServiceImpl idempotencyService;

    @Test
    void tryRecord_returnsTrueAndSaves_whenKeyNotSeenBefore() {
        UUID key = UUID.randomUUID();
        when(idempotencyKeyRepository.existsById(key)).thenReturn(false);

        boolean result = idempotencyService.tryRecord(key);

        assertThat(result).isTrue();
        verify(idempotencyKeyRepository).save(argThat(k -> key.equals(k.getKey())));
    }

    @Test
    void tryRecord_returnsFalseAndDoesNotSaveAgain_whenKeyAlreadySeen() {
        UUID key = UUID.randomUUID();
        when(idempotencyKeyRepository.existsById(key)).thenReturn(true);

        boolean result = idempotencyService.tryRecord(key);

        assertThat(result).isFalse();
        verify(idempotencyKeyRepository, never()).save(any());
    }
}
