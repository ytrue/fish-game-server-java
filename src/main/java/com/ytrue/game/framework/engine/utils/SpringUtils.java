package com.ytrue.game.framework.engine.utils;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 容器工具类。
 *
 * <p>持有 {@link ApplicationContext} 引用，为无法直接依赖注入的静态上下文（如静态容器、反射
 * 创建的对象等）提供按类型 / 名称获取 bean 的能力。</p>
 *
 * @since 1.0.0
 */
@Component
public class SpringUtils implements ApplicationContextAware {

    /**
     * Spring 应用上下文。
     */
    private static ApplicationContext applicationContext;

    /**
     * 由 Spring 容器回调注入应用上下文。
     *
     * @param applicationContext 应用上下文
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        SpringUtils.applicationContext = applicationContext;
    }


    /**
     * 获取 Spring 应用上下文。
     *
     * <p>用于访问那些「不是 bean、但上下文本身就能提供」的能力，最典型的是事件发布：
     * {@link ApplicationContext} 自身即实现了
     * {@link org.springframework.context.ApplicationEventPublisher}。
     * 这类能力 Spring 只注册为「可解析依赖」（供 {@code @Autowired} 解析），
     * 用 {@link #getBean(Class)} 是拿不到的。</p>
     *
     * @return 应用上下文
     * @throws IllegalStateException 上下文尚未就绪时抛出
     */
    public static ApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            // 静态上下文的经典陷阱：在容器就绪前被调用。给出明确提示，避免一个裸 NPE 难以定位
            throw new IllegalStateException("Spring 应用上下文尚未初始化，无法在容器启动完成前使用 SpringUtils");
        }
        return applicationContext;
    }

    /**
     * 按类型获取 bean。
     *
     * @param requiredType bean 类型
     * @param <T>          bean 类型泛型
     * @return bean 实例
     */
    public static <T> T getBean(Class<T> requiredType) {
        return getApplicationContext().getBean(requiredType);
    }

    /**
     * 按名称获取 bean。
     *
     * @param name bean 名称
     * @return bean 实例（需调用方自行强转）
     */
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    /**
     * 按名称与类型获取 bean。
     *
     * @param name         bean 名称
     * @param requiredType bean 类型
     * @param <T>          bean 类型泛型
     * @return bean 实例
     */
    public static <T> T getBean(String name, Class<T> requiredType) {
        return getApplicationContext().getBean(name, requiredType);
    }

}
