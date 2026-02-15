package club.freecity.cms.enums;

/**
 * 限流类型
 */
public enum LimitType {
    /**
     * 默认策略：基于 IP 限流
     */
    IP,
    
    /**
     * 基于用户 ID 限流
     */
    USER,
    
    /**
     * 全局限流：针对该接口的所有请求统一限流，不区分 IP 或用户
     */
    GLOBAL
}
