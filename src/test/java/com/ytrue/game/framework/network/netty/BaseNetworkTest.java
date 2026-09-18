package com.ytrue.game.framework.network.netty;

import com.google.protobuf.Message;
import com.ytrue.game.framework.engine.data.NetworkMsgType;
import com.ytrue.game.framework.network.proto.AppMessage.BaseMessage;
import com.ytrue.game.framework.network.proto.NetworkMessage.PingRequest;
import com.ytrue.game.framework.network.proto.NetworkMessage.PingResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * BaseNetwork 端到端功能测试：真起服务、真连客户端、真收发 Protobuf 消息。
 *
 * <p>不依赖 JUnit，直接跑 {@code main} 即可（IDE 里点绿色箭头）：
 * 断言结果打印到控制台，全部通过退出码 0，有失败退出码 1。</p>
 *
 * <p>覆盖点：{@code open} 幂等性、连接登记、{@code getClientIp}、服务端下发、
 * 客户端上报、向不存在的连接发消息、{@code closeClientConnect}，
 * 以及 {@code close} 的阻塞问题探测。</p>
 *
 * @since 1.0.0
 */
public class BaseNetworkTest {

    /** 通过数。 */
    static int pass = 0;

    /** 失败数。 */
    static int fail = 0;

    // ==================== 被测类的具体实现 ====================

    /**
     * 被测对象：一个最小可用的 {@link BaseNetwork} 子类。
     *
     * <p>真实实现（{@code TcpNetwork}）同样要提供这三件事：
     * 装配流水线、维护连接表、以及实现无参 {@code open()}。</p>
     */
    static class TestNetwork extends BaseNetwork {

        /** 连接表：连接标识 -> 通道上下文。 */
        final Map<String, ChannelHandlerContext> conns = new ConcurrentHashMap<>();

        /** 服务端收到的消息。 */
        final BlockingQueue<BaseMessage> serverReceived = new LinkedBlockingQueue<>();

        /** 监听端口（真实实现从 ServerConfig 读，这里由测试注入）。 */
        private final int port;

        TestNetwork(int port) {
            this.port = port;
        }

        @Override
        public void open() {
            // INetwork 的无参入口：由实现自己决定监听参数，再委托给三参重载
            open(port, 1, 2);
        }

