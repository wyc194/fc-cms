package club.freecity.cms.enums;

import club.freecity.cms.common.SecurityConstants;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRole {
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员"),
    TENANT_ADMIN("TENANT_ADMIN", "租户管理员"),
    EDITOR("EDITOR", "编辑"),
    VIEWER("VIEWER", "访客");

    private final String value;
    private final String description;

    UserRole(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 获取带有 ROLE_ 前缀的角色名称，用于 Spring Security
     */
    public String getRoleName() {
        return SecurityConstants.ROLE_PREFIX + value;
    }

    public static UserRole fromValue(String value) {
        for (UserRole role : UserRole.values()) {
            if (role.getValue().equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role value: " + value);
    }
}
