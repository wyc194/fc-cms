package club.freecity.cms.annotation;

import club.freecity.cms.enums.AuditAction;

import java.lang.annotation.*;

/**
 * 安全审计日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SecurityAudit {
    /**
     * 动作名称
     */
    AuditAction action();

    /**
     * 详情消息模板，支持 SpEL 表达式
     */
    String message() default "";

    /**
     * 是否记录请求参数，默认关闭
     */
    boolean logArgs() default false;

    /**
     * 是否记录响应结果，默认关闭
     */
    boolean logResponse() default false;

    /**
     * 单个字段记录的最大长度，超出部分将被截断（仅对字符串生效）
     */
    int maxFieldLength() default 500;

    /**
     * 集合/数组记录的最大数量，超出部分将被截断
     */
    int maxCollectionSize() default 10;
}
