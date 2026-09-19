package com.ytrue.game.framework.network;

import com.ytrue.game.framework.engine.config.ServerConfig;
import com.ytrue.game.framework.engine.container.UserContainer;
import com.ytrue.game.framework.engine.data.ServerUser;
import com.ytrue.game.framework.engine.data.TransportType;
import com.ytrue.game.framework.engine.register.AppHandlerRegister;
import com.ytrue.game.framework.engine.utils.SpringUtils;
import com.ytrue.game.framework.network.netty.tcp.TcpNetwork;
import com.ytrue.game.framework.network.netty.tcp.TcpServerHandler;
import com.ytrue.game.framework.network.netty.websocket.WebSocketNetwork;
import com.ytrue.game.framework.network.netty.websocket.WebSocketServerHandler;
import com.ytrue.game.framework.network.proto.AppMessage.BaseMessage;
import com.ytrue.game.framework.network.proto.NetworkMessage.HintMessageResponse;
import com.ytrue.game.framework.network.proto.NetworkMessage.NetworkMsgCode;
import com.ytrue.game.framework.network.proto.NetworkMessage.PingResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
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

import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link ClientSender} 的传输路由测试。
 *
 * <p>不依赖 JUnit，直接跑 {@code main} 即可（IDE 里点绿色箭头）。</p>
 *
 * <p>验证的核心是「同一个发送入口，按会话所属传输选对网络」：</p>
 * <pre>
 * 两个客户端同时接入 —— 一个走 TCP、一个走 WebSocket
 *        ↓
 * ClientSender.sendMessage(code, msg, tcpUser)  →  只有 TCP 客户端收到
 * ClientSender.sendMessage(code, msg, wsUser)   →  只有 WebSocket 客户端收到
 * </pre>
 *
 * <p>若不做传输路由（像旧工程那样写死 tcpSocketNetwork），发给 WebSocket 用户的消息
 * 会跑去 TCP 的连接表里查，查不到便静默丢弃——本测试就是防这个。</p>
 *
 * @since 1.0.0
 */
public class ClientSenderTest {

    /** 通过数。 */
    static int pass = 0;

    /** 失败数。 */
    static int fail = 0;

    /** 服务端 ping 返回消息码。 */
    static final int S_C_PING_RESPONSE = 0x20000000;

    // ==================== 客户端替身 ====================

    /** TCP 客户端：收取服务端下发的 BaseMessage。 */
    static class TcpClientHandler extends ChannelInboundHandlerAdapter {

        /** 连接就绪信号。 */
        final CountDownLatch active = new CountDownLatch(1);

        /** 收到的信封。 */
        final BlockingQueue<BaseMessage> received = new LinkedBlockingQueue<>();

        @Override
        public void channelActive(io.netty.channel.ChannelHandlerContext ctx) {
            active.countDown();
        }

        @Override
        public void channelRead(io.netty.channel.ChannelHandlerContext ctx, Object msg) {
            received.add((BaseMessage) msg);
        }
    }

    /** WebSocket 客户端：收取服务端下发的二进制帧。 */
    static class WsClientListener implements WebSocket.Listener {

        /** 收到的完整消息。 */
        final BlockingQueue<byte[]> received = new LinkedBlockingQueue<>();

        /** 分片拼装缓冲。 */
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        /** 连接就绪信号。 */
        final CountDownLatch open = new CountDownLatch(1);

