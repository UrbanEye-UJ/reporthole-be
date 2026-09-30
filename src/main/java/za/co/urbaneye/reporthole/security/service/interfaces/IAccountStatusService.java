package za.co.urbaneye.reporthole.security.service.interfaces;

import za.co.urbaneye.reporthole.security.AccountStatusSnapshot;

import java.util.UUID;

/**
 * Looks up the account fields {@code JwtAuthenticationFilter} needs on every request
 * (status, session-revocation watermark), cached so repeat requests for the same account
 * don't each cost a database round trip.
 */
public interface IAccountStatusService {

    /**
     * Returns the current status snapshot for an account.
     *
     * @param userId the account's ID (the JWT subject claim, parsed to a UUID)
     * @return the cached (or freshly loaded) snapshot; {@link AccountStatusSnapshot#exists()} is
     *         {@code false} if no such account exists
     */
    AccountStatusSnapshot getStatus(UUID userId);

    /**
     * Evicts the cached snapshot for an account. Callers must invoke this immediately after
     * saving any change to a {@code UserAuth}'s {@code status} or {@code credentialsValidFrom} —
     * otherwise a stale snapshot can keep serving a revoked or superseded status for up to the
     * cache's time-to-live.
     *
     * @param userId the account whose cached snapshot should be dropped
     */
    void evict(UUID userId);
}
