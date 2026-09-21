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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息接收器注册器。
 *
 * <p>作为 {@link BeanPostProcessor}，在 Spring 容器初始化每个 bean 之前，扫描其类上的
 * {@link AppController}/{@link GmController}/{@link AppInit} 注解：把标注了
 * {@link AppHandler}/{@link GmHandler} 的方法按消息码/关键字注册到路由表，
 * 并异步执行 {@link AppInit} 指定的初始化方法。</p>
 *
 * <p>本类是<b>具体类</b>，不能声明为 {@code abstract}：Spring 的组件扫描会跳过抽象类
 * （候选组件要求 {@code isConcrete()}），抽象 + {@code @Component} 的后果是<b>不生成 bean</b>——
 * 于是 BeanPostProcessor 不存在、路由表永远是空的、所有协议都收不到，
 * 而应用却会正常启动，属于极难排查的静默失效。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Getter
@Component
public class AppHandlerRegister implements BeanPostProcessor {

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

            // 消息码重复：后注册的会覆盖先注册的。这通常是两个控制器用了同一个消息码，
            // 或业务模块的控制器类名不同但注解值撞了——值得警告并把两边都打出来对照
            if (appHandlerWrapperMap.containsKey(handler.msgCode())) {
                AppHandlerWrapper old = appHandlerWrapperMap.get(handler.msgCode());
                log.warn("客户端消息码重复，新注册的将覆盖旧的: 消息码[{}] 已注册[{}#{}] 新注册[{}#{}]",
                        Integer.toHexString(handler.msgCode()),
                        old.bean().getClass().getSimpleName(), old.taskMethod().getName(),
                        beanClass.getSimpleName(), taskMethod.getName());
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

            // 后台路径重复：后注册的会覆盖先注册的。把两边都打出来便于对照谁和谁撞了
            if (gmHandlerWrapperMap.containsKey(key)) {
                GmHandlerWrapper old = gmHandlerWrapperMap.get(key);
                log.warn("后台路径重复，新注册的将覆盖旧的: 路径[{}] 已注册[{}#{}] 新注册[{}#{}]",
                        key,
                        old.bean().getClass().getSimpleName(), old.taskMethod().getName(),
                        beanClass.getSimpleName(), taskMethod.getName());
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
     * <p>之所以要往上找父类，是为了拿到<b>继承下来的 {@code checker}</b>——
     * 例如旧的 GM 控制器把 {@code checker} 定义在 {@code GmBaseController} 里、
     * 四个子类共用一份，子类自己不再声明，只能靠这条继承链取到。</p>
     *
     * <p><b>同名的取子类的</b>：循环从子类往父类走，先访问到的就是子类的同名方法，
     * 所以这里必须用 {@code putIfAbsent} 而不是 {@code put}——一旦让父类覆盖子类，
     * 子类的覆写会被<b>静默忽略</b>：注册时读的是父类那个 {@code Method} 上的注解，
     * 于是 {@code msgCode} / {@code checker} / {@code exp} 全按父类的来
     * （{@code Method.invoke} 本身是虚分派，执行的仍是子类实现，所以只有「路由与校验」出错，
     * 表现是消息码对不上或校验被跳过，而不是报错）。</p>
     *
     * @param beanClass 目标类
     * @return 方法名到方法的映射
     */
    private Map<String, Method> getDeclaredMethodMap(Class<?> beanClass) {
        // 返回结果
        Map<String, Method> declaredMethodMap = new HashMap<>();
        // 从子类往父类遍历：先放进来的就是子类的同名方法
        for (Class<?> clazz = beanClass; clazz != Object.class; clazz = clazz.getSuperclass()) {
            // 填充所有方法；putIfAbsent 保证后面的父类方法不覆盖子类的
            for (Method method : clazz.getDeclaredMethods()) {
                declaredMethodMap.putIfAbsent(method.getName(), method);
            }
        }
        return declaredMethodMap;
    }
}
