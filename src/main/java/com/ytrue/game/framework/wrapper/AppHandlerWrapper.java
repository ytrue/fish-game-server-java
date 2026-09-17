package com.ytrue.game.framework.wrapper;

import java.lang.reflect.Method;

/**
 * 应用处理方法包装器。
 *
 * <p>框架启动扫描 {@code @AppHandler} 注解时，把「控制器实例 + 校验方法 + 处理方法 + 附加标记值」
 * 打包成本记录，存入消息路由表；运行时客户端消息按消息码命中本包装器，并反射调用其中的处理方法。</p>
 *
 * @param bean        处理方法所在对象（控制器实例）
 * @param checkMethod 校验方法（可为 {@code null}，表示不额外校验）
 * @param taskMethod  处理方法（被 {@code @AppHandler} 标注的方法本身）
 * @param exp         附加标记值（对应 {@code @AppHandler.exp}）
 *
 * @since 1.0.0
 * @see com.ytrue.game.framework.annotation.AppHandler
 */
public record AppHandlerWrapper(
        Object bean,
        Method checkMethod,
        Method taskMethod,
        long exp
) {
}
