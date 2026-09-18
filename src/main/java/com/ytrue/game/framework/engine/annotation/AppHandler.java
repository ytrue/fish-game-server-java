package com.ytrue.game.framework.engine.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 应用处理方法注解。
 *
 * <p>标注在 {@link AppController} 控制器类的方法上，声明该方法负责处理指定的客户端消息。
 * 框架启动时通过扫描此注解，把方法与其对应的 {@link #msgCode() 消息码} 绑定，
 * 运行时客户端消息按消息码路由到对应方法。</p>
 *
 * @since 1.0.0
 * @see AppController
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AppHandler {

    /**
     * 消息码。
     *
     * <p>客户端与服务端约定的协议号，用于把客户端消息路由到本处理方法。该值不可缺省。</p>
     *
     * @return 消息码
     */
    int msgCode();

    /**
     * 校验方法名。
     *
     * <p>若指定了非空的方法名，框架在调用本处理方法之前，会先调用控制器类中该名称的方法进行校验；
     * 校验不通过则不再调用本方法。留空表示不额外校验。</p>
     *
     * @return 校验方法名，默认为空字符串
     */
    String checker() default "";

    /**
     * 处理方法附加标记值。
     *
     * <p>供框架在路由处理时使用的附加参数，例如用于控制玩家是否需要处于房间内等执行条件。
     * 具体含义由各处理器自行约定。</p>
     *
     * @return 附加标记值，默认为 {@code 0}
     */
    long exp() default 0;

}
