package club.freecity.cms.service.impl;

import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.dto.AuditContextDto;
import club.freecity.cms.dto.LoginDto;
import club.freecity.cms.entity.SecurityLog;
import club.freecity.cms.enums.AuditAction;
import club.freecity.cms.repository.SecurityLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAuditServiceImplTest {

    @Mock
    private SecurityLogRepository securityLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private SecurityAudit securityAudit;

    @InjectMocks
    private SecurityAuditServiceImpl securityAuditService;

    @BeforeEach
    void setUp() {
        // 使用 lenient() 允许在某些测试中不使用这些打桩，或者被覆盖
        lenient().when(securityAudit.action()).thenReturn(AuditAction.AUTH_LOGIN);
        lenient().when(securityAudit.message()).thenReturn("'Login Test'");
        lenient().when(securityAudit.logArgs()).thenReturn(true);
        lenient().when(securityAudit.maxFieldLength()).thenReturn(500);
        lenient().when(securityAudit.maxCollectionSize()).thenReturn(10);
        lenient().when(securityAudit.logResponse()).thenReturn(false);
    }

    @Test
    @DisplayName("审计日志处理 - 验证超长字符串截断")
    void testProcessLogData_Truncation() {
        // 1. 准备超长字符串
        StringBuilder longStr = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            longStr.append("a");
        }
        String content = longStr.toString();
        
        when(joinPoint.getArgs()).thenReturn(new Object[]{content});
        when(securityAudit.maxFieldLength()).thenReturn(10); // 设置极短限制用于测试

        AuditContextDto context = AuditContextDto.builder()
                .tenantId(1L)
                .username("test")
                .ip("127.0.0.1")
                .build();

        // 2. 执行
        securityAuditService.log(joinPoint, securityAudit, null, null, 100, context);

        // 3. 验证
        ArgumentCaptor<SecurityLog> logCaptor = ArgumentCaptor.forClass(SecurityLog.class);
        verify(securityLogRepository).save(logCaptor.capture());
        
        String message = logCaptor.getValue().getMessage();
        assertTrue(message.contains("aaaaaaaaaa...(truncated"), "Long string should be truncated");
    }

    @Test
    @DisplayName("审计日志处理 - 验证集合数量限制")
    void testProcessLogData_CollectionLimit() {
        // 1. 准备大集合
        java.util.List<String> list = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            list.add("item" + i);
        }
        
        when(joinPoint.getArgs()).thenReturn(new Object[]{list});
        when(securityAudit.maxCollectionSize()).thenReturn(5); // 设置极小限制用于测试

        AuditContextDto context = AuditContextDto.builder()
                .tenantId(1L)
                .username("test")
                .ip("127.0.0.1")
                .build();

        // 2. 执行
        securityAuditService.log(joinPoint, securityAudit, null, null, 100, context);

        // 3. 验证
        ArgumentCaptor<SecurityLog> logCaptor = ArgumentCaptor.forClass(SecurityLog.class);
        verify(securityLogRepository).save(logCaptor.capture());
        
        String message = logCaptor.getValue().getMessage();
        assertTrue(message.contains("item0"), "First items should be present");
        assertTrue(message.contains("...(truncated, total 20 items)"), "Collection truncation info should be present");
    }

    @Test
    @DisplayName("审计日志脱敏 - 验证密码字段被遮掩")
    void testMaskSensitiveData_Password() {
        // Prepare LoginDto with password
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("testuser");
        loginDto.setPassword("secret123");
        
        when(joinPoint.getArgs()).thenReturn(new Object[]{loginDto});

        AuditContextDto context = AuditContextDto.builder()
                .tenantId(1L)
                .username("test")
                .ip("127.0.0.1")
                .build();

        // Execute saveLog (internal method called by log, but we test it via reflection or by making it public/protected if needed)
        // Since log() calls saveLog(), we can just call log()
        securityAuditService.log(joinPoint, securityAudit, null, null, 100, context);

        // Capture the saved log
        ArgumentCaptor<SecurityLog> logCaptor = ArgumentCaptor.forClass(SecurityLog.class);
        verify(securityLogRepository).save(logCaptor.capture());

        SecurityLog savedLog = logCaptor.getValue();
        String message = savedLog.getMessage();

        // Verify masking
        assertTrue(message.contains("\"password\":\"******\""), "Password should be masked in log");
        assertFalse(message.contains("secret123"), "Cleartext password should NOT be present in log");
        assertTrue(message.contains("\"username\":\"testuser\""), "Non-sensitive fields should remain clear");
    }
}
