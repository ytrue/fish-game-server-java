package com.ytrue.game.framework.network.netty.websocket;

import com.google.protobuf.Message;
import com.ytrue.game.framework.engine.config.ServerConfig;
import com.ytrue.game.framework.engine.data.NetworkMsgType;
import com.ytrue.game.framework.network.netty.BaseNetwork;
import com.ytrue.game.framework.network.proto.AppMessage.BaseMessage;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * WebSocket 网络实现。
 *
 * <p>承载与 TCP 相同的那套客户端协议（同样的消息码、同样的 Protobuf 格式），
 * 区别只在传输层：WebSocket 自带帧边界，不需要长度前缀。</p>
 *
 * <p>监听参数取自 {@code application.yml} 的 {@code game.network.websocket} 配置。</p>
 *
 * <p>注意：本类<b>不加</b> {@code @Primary}——默认注入应解析到 TCP（客户端主通道）；
 * 需要 WebSocket 时用 {@code @Qualifier("websocket")} 显式指定。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@Qualifier("websocket")
@RequiredArgsConstructor
public class WebSocketNetwork extends BaseNetwork {

    /**
     * WebSocket 消息处理器（同时持有全局连接表）。
     */
    private final WebSocketServerHandler serverHandler;

    /**
     * 服务器配置。
     */
    private final ServerConfig serverConfig;

    @Override
    public void open() {
        // 从配置读取监听参数（对应 yml 中 game.network.websocket 下的 port / boss-size / worker-size）
        ServerConfig.WebSocket webSocket = serverConfig.getNetwork().getWebsocket();
        int port = webSocket.getPort();
        int bossSize = webSocket.getBossSize();
        int workerSize = webSocket.getWorkerSize();

        if (open(port, bossSize, workerSize)) {
            log.info("WebSocket网络服务启动成功~ port:[{}] path:[/ws]", port);
        } else {
            log.error("WebSocket网络服务启动失败~ port:[{}]", port);
        }
    }

    @Override
    protected ChannelHandler getChannelHandler() {
        return new WebSocketChannelInitializer(serverHandler);
    }

    /**
     * 向客户端发送消息。
     *
     * <p><b>这是与 TCP 实现最关键的差异，必须覆写基类实现。</b></p>
     *
     * <p>TCP 侧的流水线里挂着 {@code ProtobufEncoder} + {@code LengthFieldPrepender}，
     * 所以基类直接 {@code writeAndFlush(BaseMessage)} 就能被自动编码发出去。</p>
     *
     * <p>WebSocket 的流水线里只有 {@code WebSocketFrameEncoder}，它<b>只认 WebSocketFrame</b>。
     * 若沿用基类实现直接写 {@code BaseMessage}，编码器会因为类型不匹配而抛异常
     * （通常报 {@code UnsupportedMessageTypeException}）。
     * 所以这里必须自己把消息包成 {@link BinaryWebSocketFrame}。</p>
     *
     * <p>注意不要加长度前缀：WebSocket 的帧头本身就记录了负载长度，
     * 再套一层长度字段客户端反而解不开。</p>
     *
     * @param msgCode 消息码
     * @param msg     业务消息
     * @param connect 客户端连接标识
     * @param msgType 消息编解码类型（当前仅支持 BINARY，与 TCP 侧一致）
     */
    @Override
    public void sendMessageToClient(int msgCode, Message msg, Object connect, NetworkMsgType msgType) {
        // 连接标识约定为 String（channelId）；类型不符说明调用方传错了连接对象
        if (!(connect instanceof String)) {
            return;
        }

        ChannelHandlerContext channel = getClientChannel(connect);
        // 通道存在才发送；客户端可能已断开，此处必须判空
        if (channel == null) {
            return;
        }

        // 与 TCP 侧一样，先套上统一的「信封」：消息码 + 消息体字节
        BaseMessage message = BaseMessage.newBuilder()
                .setCode(msgCode)
                .setBody(msg.toByteString())
                .build();

        // 包成二进制帧发出。toByteArray() 返回的是新数组，Unpooled.wrappedBuffer 直接包住它，
        // 不涉及额外的内存拷贝
        channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(message.toByteArray())));
    }

    @Override
    protected ChannelHandlerContext getClientChannel(Object connect) {
        return serverHandler.getClientChannel(connect.toString());
    }

    @Override
    protected void removeClientChannel(Object connect) {
        serverHandler.removeClientChannel(connect.toString());
    }

}
