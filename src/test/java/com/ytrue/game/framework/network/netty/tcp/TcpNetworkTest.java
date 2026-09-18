package com.ytrue.game.framework.network.netty.tcp;

import com.google.protobuf.Message;
import com.ytrue.game.framework.engine.config.ServerConfig;
import com.ytrue.game.framework.engine.container.UserContainer;
import com.ytrue.game.framework.engine.data.ServerUser;
import com.ytrue.game.framework.engine.register.AppHandlerRegister;
import com.ytrue.game.framework.engine.utils.SpringUtils;
import com.ytrue.game.framework.engine.wrapper.AppHandlerWrapper;
import com.ytrue.game.framework.network.event.NetExitEvent;
import com.ytrue.game.framework.network.proto.AppMessage.BaseMessage;
import com.ytrue.game.framework.network.proto.NetworkMessage.PingRequest;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
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
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TCP 网络层端到端测试：真起服务、真连客户端，验证「消息码 → 控制器方法」的全链路反射分发。
 *
 * <p>不依赖 JUnit，直接跑 {@code main} 即可（IDE 里点绿色箭头）：
 * 断言结果打印到控制台，全部通过退出码 0，有失败退出码 1。</p>
 *
 * <p>覆盖点：配置读取、启动监听、连接登记（连接表 + 用户容器）、消息分发（业务消息内容 /
 * ServerUser / exp 是否都正确传递）、未注册消息码的容错、断开后的连接清理与退出事件发布。</p>
 *
 * @since 1.0.0
 */
public class TcpNetworkTest {

    /** 通过数。 */
    static int pass = 0;

    /** 失败数。 */
    static int fail = 0;

    /** 客户端 ping 请求消息码（NetworkMsgCode.C_S_PING_REQUEST）。 */
    static final int C_S_PING_REQUEST = 0x10000000;

    // ==================== 测试替身 ====================

    /** 模拟一个业务控制器：记录被调用的情况。 */
    public static class TestController {

        /** 被调用次数。 */
        static final AtomicInteger count = new AtomicInteger();

        /** 收到的业务消息。 */
        static final AtomicReference<PingRequest> msg = new AtomicReference<>();

        /** 收到的用户。 */
        static final AtomicReference<ServerUser> user = new AtomicReference<>();

        /** 收到的附加标记。 */
        static final AtomicReference<Long> exp = new AtomicReference<>();

        /**
         * 处理方法，签名须为 {@code (业务消息, ServerUser, Long)}。
         *
         * @param m 业务消息
         * @param u 用户
         * @param e 附加标记
         */
        public void handlePing(PingRequest m, ServerUser u, Long e) {
            count.incrementAndGet();
            msg.set(m);
            user.set(u);
            exp.set(e);
        }
    }

    /** 记录退出事件是否被发布。 */
    static class ExitListener implements ApplicationListener<NetExitEvent> {

        /** 收到的事件数。 */
        static final AtomicInteger count = new AtomicInteger();

        /** 事件携带的连接标识。 */
        static final AtomicReference<String> connect = new AtomicReference<>();

        @Override
        public void onApplicationEvent(NetExitEvent event) {
            count.incrementAndGet();
            connect.set(event.getUser().getConnect());
        }
    }

    /** 客户端处理器：只用来感知连接建立。 */
    static class ClientHandler extends ChannelInboundHandlerAdapter {

