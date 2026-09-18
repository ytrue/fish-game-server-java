package com.ytrue.game.framework.engine.register;

import com.google.protobuf.Message;
import com.ytrue.game.framework.engine.annotation.*;
import com.ytrue.game.framework.engine.data.ServerUser;
import com.ytrue.game.framework.engine.utils.ThreadPoolFactory;
import com.ytrue.game.framework.engine.wrapper.AppHandlerWrapper;
import com.ytrue.game.framework.engine.wrapper.GmHandlerWrapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息接收器注册基类。
 *
 * <p>作为 {@link BeanPostProcessor}，在 Spring 容器初始化每个 bean 之前，扫描其类上的
 * {@link AppController}/{@link GmController}/{@link AppInit} 注解：把标注了
 * {@link AppHandler}/{@link GmHandler} 的方法按消息码/关键字注册到路由表，
 * 并异步执行 {@link AppInit} 指定的初始化方法。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Getter
public abstract class BaseHandlerRegister implements BeanPostProcessor {

    /**
     * 客户端消息处理方法表（消息码 → 处理器包装器）。
     */
    private final Map<Integer, AppHandlerWrapper> appHandlerWrapperMap = new ConcurrentHashMap<>();

    /**
     * 后台管理请求处理方法表（关键字 → 处理器包装器）。
     */
    private final Map<String, GmHandlerWrapper> gmHandlerWrapperMap = new ConcurrentHashMap<>();

    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        try {
            // 获取注册bean
            Class<?> beanClass = bean.getClass();
            // 注册 app
            registerAppHandlers(bean, beanClass);
            // 注册 gm
            registerGmHandlers(bean, beanClass);
            // 执行方法
            invokeInitMethod(bean, beanClass);
        } catch (Exception e) {
            log.error("执行类初始化出现异常:[{}]", e.getMessage(), e);
        }
        return bean;
    }

    /**
     * 注册 {@link AppController} 控制器类中被 {@link AppHandler} 标注的处理方法。
     *
     * @param bean      控制器实例
     * @param beanClass 控制器类
     */
    private void registerAppHandlers(Object bean, Class<?> beanClass) {
        // 判断类上是否有 @AppController 注解
        AppController appController = beanClass.getAnnotation(AppController.class);
        if (appController == null) {
            return;
        }

        // 获取所有方法
        Map<String, Method> declaredMethodMap = getDeclaredMethodMap(beanClass);
        // 类级校验方法（@AppController.checkMethod 指定的方法名，默认 "checker"）
        Method checkMethod = declaredMethodMap.get(appController.checkMethod());

        for (Method taskMethod : declaredMethodMap.values()) {
            AppHandler handler = taskMethod.getAnnotation(AppHandler.class);
            if (Objects.isNull(handler)) {
                continue;
            }

            // 方法级校验方法：@AppHandler.checker 非空时，覆盖类级校验方法
            Method innerCheckMethod = checkMethod;
            if (StringUtils.hasText(handler.checker())) {
                try {
                    // 根据方法名 + 参数类型，从指定的 Class 中找到对应的方法。
                    innerCheckMethod = beanClass.getDeclaredMethod(handler.checker(), Method.class, Message.class, ServerUser.class, Long.class);
                } catch (NoSuchMethodException e) {
                    log.error("搜索检查方法发生异常:[{}]", e.getMessage(), e);
                }
            }

            // 判断是否注册过了，给个提示就好了
            if (appHandlerWrapperMap.containsKey(handler.msgCode())) {
                log.warn("App Handler already registered, overwrite: key={}, controller={}, method={}", handler.msgCode(), beanClass.getName(), taskMethod.getName());
            }

            appHandlerWrapperMap.put(handler.msgCode(), new AppHandlerWrapper(bean, innerCheckMethod, taskMethod, handler.exp()));
        }
    }

    /**
     * 注册 {@link GmController} 控制器类中被 {@link GmHandler} 标注的处理方法。
     *
     * @param bean      控制器实例
     * @param beanClass 控制器类
     */
    private void registerGmHandlers(Object bean, Class<?> beanClass) {
        GmController gmController = beanClass.getAnnotation(GmController.class);
        if (gmController == null) {
            return;
        }
        // 获取类所有方法
        Map<String, Method> declaredMethodMap = getDeclaredMethodMap(beanClass);
        // 先获取 checker 方法
        Method checkMethod = declaredMethodMap.get(gmController.checkMethod());

        // 循环处理
        for (Method taskMethod : declaredMethodMap.values()) {
            // 获取所有的 方法
            GmHandler handler = taskMethod.getAnnotation(GmHandler.class);
            // 为空不处理
            if (Objects.isNull(handler)) {
                continue;
            }

            // 关键字统一补前导斜杠，保证注册与路由查找时一致
            String key = handler.key();
            // 判断 是否是 /xxx, 没有就加上去
            if (!key.startsWith("/")) {
                key = "/" + key;
            }

            // 判断是否注册过了，给个提示就好了
            if (gmHandlerWrapperMap.containsKey(key)) {
                log.warn("GM Handler already registered, overwrite: key={}, controller={}, method={}", key, beanClass.getName(), taskMethod.getName());
            }

            gmHandlerWrapperMap.put(key, new GmHandlerWrapper(bean, checkMethod, taskMethod));
        }
    }

    /**
     * 执行 {@link AppInit} 控制器类的初始化方法（异步）。
     *
     * @param bean      控制器实例
     * @param beanClass 控制器类
     */
    private void invokeInitMethod(Object bean, Class<?> beanClass) {
        AppInit appInit = beanClass.getAnnotation(AppInit.class);
        if (Objects.isNull(appInit)) {
            return;
        }

        Method initMethod;
        try {
            initMethod = beanClass.getDeclaredMethod(appInit.initMethod());
        } catch (NoSuchMethodException e) {
            log.error("查找初始化方法发生异常:[{}]", e.getMessage(), e);
            return;
        }

        // 异步执行初始化任务，避免阻塞 Spring 容器启动
        ThreadPoolFactory.TASK_SERVICE_POOL.submit(() -> {
            try {
                initMethod.invoke(bean);
            } catch (Exception e) {
                log.error("执行初始化方法出现异常:[{}]", e.getMessage(), e);
            }
        });
    }

    /**
     * 获取类及其所有父类声明的方法（方法名 → 方法）。
     *
     * @param beanClass 目标类
     * @return 方法名到方法的映射
     */
    private Map<String, Method> getDeclaredMethodMap(Class<?> beanClass) {
        // 返回结果
        Map<String, Method> declaredMethodMap = new HashMap<>();
        // 循环处理
        for (Class<?> clazz = beanClass; clazz != Object.class; clazz = clazz.getSuperclass()) {
            // 填充所有方法
            for (Method method : clazz.getDeclaredMethods()) {
                declaredMethodMap.put(method.getName(), method);
            }
        }
        return declaredMethodMap;
    }
}
