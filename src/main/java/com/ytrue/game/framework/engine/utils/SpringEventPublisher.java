package com.ytrue.game.framework.engine.utils;

import org.springframework.context.ApplicationContext;

/**
 * Spring 事件发布工具类。
 *
 * <p>把 Spring 的事件发布包装成静态方法，供无法直接依赖注入的静态上下文
 * （如各类静态容器、工具类、反射创建的对象）发布事件。</p>
 *
 * <p>发布走 {@link SpringUtils#getApplicationContext()}——{@link ApplicationContext} 自身即实现了
 * {@code ApplicationEventPublisher}。注意不能改用
 * {@code SpringUtils.getBean(ApplicationEventPublisher.class)}：Spring 只把它注册为
 * 「可解析依赖」（供 {@code @Autowired} 解析），并未注册成 bean，用 {@code getBean} 会抛
 * {@code NoSuchBeanDefinitionException}。</p>
 *
 * <p>工具类自身不缓存任何静态状态，避免同一个对象被多处持有。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * SpringEventPublisher.publish(new NetExitEvent(user));
 * }</pre>
 *
 * @since 1.0.0
 */
public final class SpringEventPublisher {

    /**
     * 私有构造器，禁止实例化（纯工具类）。
     */
    private SpringEventPublisher() {
    }

    /**
     * 发布一个 Spring 事件。
     *
     * <p>参数类型与 Spring 原生接口保持一致（{@link Object}）：除了
     * {@link org.springframework.context.ApplicationEvent} 的子类，Spring 也允许直接发布任意
     * POJO 事件，本方法不做额外限制。</p>
     *
     * <p>该调用是同步的——所有监听器会在当前线程依次执行完毕后才返回。</p>
     *
     * @param event 待发布的事件对象
     * @throws IllegalStateException Spring 上下文尚未就绪时抛出
     */
    public static void publish(Object event) {
        SpringUtils.getApplicationContext().publishEvent(event);
    }

}