        /** 连接就绪信号。 */
        final CountDownLatch active = new CountDownLatch(1);

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            active.countDown();
        }
    }

    // ==================== 测试主体 ====================

    public static void main(String[] args) throws Exception {
        // 退出事件发布依赖 Spring 上下文：SpringEventPublisher 从 ApplicationContext 取发布器
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SpringUtils.class);
        ctx.register(ExitListener.class);
        ctx.refresh();

        int port = freePort();

        // ---- 装配：路由表 → 处理器 → 网络实现 ----
        AppHandlerRegister register = new AppHandlerRegister();
        TestController controller = new TestController();
        Method taskMethod = TestController.class.getMethod(
                "handlePing", PingRequest.class, ServerUser.class, Long.class);
        // checkMethod 传 null，走「直接反射调用任务方法」这条路径
        register.getAppHandlerWrapperMap().put(
                C_S_PING_REQUEST, new AppHandlerWrapper(controller, null, taskMethod, 7L));

        TcpServerHandler serverHandler = new TcpServerHandler(register);

        ServerConfig config = new ServerConfig();
        config.getNetwork().getTcpsocket().setPort(port);
        config.getNetwork().getTcpsocket().setBossSize(1);
        config.getNetwork().getTcpsocket().setWorkerSize(2);

        TcpNetwork network = new TcpNetwork(serverHandler, config);
        check("配置读取正确", config.getNetwork().getTcpsocket().getPort(), port);

        section("1. 启动监听");
        network.open();

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
        check("客户端已连接", client.isActive(), true);

        String channelId = waitForConnection(serverHandler, 3, TimeUnit.SECONDS);
        check("服务端已登记该连接", channelId != null, true);
        System.out.println("       连接标识 = " + channelId);

        ServerUser session = UserContainer.getUserByConnect(channelId);
        check("用户容器中已建立会话", session != null, true);

        section("3. 消息分发（消息码 → 控制器方法）");
        long pingTime = 1234567890123L;
        client.writeAndFlush(BaseMessage.newBuilder()
                .setCode(C_S_PING_REQUEST)
                .setBody(PingRequest.newBuilder().setPingTime(pingTime).build().toByteString())
                .build()).sync();

        Thread.sleep(500);
        check("控制器方法被调用次数", TestController.count.get(), 1);
        check("  业务消息内容正确", TestController.msg.get() == null
                ? null : TestController.msg.get().getPingTime(), pingTime);
        check("  传入的用户即本连接会话", TestController.user.get() == session, true);
        check("  传入的 exp 正确", TestController.exp.get(), 7L);

        section("4. 未注册的消息码（不应断连）");
        client.writeAndFlush(BaseMessage.newBuilder()
                .setCode(0x7FFFFFFF)
                .setBody(PingRequest.getDefaultInstance().toByteString())
                .build()).sync();
        Thread.sleep(300);
        check("未知消息码后连接仍在", client.isActive(), true);
        check("  控制器未被误调用", TestController.count.get(), 1);

        section("5. 断开连接");
        client.close().sync();
        Thread.sleep(500);
        check("连接表已清理", serverHandler.getClientChannel(channelId), null);
        check("退出事件已发布", ExitListener.count.get(), 1);
        check("  事件携带的连接标识正确", ExitListener.connect.get(), channelId);
        if (session != null) {
            check("  会话已置为不在线", session.isOnline(), false);
        }

        clientGroup.shutdownGracefully();
        ctx.close();

        System.out.printf("%n========== 通过 %d / 失败 %d ==========%n", pass, fail);
        System.exit(fail > 0 ? 1 : 0);
    }

    // ==================== 工具 ====================

    /** 找一个空闲端口。 */
    static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    /**
     * 轮询等待连接建立，返回连接标识；超时返回 {@code null}。
     *
     * <p>连接表是 handler 的私有字段，这里改用 {@code UserContainer} 的连接索引探测——
     * {@code channelActive} 里两者是同步写入的。</p>
     *
     * @param handler 服务端处理器
     * @param timeout 超时时长
     * @param unit    时间单位
     * @return 连接标识
     */
    static String waitForConnection(TcpServerHandler handler, long timeout, TimeUnit unit)
            throws Exception {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            for (ServerUser u : UserContainer.getActiveServerUsers()) {
                // 既在用户容器里、又在 handler 连接表里，才算登记完成
                if (handler.getClientChannel(u.getConnect()) != null) {
                    return u.getConnect();
                }
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
            pass++;
            System.out.printf("  [通过] %-32s = %s%n", what, actual);
        } else {
            fail++;
            System.out.printf("  [失败] %-32s 期望=%s 实际=%s%n", what, expected, actual);
        }
    }

}
