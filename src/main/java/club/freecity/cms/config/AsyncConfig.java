package club.freecity.cms.config;

import club.freecity.cms.common.NamedThreadFactory;
import club.freecity.cms.common.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.*;

/**
 * 异步任务配置
 */
@Configuration
public class AsyncConfig {

    /**
     * 安全审计日志专用线程池 (Java 原生实现，支持上下文传递)
     */
    @Bean("auditLogExecutor")
    public Executor auditLogExecutor() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = corePoolSize * 2;
        long keepAliveTime = 60L;

        return new ContextAwareThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                new NamedThreadFactory("audit-log"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 支持上下文传递的原生线程池实现
     */
    static class ContextAwareThreadPoolExecutor extends ThreadPoolExecutor {
        public ContextAwareThreadPoolExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit unit,
                                              BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                              RejectedExecutionHandler handler) {
            super(corePoolSize, maxPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        @Override
        public void execute(Runnable command) {
            // 在提交任务的主线程中捕获上下文
            Long tenantId = TenantContext.getCurrentTenantId();
            String tenantCode = TenantContext.getCurrentTenantCode();
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            super.execute(() -> {
                try {
                    // 在异步执行的子线程中还原上下文
                    if (tenantId != null) {
                        TenantContext.setCurrentTenant(tenantId, tenantCode);
                    }
                    if (attributes != null) {
                        RequestContextHolder.setRequestAttributes(attributes, true); // true 表示在子线程中也可用
                    }
                    if (authentication != null) {
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                    command.run();
                } finally {
                    // 执行完毕后清理上下文，防止线程污染
                    TenantContext.clear();
                    RequestContextHolder.resetRequestAttributes();
                    SecurityContextHolder.clearContext();
                }
            });
        }
    }
}
