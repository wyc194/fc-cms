package club.freecity.cms.aspect;

import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.common.TenantContext;
import club.freecity.cms.dto.AuditContextDto;
import club.freecity.cms.security.CustomUserDetails;
import club.freecity.cms.service.SecurityAuditService;
import club.freecity.cms.util.IpUtils;
import club.freecity.cms.util.UserAgentUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * 安全审计日志切面
 * 通过 Before, AfterReturning, AfterThrowing 拆分拦截逻辑，逻辑清晰且职责分明
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAuditAspect {

    private final SecurityAuditService securityAuditService;
    private final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<AuditContextDto> contextThreadLocal = new ThreadLocal<>();

    @Pointcut("@annotation(securityAudit)")
    public void auditPointcut(SecurityAudit securityAudit) {}

    @Before(value = "auditPointcut(securityAudit)", argNames = "securityAudit")
    public void doBefore(SecurityAudit securityAudit) {
        startTimeThreadLocal.set(System.currentTimeMillis());
        // 在主线程捕获请求环境信息，防止异步处理时 Request 对象被回收
        contextThreadLocal.set(captureAuditContext());
    }

    @AfterReturning(value = "auditPointcut(securityAudit)", returning = "result", argNames = "joinPoint,securityAudit,result")
    public void doAfterReturning(JoinPoint joinPoint, SecurityAudit securityAudit, Object result) {
        long executionTime = calculateExecutionTime();
        AuditContextDto context = contextThreadLocal.get();
        contextThreadLocal.remove();
        securityAuditService.log(joinPoint, securityAudit, result, null, executionTime, context);
    }

    @AfterThrowing(value = "auditPointcut(securityAudit)", throwing = "throwable", argNames = "joinPoint,securityAudit,throwable")
    public void doAfterThrowing(JoinPoint joinPoint, SecurityAudit securityAudit, Throwable throwable) {
        long executionTime = calculateExecutionTime();
        AuditContextDto context = contextThreadLocal.get();
        contextThreadLocal.remove();
        securityAuditService.log(joinPoint, securityAudit, null, throwable, executionTime, context);
    }

    private AuditContextDto captureAuditContext() {
        AuditContextDto.AuditContextDtoBuilder builder = AuditContextDto.builder();
        
        // 1. 租户信息
        builder.tenantId(TenantContext.getCurrentTenantId());
        
        // 2. 用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            builder.userId(userDetails.getUserId());
            builder.username(userDetails.getUsername());
        }
        
        // 3. 请求环境信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            builder.ip(IpUtils.getClientIp(request));
            
            Map<String, String> uaInfo = UserAgentUtils.parse(request);
            builder.browser(uaInfo.get("browser"));
            builder.os(uaInfo.get("os"));
            builder.device(uaInfo.get("device"));
        }
        
        return builder.build();
    }

    private long calculateExecutionTime() {
        Long startTime = startTimeThreadLocal.get();
        long executionTime = startTime != null ? System.currentTimeMillis() - startTime : 0;
        startTimeThreadLocal.remove();
        return executionTime;
    }
}