        @Override
        protected ChannelHandler getChannelHandler() {
            // 返回 ChannelInitializer：Netty 每个新连接建立时都回调它来装配流水线
            return new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline()
                            // 拆包：4 字节长度前缀（与 Protobuf 帧格式配套）
                            .addLast(new LengthFieldBasedFrameDecoder(2036334592, 0, 4, 0, 4))
                            // 字节流 -> BaseMessage 对象
                            .addLast(new ProtobufDecoder(BaseMessage.getDefaultInstance()))
                            // 发送方向：先加 4 字节长度前缀，再序列化
                            .addLast(new LengthFieldPrepender(4))
                            .addLast(new ProtobufEncoder())
                            // 业务处理器（每个连接一个实例，故不需要 @Sharable）
                            .addLast(new ServerHandler(conns, serverReceived));
                }
            };
        }

        @Override
        protected ChannelHandlerContext getClientChannel(Object connect) {
            return conns.get(connect.toString());
        }

        @Override
        protected void removeClientChannel(Object connect) {
            conns.remove(connect.toString());
        }
    }

    /** 服务端业务处理器：维护连接表 + 收集收到的消息。 */
    static class ServerHandler extends ChannelInboundHandlerAdapter {

        /** 连接表（与 TestNetwork 共享同一份）。 */
        final Map<String, ChannelHandlerContext> conns;

        /** 收到的消息。 */
        final BlockingQueue<BaseMessage> received;

        ServerHandler(Map<String, ChannelHandlerContext> conns, BlockingQueue<BaseMessage> received) {
            this.conns = conns;
            this.received = received;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            // 连接建立：登记到连接表，key 的取法与 BaseNetwork 约定一致（channelId 的短文本）
            conns.put(ctx.channel().id().asShortText(), ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            conns.remove(ctx.channel().id().asShortText());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            received.add((BaseMessage) msg);
        }
    }

    /** 客户端：收集服务端下发的消息。 */
    static class ClientHandler extends ChannelInboundHandlerAdapter {

        /** 收到的消息。 */
        final BlockingQueue<BaseMessage> received = new LinkedBlockingQueue<>();

        /** 连接就绪信号。 */
        final CountDownLatch active = new CountDownLatch(1);

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            active.countDown();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            received.add((BaseMessage) msg);
        }
    }

    // ==================== 测试主体 ====================

    public static void main(String[] args) throws Exception {
        int port = freePort();
        TestNetwork network = new TestNetwork(port);

        section("1. 启动监听");
        check("open(port,boss,worker) 首次返回 true", network.open(port, 1, 2), true);
        check("重复调用返回 false（幂等）", network.open(port, 1, 2), false);
        network.open();
        check("无参 open() 在已启动时不重复绑定", network.conns.isEmpty(), true);

        section("2. 客户端连接");
        ClientHandler clientHandler = new ClientHandler();
        MultiThreadIoEventLoopGroup clientGroup =
                new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientGroup).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(2036334592, 0, 4, 0, 4))
                                .addLast(new ProtobufDecoder(BaseMessage.getDefaultInstance()))
                                .addLast(new LengthFieldPrepender(4))
                                .addLast(new ProtobufEncoder())
                                .addLast(clientHandler);
                    }
                });
        Channel client = bootstrap.connect("127.0.0.1", port).sync().channel();
        clientHandler.active.await(3, TimeUnit.SECONDS);
        check("客户端连接成功", client.isActive(), true);

        String connect = waitForConnect(network, 3, TimeUnit.SECONDS);
        check("服务端连接表已登记该连接", connect != null, true);
        System.out.println("       连接标识 = " + connect);

        section("3. getClientIp");
        check("返回回环地址", network.getClientIp(connect), "127.0.0.1");
        check("连接不存在时返回空串", network.getClientIp("no-such-conn"), "");
        check("连接类型不符时返回空串", network.getClientIp(12345), "");

        section("4. 服务端下发消息（真实协议：业务消息被包进 BaseMessage 信封）");
        // 注意：sendMessageToClient 的第二个参数是「业务消息」，不是 BaseMessage
        // —— 方法内部会把它塞进 BaseMessage 的 body 字段
        long pingTime = 1234567890123L;
        Message out = PingResponse.newBuilder().setPingTime(pingTime).build();
        network.sendMessageToClient(NETWORK_MSG_CODE_S_C_PING_RESPONSE, out, connect, NetworkMsgType.BINARY);

        BaseMessage envelope = clientHandler.received.poll(3, TimeUnit.SECONDS);
        check("客户端收到信封", envelope != null, true);
        if (envelope != null) {
            check("  信封消息码正确", envelope.getCode(), NETWORK_MSG_CODE_S_C_PING_RESPONSE);
            // 解开信封：body 反序列化回业务消息
            PingResponse decoded = PingResponse.parseFrom(envelope.getBody());
            check("  解出业务消息内容正确", decoded.getPingTime(), pingTime);
        }

        section("5. 客户端上报消息");
        Message in = PingRequest.newBuilder().setPingTime(pingTime).build();
        client.writeAndFlush(BaseMessage.newBuilder()
                .setCode(NETWORK_MSG_CODE_C_S_PING_REQUEST)
                .setBody(in.toByteString())
                .build()).sync();

        BaseMessage upEnvelope = network.serverReceived.poll(3, TimeUnit.SECONDS);
        check("服务端收到信封", upEnvelope != null, true);
        if (upEnvelope != null) {
            check("  信封消息码正确", upEnvelope.getCode(), NETWORK_MSG_CODE_C_S_PING_REQUEST);
            PingRequest decoded = PingRequest.parseFrom(upEnvelope.getBody());
            check("  解出业务消息内容正确", decoded.getPingTime(), pingTime);
        }

        section("6. 异常输入");
        try {
            network.sendMessageToClient(1, in, "ghost-conn", NetworkMsgType.BINARY);
            ok("向不存在的连接发消息不抛异常");
        } catch (Exception e) {
            bad("向不存在的连接发消息抛了异常", e.toString());
        }
        try {
            network.sendMessageToClient(1, in, 99999, NetworkMsgType.BINARY);
            ok("连接标识类型不符时不抛异常");
        } catch (Exception e) {
            bad("连接标识类型不符时抛了异常", e.toString());
        }

        section("7. 断开连接");
        network.closeClientConnect(connect);
        Thread.sleep(300);
        check("连接表已移除该连接", network.conns.containsKey(connect), false);
        check("客户端已断开", client.isActive(), false);

        section("8. 关闭服务");
        checkClose(network);

        clientGroup.shutdownGracefully();

        System.out.printf("%n========== 通过 %d / 失败 %d ==========%n", pass, fail);
        System.exit(fail > 0 ? 1 : 0);
    }

    // ==================== 断言与工具 ====================

    /** 与 NetworkMessage.proto 中 NetworkMsgCode 对应。 */
    static final int NETWORK_MSG_CODE_C_S_PING_REQUEST = 0x10000000;
    static final int NETWORK_MSG_CODE_S_C_PING_RESPONSE = 0x20000000;

    /**
     * 验证 {@link BaseNetwork#close()} 能正常关闭且不会阻塞。
     *
     * <p>回归点：{@code close()} 里若写成 {@code serverChannel.closeFuture().sync()}，
     * 只会干等「通道被关闭」这件事发生，而没有任何地方发起关闭，会永久阻塞。
     * 必须在独立线程里调用并设超时——真阻塞住时主线程才不会被一起拖死。</p>
     *
     * @param network 被测网络对象
     */
    static void checkClose(TestNetwork network) throws Exception {
        CountDownLatch returned = new CountDownLatch(1);
        long start = System.currentTimeMillis();
        Thread closer = new Thread(() -> {
            network.close();
            returned.countDown();
        }, "close-probe");
        // 设为守护线程：万一此处仍阻塞，不会阻止 JVM 退出
        closer.setDaemon(true);
        closer.start();

        // 超时给足：shutdownGracefully() 每个线程池有默认 2 秒静默期，属正常耗时而非阻塞
        boolean finished = returned.await(30, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("       close() 耗时 " + elapsed + " ms");

        check("close() 正常返回（无永久阻塞）", finished, true);
        if (!finished) {
            System.out.println("       —— 30 秒仍未返回，属真阻塞，后面的断言无意义");
            return;
        }

        // 关闭后引用应被置空，使 open() 能重新启动
        check("服务端通道已置空", network.serverChannel, null);
        check("boss 线程池已置空", network.bossGroup, null);
        check("worker 线程池已置空", network.workerGroup, null);
    }

    /** 找一个空闲端口。 */
    static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    /** 轮询等待服务端登记连接，返回连接标识；超时返回 {@code null}。 */
    static String waitForConnect(TestNetwork network, long timeout, TimeUnit unit) throws Exception {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            if (!network.conns.isEmpty()) {
                return network.conns.keySet().iterator().next();
            }
            Thread.sleep(20);
        }
        return null;
    }

    /** 打印分段标题。 */
    static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    /** 断言相等。 */
    static void check(String what, Object actual, Object expected) {
        if (String.valueOf(expected).equals(String.valueOf(actual))) {
            ok(String.format("%-34s = %s", what, actual));
        } else {
            bad(String.format("%-34s", what), "期望=" + expected + " 实际=" + actual);
        }
    }

    /** 记一次通过。 */
    static void ok(String msg) {
        pass++;
        System.out.println("  [通过] " + msg);
    }

    /** 记一次失败。 */
    static void bad(String what, String detail) {
        fail++;
        System.out.println("  [失败] " + what + " " + detail);
    }

}
