package club.freecity.cms.tenant;

import club.freecity.cms.common.RoleConstants;
import club.freecity.cms.common.TenantContext;
import club.freecity.cms.security.CustomUserDetails;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TenantAspect {

    private final EntityManager entityManager;
    
    // 将忽略过滤器的状态内聚在切面内部，不对外暴露
    private static final ThreadLocal<Boolean> IGNORE_TENANT_FILTER = ThreadLocal.withInitial(() -> false);

    @Pointcut("@annotation(club.freecity.cms.common.GlobalOperation) || @within(club.freecity.cms.common.GlobalOperation)")
    public void globalOperationPointcut() {}

    @Around("globalOperationPointcut()")
    public Object aroundGlobalOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean previousState = IGNORE_TENANT_FILTER.get();
        try {
            IGNORE_TENANT_FILTER.set(true);
            return joinPoint.proceed();
        } finally {
            IGNORE_TENANT_FILTER.set(previousState);
        }
    }

    @Before("execution(* club.freecity.cms.repository.*.*(..))")
    public void beforeRepositoryCall() {
        // 1. 如果处于 @GlobalOperation 标记的上下文中，禁用过滤器
        if (IGNORE_TENANT_FILTER.get()) {
            disableFilter();
            return;
        }

        // 2. 如果是超级管理员，禁用过滤器
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            if (RoleConstants.SUPER_ADMIN.equals(userDetails.getRole())) {
                disableFilter();
                return;
            }
        }

        // 3. 正常租户场景，启用过滤器
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId != null) {
            enableFilter(tenantId);
        } else {
            // 如果既不是全局操作，又没有租户上下文，默认禁用（防止漏掉 NULL 租户的数据，或者根据业务需求决定）
            disableFilter();
        }
    }

    private void disableFilter() {
        Session session = entityManager.unwrap(Session.class);
        session.disableFilter("tenantFilter");
    }

    private void enableFilter(Long tenantId) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
    }
}
