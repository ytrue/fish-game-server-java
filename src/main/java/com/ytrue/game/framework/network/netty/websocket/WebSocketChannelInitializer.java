package com.ytrue.game.framework.network.netty.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * WebSocket 通道初始化器。
 *
 * <p>与 {@code TcpChannelInitializer} 最大的不同：<b>这里不需要长度字段分帧</b>。</p>
 *
 * <pre>
 * TCP 是字节流，没有消息边界，必须靠 LengthFieldBasedFrameDecoder 按 4 字节长度前缀切帧：
 *      收：  LengthFieldBasedFrameDecoder → ProtobufDecoder → 业务处理器
 *      发：  ProtobufEncoder → LengthFieldPrepender
 *
 * WebSocket 自带帧边界，一个 frame 就是一条完整消息，无需任何长度前缀：
 *      收：  WebSocketFrameDecoder → 业务处理器（拿到 BinaryWebSocketFrame）
 *      发：  WebSocketFrameEncoder
 * </pre>
 *
 * <p>流水线说明：</p>
 * <pre>
 * HttpServerCodec                 WebSocket 握手走的是 HTTP，所以先要有 HTTP 编解码
 * HttpObjectAggregator            把分片的握手请求聚合成完整的 FullHttpRequest
 * WebSocketServerProtocolHandler  处理握手与协议升级，升级成功后自动往 pipeline 里
 *                                 插入 WebSocketFrameDecoder / WebSocketFrameEncoder，
 *                                 之后流经的数据就是 WebSocketFrame 而不是 HTTP 报文了
 * 业务处理器                       收 BinaryWebSocketFrame
 * </pre>
 *
 * @since 1.0.0
 */
public class WebSocketChannelInitializer extends ChannelInitializer<Channel> {

    /**
     * 握手请求体的最大字节数（512 KB）。
     *
     * <p>只作用于 HTTP 握手阶段，握手报文很小，这个值只是为了防御异常请求。</p>
     */
    private static final int MAX_CONTENT_LENGTH = 524288;

    /**
     * WebSocket 的连接路径。
     *
     * <p>客户端要连 {@code ws://host:port/ws}，路径不符会被协议处理器直接拒绝。</p>
     */
    private static final String WEBSOCKET_PATH = "/ws";

    /**
     * 单个 WebSocket 帧的最大字节数。
     *
     * <p>与 TCP 侧 {@code MAX_FRAME_LENGTH} 取同一个值，保证同一条业务消息在两种传输上
     * 都不会因为长度限制出现「TCP 能发、WebSocket 发不了」的差异。</p>
     */
    private static final int MAX_FRAME_SIZE = 2036334592;

    /**
     * 业务处理器（连接建立后处理收到的帧）。
     */
    private final ChannelInboundHandler channelInboundHandler;

    /**
     * 构造器。
     *
     * @param channelInboundHandler 业务处理器
     */
    public WebSocketChannelInitializer(ChannelInboundHandler channelInboundHandler) {
        this.channelInboundHandler = channelInboundHandler;
    }

    @Override
    protected void initChannel(Channel ch) {
        ch.pipeline()
                // WebSocket 握手是 HTTP 请求，所以流水线前两环与 HTTP 服务一致
                .addLast(new HttpServerCodec())
                .addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH))
                // 处理握手 + 协议升级；升级完成后它会把 WebSocketFrame 的编解码器插进流水线。
                // 用 Config 而非简单构造器，是为了把单帧上限从默认的 64KB 调大——
                // 游戏消息（如房间快照）很容易超过这个值
                .addLast(new WebSocketServerProtocolHandler(
                        WebSocketServerProtocolConfig.newBuilder()
                                .websocketPath(WEBSOCKET_PATH)
                                .maxFramePayloadLength(MAX_FRAME_SIZE)
                                .build()))
                // 业务处理器放在最后：入站时它拿到的是 BinaryWebSocketFrame
                .addLast(channelInboundHandler);
    }

}
