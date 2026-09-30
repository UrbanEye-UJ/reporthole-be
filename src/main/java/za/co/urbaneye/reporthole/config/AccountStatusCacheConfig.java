package za.co.urbaneye.reporthole.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Enables an in-process Caffeine cache for the {@code accountStatus} cache used by
 * {@code AccountStatusServiceImpl} (in turn read by {@code JwtAuthenticationFilter} on every
 * authenticated request), so the account-status check doesn't cost a database round trip per
 * request.
 *
 * <p>In-process rather than a shared/distributed cache because the project runs a single
 * backend instance — there is no second node whose cache could drift out of sync.</p>
 *
 * <p>The short time-to-live is a safety net only. Every known write site that changes account
 * status or {@code credentialsValidFrom} (suspend, force-logout, admin-application approval,
 * self-logout, self-delete) evicts the affected entry immediately, so the TTL should rarely be
 * the thing that catches a stale read.</p>
 */
@Configuration
@EnableCaching
public class AccountStatusCacheConfig {

    private static final String ACCOUNT_STATUS_CACHE = "accountStatus";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(ACCOUNT_STATUS_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(10_000));
        return cacheManager;
    }
}
