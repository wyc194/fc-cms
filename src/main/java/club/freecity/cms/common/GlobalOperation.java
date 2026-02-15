package club.freecity.cms.common;

import java.lang.annotation.*;

/**
 * 标记该方法执行时应跳过租户过滤器（全局操作）
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlobalOperation {
}
