package club.freecity.cms.aspect;

import club.freecity.cms.annotation.RateLimit;
import club.freecity.cms.common.ResultCode;
import club.freecity.cms.exception.BusinessException;
import club.freecity.cms.support.ratelimit.RateLimiter;
import club.freecity.cms.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 限流切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiter rateLimiter;

    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint joinPoint, RateLimit rateLimit) {
        int window = rateLimit.window();
        int count = rateLimit.count();
        // 将时间窗口转换为秒
        int windowInSeconds = (int) rateLimit.timeUnit().toSeconds(window);

        String combineKey = getCombineKey(rateLimit, joinPoint);
        boolean allowed = rateLimiter.isAllowed(combineKey, count, windowInSeconds);
        
        if (!allowed) {
            log.warn("接口限流触发: key={}, window={}s, count={}", combineKey, windowInSeconds, count);
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
        }
    }

    /**
     * 生成限流 key
     */
    private String getCombineKey(RateLimit rateLimit, JoinPoint joinPoint) {
        StringBuilder sb = new StringBuilder(rateLimit.key());
        
        // 加上方法全路径，确保不同接口限流隔离
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        sb.append(method.getDeclaringClass().getName())
          .append(".")
          .append(method.getName())
          .append(":");

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        switch (rateLimit.limitType()) {
            case IP:
                if (request != null) {
                    sb.append(IpUtils.getClientIp(request));
                } else {
                    sb.append("unknown_ip");
                }
                break;
            case USER:
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    sb.append(authentication.getName());
                } else {
                    sb.append("anonymous");
                }
                break;
            case GLOBAL:
                sb.append("global");
                break;
        }
        return sb.toString();
    }
}