        @Override
        public void onOpen(WebSocket webSocket) {
            open.countDown();
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] chunk = new byte[data.remaining()];
            data.get(chunk);
            buffer.write(chunk, 0, chunk.length);
            if (last) {
                received.add(buffer.toByteArray());
                buffer.reset();
            }
            webSocket.request(1);
            return null;
        }
    }

    // ==================== 测试主体 ====================

    public static void main(String[] args) throws Exception {
        int tcpPort = freePort();
        int wsPort = freePort();

        // ---- 装配两条网络，并按固定的 bean 名注册进容器（ClientSender 靠名字找它们） ----
        AppHandlerRegister register = new AppHandlerRegister();

        ServerConfig config = new ServerConfig();
        config.getNetwork().getTcpsocket().setPort(tcpPort);
        config.getNetwork().getTcpsocket().setBossSize(1);
        config.getNetwork().getTcpsocket().setWorkerSize(2);
        config.getNetwork().getWebsocket().setPort(wsPort);
        config.getNetwork().getWebsocket().setBossSize(1);
        config.getNetwork().getWebsocket().setWorkerSize(2);

        TcpNetwork tcpNetwork = new TcpNetwork(new TcpServerHandler(register), config);
        WebSocketNetwork wsNetwork =
                new WebSocketNetwork(new WebSocketServerHandler(register), config);

        // SpringUtils 需要上下文才能工作；两条网络按 BEAN_NAME 注册
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SpringUtils.class);
        ctx.registerBean(TcpNetwork.BEAN_NAME, INetwork.class, () -> tcpNetwork);
        ctx.registerBean(WebSocketNetwork.BEAN_NAME, INetwork.class, () -> wsNetwork);
        ctx.refresh();

        section("1. 启动两条网络");
        tcpNetwork.open();
        wsNetwork.open();

        section("2. 两个客户端分别接入");
        // TCP 客户端
        TcpClientHandler tcpClient = new TcpClientHandler();
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
                                .addLast(tcpClient);
                    }
                });
        Channel tcpChannel = bootstrap.connect("127.0.0.1", tcpPort).sync().channel();
        tcpClient.active.await(5, TimeUnit.SECONDS);
        check("TCP 客户端已连接", tcpChannel.isActive(), true);

        // WebSocket 客户端
        WsClientListener wsClient = new WsClientListener();
        WebSocket ws = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create("ws://127.0.0.1:" + wsPort + "/ws"), wsClient)
                .join();
        wsClient.open.await(5, TimeUnit.SECONDS);
        check("WebSocket 客户端已连接", !ws.isOutputClosed(), true);

        String tcpChannelId = waitFor(TransportType.TCP, 5);
        String wsChannelId = waitFor(TransportType.WEBSOCKET, 5);
        check("服务端登记了 TCP 会话", tcpChannelId != null, true);
        check("服务端登记了 WebSocket 会话", wsChannelId != null, true);

        ServerUser tcpUser = UserContainer.getUserByConnect(tcpChannelId);
        ServerUser wsUser = UserContainer.getUserByConnect(wsChannelId);

        section("3. 会话是否正确打上了传输标记");
        check("TCP 会话标记为 TCP", tcpUser == null ? null : tcpUser.getTransportType(), TransportType.TCP);
        check("WS  会话标记为 WEBSOCKET", wsUser == null ? null : wsUser.getTransportType(), TransportType.WEBSOCKET);

        section("4. 按会话路由：发给 TCP 用户");
        long pingTime = 1234567890123L;
        ClientSender.sendMessage(S_C_PING_RESPONSE,
                PingResponse.newBuilder().setPingTime(pingTime).build(), tcpUser);

        BaseMessage gotByTcp = tcpClient.received.poll(3, TimeUnit.SECONDS);
        check("TCP 客户端收到消息", gotByTcp != null, true);
        if (gotByTcp != null) {
            check("  消息码正确", gotByTcp.getCode(), S_C_PING_RESPONSE);
            check("  内容正确", PingResponse.parseFrom(gotByTcp.getBody()).getPingTime(), pingTime);
        }
        // 关键：另一个传输不该收到
        byte[] leakedToWs = wsClient.received.poll(500, TimeUnit.MILLISECONDS);
        check("WebSocket 客户端未收到（未串台）", leakedToWs, null);

        section("5. 按会话路由：发给 WebSocket 用户");
        long wsPingTime = 9876543210L;
        ClientSender.sendMessage(S_C_PING_RESPONSE,
                PingResponse.newBuilder().setPingTime(wsPingTime).build(), wsUser);

        byte[] gotByWs = wsClient.received.poll(3, TimeUnit.SECONDS);
        check("WebSocket 客户端收到消息", gotByWs != null, true);
        if (gotByWs != null) {
            BaseMessage envelope = BaseMessage.parseFrom(gotByWs);
            check("  消息码正确", envelope.getCode(), S_C_PING_RESPONSE);
            check("  内容正确", PingResponse.parseFrom(envelope.getBody()).getPingTime(), wsPingTime);
        }
        BaseMessage leakedToTcp = tcpClient.received.poll(500, TimeUnit.MILLISECONDS);
        check("TCP 客户端未收到（未串台）", leakedToTcp, null);

        section("6. 提示框消息也按会话路由");
        ClientSender.sendHintMessage("金币不足", wsUser);
        byte[] hintFrame = wsClient.received.poll(3, TimeUnit.SECONDS);
        check("WebSocket 客户端收到提示框", hintFrame != null, true);
        if (hintFrame != null) {
            BaseMessage envelope = BaseMessage.parseFrom(hintFrame);
            check("  消息码为提示框", envelope.getCode(),
                    NetworkMsgCode.S_C_HINT_MESSAGE_RESPONSE_VALUE);
            HintMessageResponse hint = HintMessageResponse.parseFrom(envelope.getBody());
            check("  内容正确", hint.getContent(), "金币不足");
            check("  级别为提示(0)", hint.getLevel(), 0);
        }

        section("7. 取 IP");
        check("TCP 用户 IP", ClientSender.getIpAddress(tcpUser), "127.0.0.1");
        check("WS  用户 IP", ClientSender.getIpAddress(wsUser), "127.0.0.1");

        section("8. 关闭连接");
        ClientSender.closeClientConnect(wsUser);
        Thread.sleep(500);
        check("WS 会话已置为不在线", wsUser.isOnline(), false);
        // 连接已从 WebSocket 的连接表移除，再查 IP 应返回空串
        check("WS 连接表已清理", wsNetwork.getClientIp(wsChannelId), "");

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
     * 轮询等待指定传输的会话建立，返回连接标识。
     *
     * @param transport 目标传输类型
     * @param seconds   超时秒数
     * @return 连接标识；超时返回 {@code null}
     */
    static String waitFor(TransportType transport, int seconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) {
            for (ServerUser u : UserContainer.getActiveServerUsers()) {
                if (transport == u.getTransportType()) {
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
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass++;
            System.out.printf("  [通过] %-34s%n", what);
        } else {
            fail++;
            System.out.printf("  [失败] %-34s 期望=%s 实际=%s%n", what, expected, actual);
        }
    }

}
