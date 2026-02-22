package club.freecity.cms.enums;

import lombok.Getter;

/**
 * 租户状态枚举
 */
@Getter
public enum TenantStatus {
    ACTIVE("ACTIVE", "正常"),
    DISABLED("DISABLED", "禁用"),
    PENDING("PENDING", "待审核");

    private final String value;
    private final String description;

    TenantStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static TenantStatus fromValue(String value) {
        for (TenantStatus status : TenantStatus.values()) {
            if (status.getValue().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }
}
