package club.freecity.cms.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * validateToken 语义回归测试：
 * 防止"未过期才有效"的布尔逻辑再被写反（曾导致产线全量 401）。
 */
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // Base64 解码后为 32 字节（256 位），满足 HS256 最低强度要求
        ReflectionTestUtils.setField(jwtUtils, "secret", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        jwtUtils.init();
    }

    @Test
    void freshTokenMustValidate() {
        String token = jwtUtils.generateAccessToken(1L, "admin", "TENANT_ADMIN", 1L, "admin", null);
        assertTrue(jwtUtils.validateToken(token, "admin"), "未过期的合法令牌必须通过校验");
    }

    @Test
    void mismatchedSubjectMustNotValidate() {
        String token = jwtUtils.generateAccessToken(1L, "admin", "TENANT_ADMIN", 1L, "admin", null);
        assertFalse(jwtUtils.validateToken(token, "someone-else"), "主体不匹配的令牌不得通过校验");
    }
}
