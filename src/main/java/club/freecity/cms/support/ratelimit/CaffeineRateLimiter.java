package club.freecity.cms.support.ratelimit;

import club.freecity.cms.config.CacheConfig;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * 基于 Caffeine 缓存令牌桶的单机限流实现
 */
@Component
@RequiredArgsConstructor
public class CaffeineRateLimiter implements RateLimiter {

    private final CacheManager cacheManager;

    @Override
    @SuppressWarnings("unchecked")
    public boolean isAllowed(String key, int count, int window) {
        // 从统一的 CacheManager 中获取 rateLimit 专用缓存实例
        org.springframework.cache.Cache springCache = cacheManager.getCache(CacheConfig.CACHE_RATE_LIMIT);
        if (springCache == null) {
            return true;
        }
        
        // 获取底层的 Caffeine Cache 实例
        Cache<String, TokenBucket> nativeCache = (Cache<String, TokenBucket>) springCache.getNativeCache();
        
        // 获取或创建令牌桶
        TokenBucket bucket = nativeCache.get(key, k -> new TokenBucket(count, window));
        
        // 尝试获取令牌
        return bucket.tryAcquire();
    }
}
