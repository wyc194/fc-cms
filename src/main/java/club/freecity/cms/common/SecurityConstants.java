package club.freecity.cms.common;

/**
 * 安全相关常量定义
 */
public class SecurityConstants {
    
    // JWT 相关
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    
    // HTTP 头相关
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    
    // JWT Claims Key
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_TENANT_CODE = "tenantCode";
    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_PASSWORD_UPDATE_TIME = "pwdUpdateTime";
    
    // 角色前缀
    public static final String ROLE_PREFIX = "ROLE_";
}
