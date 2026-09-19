package com.ytrue.game.framework.network.netty.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * HTTP 通道初始化器。
 *
 * <p>为每个新建立的连接装配 HTTP 编解码流水线：</p>
 *
 * <pre>
 * HttpRequestDecoder    字节流       → HttpRequest / HttpContent 对象
 * HttpResponseEncoder   HttpResponse → 字节流
 * HttpObjectAggregator  把分片的请求体聚合成完整的 FullHttpRequest
 * ChunkedWriteHandler   支持分块写出大响应
 * 业务处理器            拿到聚合后的完整请求
 * </pre>
 *
 * <p>{@link HttpObjectAggregator} 是必须的：HTTP 请求的 body 可能被拆成多个
 * {@code HttpContent} 分片传来，不聚合的话业务层拿到的是半截数据。
 * 构造参数 {@code maxContentLength} 限制单次请求体上限，超过会返回 413，
 * 防止超大请求撑爆内存。</p>
 *
 * @since 1.0.0
 */
public class HttpChannelInitializer extends ChannelInitializer<Channel> {

    /**
     * 单次请求体的最大字节数（512 KB）。
     */
    private static final int MAX_CONTENT_LENGTH = 524288;

    /**
     * 业务处理器（连接建立后处理聚合完成的请求）。
     */
    private final ChannelInboundHandler channelInboundHandler;

    /**
     * 构造器。
     *
     * @param channelInboundHandler 业务处理器
     */
    public HttpChannelInitializer(ChannelInboundHandler channelInboundHandler) {
        this.channelInboundHandler = channelInboundHandler;
    }

    @Override
    protected void initChannel(Channel ch) {
        ch.pipeline()
                // 入站：字节流 → HttpRequest / HttpContent
                .addLast("http-decoder", new HttpRequestDecoder())
                // 出站：HttpResponse → 字节流
                .addLast("http-encoder", new HttpResponseEncoder())
                // 入站：把分片的请求体聚合成一个 FullHttpRequest
                .addLast("http-aggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH))
                // 出站：支持分块写出（大响应不必一次性驻留内存）
                .addLast("http-chunked", new ChunkedWriteHandler())
                // 业务处理器
                .addLast("http-serverHandler", channelInboundHandler);
    }

}
