package club.freecity.cms.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

import club.freecity.cms.enums.LimitType;

/**
 * 接口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 限流 key 前缀
     */
    String key() default "rate_limit:";

    /**
     * 限流时间窗口，配合 timeUnit 使用
     */
    int window() default 60;

    /**
     * 时间单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 在时间窗口内的限制次数，默认 10 次
     */
    int count() default 10;

    /**
     * 限流类型，默认基于 IP
     */
    LimitType limitType() default LimitType.IP;
}
