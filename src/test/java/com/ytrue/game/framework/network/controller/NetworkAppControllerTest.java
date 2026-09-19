package com.ytrue.game.framework.network.controller;

import com.ytrue.game.framework.engine.config.ServerConfig;
import com.ytrue.game.framework.engine.register.AppHandlerRegister;
import com.ytrue.game.framework.engine.utils.SpringUtils;
import com.ytrue.game.framework.network.INetwork;
import com.ytrue.game.framework.network.netty.tcp.TcpNetwork;
import com.ytrue.game.framework.network.netty.tcp.TcpServerHandler;
import com.ytrue.game.framework.network.proto.AppMessage.BaseMessage;
import com.ytrue.game.framework.network.proto.NetworkMessage.NetworkMsgCode;
import com.ytrue.game.framework.network.proto.NetworkMessage.PingRequest;
import com.ytrue.game.framework.network.proto.NetworkMessage.PingResponse;
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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.net.ServerSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link NetworkAppController} 端到端测试。
 *
 * <p>不依赖 JUnit，直接跑 {@code main} 即可（IDE 里点绿色箭头）。</p>
 *
 * <p>走的是完整闭环，全程用真实组件、不打桩：</p>
 * <pre>
 * Spring 扫描 @AppController / @AppHandler    → 装配路由表
 *        ↓
 * 客户端经 TCP 发 C_S_PING_REQUEST
 *        ↓
 * TcpServerHandler 收消息 → 查路由表 → 调 checker → checker 转发给 doPingTask
 *        ↓
 * doPingTask 经 ClientSender 回 S_C_PING_RESPONSE
 *        ↓
 * 客户端收到，校验时间戳原样回传
 * </pre>
 *
 * <p>这一条链路同时验证了：注解扫描、路由装配、校验方法转发、ClientSender 发送。</p>
 *
 * @since 1.0.0
 */
public class NetworkAppControllerTest {

    /** 通过数。 */
    static int pass = 0;

    /** 失败数。 */
    static int fail = 0;

    /** 客户端：收取服务端下发的信封。 */
    static class ClientHandler extends ChannelInboundHandlerAdapter {

        /** 连接就绪信号。 */
        final CountDownLatch active = new CountDownLatch(1);

        /** 收到的信封。 */
        final BlockingQueue<BaseMessage> received = new LinkedBlockingQueue<>();

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            active.countDown();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            received.add((BaseMessage) msg);
        }
    }

    public static void main(String[] args) throws Exception {
        int port = freePort();

        // ---- 1. 先启动 Spring，让 BeanPostProcessor 扫描控制器装配路由表 ----
        //    必须「先拿到扫描好的 register，再拿它装配网络」。
        //    反过来（自己 new 一个 AppHandlerRegister 交给 handler）会踩坑：
        //    手工 new 出来的实例没经过 BeanPostProcessor，路由表是空的，
        //    于是断言查 Spring 那个有路由，handler 用的却是空表，问题被掩盖。
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SpringUtils.class);
        ctx.register(AppHandlerRegister.class);
        ctx.register(NetworkAppController.class);
        ctx.refresh();

        AppHandlerRegister register = ctx.getBean(AppHandlerRegister.class);

        // ---- 2. 用这个 register 装配网络，并把网络注册进容器（ClientSender 按名取它）----
        ServerConfig config = new ServerConfig();
        config.getNetwork().getTcpsocket().setPort(port);
        config.getNetwork().getTcpsocket().setBossSize(1);
        config.getNetwork().getTcpsocket().setWorkerSize(2);

        TcpNetwork network = new TcpNetwork(new TcpServerHandler(register), config);
        // 用 registerSingleton 而非 registerBean：此时上下文已 refresh，
        // 再注册 bean 定义不会被处理，直接登记单例更稳
        ctx.getBeanFactory().registerSingleton(TcpNetwork.BEAN_NAME, network);

        section("1. 注解扫描与路由装配");
        int pingCode = NetworkMsgCode.C_S_PING_REQUEST_VALUE;
        int beatCode = NetworkMsgCode.C_S_HEART_BEAT_REQUEST_VALUE;
        check("已注册 Ping 请求", register.getAppHandlerWrapperMap().containsKey(pingCode), true);
        check("已注册心跳请求", register.getAppHandlerWrapperMap().containsKey(beatCode), true);
        System.out.println("       已注册的消息码: " + register.getAppHandlerWrapperMap().keySet().stream()
                .map(c -> "0x" + Integer.toHexString(c)).toList());

        section("2. 启动监听并接入客户端");
        network.open();

        ClientHandler client = new ClientHandler();
        MultiThreadIoEventLoopGroup group =
                new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(2036334592, 0, 4, 0, 4))
                                .addLast(new ProtobufDecoder(BaseMessage.getDefaultInstance()))
                                .addLast(new LengthFieldPrepender(4))
                                .addLast(new ProtobufEncoder())
                                .addLast(client);
                    }
                });
        Channel channel = bootstrap.connect("127.0.0.1", port).sync().channel();
        client.active.await(5, TimeUnit.SECONDS);
        check("客户端已连接", channel.isActive(), true);
        Thread.sleep(200);   // 等服务端登记会话

        section("3. 发网络诊断请求，应原样回传时间戳");
        long pingTime = 1234567890123L;
        channel.writeAndFlush(BaseMessage.newBuilder()
                .setCode(pingCode)
                .setBody(PingRequest.newBuilder().setPingTime(pingTime).build().toByteString())
                .build()).sync();

        BaseMessage response = client.received.poll(5, TimeUnit.SECONDS);
        check("收到响应", response != null, true);
        if (response != null) {
            check("  消息码为 Ping 响应", response.getCode(),
                    NetworkMsgCode.S_C_PING_RESPONSE_VALUE);
            check("  时间戳原样回传",
                    PingResponse.parseFrom(response.getBody()).getPingTime(), pingTime);
        }

        section("4. 发心跳请求（处理方法是空实现，不应有响应、也不该断连）");
        channel.writeAndFlush(BaseMessage.newBuilder()
                .setCode(beatCode)
                .setBody(com.ytrue.game.framework.network.proto.NetworkMessage.HeartBeatRequest
                        .getDefaultInstance().toByteString())
                .build()).sync();
        Thread.sleep(500);
        check("心跳不应有立即响应", client.received.poll(), null);
        check("  连接仍然存活", channel.isActive(), true);

        section("5. 再发一次诊断，确认连接可继续复用");
        long secondPing = 9876543210L;
        channel.writeAndFlush(BaseMessage.newBuilder()
                .setCode(pingCode)
                .setBody(PingRequest.newBuilder().setPingTime(secondPing).build().toByteString())
                .build()).sync();
        BaseMessage second = client.received.poll(5, TimeUnit.SECONDS);
        check("收到第二次响应", second != null, true);
        if (second != null) {
            check("  时间戳正确",
                    PingResponse.parseFrom(second.getBody()).getPingTime(), secondPing);
        }

        group.shutdownGracefully();
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

    /** 打印分段标题。 */
    static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    /** 断言相等。 */
    static void check(String what, Object actual, Object expected) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass++;
            System.out.printf("  [通过] %-34s%n", what);
        } else {
            fail++;
            System.out.printf("  [失败] %-34s 期望=%s 实际=%s%n", what, expected, actual);
        }
    }

}
