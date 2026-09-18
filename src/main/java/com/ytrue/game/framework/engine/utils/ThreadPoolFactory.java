package com.ytrue.game.framework.engine.utils;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池工厂。
 *
 * <p>统一创建游戏服务端使用的线程池，并提供两个全局共享的定时线程池。</p>
 *
 * <p>与 {@link java.util.concurrent.Executors} 工厂方法不同，本类通过自定义的
 * {@link ThreadFactory} 显式创建线程，为每个线程赋予有意义的名称（便于排查问题）
 * 并将其设置为守护线程（daemon），避免后台线程阻塞 JVM 正常退出。</p>
 *
 * @since 1.0.0
 */
public final class ThreadPoolFactory {

    /**
     * 任务线程池：用于房间帧循环等周期性任务的调度。
     */
    public static final ScheduledExecutorService TASK_SERVICE_POOL = createThreadPool("task-pool", 50);

    /**
     * 定时线程池：用于各类定时器任务。
     */
    public static final ScheduledExecutorService TIMER_SERVICE_POOL = createThreadPool("timer-pool", 50);

    /**
     * 私有构造器，禁止实例化（纯工厂类）。
     */
    private ThreadPoolFactory() {
    }

    /**
     * 创建指定名称与线程数的定时线程池。
     *
     * @param name       线程名前缀，最终线程名为 {@code name-N}（N 为序号）
     * @param threadSize 核心线程数
     * @return 配置好的定时线程池
     */
    public static ScheduledExecutorService createThreadPool(String name, int threadSize) {
        return new ScheduledThreadPoolExecutor(threadSize, new NamedThreadFactory(name));
    }

    /**
     * 创建指定线程数的定时线程池（使用默认线程名前缀 {@code "task-pool"}）。
     *
     * @param threadSize 核心线程数
     * @return 配置好的定时线程池
     */
    public static ScheduledExecutorService createThreadPool(int threadSize) {
        return createThreadPool("task-pool", threadSize);
    }

    /**
     * 创建指定名称的单线程定时执行器。
     *
     * @param name 线程名前缀
     * @return 单线程定时执行器
     */
    public static ScheduledExecutorService createSingleThread(String name) {
        return new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(name));
    }

    /**
     * 创建单线程定时执行器（使用默认线程名前缀 {@code "task-pool"}）。
     *
     * @return 单线程定时执行器
     */
    public static ScheduledExecutorService createSingleThread() {
        return createSingleThread("task-pool");
    }

    /**
     * 自定义线程工厂：为线程赋予「前缀-序号」形式的名称，并设置为守护线程。
     */
    private static final class NamedThreadFactory implements ThreadFactory {

        /** 线程名前缀。 */
        private final String prefix;

        /** 线程序号（从 1 开始自增）。 */
        private final AtomicInteger counter = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

}
