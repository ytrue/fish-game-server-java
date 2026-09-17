package com.ytrue.game.framework.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 后台处理方法注解（GM Handler）。
 *
 * <p>标注在 {@link GmController} 控制器类的方法上，声明该方法负责处理指定的后台管理请求。
 * 框架通过 {@link #key() 关键字} 把后台请求路由到对应方法。</p>
 *
 * @since 1.0.0
 * @see GmController
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GmHandler {

    /**
     * 处理方法关键字。
     *
     * <p>用于匹配后台请求路径，把请求路由到本处理方法。</p>
     *
     * @return 关键字，默认为空字符串
     */
    String key() default "";

}
