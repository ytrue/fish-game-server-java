package com.ytrue.game.framework.engine.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Controller;

/**
 * 后台管理控制器注解（GM Controller）。
 *
 * <p>标注在后台管理接口类上，用于声明该类提供 GM（Game Master，运营后台）接口。
 * 框架启动时扫描所有标注本注解的类，并将其中标注了 {@link GmHandler} 的方法按
 * 关键字（{@code key}）注册到后台接口路由表。</p>
 *
 * <p>本注解以 {@code @Controller} 作为元注解，被标注的类由 Spring 容器托管。</p>
 *
 * @since 1.0.0
 * @see GmHandler
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Controller
public @interface GmController {

    /**
     * 校验方法名。
     *
     * @return 校验方法的方法名，默认为 {@code "checker"}
     */
    String checkMethod() default "checker";

}
