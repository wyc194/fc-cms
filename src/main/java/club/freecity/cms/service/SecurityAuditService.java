package club.freecity.cms.service;

import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.dto.AuditContextDto;
import org.aspectj.lang.JoinPoint;

/**
 * 安全审计日志服务接口
 */
public interface SecurityAuditService {

    /**
     * 审计状态：成功
     */
    String STATUS_SUCCESS = "SUCCESS";

    /**
     * 审计状态：失败
     */
    String STATUS_FAILURE = "FAILURE";

    /**
     * 记录审计日志
     */
    void log(JoinPoint joinPoint, SecurityAudit securityAudit, Object result, Throwable throwable, long executionTime, AuditContextDto context);
}
