package com.ytrue.game.framework.engine.register;

import org.springframework.stereotype.Component;

/**
 * 框架消息注册器。
 *
 * <p>{@link BaseHandlerRegister} 的具体实现——它本身不写任何逻辑，只负责作为 Spring bean 存在，
 * 让容器启动时能触发基类的 {@code BeanPostProcessor} 扫描，把各控制器上的
 * {@code @AppHandler} / {@code @GmHandler} 注册进路由表。</p>
 *
 * <p>网络层直接从本类取路由表，不再需要额外的中间转发层。</p>
 *
 * @since 1.0.0
 */
@Component
public class AppHandlerRegister extends BaseHandlerRegister {

}
