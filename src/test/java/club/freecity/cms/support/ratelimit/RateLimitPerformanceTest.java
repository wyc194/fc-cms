package club.freecity.cms.support.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitPerformanceTest {

    @Test
    @DisplayName("令牌桶限流 - 单线程逻辑验证")
    void testTokenBucket_Basic() {
        // 容量 10，窗口 1 秒
        TokenBucket bucket = new TokenBucket(10, 1);

        // 连续获取 10 个令牌
        for (int i = 0; i < 10; i++) {
            assertThat(bucket.tryAcquire()).isTrue();
        }

        // 第 11 个令牌应该获取失败
        assertThat(bucket.tryAcquire()).isFalse();
    }

    @Test
    @DisplayName("令牌桶限流 - 高并发压测")
    void testTokenBucket_HighConcurrency() throws InterruptedException {
        int capacity = 100;
        int threads = 50;
        int requestsPerThread = 10; // 总共 500 次请求
        
        TokenBucket bucket = new TokenBucket(capacity, 60); // 1分钟 100 次，确保不会因为时间太短而自动恢复

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (bucket.tryAcquire()) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 验证：成功的请求数绝对不能超过 capacity (100)
        assertThat(successCount.get()).isEqualTo(capacity);
        assertThat(failCount.get()).isEqualTo(threads * requestsPerThread - capacity);
        
        System.out.println("并发压测结果: 成功=" + successCount.get() + ", 失败=" + failCount.get());
    }

    @Test
    @DisplayName("令牌桶限流 - 时间窗口恢复验证")
    void testTokenBucket_Refill() throws InterruptedException {
        // 容量 2，窗口 1 秒 -> 速率为 2 tokens/sec
        TokenBucket bucket = new TokenBucket(2, 1);

        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(bucket.tryAcquire()).isFalse();

        // 等待 1.1 秒，应该恢复至少 2 个令牌
        Thread.sleep(1100);

        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(bucket.tryAcquire()).isTrue();
    }
}
