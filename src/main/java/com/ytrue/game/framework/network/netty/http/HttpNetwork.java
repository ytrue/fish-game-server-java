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
 * 「监听端口 + 装配流水线」，连接表的三个钩子都返回空实现并记录错误——
 * 上层若误用主动下发能力，日志里会立刻暴露。</p>
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
            log.info("Http     网络服务启动成功~ port:[{}]", port);
        } else {
            log.error("Http     网络服务启动失败~ port:[{}]", port);
        }
    }

    @Override
    protected ChannelHandler getChannelHandler() {
        return new HttpChannelInitializer(serverHandler);
    }

    @Override
    protected ChannelHandlerContext getClientChannel(Object connect) {
        // HTTP 不保存连接，无连接表可查
        return null;
    }

    @Override
    protected void removeClientChannel(Object connect) {
        log.error("netty http - Http服务器没有保存客户端连接信息");
    }

    @Override
    public void sendMessageToClient(int msgCode, Message msg, Object connect, NetworkMsgType msgType) {
        // HTTP 是请求-响应模型，服务端拿不到「客户端连接」，无法主动推送。
        // 后台需要实时通知时应该用轮询，或另走 WebSocket。
        log.error("netty http - 无法主动向HTTP客户端发送消息");
    }

    @Override
    public void closeClientConnect(Object connect) {
        log.error("netty http - Http服务器没有保存客户端连接信息");
    }

}
