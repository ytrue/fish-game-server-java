package com.ytrue.game.framework.network.netty.http;

import com.google.protobuf.Message;
import com.ytrue.game.framework.engine.config.ServerConfig;
import com.ytrue.game.framework.engine.data.NetworkMsgType;
import com.ytrue.game.framework.network.netty.BaseNetwork;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * HTTP 网络实现（后台 GM 接口）。
 *
 * <p>与 TCP 那条「客户端长连接」不同，HTTP 是短连接、一问一答：
 * 服务端不需要记住谁连着，也无法主动向客户端推送消息。因此本类只实现
 * 「监听端口 + 装配流水线」，连接表的三个钩子返回空实现。</p>
 *
 * <p>这三个空实现留了日志，按「误用会造成什么后果」分级：</p>
 * <ul>
 *     <li>{@code sendMessageToClient} —— 消息会被静默丢弃，调用方却以为发成功了，
 *         故用 {@code warn}，保证日志里留有痕迹；</li>
 *     <li>{@code closeClientConnect} / {@code removeClientChannel} —— 空操作无副作用，
 *         且正常流程下根本不会走到，故用 {@code debug}，平时不产生噪声。</li>
 * </ul>
 *
 * <p>监听参数取自 {@code application.yml} 的 {@code game.network.http} 配置。</p>
 *
 * <p>注意：本类<b>不加</b> {@code @Primary}——存在多个 {@code INetwork} 实现时，
 * 默认注入应解析到 TCP（客户端主通道）；需要 HTTP 时用
 * {@code @Qualifier("http")} 显式指定。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@Qualifier("http")
@RequiredArgsConstructor
public class HttpNetwork extends BaseNetwork {

    /**
     * HTTP 请求处理器。
     */
    private final HttpServerHandler serverHandler;

    /**
     * 服务器配置。
     */
    private final ServerConfig serverConfig;

    @Override
    public void open() {
        // 从配置读取监听参数（对应 yml 中 game.network.http 下的 port / boss-size / worker-size）
        ServerConfig.Http http = serverConfig.getNetwork().getHttp();
        int port = http.getPort();
        int bossSize = http.getBossSize();
        int workerSize = http.getWorkerSize();

        if (open(port, bossSize, workerSize)) {
            log.info("Http服务启动成功~ port:[{}]", port);
        } else {
            log.error("Http服务启动失败~ port:[{}]", port);
        }
    }

    @Override
    protected ChannelHandler getChannelHandler() {
        return new HttpChannelInitializer(serverHandler);
    }

    @Override
    protected ChannelHandlerContext getClientChannel(Object connect) {
        // HTTP 不保存连接，无连接表可查。
        // 这里不记日志：本方法会被 closeClientConnect 等通用流程调用，
        // 返回 null 是 HTTP 的正常语义，记日志只会制造噪声
        return null;
    }

    @Override
    public void sendMessageToClient(int msgCode, Message msg, Object connect, NetworkMsgType msgType) {
        // HTTP 是请求-响应模型，服务端拿不到「客户端连接」，无法主动推送。
        //
        // 这条用 warn 而非 debug：与另外两个空操作不同，这里**消息会被静默丢弃**——
        // 调用方以为发出去了，实际对方永远收不到。必须留下可见的痕迹，否则这种
        // 「消息石沉大海」的问题极难排查。
        // 后台需要实时通知时应改用轮询，或另走 WebSocket 长连接。
        log.warn("netty http - HTTP 不支持服务端主动推送，消息[{}]已被丢弃，连接:[{}]",
                Integer.toHexString(msgCode), connect);
    }

    @Override
    public void closeClientConnect(Object connect) {
        // 同 removeClientChannel：正常路径下不会走到（getClientChannel 恒为 null，
        // 通用流程会提前返回），空操作无副作用，用 debug
        log.debug("netty http - 收到关闭连接的请求，但 HTTP 不维护连接表，忽略:[{}]", connect);
    }

    @Override
    protected void removeClientChannel(Object connect) {
        // 走到这里说明上层绕过了 getClientChannel 的判空直接调用——正常情况下不会发生。
        // 空操作无副作用，所以用 debug：平时不打印，出问题时开 debug 才能看到线索
        log.debug("netty http - 收到移除连接的请求，但 HTTP 不维护连接表，忽略:[{}]", connect);
    }
}
