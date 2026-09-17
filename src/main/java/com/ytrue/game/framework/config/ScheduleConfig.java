package com.ytrue.game.framework.config;

import com.ytrue.game.framework.utils.ThreadPoolFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Spring 定时任务配置。
 *
 * <p>实现 {@link SchedulingConfigurer}，将 Spring 的 {@code @Scheduled} 定时任务调度器
 * 替换为框架自定义的 {@link ThreadPoolFactory#TIMER_SERVICE_POOL}，使所有定时任务运行在
 * 「命名 + 守护线程」的线程池上，而非 JDK 默认的线程池。</p>
 *
 * @since 1.0.0
 */
@Configuration
public class ScheduleConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(ThreadPoolFactory.TIMER_SERVICE_POOL);
    }

}
