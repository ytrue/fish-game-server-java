package com.ytrue.game.framework.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Controller;

/**
 * 应用数据（静态配置）注解。
 *
 * <p>标注在静态配置数据类上，声明该类对应的配置文件（通常是 CSV）。
 * 框架启动时读取 {@link #fileUrl() 指定的文件}，按 {@link #charSet() 指定编码} 解析，
 * 并将内容注入到被标注的类中。</p>
 *
 * <p>本注解以 {@code @Controller} 作为元注解，被标注的类由 Spring 容器托管。</p>
 *
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Controller
public @interface AppData {

    /**
     * 配置文件路径。
     *
     * @return 配置文件的路径或 URL
     */
    String fileUrl();

    /**
     * 文件编码。
     *
     * @return 配置文件的字符编码，默认为 {@code "GBK"}
     */
    String charSet() default "GBK";

}
