package com.ytrue.game.framework.network.netty.http;

import com.ytrue.game.framework.engine.config.ServerConfig;
import com.ytrue.game.framework.engine.data.NetworkMsgType;
import com.ytrue.game.framework.engine.register.AppHandlerRegister;
import com.ytrue.game.framework.engine.utils.SpringUtils;
import com.ytrue.game.framework.network.controller.NetworkGmController;
import com.ytrue.game.framework.network.proto.NetworkMessage.PingRequest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP 网络层端到端测试。
 *
 * <p>不依赖 JUnit，直接跑 {@code main} 即可（IDE 里点绿色箭头）。</p>
 *
 * <p>走的是完整链路，不做任何桩：</p>
 * <pre>
 * Spring 启动 → BaseHandlerRegister 扫描 @GmHandler 装配路由表
 *            → HttpNetwork 监听端口
 *            → 真实 HTTP 请求 → HttpServerHandler 解析 → 反射调用 NetworkGmController
 * </pre>
 *
 * @since 1.0.0
 */
public class HttpNetworkTest {

    /** 通过数。 */
    static int pass = 0;

    /** 失败数。 */
    static int fail = 0;

    public static void main(String[] args) throws Exception {
        // ---- 启动 Spring 上下文：让 BeanPostProcessor 扫描并装配 GM 路由表 ----
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SpringUtils.class);
        ctx.register(AppHandlerRegister.class);
        ctx.register(NetworkGmController.class);
        ctx.register(HttpServerHandler.class);
        ctx.refresh();

        AppHandlerRegister register = ctx.getBean(AppHandlerRegister.class);

        section("1. 路由表装配");
        check("已注册 /ping", register.getGmHandlerWrapperMap().containsKey("/ping"), true);
        System.out.println("       已注册的 GM 路径: " + register.getGmHandlerWrapperMap().keySet());

        // ---- 启动 HTTP 服务 ----
        int port = freePort();
        ServerConfig config = new ServerConfig();
        config.getNetwork().getHttp().setPort(port);
        config.getNetwork().getHttp().setBossSize(1);
        config.getNetwork().getHttp().setWorkerSize(2);

        HttpNetwork network = new HttpNetwork(ctx.getBean(HttpServerHandler.class), config);

        section("2. 启动 HTTP 监听");
        network.open();

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        String base = "http://127.0.0.1:" + port;

        section("3. GET 带参数");
        HttpResponse<String> r1 = get(client, base + "/ping?pingTime=1234567890123");
        check("状态码", r1.statusCode(), 200);
        check("  Content-Type", r1.headers().firstValue("Content-Type").orElse(""), "application/json; charset=UTF-8");
        check("  CORS 头", r1.headers().firstValue("Access-Control-Allow-Origin").orElse(""), "*");
        // 注意 pingTime 是「数字」而非字符串：GsonUtils 虽然设了 LongSerializationPolicy.STRING，
        // 但它同时注册了自定义的 LongAdapter，而自定义适配器优先级更高、序列化时写的是数字，
        // 把那个策略架空了。（GsonUtils 的类注释声称是字符串，与实际不符，待确认）
        check("  响应体", r1.body(), "{\"pingTime\":1234567890123}");

        section("4. GET 无参数");
        HttpResponse<String> r2 = get(client, base + "/ping");
        check("状态码", r2.statusCode(), 200);
        // 键不存在时 parseObject 返回 null，Gson 默认不序列化 null 字段，故为空对象
        check("  响应体", r2.body(), "{}");

        section("5. GET 未注册路径");
        HttpResponse<String> r3 = get(client, base + "/not-exist");
        check("状态码为 404", r3.statusCode(), 404);

        section("6. POST 带 JSON 请求体");
        HttpResponse<String> r4 = post(client, base + "/ping", "{\"pingTime\":9876543210}");
        check("状态码", r4.statusCode(), 200);
        check("  响应体", r4.body(), "{\"pingTime\":9876543210}");

        section("7. POST 请求体为空（按无参数处理，不报错）");
        HttpResponse<String> r5 = post(client, base + "/ping", "");
        check("状态码", r5.statusCode(), 200);
        check("  响应体", r5.body(), "{}");

        section("8. POST 请求体是非法 JSON（按无参数处理，不报错）");
        HttpResponse<String> r6 = post(client, base + "/ping", "这不是JSON");
        check("状态码", r6.statusCode(), 200);
        check("  响应体", r6.body(), "{}");

        section("9. 不支持的方法");
        HttpResponse<String> r7 = send(client, "PUT", base + "/ping", null);
        check("状态码为 405", r7.statusCode(), 405);

        section("10. 请求路径结尾带斜杠（应被归一化）");
        HttpResponse<String> r8 = get(client, base + "/ping/?pingTime=42");
        check("状态码", r8.statusCode(), 200);
        check("  响应体", r8.body(), "{\"pingTime\":42}");

        section("11. HTTP 不支持的能力（应记日志但绝不抛异常）");
        // 这几个是 BaseNetwork 的通用钩子，HTTP 无法真正实现。
        // 关键契约：调用方不该因为用了 HTTP 就崩掉，只是能力不生效而已
        try {
            network.sendMessageToClient(0x20000000, PingRequest.getDefaultInstance(),
                    "some-connect", NetworkMsgType.BINARY);
            ok("sendMessageToClient 未抛异常（消息被丢弃并记 warn）");
        } catch (Exception e) {
            bad("sendMessageToClient 抛了异常", e.toString());
        }
        try {
            network.closeClientConnect("some-connect");
            ok("closeClientConnect 未抛异常");
        } catch (Exception e) {
            bad("closeClientConnect 抛了异常", e.toString());
        }
        check("getClientChannel 返回 null", network.getClientChannel("some-connect"), null);
        check("getClientIp 返回空串", network.getClientIp("some-connect"), "");

        ctx.close();
        System.out.printf("%n========== 通过 %d / 失败 %d ==========%n", pass, fail);
        System.exit(fail > 0 ? 1 : 0);
    }

    // ==================== HTTP 客户端工具 ====================

    /** 发一个 GET 请求。 */
    static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return send(client, "GET", url, null);
    }

    /** 发一个 POST 请求。 */
    static HttpResponse<String> post(HttpClient client, String url, String body) throws Exception {
        return send(client, "POST", url, body);
    }

    /**
     * 发一个任意方法的请求。
     *
     * @param client 客户端
     * @param method HTTP 方法
     * @param url    完整地址
     * @param body   请求体，为 {@code null} 表示不带体
     * @return 响应
     */
    static HttpResponse<String> send(HttpClient client, String method, String url, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
        if (body != null) {
            builder.header("Content-Type", "application/json; charset=UTF-8");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
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
        if (String.valueOf(expected).equals(String.valueOf(actual))) {
            pass++;
            System.out.printf("  [通过] %-32s = %s%n", what, actual);
        } else {
            fail++;
            System.out.printf("  [失败] %-32s 期望=[%s] 实际=[%s]%n", what, expected, actual);
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
        System.out.println("  [失败] " + what + " -> " + detail);
    }

}
