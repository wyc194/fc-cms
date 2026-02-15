package club.freecity.cms.support.ratelimit;

/**
 * 限流器接口，支持后续扩展 Redis 实现
 */
public interface RateLimiter {
    /**
     * 检查是否触发限流
     *
     * @param key    限流唯一标识
     * @param count  限制次数
     * @param window 时间窗口（秒）
     * @return true 如果已被限流，false 如果允许访问
     */
    boolean isAllowed(String key, int count, int window);
}
