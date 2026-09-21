package com.ytrue.game.framework.network.netty;

import com.google.protobuf.Message;
import com.ytrue.game.framework.engine.data.NetworkMsgType;
import com.ytrue.game.framework.network.INetwork;
import com.ytrue.game.framework.network.proto.AppMessage.BaseMessage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * 网络基类（Netty 实现）。
 *
 * <p>把基于 Netty 的「监听端口 → 收发消息 → 断开连接 → 取客户端 IP」这套通用流程固化下来，
 * 具体的「通道怎么装配」（{@link #getChannelHandler()}）、「连接表怎么存」
 * （{@link #getClientChannel(Object)} / {@link #removeClientChannel(Object)}）
 * 交给子类实现——例如 TCP Socket 与 HTTP 两个实现共用本类。</p>
 *
 * <p>数据流：{@code open()} 启动 Netty 并绑定端口 → 子类装配的 Handler 处理收发 →
 * {@code sendMessageToClient()} 通过连接表拿到通道主动下发 → {@code close()} 释放资源。</p>
 *
 * @since 1.0.0
 */
@Slf4j
public abstract class BaseNetwork implements INetwork {

    /**
     * 客户端连接处理线程池（boss）。
     *
     * <p>只负责接收 TCP 连接，把建立好的连接交给 worker，本身不做耗时业务处理。</p>
     *
     * <p>Netty 4.2 起不再用与传输绑定的 {@code NioEventLoopGroup}（已废弃），改为通用的
     * {@link MultiThreadIoEventLoopGroup} + 传输相关的 {@link NioIoHandler}：
     * IO 多路复用的具体实现被抽成可注入的 {@code IoHandlerFactory}，
     * 于是同一个 EventLoopGroup 实现可以服务 NIO / Epoll / KQueue 等各种传输。</p>
     */
    protected MultiThreadIoEventLoopGroup bossGroup = null;

    /**
     * 客户端消息处理线程池（worker）。
     *
     * <p>负责已建立连接上的读写事件，实际的消息处理跑在这里。</p>
     */
    protected MultiThreadIoEventLoopGroup workerGroup = null;

    /**
     * 服务器连接通道。
     *
     * <p>即绑定在监听端口上的那个 ServerChannel。为空表示尚未启动监听。</p>
     */
    protected Channel serverChannel = null;

    /**
     * 网络连接操作锁。
     *
     * <p>用于串行化 {@link #open} 中对启动流程的检查与赋值，避免并发调用时重复启动监听。</p>
     */
    protected final Object operationLock = new Object();

    /**
     * 打开网络监听并绑定指定端口。
     *
     * <p>可安全重复调用：已启动时直接返回 {@code false}，不会重复绑定。</p>
     *
     * @param port       监听端口
     * @param bossSize   boss 线程池大小
     * @param workerSize worker 线程池大小
     * @return 本次调用是否成功启动了监听（已启动过或启动失败均为 {@code false}）
     */
    public boolean open(int port, int bossSize, int workerSize) {
        // 加锁：open 可能被多处并发调用，把「判断是否已启动 + 启动 + 记录通道」做成不可分割的整体
        synchronized (operationLock) {
            // 仅在尚未启动时执行；serverChannel 非空说明监听已就绪，直接走到最后返回 false
            if (serverChannel == null) {
                try {
                    // NIO 的 IO 处理器工厂：boss 与 worker 共用同一个即可
                    // （它只是「怎么创建 IoHandler」的工厂，本身无状态，每创建一个 EventLoop 时会调用它一次）
                    IoHandlerFactory ioHandler = NioIoHandler.newFactory();
                    // 创建 boss 线程池：负责接收新连接，线程数与「同时接入的并发量」相关，通常很小
                    bossGroup = new MultiThreadIoEventLoopGroup(bossSize, ioHandler);
                    // 创建 worker 线程池：负责已连接通道的读写与业务回调，工作量主要在这里，通常较大
                    workerGroup = new MultiThreadIoEventLoopGroup(workerSize, ioHandler);

                    // 初始化服务器引导类：Netty 服务端的统一装配入口
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    // 绑定主从两个线程组（第一个处理接入，第二个处理读写）
                    bootstrap.group(bossGroup, workerGroup);
                    // 指定服务端通道实现为 NIO（Java NIO 的 ServerSocketChannel 封装）
                    bootstrap.channel(NioServerSocketChannel.class);
                    // 指定要监听的本地地址（只给端口，表示绑定本机所有网卡的该端口）
                    bootstrap.localAddress(new InetSocketAddress(port));
                    // 设置每个新连接建立后，其 ChannelPipeline 的装配方式（由子类提供）
                    bootstrap.childHandler(getChannelHandler());

                    // 执行绑定并阻塞等待完成；sync() 保证端口真的绑定成功后才继续往下走
                    ChannelFuture future = bootstrap.bind().sync();
                    // 记下服务端通道，供 close() 关闭、以及作为「已启动」的标记
                    serverChannel = future.channel();
                    // 启动成功
                    return true;
                } catch (Exception e) {
                    // 端口被占用是最常见的启动失败原因，单独给出更明确的提示
                    // 其它异常统一打日志，带上堆栈方便排查
                    log.error("启动服务出错:[{}]", e.getMessage(), e);
                }
            }
        }
        // 走到这里有两种情况：本来就已经启动过，或本次启动抛了异常
        return false;
    }

    @Override
    public void close() {
        // 只有启动过才需要关闭
        synchronized (operationLock) {
            try {
                // 关闭服务端通道并等待关闭完成。
                // closeFuture().sync()
                //    = 等别人把 Channel 关掉
                //close().sync()
                //    = 我现在就把 Channel 关掉，然后等关闭完成
                if (serverChannel != null) {
                    serverChannel.close().sync();
                    // 置空引用，使 open() 可以再次启动
                    serverChannel = null;
                }
                // 优雅关闭 boss 线程池，等待其中任务结束
                bossGroup.shutdownGracefully().sync();
                // 优雅关闭 worker 线程池，等待其中任务结束
                workerGroup.shutdownGracefully().sync();
                bossGroup = null;
                workerGroup = null;
            } catch (Exception e) {
                // 关闭过程中的异常只记录、不向外抛（关闭流程要尽量走完）。
                // 用 warn 而非 info：关网络失败意味着端口可能没释放，
                // 不属于「正常流程」，用 info 会让人以为一切正常
                log.warn("关闭监听出错:[{}]", e.getMessage(), e);
            }
        }
    }


    @Override
    public void sendMessageToClient(int msgCode, Message msg, Object connect, NetworkMsgType msgType) {
        // 连接标识约定为 String 类型（TCP 实现里是 channelId）；类型不符说明调用方传错了连接对象
        if (connect instanceof String) {
            // 合法的 channelId 类型，从连接表取出对应的通道上下文
            ChannelHandlerContext channel = getClientChannel(connect);
            // 通道存在才发送；客户端可能已断开，此处必须判空
            if (channel != null) {
                // 按统一信封封装：消息码 + 消息体字节，客户端据此分发到对应的处理逻辑
                BaseMessage message = BaseMessage.newBuilder()
                        .setCode(msgCode)
                        .setBody(msg.toByteString())
                        .build();
                // 写入并立即冲刷（writeAndFlush = 入队 + 发送，避免消息滞留缓冲区）
                channel.writeAndFlush(message);
            }
        }
    }

    @Override
    public void closeClientConnect(Object connect) {
        // 从连接表取出通道上下文
        ChannelHandlerContext channel = getClientChannel(connect);
        // 通道存在才有关闭的意义
        if (channel != null) {
            // 关闭该客户端连接（会触发其 channelInactive 回调）
            channel.close();
            // 从连接表中移除，避免连接表持续增长
            removeClientChannel(connect);
        }
    }

    @Override
    public String getClientIp(Object connect) {
        // 与 sendMessageToClient 相同，连接标识约定为 String
        if (connect instanceof String) {
            // 取出通道上下文
            ChannelHandlerContext channel = getClientChannel(connect);
            // 通道还在才能拿到对端地址
            if (channel != null && channel.channel().remoteAddress() != null) {
                return ((InetSocketAddress) channel.channel().remoteAddress()).getAddress().getHostAddress();
            }
        }
        // 连接不存在或类型不符时返回空串，调用方无需判空
        return "";
    }

    /**
     * 获取子类对应的通道装配器。
     *
     * <p>每个新连接建立时，Netty 会调用它返回的 {@link ChannelHandler} 来装配该连接的流水线，
     * 例如 TCP 实现装配「长度字段拆包 + Protobuf 编解码 + 业务处理器」。</p>
     *
     * @return 用于装配连接流水线的处理器
     */
    protected abstract ChannelHandler getChannelHandler();

    /**
     * 获取客户端连接对象。
     *
     * @param connect 客户端连接标识
     * @return 连接对应的通道上下文；不存在时返回 {@code null}
     */
    protected abstract ChannelHandlerContext getClientChannel(Object connect);

    /**
     * 移除客户端连接对象。
     *
     * @param connect 客户端连接标识
     */
    protected abstract void removeClientChannel(Object connect);

}
