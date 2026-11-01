package club.freecity.cms.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String CACHE_USER_DETAILS = "userDetails";
    public static final String CACHE_TENANTS = "tenants";
    public static final String CACHE_RATE_LIMIT = "rateLimit";
    public static final String CACHE_VERIFICATION_CODE = "verificationCode";
    public static final String CACHE_AUTH_REVOCATION = "authRevocation";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 1. 默认配置（针对 userDetails 和 tenants）
        Caffeine<Object, Object> defaultCaffeine = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .initialCapacity(100)
                .maximumSize(1000);
        
        // 2. 针对 rateLimit 的独立配置
        Caffeine<Object, Object> rateLimitCaffeine = Caffeine.newBuilder()
                .expireAfterAccess(60, TimeUnit.MINUTES)
                .initialCapacity(500)
                .maximumSize(5000);

        // 3. 验证码缓存配置 (5分钟过期)
        Caffeine<Object, Object> verificationCaffeine = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .initialCapacity(100)
                .maximumSize(2000);

        // 4. 令牌撤销记录：登出后记录撤销时间，早于该时间签发的所有令牌全部失效。
        //    TTL 与 Refresh Token 有效期（7 天）一致，超过后旧令牌本身也已自然过期
        Caffeine<Object, Object> revocationCaffeine = Caffeine.newBuilder()
                .expireAfterWrite(7, TimeUnit.DAYS)
                .initialCapacity(100)
                .maximumSize(10000);

        cacheManager.registerCustomCache(CACHE_USER_DETAILS, defaultCaffeine.build());
        cacheManager.registerCustomCache(CACHE_TENANTS, defaultCaffeine.build());
        cacheManager.registerCustomCache(CACHE_RATE_LIMIT, rateLimitCaffeine.build());
        cacheManager.registerCustomCache(CACHE_VERIFICATION_CODE, verificationCaffeine.build());
        cacheManager.registerCustomCache(CACHE_AUTH_REVOCATION, revocationCaffeine.build());

        return cacheManager;
    }
}
