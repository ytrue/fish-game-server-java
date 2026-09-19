package com.ytrue.game.framework.network.init;

import com.ytrue.game.framework.engine.config.ServerConfig;
import com.ytrue.game.framework.engine.register.AppHandlerRegister;
import com.ytrue.game.framework.engine.utils.SpringUtils;
import com.ytrue.game.framework.network.INetwork;
import com.ytrue.game.framework.network.netty.http.HttpNetwork;
import com.ytrue.game.framework.network.netty.http.HttpServerHandler;
import com.ytrue.game.framework.network.netty.tcp.TcpNetwork;
import com.ytrue.game.framework.network.netty.tcp.TcpServerHandler;
import com.ytrue.game.framework.network.netty.websocket.WebSocketNetwork;
import com.ytrue.game.framework.network.netty.websocket.WebSocketServerHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.net.ServerSocket;
import java.util.List;

/**
 * 网络启动器测试：验证三个监听能起得来、也能关得掉。
 *
 * <p>不依赖 JUnit，直接跑 {@code main} 即可（IDE 里点绿色箭头）。</p>
 *
 * <p>关停部分用「关闭 Spring 上下文」来触发 {@code @PreDestroy}——
 * 比在 Windows 上给 JVM 发 SIGTERM 可靠（MSYS 的 timeout 送不出可捕获的信号，
 * 关停钩子根本跑不到）。</p>
 *
 * @since 1.0.0
 */
public class NetworkInitTest {

    /** 通过数。 */
    static int pass = 0;

    /** 失败数。 */
    static int fail = 0;

    public static void main(String[] args) throws Exception {
        int tcpPort = freePort();
        int wsPort = freePort();
        int httpPort = freePort();
        int sparePort = freePort();   // 用作对照：从未被监听，端口应始终可绑定

        AppHandlerRegister register = new AppHandlerRegister();

        ServerConfig config = new ServerConfig();
        config.getNetwork().getTcpsocket().setPort(tcpPort);
        config.getNetwork().getTcpsocket().setBossSize(1);
        config.getNetwork().getTcpsocket().setWorkerSize(2);
        config.getNetwork().getWebsocket().setPort(wsPort);
        config.getNetwork().getWebsocket().setBossSize(1);
        config.getNetwork().getWebsocket().setWorkerSize(2);
        config.getNetwork().getHttp().setPort(httpPort);
        config.getNetwork().getHttp().setBossSize(1);
        config.getNetwork().getHttp().setWorkerSize(2);

        TcpNetwork tcpNetwork = new TcpNetwork(new TcpServerHandler(register), config);
        WebSocketNetwork wsNetwork =
                new WebSocketNetwork(new WebSocketServerHandler(register), config);
        HttpNetwork httpNetwork = new HttpNetwork(new HttpServerHandler(register), config);

        NetworkInit networkInit = new NetworkInit(
                List.of(tcpNetwork, wsNetwork, httpNetwork));

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SpringUtils.class);
        ctx.registerBean(TcpNetwork.BEAN_NAME, INetwork.class, () -> tcpNetwork);
        ctx.registerBean(WebSocketNetwork.BEAN_NAME, INetwork.class, () -> wsNetwork);
        ctx.registerBean(HttpNetwork.class.getName(), INetwork.class, () -> httpNetwork);
        // 必须把它也注册成 bean：@PreDestroy 只对容器管理的 bean 生效，
        // 直接 new 出来的对象关上下文时不会收到回调
        ctx.registerBean(NetworkInit.class, () -> networkInit);
        ctx.refresh();

        section("1. 启动前：所有端口都可绑定");
        check("TCP 端口空闲", isFree(tcpPort), true);
        check("WS  端口空闲", isFree(wsPort), true);
        check("HTTP 端口空闲", isFree(httpPort), true);

        section("2. 执行启动");
        networkInit.run(null);

        check("TCP 端口已被监听", isFree(tcpPort), false);
        check("WS  端口已被监听", isFree(wsPort), false);
        check("HTTP 端口已被监听", isFree(httpPort), false);
        check("  对照端口仍未占用", isFree(sparePort), true);

        section("3. 关闭上下文（触发 @PreDestroy）");
        // BaseNetwork.close() 内部有 shutdownGracefully 的静默期，单个网络约 4 秒
        ctx.close();

        check("TCP 端口已释放", isFree(tcpPort), true);
        check("WS  端口已释放", isFree(wsPort), true);
        check("HTTP 端口已释放", isFree(httpPort), true);

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
     * 判断端口是否空闲。
     *
     * <p>能绑定成功说明没有别人在监听；抛 {@link java.net.BindException} 说明已被占用。</p>
     *
     * @param port 端口号
     * @return 空闲返回 {@code true}
     */
    static boolean isFree(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
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
