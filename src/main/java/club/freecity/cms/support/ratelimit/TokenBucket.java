package club.freecity.cms.support.ratelimit;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 令牌桶实体类
 */
@Getter
public class TokenBucket {
    /**
     * 桶的最大容量（即 count）
     */
    private final long capacity;
    
    /**
     * 令牌填充速率（每秒生成的令牌数）
     */
    private final double refillRate;
    
    /**
     * 当前桶中的令牌数
     */
    private final AtomicLong tokens;
    
    /**
     * 上次填充令牌的时间戳（毫秒）
     */
    private final AtomicLong lastRefillTimestamp;

    public TokenBucket(long capacity, int windowSeconds) {
        this.capacity = capacity;
        // 计算填充速率：总数 / 窗口时间（秒）
        this.refillRate = (double) capacity / windowSeconds;
        this.tokens = new AtomicLong(capacity); // 初始满桶
        this.lastRefillTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    /**
     * 尝试获取令牌
     * @return 是否获取成功
     */
    public boolean tryAcquire() {
        refill();
        long currentTokens = tokens.get();
        while (currentTokens > 0) {
            if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                return true;
            }
            currentTokens = tokens.get();
        }
        return false;
    }

    /**
     * 填充令牌
     */
    private void refill() {
        long now = System.currentTimeMillis();
        long last = lastRefillTimestamp.get();
        long delta = now - last;
        
        if (delta <= 0) {
            return;
        }

        // 计算这段时间内应该生成的令牌数
        long newTokens = (long) (delta * refillRate / 1000.0);
        if (newTokens > 0) {
            if (lastRefillTimestamp.compareAndSet(last, now)) {
                tokens.updateAndGet(current -> Math.min(capacity, current + newTokens));
            }
        }
    }
}
