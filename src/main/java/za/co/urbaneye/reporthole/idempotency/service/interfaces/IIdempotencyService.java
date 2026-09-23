package za.co.urbaneye.reporthole.idempotency.service.interfaces;

import java.util.UUID;

public interface IIdempotencyService {

    /**
     * Records {@code key} as processed if it hasn't been seen before.
     *
     * @return {@code true} the first time a given key is seen (caller should proceed with the
     *         action), {@code false} if it was already recorded (caller should skip the action's
     *         side effects and just return the current state).
     */
    boolean tryRecord(UUID key);
}
