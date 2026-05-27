package com.shanyangcode.infinitechat.messageingservice.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解：用于防止重复提交
 */
@Target(ElementType.METHOD) // 注解应用于方法上
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时可用
public @interface PreventDuplicateSubmit {
    /**
     * 超时时间（单位：毫秒），默认 5 秒
     */
    long timeout() default 5000;
}
