package com.ytrue.game.framework.network.timer;

import com.ytrue.game.framework.engine.config.ServerConfig;
import com.ytrue.game.framework.engine.container.UserContainer;
import com.ytrue.game.framework.engine.data.ServerUser;
import com.ytrue.game.framework.engine.data.TransportType;
import com.ytrue.game.framework.engine.register.AppHandlerRegister;
import com.ytrue.game.framework.engine.utils.SpringUtils;
import com.ytrue.game.framework.network.INetwork;
import com.ytrue.game.framework.network.event.NetExitEvent;
import com.ytrue.game.framework.network.netty.tcp.TcpNetwork;
import com.ytrue.game.framework.network.netty.tcp.TcpServerHandler;
import com.ytrue.game.framework.network.proto.AppMessage.BaseMessage;
import com.ytrue.game.framework.network.proto.NetworkMessage.HeartBeatResponse;
import com.ytrue.game.framework.network.proto.NetworkMessage.NetworkMsgCode;
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

import java.net.ServerSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 心跳任务测试。
 *
 * <p>不依赖 JUnit，直接跑 {@code main} 即可（IDE 里点绿色箭头）。</p>
 *
 * <p>直接调用 {@link HeartBeatTask#checkHeartBeat()} 而非等调度触发——后者要等 5 秒且时机不定。
 * 被验证的是判定逻辑本身；{@code @Scheduled} 的装配由 Spring 负责，已在应用启动时验证过。</p>
 *
 * <p>超时阈值 60 秒没法真等，改为直接改会话的 {@code lastActiveTime} 把它推到过去。</p>
 *
 * @since 1.0.0
 */
public class HeartBeatTaskTest {

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

    /** 记录退出事件的发布次数。 */
    static class ExitListener implements ApplicationListener<NetExitEvent> {

        /** 发布次数。 */
        static final AtomicInteger count = new AtomicInteger();

        /** 最近一次事件的连接标识。 */
        static final AtomicReference<String> connect = new AtomicReference<>();

        @Override
        public void onApplicationEvent(NetExitEvent event) {
            count.incrementAndGet();
            connect.set(event.getUser().getConnect());
        }
    }

    // ==================== 测试主体 ====================

    public static void main(String[] args) throws Exception {
        int port = freePort();

        // ---- 装配：网络 + Spring 上下文（退出事件发布需要） ----
        AppHandlerRegister register = new AppHandlerRegister();
        ServerConfig config = new ServerConfig();
        config.getNetwork().getTcpsocket().setPort(port);
        config.getNetwork().getTcpsocket().setBossSize(1);
        config.getNetwork().getTcpsocket().setWorkerSize(2);

        TcpNetwork network = new TcpNetwork(new TcpServerHandler(register), config);

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SpringUtils.class);
        ctx.register(ExitListener.class);
        ctx.registerBean(TcpNetwork.BEAN_NAME, INetwork.class, () -> network);
        ctx.refresh();

        HeartBeatTask heartBeatTask = new HeartBeatTask();

        section("1. 启动监听并接入一个客户端");
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

        ServerUser user = waitForSession(5);
        check("服务端已登记会话", user != null, true);
        if (user == null) {
            System.exit(1);
        }

        section("2. 未登录的会话被跳过（不发心跳）");
        // online 默认为 false，代表「连上了但还没登录」
        check("会话当前未登录", user.isOnline(), false);
        user.setLastActiveTime(System.currentTimeMillis());
        heartBeatTask.checkHeartBeat();
        Thread.sleep(300);
        check("未登录会话未收到心跳", client.received.poll(), null);

        section("3. 活跃会话收到心跳");
        user.setOnline(true);
        user.setLastActiveTime(System.currentTimeMillis());
        heartBeatTask.checkHeartBeat();

        BaseMessage beat = client.received.poll(3, TimeUnit.SECONDS);
        check("收到心跳消息", beat != null, true);
        if (beat != null) {
            check("  消息码为心跳返回", beat.getCode(),
                    NetworkMsgCode.S_C_HEART_BEAT_RESPONSE_VALUE);
            check("  心跳消息体为空", HeartBeatResponse.parseFrom(beat.getBody())
                    .equals(HeartBeatResponse.getDefaultInstance()), true);
        }
        check("  连接仍然存活", channel.isActive(), true);

        section("4. 超时会话被踢下线");
        // 把最后活跃时间推到 61 秒前，模拟掉线
        user.setLastActiveTime(System.currentTimeMillis() - 61 * 1000);
        heartBeatTask.checkHeartBeat();
        Thread.sleep(800);

        check("会话已置为不在线", user.isOnline(), false);
        check("客户端连接已被关闭", channel.isActive(), false);

        section("5. 退出事件只发布一次");
        // 关键：心跳只负责关连接，退出事件由 channelInactive 统一发布。
        // 若心跳里再发一次，同一次掉线会触发两遍业务监听器
        check("退出事件发布次数", ExitListener.count.get(), 1);
        check("  事件携带的连接标识正确", ExitListener.connect.get(), user.getConnect());

        section("6. 已离线的会话不会被重复处理");
        heartBeatTask.checkHeartBeat();
        Thread.sleep(300);
        check("退出事件仍为 1 次（未重复触发）", ExitListener.count.get(), 1);

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

    /**
     * 轮询等待 TCP 会话建立，返回会话对象。
     *
     * @param seconds 超时秒数
     * @return 会话；超时返回 {@code null}
     */
    static ServerUser waitForSession(int seconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) {
            for (ServerUser u : UserContainer.getActiveServerUsers()) {
                if (u.getTransportType() == TransportType.TCP) {
                    return u;
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
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass++;
            System.out.printf("  [通过] %-34s%n", what);
        } else {
            fail++;
            System.out.printf("  [失败] %-34s 期望=%s 实际=%s%n", what, expected, actual);
        }
    }

}
