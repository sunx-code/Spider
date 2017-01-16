package com.sunx.moudle.annotation;

import java.lang.annotation.*;

/**
 * 提供注解程序
 * 1 用于标记id
 * 2 用于标记服务是哪个
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface Service {
    /**
     * 默认的id标记
     */
    long id() default -1;

    /**
     * 默认的服务地址
     * @return
     */
    String service() default "";
}
