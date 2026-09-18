package com.ytrue.game.framework.engine.wrapper;

import com.ytrue.game.framework.engine.annotation.GmHandler;

import java.lang.reflect.Method;

/**
 * 后台处理方法包装器。
 *
 * <p>框架启动扫描 {@code @GmHandler} 注解时，把「控制器实例 + 校验方法 + 处理方法」打包成本记录，
 * 存入后台路由表；运行时后台请求按关键字命中本包装器，并反射调用其中的处理方法。</p>
 *
 * @param bean        处理方法所在对象（控制器实例）
 * @param checkMethod 校验方法（对应 {@code @GmController.checkMethod} 指定的方法名，可为 {@code null}）
 * @param taskMethod  处理方法（被 {@code @GmHandler} 标注的方法本身）
 *
 * @since 1.0.0
 * @see GmHandler
 */
public record GmHandlerWrapper(
        Object bean,
        Method checkMethod,
        Method taskMethod
) {
}
