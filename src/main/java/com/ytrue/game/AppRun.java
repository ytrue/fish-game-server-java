package com.ytrue.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 服务端启动入口。
 *
 * <p>定时任务开关在 {@code ScheduleConfig} 上（{@code @EnableScheduling}），
 * 与调度器定制放在一起，本类只负责启动 Spring 容器。</p>
 *
 * @since 1.0.0
 */
@SpringBootApplication
public class AppRun {

    /**
     * 启动入口。
     *
     * <p>必须是 {@code public}：Spring Boot 打包后的启动器通过反射
     * {@code getMethod("main", String[].class)} 调用本方法，而 {@code getMethod} 只查找
     * public 成员。写成包级私有在 IDE 里能跑（Java 25 的直接启动已放宽可见性要求），
     * 但 {@code java -jar} 会抛 {@code NoSuchMethodException}。</p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AppRun.class, args);
    }

}
