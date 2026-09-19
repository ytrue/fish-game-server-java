package com.ytrue.game.framework.network.init;

import com.ytrue.game.framework.network.INetwork;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网络服务启动器。
 *
 * <p>容器完全就绪后，把所有的网络监听（TCP / WebSocket / HTTP GM）逐个启动；
 * 应用关闭时再逐个关停。</p>
 *
 * <p><b>为什么用 {@link ApplicationRunner} 而不是 {@code @AppInit}</b>：</p>
 * <p>{@code @AppInit} 是由 {@code AppHandlerRegister}（一个 BeanPostProcessor）在
 * <b>每个 bean 初始化的过程中</b>触发的，此时容器尚未 refresh 完成，且 init 方法被丢到
 * 线程池异步执行——真正跑起来的时机是不确定的。而网络一旦开始监听，客户端可能立刻连上又断开，
 * 触发 {@code channelInactive} → 发布退出事件 → 走 {@code SpringUtils} 取 bean。
 * 若此时 {@code SpringUtils} 尚未初始化，就会抛异常。</p>
 * <p>{@link ApplicationRunner} 在容器 refresh 完成之后才执行，不存在这个竞态。</p>
 *
 * <p><b>关于 {@code List<INetwork>} 注入</b>：Spring 会把容器里所有 {@code INetwork} 实现
 * 一次性注入进来，因此新增一种传输（比如将来再加个 UDP）只要写成 {@code @Component}，
 * 本类无需改动。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NetworkInit implements ApplicationRunner {

    /**
     * 所有网络实现（TCP / WebSocket / HTTP）。
     *
     * <p>若某个实现暂时不需要启动，把它从容器里去掉即可（例如去掉 {@code @Component}），
     * 本类会自动少启一个。</p>
     */
    private final List<INetwork> networks;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        log.info("开始启动网络服务，共 {} 个监听", networks.size());
        for (INetwork network : networks) {
            // 逐个启动。bind 端口本身很快（毫秒级），不必像旧实现那样每个网络单开一个线程
            network.open();
        }
        // 只报总数、不宣称「全部成功」：INetwork.open() 不返回结果，
        // 每个网络到底起没起来看上面对应的那行日志
        log.info("网络服务启动流程结束（共 {} 个监听）", networks.size());
    }

    /**
     * 应用关闭时逐个关停网络监听。
     *
     * <p>不做这件事的话，进程退出前端口会一直被占用；配合 Spring Boot 的优雅停机，
     * 客户端也能收到连接关闭而不是直接断流。</p>
     *
     * <p>注意耗时：{@code BaseNetwork.close()} 内部调用了
     * {@code shutdownGracefully()}，Netty 每个线程池有默认 2 秒静默期，
     * 因此单个网络约需 4 秒；多个网络串行关停时总耗时是叠加的。</p>
     */
    @PreDestroy
    public void shutdown() {
        log.info("开始关停网络服务");
        for (INetwork network : networks) {
            try {
                network.close();
            } catch (Exception e) {
                // 关停阶段不因单个网络的异常中断，保证其余网络也能走到
                log.error("关停网络服务出错:[{}]", e.getMessage(), e);
            }
        }
        log.info("网络服务已关停");
    }

}
