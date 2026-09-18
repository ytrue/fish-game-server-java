package com.ytrue.game.framework.engine.config;

import com.ytrue.game.framework.engine.utils.ThreadPoolFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Spring 定时任务配置。
 *
 * <p>{@link EnableScheduling} 是开启定时任务的总开关——没有它，Spring 不会注册
 * {@code ScheduledAnnotationBeanPostProcessor}，全工程的 {@code @Scheduled} 都会被静默忽略。
 * 仅实现 {@link SchedulingConfigurer} 不起这个作用，它只负责定制调度器。</p>
 *
 * <p>实现 {@link SchedulingConfigurer}，将 {@code @Scheduled} 的调度器替换为框架自定义的
 * {@link ThreadPoolFactory#TIMER_SERVICE_POOL}，使所有定时任务运行在「命名 + 守护线程」
 * 的线程池上，而非 JDK 默认的单线程池。</p>
 *
 * @since 1.0.0
 */
@Configuration
@EnableScheduling
public class ScheduleConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(ThreadPoolFactory.TIMER_SERVICE_POOL);
    }

}
