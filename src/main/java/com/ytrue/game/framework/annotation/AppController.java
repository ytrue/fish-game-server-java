package com.ytrue.game.framework.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Controller;

/**
 * 应用控制器注解。
 *
 * <p>标注在客户端协议处理器类上，用于声明该类是一个「应用控制器」（Application Controller）。
 * 框架启动时会扫描所有标注了本注解的类，并把其中标注了 {@link AppHandler} 的方法，
 * 按消息码（{@code msgCode}）注册到协议路由表中，从而实现「消息码 → 处理方法」的自动分发。</p>
 *
 * <p>本注解以 {@code @Controller} 作为元注解，因此被标注的类同样会被 Spring 容器托管，
 * 支持依赖注入（如 {@code @Autowired}）。</p>
 *
 * @since 1.0.0
 * @see AppHandler
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Controller
public @interface AppController {

    /**
     * 校验方法名。
     *
     * <p>框架在调用具体的 {@link AppHandler} 处理方法之前，会先反射调用控制器类中该名称的方法，
     * 用于做参数合法性校验（例如判断玩家是否在线、是否处于房间内等）。默认方法名为 {@code "checker"}。</p>
     *
     * @return 校验方法的方法名
     */
    String checkMethod() default "checker";

}
