package club.freecity.cms.service;

import club.freecity.cms.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final CacheManager cacheManager;

    /**
     * 清除指定用户的缓存
     * @param tenantId 租户ID
     * @param username 用户名
     */
    public void evictUserCache(Long tenantId, String username) {
        if (cacheManager != null && cacheManager.getCache(CacheConfig.CACHE_USER_DETAILS) != null) {
            cacheManager.getCache(CacheConfig.CACHE_USER_DETAILS).evict(tenantId + ":" + username);
        }
    }
}
