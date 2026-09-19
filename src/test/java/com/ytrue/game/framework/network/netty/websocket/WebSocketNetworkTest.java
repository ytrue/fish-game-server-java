package com.ytrue.game.framework.network.netty.websocket;

import com.google.protobuf.Message;
import com.ytrue.game.framework.engine.config.ServerConfig;
import com.ytrue.game.framework.engine.container.UserContainer;
import com.ytrue.game.framework.engine.data.NetworkMsgType;
import com.ytrue.game.framework.engine.data.ServerUser;
import com.ytrue.game.framework.engine.register.AppHandlerRegister;
import com.ytrue.game.framework.engine.utils.SpringUtils;
import com.ytrue.game.framework.engine.wrapper.AppHandlerWrapper;
import com.ytrue.game.framework.network.event.NetExitEvent;
import com.ytrue.game.framework.network.proto.AppMessage.BaseMessage;
import com.ytrue.game.framework.network.proto.NetworkMessage.PingRequest;
import com.ytrue.game.framework.network.proto.NetworkMessage.PingResponse;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WebSocket 网络层端到端测试。
 *
 * <p>不依赖 JUnit，直接跑 {@code main} 即可（IDE 里点绿色箭头）。
 * 客户端用 JDK 自带的 {@link java.net.http.WebSocket}，无需额外依赖。</p>
 *
 * <p>重点验证三件事：</p>
 * <ol>
 *     <li>WebSocket 链路能跑通：握手 → 收帧 → 解出 BaseMessage → 反射分发到控制器；</li>
 *     <li>与 TCP <b>共用同一张路由表</b>——同一个 {@code @AppHandler} 方法两种传输都能调；</li>
 *     <li>服务端主动下发走的是 {@link WebSocketNetwork#sendMessageToClient} 的覆写实现
 *         （包成 BinaryWebSocketFrame），而不是基类那份（会抛类型不匹配）。</li>
 * </ol>
 *
 * @since 1.0.0
 */
public class WebSocketNetworkTest {

    /** 通过数。 */
    static int pass = 0;

    /** 失败数。 */
    static int fail = 0;

    /** 客户端 ping 请求消息码（NetworkMsgCode.C_S_PING_REQUEST）。 */
    static final int C_S_PING_REQUEST = 0x10000000;

    /** 服务端 ping 返回消息码（NetworkMsgCode.S_C_PING_RESPONSE）。 */
    static final int S_C_PING_RESPONSE = 0x20000000;

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

    /** 客户端：接收服务端下发的二进制帧。 */
    static class ClientListener implements WebSocket.Listener {

        /** 收到的完整消息（已按 last 标志拼装）。 */
        final BlockingQueue<byte[]> received = new LinkedBlockingQueue<>();

        /** 分片拼装缓冲。 */
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        /** 连接就绪信号。 */
        final CountDownLatch open = new CountDownLatch(1);

        /** 关闭信号。 */
        final CountDownLatch closed = new CountDownLatch(1);

        @Override
        public void onOpen(WebSocket webSocket) {
            open.countDown();
            // 必须调用：告诉客户端「我准备好再收一条消息了」
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] chunk = new byte[data.remaining()];
            data.get(chunk);
            buffer.write(chunk, 0, chunk.length);
            // last=true 表示这一条消息发完了（服务端一帧发完时首次回调就是 true）
            if (last) {
                received.add(buffer.toByteArray());
                buffer.reset();
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.countDown();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.out.println("       客户端收到错误: " + error);
        }
    }

    // ==================== 测试主体 ====================

    public static void main(String[] args) throws Exception {
        // 退出事件发布依赖 Spring 上下文
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
        register.getAppHandlerWrapperMap().put(
                C_S_PING_REQUEST, new AppHandlerWrapper(controller, null, taskMethod, 7L));

        WebSocketServerHandler serverHandler = new WebSocketServerHandler(register);

        ServerConfig config = new ServerConfig();
        config.getNetwork().getWebsocket().setPort(port);
        config.getNetwork().getWebsocket().setBossSize(1);
        config.getNetwork().getWebsocket().setWorkerSize(2);

        WebSocketNetwork network = new WebSocketNetwork(serverHandler, config);
        check("配置读取正确", config.getNetwork().getWebsocket().getPort(), port);

        section("1. 启动监听");
        network.open();

        section("2. WebSocket 握手");
        ClientListener listener = new ClientListener();
        HttpClient httpClient = HttpClient.newHttpClient();
        // 路径必须是 /ws，与 WebSocketChannelInitializer 里配置的一致
        WebSocket ws = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/ws"), listener)
                .join();
        listener.open.await(5, TimeUnit.SECONDS);
        check("握手完成", !ws.isOutputClosed(), true);

        String channelId = waitForConnection(serverHandler, 5, TimeUnit.SECONDS);
        check("服务端已登记该连接", channelId != null, true);
        System.out.println("       连接标识 = " + channelId);

        ServerUser session = UserContainer.getUserByConnect(channelId);
        check("用户容器中已建立会话", session != null, true);

        section("3. 客户端上报消息（帧 → BaseMessage → 反射分发）");
        long pingTime = 1234567890123L;
        byte[] upFrame = BaseMessage.newBuilder()
                .setCode(C_S_PING_REQUEST)
                .setBody(PingRequest.newBuilder().setPingTime(pingTime).build().toByteString())
                .build()
                .toByteArray();
        ws.sendBinary(ByteBuffer.wrap(upFrame), true).join();

        Thread.sleep(500);
        check("控制器方法被调用次数", TestController.count.get(), 1);
        check("  业务消息内容正确", TestController.msg.get() == null
                ? null : TestController.msg.get().getPingTime(), pingTime);
        check("  传入的用户即本连接会话", TestController.user.get() == session, true);
        check("  传入的 exp 正确", TestController.exp.get(), 7L);

        section("4. 服务端主动下发（覆写的 sendMessageToClient，包成 BinaryWebSocketFrame）");
        Message out = PingResponse.newBuilder().setPingTime(pingTime).build();
        network.sendMessageToClient(S_C_PING_RESPONSE, out, channelId, NetworkMsgType.BINARY);

        byte[] downFrame = listener.received.poll(5, TimeUnit.SECONDS);
        check("客户端收到二进制帧", downFrame != null, true);
        if (downFrame != null) {
            BaseMessage envelope = BaseMessage.parseFrom(downFrame);
            check("  信封消息码正确", envelope.getCode(), S_C_PING_RESPONSE);
            check("  解出业务消息内容正确",
                    PingResponse.parseFrom(envelope.getBody()).getPingTime(), pingTime);
        }

        section("5. 解不出信封的帧（应丢弃该帧，不断连接）");
        ws.sendBinary(ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5}), true).join();
        Thread.sleep(300);
        check("连接仍然存活", !ws.isOutputClosed(), true);
        check("  控制器未被误调用", TestController.count.get(), 1);

        section("6. 未注册的消息码（应不断连接）");
        byte[] unknown = BaseMessage.newBuilder()
                .setCode(0x7FFFFFFF)
                .setBody(PingRequest.getDefaultInstance().toByteString())
                .build()
                .toByteArray();
        ws.sendBinary(ByteBuffer.wrap(unknown), true).join();
        Thread.sleep(300);
        check("连接仍然存活", !ws.isOutputClosed(), true);
        check("  控制器未被误调用", TestController.count.get(), 1);

        section("7. 断开连接");
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
        listener.closed.await(3, TimeUnit.SECONDS);
        Thread.sleep(500);
        check("连接表已清理", serverHandler.getClientChannel(channelId), null);
        check("退出事件已发布", ExitListener.count.get(), 1);
        check("  事件携带的连接标识正确", ExitListener.connect.get(), channelId);
        if (session != null) {
            check("  会话已置为不在线", session.isOnline(), false);
        }

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
    static String waitForConnection(WebSocketServerHandler handler, long timeout, TimeUnit unit)
            throws Exception {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            for (ServerUser u : UserContainer.getActiveServerUsers()) {
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
            System.out.printf("  [通过] %-34s = %s%n", what, actual);
        } else {
            fail++;
            System.out.printf("  [失败] %-34s 期望=%s 实际=%s%n", what, expected, actual);
        }
    }

}
