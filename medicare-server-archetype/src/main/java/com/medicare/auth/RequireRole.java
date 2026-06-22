package com.medicare.auth;

import java.lang.annotation.*;

/**
 * 角色校验注解 — 标注在 Controller 方法上，声明所需角色
 * 示例：@RequireRole({"admin", "doctor"})
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    String[] value() default {};
}
