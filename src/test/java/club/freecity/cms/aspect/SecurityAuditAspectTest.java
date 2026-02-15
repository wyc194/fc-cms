package club.freecity.cms.aspect;

import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.dto.AuditContextDto;
import club.freecity.cms.service.SecurityAuditService;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAuditAspectTest {

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private SecurityAudit securityAudit;

    @InjectMocks
    private SecurityAuditAspect securityAuditAspect;

    @BeforeEach
    void setUp() {
        // Aspect 本身只是透传 securityAudit 对象，不直接调用其方法
        // 所以这里不需要 stub securityAudit 的方法，除非是在测试 service 层
    }

    @Test
    @DisplayName("审计切面 - 正常流程验证")
    void testAuditAspect_Success() {
        // 1. Before
        securityAuditAspect.doBefore(securityAudit);

        // 2. AfterReturning
        Object result = "success-result";
        securityAuditAspect.doAfterReturning(joinPoint, securityAudit, result);

        // 验证 service.log 被调用，且 result 正确，throwable 为 null
        verify(securityAuditService, times(1)).log(
                eq(joinPoint),
                eq(securityAudit),
                eq(result),
                isNull(),
                anyLong(),
                any(AuditContextDto.class)
        );
    }

    @Test
    @DisplayName("审计切面 - 异常流程验证")
    void testAuditAspect_Exception() {
        // 1. Before
        securityAuditAspect.doBefore(securityAudit);

        // 2. AfterThrowing
        RuntimeException exception = new RuntimeException("test error");
        securityAuditAspect.doAfterThrowing(joinPoint, securityAudit, exception);

        // 验证 service.log 被调用，且 result 为 null，throwable 正确
        verify(securityAuditService, times(1)).log(
                eq(joinPoint),
                eq(securityAudit),
                isNull(),
                eq(exception),
                anyLong(),
                any(AuditContextDto.class)
        );
    }
}
