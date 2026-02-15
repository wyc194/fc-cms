package club.freecity.cms.common;

/**
 * 角色常量类，主要用于 Spring Security 注解
 */
public class RoleConstants {
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String TENANT_ADMIN = "TENANT_ADMIN";
    public static final String EDITOR = "EDITOR";
    public static final String VIEWER = "VIEWER";

    // 带前缀的角色常量
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    public static final String ROLE_EDITOR = "ROLE_EDITOR";
    public static final String ROLE_VIEWER = "ROLE_VIEWER";

    // 常用权限组合表达式
    public static final String HAS_ROLE_SUPER_ADMIN = "hasRole('" + SUPER_ADMIN + "')";
    public static final String HAS_ANY_ROLE_ADMIN = "hasAnyRole('" + SUPER_ADMIN + "', '" + TENANT_ADMIN + "')";
    public static final String HAS_ANY_ROLE_EDITOR = "hasAnyRole('" + SUPER_ADMIN + "', '" + TENANT_ADMIN + "', '" + EDITOR + "')";
}
