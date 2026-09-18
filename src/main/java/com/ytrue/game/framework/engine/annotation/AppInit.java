package com.ytrue.game.framework.engine.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Controller;

/**
 * 应用初始化注解。
 *
 * <p>标注在初始化类上，用于声明该类需要在服务启动时执行初始化逻辑。
 * 框架启动时扫描所有标注本注解的类，并反射调用其 {@link #initMethod() 指定的初始化方法}。</p>
 *
 * <p>本注解以 {@code @Controller} 作为元注解，因此被标注的类会被 Spring 容器托管，
 * 可以在初始化方法中注入并使用所需的依赖。</p>
 *
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Controller
public @interface AppInit {

    /**
     * 初始化方法名。
     *
     * @return 需要执行的初始化方法名，默认为 {@code "init"}
     */
    String initMethod() default "init";

}
