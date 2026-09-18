package com.ytrue.game.framework.network.netty.tcp;

import com.google.protobuf.MessageLite;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

/**
 * TCP Socket 的 Protobuf 编解码初始化器。
 *
 * <p>为每个新建立的连接装配流水线，收发两侧的处理器成对出现：</p>
 *
 * <pre>
 * 收（客户端 → 服务端）： LengthFieldBasedFrameDecoder → ProtobufDecoder → 业务处理器
 * 发（服务端 → 客户端）： ProtobufEncoder → LengthFieldPrepender
 * </pre>
 *
 * <p>「长度字段」是这套协议的分帧约定：每个数据包前 4 字节是本包长度，
 * 接收时先按长度切出完整一帧再解码，发送时先写入长度再写内容。
 * 没有它就无法区分粘在一起的多个包，也无法处理半包。</p>
 *
 * @since 1.0.0
 */
public class TcpChannelInitializer extends ChannelInitializer<Channel> {

    /**
     * 用于解码的空消息实例（{@link ProtobufDecoder} 需要它来获取消息类型描述符）。
     */
    private final MessageLite lite;

    /**
     * 单帧最大长度，超过则丢弃该连接，防止恶意超长包撑爆内存。
     */
    private final int maxFrameLength;

    /**
     * 长度字段的起始偏移量。
     */
    private final int lengthFieldOffset;

    /**
     * 长度字段占用的字节数。
     */
    private final int lengthFieldLength;

    /**
     * 长度修正值：长度字段的值加上它才是整帧长度。
     */
    private final int lengthAdjustment;

    /**
     * 解码后需要跳过的字节数（即剥掉长度字段本身）。
     */
    private final int initialBytesToStrip;

    /**
     * 业务处理器（连接建立后处理收到的消息）。
     */
    private final ChannelInboundHandler channelInboundHandler;

    /**
     * 构造器。
     *
     * @param channelInboundHandler 业务处理器
     * @param lite                  用于解码的空消息实例
     * @param maxFrameLength        单帧最大长度
     * @param lengthFieldOffset     长度字段起始偏移量
     * @param lengthFieldLength     长度字段占用字节数
     * @param lengthAdjustment      长度修正值
     * @param initialBytesToStrip   解码后跳过的字节数
     */
    public TcpChannelInitializer(ChannelInboundHandler channelInboundHandler,
                                 MessageLite lite,
                                 int maxFrameLength,
                                 int lengthFieldOffset,
                                 int lengthFieldLength,
                                 int lengthAdjustment,
                                 int initialBytesToStrip
    ) {
        this.lite = lite;
        this.channelInboundHandler = channelInboundHandler;
        this.maxFrameLength = maxFrameLength;
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.lengthAdjustment = lengthAdjustment;
        this.initialBytesToStrip = initialBytesToStrip;
    }

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        // 收：按长度字段切出完整帧，并剥掉 4 字节长度前缀
        pipeline.addLast(new LengthFieldBasedFrameDecoder(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip));
        // 收：字节流 -> Protobuf 消息对象（剥离长度后剩下的就是纯 Protobuf 数据）
        // ProtobufDecoder知道：“我要把收到的数据解析成 BaseMessage”
        pipeline.addLast(new ProtobufDecoder(lite));
        // 发：先写入 4 字节长度前缀。Netty 的 pipeline 出站方向是「从后往前」，所以这里写在 Decoder 之后，实际执行顺序是 Encoder 先编码、Prepender 再加长度
        // 发包的长度占位 4 字节
        pipeline.addLast(new LengthFieldPrepender(lengthFieldLength));
        // 发：Protobuf 消息对象 -> 字节流
        pipeline.addLast(new ProtobufEncoder());
        // 业务处理器放在最后：入站时它拿到的是解码后的消息对象
        pipeline.addLast(channelInboundHandler);
    }

}
