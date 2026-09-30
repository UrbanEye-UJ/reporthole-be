package za.co.urbaneye.reporthole.security.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.security.AccountStatusSnapshot;
import za.co.urbaneye.reporthole.security.service.interfaces.IAccountStatusService;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;

import java.util.UUID;

/**
 * @see IAccountStatusService
 */
@Service
@RequiredArgsConstructor
public class AccountStatusServiceImpl implements IAccountStatusService {

    private final IUserAuthRepository userAuthRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Cached under {@code accountStatus} keyed by {@code userId}. Callers that change an
     * account's status or {@code credentialsValidFrom} must evict this cache entry — see the
     * {@code @CacheEvict} usages at each write site (account suspension, forced session bump,
     * admin-application approval, self-logout, self-delete).</p>
     */
    @Override
    @Cacheable(cacheNames = "accountStatus", key = "#userId")
    public AccountStatusSnapshot getStatus(UUID userId) {
        return userAuthRepository.findById(userId)
                .map(account -> new AccountStatusSnapshot(true, account.getStatus(), account.getCredentialsValidFrom()))
                .orElseGet(() -> new AccountStatusSnapshot(false, null, null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CacheEvict(cacheNames = "accountStatus", key = "#userId")
    public void evict(UUID userId) {
        // Body intentionally empty — eviction is performed by @CacheEvict above.
    }
}
