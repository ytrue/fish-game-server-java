package com.ytrue.game.framework.network.netty.http;

import com.ytrue.game.framework.engine.register.AppHandlerRegister;
import com.ytrue.game.framework.engine.utils.GsonUtils;
import com.ytrue.game.framework.engine.wrapper.GmHandlerWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * HTTP 请求处理器（后台 GM 接口）。
 *
 * <p>处理流程：</p>
 * <pre>
 * 取真实 IP → 解析 URI 与查询串 → 按路径查 GM 路由表 → 反射调用处理方法 → 返回 JSON
 * </pre>
 *
 * <p><b>完整例子</b>——后台页面发起一次网络诊断：</p>
 * <pre>
 * ① 客户端请求
 *      GET /ping?pingTime=1234567890123 HTTP/1.1
 *
 * ② 本类解析（channelRead0）
 *      ip         = "192.168.1.50"
 *      path       = "/ping"                        ← 路由键
 *      queryStr   = "pingTime=1234567890123"
 *
 * ③ 查路由表命中（handleGet）
 *      GmHandlerWrapper{
 *          bean        = NetworkGmController 实例
 *          checkMethod = NetworkGmController.checker(Method, Map)
 *          taskMethod  = NetworkGmController.doPingTask(Map, Map)
 *      }
 *
 * ④ 反射调用（invoke）
 *      ctrl.checker(ctrl.doPingTask, {pingTime="1234567890123"})
 *        └─ 内部：Map resultMap = new HashMap();
 *                 doPingTask.invoke(ctrl, param, resultMap);
 *                 return GsonUtils.toJson(resultMap);
 *
 * ⑤ 响应
 *      HTTP/1.1 200 OK
 *      content-type: application/json; charset=UTF-8
 *
 *      {"pingTime":1234567890123}
 * </pre>
 *
 * <p>约定：GM 处理方法的签名固定为
 * {@code (Map&lt;String, Object&gt; param, Map&lt;String, Object&gt; resultMap)}——
 * 入参从第一个 Map 里取、出参往第二个 Map 里塞，而不是用返回值。
 * 由控制器上的「校验方法」负责调用它并序列化成 JSON 字符串
 * （见 {@code NetworkGmController.checker}）。</p>
 *
 * <p>本类被 {@link Sharable} 标注，是全局唯一实例，自身不持有任何连接状态，
 * 故可被所有连接共享。</p>
 *
 * <p><b>日志规范</b>——统一用 {@code GM - } 前缀（{@code netty http - } 之类会把传输层细节
 * 混进来），统一格式为
 * {@code GM - <发生了什么>:<方法 路径> from <来源IP>，<结果或原因>}，
 * 级别按「这件事需不需要人管」划分：</p>
 * <table border="1">
 *     <caption>各场景的日志内容与级别</caption>
 *     <tr><th>场景</th><th>级别</th><th>实际输出示例</th></tr>
 *     <tr><td>路径未注册（404）</td><td>{@code warn}</td>
 *         <td>{@code GM - 未注册的接口:[GET /not-exist] from 127.0.0.1，已返回 404}</td></tr>
 *     <tr><td>业务处理异常</td><td>{@code error}</td>
 *         <td>{@code GM - 接口处理异常:[POST /draw] from 192.168.1.50，原因: NumberFormatException: ...}</td></tr>
 *     <tr><td>连接级异常</td><td>{@code warn}</td>
 *         <td>{@code GM - 连接异常，已关闭连接:[3a6ee7b3] from 192.168.1.50，原因: IOException: ...}</td></tr>
 *     <tr><td>请求体内容 / 解析失败</td><td>{@code debug}</td>
 *         <td>{@code GM - 请求体:[POST /ping] from 127.0.0.1，内容: {"pingTime":123}}</td></tr>
 * </table>
 *
 * <p>两点取舍：</p>
 * <ul>
 *     <li><b>记「接口」不记「消息」</b>——这里定位的是 HTTP 路径，不是某条业务消息；
 *         带上 HTTP 方法是因为 {@code GET /ping} 与 {@code POST /ping} 虽走同一个处理方法，
 *         排查时却需要知道对方究竟怎么调的</li>
 *     <li><b>日志始终带来源 IP</b>——否则只知道「有个请求出错了」，不知道是后台前端在调
 *         还是外界在探测</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@Sharable
@Component
@RequiredArgsConstructor
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    /**
     * GM 路由表（由框架启动时扫描 {@code @GmHandler} 装配）。
     */
    private final AppHandlerRegister register;

    /**
     * 浏览器 / 爬虫会自动请求、但与业务无关的路径。
     *
     * <p>用任何浏览器打开后台地址，它都会顺带请求 {@code /favicon.ico}；
     * 爬虫会请求 {@code /robots.txt}。这类请求落到 GM 接口上，只会产生
     * 「未注册的接口」告警，把真正的路径错误淹掉，所以单独识别、静默处理。</p>
     */
    private static final Set<String> IGNORED_PATHS = Set.of("/favicon.ico", "/robots.txt");

    /**
     * 处理一次完整的 HTTP 请求。
     *
     * <p>以 {@code GET /ping?pingTime=1234567890123 HTTP/1.1} 为例，各步骤变量的值：</p>
     * <pre>
     * msg.uri()   = "/ping?pingTime=1234567890123"
     * msg.method()= GET
     * ip          = "192.168.1.50"（取自 x-forwarded-for，没有则取 TCP 对端地址）
     * uri         = "/ping?pingTime=1234567890123"   ← URL 解码后（本例无转义，不变）
     * queryIndex  = 5                                ← '?' 的下标
     * path        = "/ping"                          ← 路由键
     * queryStr    = "pingTime=1234567890123"
     * </pre>
     *
     * <p>随后按 {@code path} 查 GM 路由表，命中 {@code NetworkGmController.doPingTask}，
     * 返回 JSON 响应体。</p>
     *
     * @param ctx 通道上下文
     * @param msg 聚合后的完整请求（由 {@link HttpChannelInitializer} 装配的 HttpObjectAggregator 产出）
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        // ① 解码失败的请求直接拒掉（协议格式错误、超长等）
        //    decoderResult() 由上游的 HttpRequestDecoder 写入，标记这次解码是否成功
        if (!msg.decoderResult().isSuccess()) {
            // 返回 400
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        // ② 取真实客户端 IP。经 Nginx 等反向代理时真实 IP 在 x-forwarded-for 头里，
        //    取不到才回退到 TCP 对端地址。
        //    例：直连时头不存在 -> hasText 为 false -> 走回退分支取到 "127.0.0.1"
        //        经代理时头可能是 "192.168.1.50" 或 "unknown"（部分代理会填字面量 unknown）
        String ip = msg.headers().get("x-forwarded-for");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = remoteIp(ctx);
        }
        if ("unknown".equals(ip)) {
            // 拿不到来源地址，拒绝服务。注：当前未把 ip 传给业务方法，此处仅作准入判断
            // 返回 403
            sendError(ctx, HttpResponseStatus.FORBIDDEN);
            return;
        }

        // ③ 解析 URI
        //    URL 解码：把 %E4%B8%AD 这类转义还原成中文。
        //    例：路径里有中文参数时 "/gm?name=%E5%BC%A0%E4%B8%89" 解码后为 "/gm?name=张三"
        String uri = URLDecoder.decode(msg.uri(), StandardCharsets.UTF_8);

        //    查询串起始位置（取第一个 '?'），-1 表示无参数
        //    例："/ping?pingTime=1" -> 5；"/ping" -> -1
        int queryIndex = uri.indexOf('?');

        //    路由键 = 去掉查询串的那部分。
        //    注意顺序：必须先剥离查询串、再处理结尾斜杠。
        //    反过来的话 "/ping/?a=1" 这种 URI 不以 '/' 结尾（结尾是 '1'），
        //    斜杠剥不掉，路由键就成了 "/ping/"，查路由表必然落空。
        String path = queryIndex < 0 ? uri : uri.substring(0, queryIndex);
        //    去掉结尾多余的斜杠；根路径 "/" 要保留，不能剥成空串
        //    例："/ping/" -> "/ping"；"/" -> 仍是 "/"（因为长度不大于 1）
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // ④ 浏览器 / 爬虫的自动请求：静默处理掉再返回。
        //    不这么做的话，每次用浏览器打开后台地址都会多出一条「未注册的接口」告警，
        //    真正的路径错误反而被淹没。回 204（无内容）并只在 debug 级别留痕
        if (IGNORED_PATHS.contains(path)) {
            sendNoContent(ctx);
            log.debug("GM - 忽略浏览器自动请求:[{} {}] from {}", msg.method().name(), path, ip);
            return;
        }

        // ⑤ 按请求方法分派。注意用 equals 而不是 ==：
        //    HttpMethod 的常量是单例，但客户端传来的 method() 可能是新构造的实例，
        //    用 == 会漏判
        // 请求方法名仅用于日志：GET /ping 与 POST /ping 走的是同一个处理方法，
        // 但排查问题时需要知道到底是谁调的哪条路径
        String methodName = msg.method().name();

        String result;
        if (HttpMethod.GET.equals(msg.method())) {
            // GET 的参数在查询串里
            String queryStr = queryIndex < 0 ? "" : uri.substring(queryIndex + 1);
            result = handleGet(ctx, path, queryStr, ip, methodName);
        } else if (HttpMethod.POST.equals(msg.method())) {
            // POST 的参数在请求体里
            result = handlePost(ctx, msg, path, ip, methodName);
        } else {
            // PUT / DELETE 等未开放（CORS 头里虽然允许，但服务端不处理）
            // 405
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        // ⑥ 写出响应。result 为 null 说明处理过程已经自行发过响应（如 404），无需再写
        if (result != null) {
            sendJson(ctx, result);
        }
        // 注：msg 由父类 SimpleChannelInboundHandler 在本方法返回后自动释放，
        //     这里不需要（也不能）手动 release
    }

    /**
     * 处理 GET 请求：参数写在 URL 查询串里。
     *
     * <p>以 {@code GET /ping?pingTime=1234567890123&uid=100001} 为例，
     * 传进本方法时各变量的值：</p>
     * <pre>
     * routeKey = "/ping"                                  ← 问号之前的部分（结尾斜杠已在上游剥掉）
     * queryStr = "pingTime=1234567890123&amp;uid=100001"      ← 问号之后的部分
     *
     * 解析后 paramMap = {
     *     pingTime = "1234567890123",   ← 注意！键值都是「字符串」，不是数字
     *     uid      = "100001"
     * }
     * </pre>
     *
     * <p>因此业务侧取值必须走 {@code JsonMapUtils.parseObject(paramMap, "pingTime", TYPE_LONG)}，
     * 它会把字符串解析成 Long；直接 {@code (Long) paramMap.get("pingTime")} 会抛
     * {@link ClassCastException}（字符串转不了 Long）。</p>
     *
     * @param ctx        通道上下文
     * @param routeKey   已归一化的路由键（不含查询串与结尾斜杠），用于查路由表
     * @param queryStr   查询串（不含问号），无参数时为空串
     * @param ip         客户端 IP（仅用于日志，便于定位是谁在调用）
     * @param methodName HTTP 方法名（仅用于日志，区分 GET / POST）
     * @return 响应 JSON 字符串；已自行发送错误响应时返回 {@code null}
     */
    private String handleGet(ChannelHandlerContext ctx, String routeKey, String queryStr,
                             String ip, String methodName) {
        // 按后台方法的签名约定，参数要打包成 Map 传进去（而不是按位置传参）
        Map<String, Object> paramMap = new HashMap<>();
        // 没有查询串（如 GET /ping）时 paramMap 保持为空，由业务方法自己校验必填项
        if (!queryStr.isEmpty()) {
            // 查询串形如 "pingTime=123&uid=100001"，先按 & 拆成一个个键值对
            for (String kv : queryStr.split("&")) {
                // 再按 = 拆成「参数名」与「参数值」
                String[] kav = kv.split("=");
                // 只接受完整的 key=value 两段形式；
                // 残缺片段（如 "&&"、"=x" 这类）直接忽略，避免解析出无意义的键
                if (kav.length == 2) {
                    paramMap.put(kav[0], kav[1]);
                }
            }
        }

        // 拿归一化后的路由键查 GM 路由表。
        // 例："/ping" -> GmHandlerWrapper{
        //        bean        = NetworkGmController 实例
        //        checkMethod = NetworkGmController.checker(Method, Map)
        //        taskMethod  = NetworkGmController.doPingTask(Map, Map)
        //     }
        GmHandlerWrapper wrapper = register.getGmHandlerWrapperMap().get(routeKey);
        if (wrapper == null) {
            // 路径没注册过：回 404，并在这里就把响应发出去。
            // 返回 null 是给调用方的信号——「我已经回过响应了，你不用再写」
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            // 说「接口」而不是「消息」：这里找不到的是 HTTP 路径，不是某条业务消息。
            // 用 warn 而非 info：GM 是内网接口，出现未注册路径说明前端调错了，
            // 或是有人在扫接口——两种情况都值得关注。带上方法与 IP 便于区分
            log.warn("GM - 未注册的接口:[{} {}] from {}，已返回 404", methodName, routeKey, ip);
            return null;
        }

        try {
            // 反射调用，拿到 JSON 响应体
            return invoke(wrapper, paramMap);
        } catch (Exception e) {
            // 业务方法内部抛异常：不让连接挂掉，而是把错误信息作为响应体返回，
            // 后台页面能直接看到出错原因。
            // 日志里带上「方法 路径」与来源 IP，否则只看到一句异常，
            // 不知道是哪个接口、谁调的
            log.error("GM - 接口处理异常:[{} {}] from {}，原因: {}",
                    methodName, routeKey, ip, e.getMessage(), e);
            return errorResult(e);
        }
    }

    /**
     * 处理 POST 请求：参数写在请求体里（JSON 格式）。
     *
     * <p>以 {@code POST /ping}、请求体 {@code {"pingTime":1234567890123}} 为例：</p>
     * <pre>
     * routeKey = "/ping"
     * jsonStr  = "{\"pingTime\":1234567890123}"
     *
     * 解析后 paramMap = {
     *     pingTime = 1.234567890123E12    ← 注意！这里是 Double，不是字符串
     * }
     * </pre>
     *
     * <p><b>GET 与 POST 的参数值类型并不相同</b>，这是很容易踩的一点：</p>
     * <table border="1">
     *     <caption>同一份参数经两种方式传入后的实际类型</caption>
     *     <tr><th>请求方式</th><th>参数来源</th><th>{@code pingTime} 的实际类型</th></tr>
     *     <tr><td>GET</td><td>URL 查询串</td><td>{@code String "1234567890123"}</td></tr>
     *     <tr><td>POST</td><td>JSON 请求体</td><td>{@code Double 1.234567890123E12}</td></tr>
     * </table>
     *
     * <p>好在 {@code JsonMapUtils.parseObject} 内部分别处理了 String 与 Double 两个分支，
     * 业务侧统一用它取值即可，无需关心请求是 GET 还是 POST。</p>
     *
     * @param ctx        通道上下文
     * @param msg        完整请求
     * @param routeKey   已归一化的路由键（不含查询串与结尾斜杠），用于查路由表
     * @param ip         客户端 IP（仅用于日志，便于定位是谁在调用）
     * @param methodName HTTP 方法名（仅用于日志，区分 GET / POST）
     * @return 响应 JSON 字符串；已自行发送错误响应时返回 {@code null}
     */
    private String handlePost(ChannelHandlerContext ctx, FullHttpRequest msg, String routeKey,
                              String ip, String methodName) {
        // 先查路由表，路径没注册就直接 404 返回，不必浪费力气解析请求体
        GmHandlerWrapper wrapper = register.getGmHandlerWrapperMap().get(routeKey);
        if (wrapper == null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            // 与 GET 分支保持同样的级别与措辞，避免同一件事在两条路径上表现不一致
            log.warn("GM - 未注册的接口:[{} {}] from {}，已返回 404", methodName, routeKey, ip);
            return null;
        }

        Map<String, Object> paramMap = new HashMap<>();

        // 取出请求体。上游的 HttpObjectAggregator 已经把分片聚合过，
        // 所以这里 content() 拿到的是完整内容，不会是半截
        ByteBuf content = msg.content();
        // 按 UTF-8 解码成字符串（请求头里声明的是 application/json; charset=UTF-8）
        String jsonStr = content.toString(CharsetUtil.UTF_8);

        try {
            // JSON 字符串 -> Map。
            // 解析用的 Gson 会把所有数字统一解析成 Double（JSON 规范不区分整数与浮点，
            // Gson 默认一律按 Double 处理），所以取值时务必走 JsonMapUtils.parseObject 转换
            Map<String, Object> parsed = GsonUtils.fromJsonToMap(jsonStr);
            if (parsed != null) {
                paramMap.putAll(parsed);
            }
            // 请求体只在 debug 级别打印：GM 后台改的是玩家数据，
            // 全量打出来既有体积问题、也可能把敏感字段写进日志文件。
            // 排查时临时开 debug 即可看到。带上接口路径，避免并发请求的日志串在一起分不清
            log.debug("GM - 请求体:[{} {}] from {}，内容: {}", methodName, routeKey, ip, jsonStr);
        } catch (Exception e) {
            // 请求体为空串、或不是合法 JSON 时，按「无参数」继续往下走。
            // 不在这里直接报错，是为了把「参数缺失」的判定交给业务方法——
            // 它更清楚哪些参数是必填的，能给出比「JSON 格式错误」更有用的提示。
            // 因此这里用 debug：空 body 是常见且合法的情况，不该在 info 级别刷屏。
            // 日志里带上原始内容，否则光看「解析失败」不知道失败的输入长什么样
            log.debug("GM - 请求体解析失败:[{} {}] from {}，原内容: [{}]，按无参数继续。原因: {}",
                    methodName, routeKey, ip, jsonStr, e.getMessage());
        }

        try {
            // 反射调用，拿到 JSON 响应体
            return invoke(wrapper, paramMap);
        } catch (Exception e) {
            // 同 GET：业务异常转成响应内容返回，不中断连接。
            // 日志格式也与 GET 分支保持一致
            log.error("GM - 接口处理异常:[{} {}] from {}，原因: {}",
                    methodName, routeKey, ip, e.getMessage(), e);
            return errorResult(e);
        }
    }

    /**
     * 反射调用 GM 处理方法。
     *
     * <p><b>为什么参数是「方法」而不是「参数」</b>：GM 的调用方式由控制器上的校验方法决定，
     * 所以框架不直接调处理方法，而是把「处理方法本身」当成一个参数交给校验方法，由它决定
     * 怎么调（可以先做权限校验再调、也可以用不同的参数组合调）。</p>
     *
     * <p>以命中 {@code /ping} 为例，本方法执行的就是：</p>
     * <pre>
     * wrapper.checkMethod() = NetworkGmController.checker(Method, Map)
     * wrapper.bean()        = NetworkGmController 实例（记为 ctrl）
     * wrapper.taskMethod()  = NetworkGmController.doPingTask(Map, Map)
     * paramMap              = { pingTime = "1234567890123" }
     *
     * 实际调用：ctrl.checker(ctrl.doPingTask, paramMap)
     *
     * 而 checker 的内部实现是：
     *     Map&lt;String, Object&gt; resultMap = new HashMap&lt;&gt;();
     *     taskMethod.invoke(this, param, resultMap);   // 调 doPingTask(paramMap, resultMap)
     *     return GsonUtils.toJson(resultMap);
     * 最终返回：{"pingTime":1234567890123}
     * </pre>
     *
     * @param wrapper  命中的处理方法包装器
     * @param paramMap 请求参数
     * @return 响应 JSON 字符串
     * @throws Exception 反射调用失败或处理方法内部抛出异常
     */
    private String invoke(GmHandlerWrapper wrapper, Map<String, Object> paramMap) throws Exception {
        if (wrapper.checkMethod() == null) {
            // GM 的校验方法同时承担「调用任务方法并产出 JSON」的职责，缺了它就没有响应可返回。
            // 通常是控制器忘了写 @GmController(checkMethod) 指定的那个方法。
            // 这里给明确报错，避免直接抛一个难以定位的空指针。
            throw new IllegalStateException("GM 控制器缺少校验方法，无法处理请求: " + wrapper.bean().getClass().getSimpleName() + "#" + wrapper.taskMethod().getName());
        }
        // invoke(目标对象, 参数1, 参数2...)：
        //   目标对象  —— checker 是实例方法，所以要传控制器实例
        //   后两个参数 —— 依次对应 checker(Method taskMethod, Map param) 的形参
        // 返回值强转 String：约定校验方法返回的就是 JSON 字符串
        return (String) wrapper.checkMethod().invoke(wrapper.bean(), wrapper.taskMethod(), paramMap);
    }

    /**
     * 构造异常情况下的响应内容。
     *
     * <p>把异常信息与堆栈一并返回，便于后台页面直接看到出错原因。注意这会把内部细节
     * 暴露给调用方，仅适用于内网后台接口。</p>
     *
     * <p>输出示例：</p>
     * <pre>
     * For input string: "abc" --- [{"className":"java.lang.NumberFormatException","methodName":"forInputString","lineNumber":65,...}]
     * </pre>
     *
     * @param e 处理过程中抛出的异常
     * @return 描述异常的 JSON 字符串
     */
    private String errorResult(Exception e) {
        // 异常消息 + 堆栈 JSON，便于排查；e.getMessage() 可能为 null，此时响应以 "null --- " 开头
        return e.getMessage() + " --- " + GsonUtils.toJson(e.getStackTrace());
    }

    /**
     * 写回一个 JSON 响应并关闭连接。
     *
     * <p>以 {@code result = "{\"pingTime\":1234567890123}"} 为例，最终发到客户端的是：</p>
     * <pre>
     * HTTP/1.1 200 OK
     * content-type: application/json; charset=UTF-8
     * access-control-allow-origin: *
     * access-control-allow-headers: Origin, X-Requested-With, Content-Type, Accept
     * access-control-allow-methods: GET, POST, PUT, DELETE
     * content-length: 26
     *
     * {"pingTime":1234567890123}
     * </pre>
     *
     * @param ctx    通道上下文
     * @param result 响应体
     */
    private static void sendJson(ChannelHandlerContext ctx, String result) {
        // 构造一个「完整」的 HTTP 响应：状态行 + 头 + 体一次性准备好
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        // 显式用 UTF-8 编码：getBytes() 走平台默认字符集，在 GBK 环境下会把中文写坏
        response.content().writeBytes(result.getBytes(StandardCharsets.UTF_8));

        // 规定返回值为 json 字符串
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        // 允许跨域访问（后台页面与接口域名不同时需要）
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Origin, X-Requested-With, Content-Type, Accept");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE");

        // 必须设置 Content-Length：不设置的话客户端要靠连接关闭来判断响应结束，
        // 且无法复用连接。readableBytes() 就是响应体的字节数（中文按 UTF-8 算 3 字节/字）
        HttpUtil.setContentLength(response, response.content().readableBytes());
        // writeAndFlush = 写入待发送队列 + 立即冲刷出去
        // 挂 CLOSE 监听器：这条响应写完后关闭连接（本接口不保持长连接）
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 连接级异常（客户端半路断开、请求体超限、协议错乱等）会走到这里。
        //
        // 用 warn 而非 info：这是「出了问题」而不是「正常流程」。
        // 但也不用 error——多数情况是客户端主动断开，不是服务端的错。
        //
        // 合并成一条：原来拆成两条打，看日志的人容易误以为是两个独立问题。
        // 带上来源 IP：不然只知道某条连接出错了，不知道是谁。
        // 例：GM - 连接异常，已关闭连接:[3a6ee7b3] from 192.168.1.50，原因: IOException: Connection reset by peer
        log.warn("GM - 连接异常，已关闭连接:[{}] from {}，原因: {}: {}",
                ctx.channel().id().asShortText(), remoteIp(ctx),
                cause.getClass().getSimpleName(), cause.getMessage());
        // 堆栈单独放 debug：warn 那条始终只占一行，需要深挖时再开 debug。
        // 直接把它挂在 warn 上会让日志被大片堆栈淹没，而这些堆栈多数没有价值
        log.debug("GM - 连接异常堆栈:", cause);

        // 关闭出错的连接。原实现只把异常抛给后续处理器，连接不会被关闭
        ctx.close();
    }

    /**
     * 安全地取 TCP 对端 IP。
     *
     * <p>连接已断开时 {@code remoteAddress()} 会返回 {@code null}，不确定的地址类型也无法
     * 直接强转成 {@link InetSocketAddress}，强转都会抛异常。这里统一兜底成 {@code "unknown"}，
     * 让调用方只需判这一个值。</p>
     *
     * @param ctx 通道上下文
     * @return 对端 IP；取不到时返回 {@code "unknown"}
     */
    private static String remoteIp(ChannelHandlerContext ctx) {
        if (ctx.channel().remoteAddress() instanceof InetSocketAddress isa && isa.getAddress() != null) {
            return isa.getAddress().getHostAddress();
        }
        return "unknown";
    }

    /**
     * 回一个 204（无内容）并关闭连接。
     *
     * <p>用于浏览器 / 爬虫的自动请求：明确告诉对方「没什么可给你的」，
     * 又不当作错误——比 404 更贴合语义，也能避免对方重试。</p>
     *
     * @param ctx 通道上下文
     */
    private static void sendNoContent(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT))
                .addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 发送错误响应并关闭连接。
     *
     * <p>响应体是纯文本而非 JSON——错误码本身已经表达了问题，不再额外包一层。
     * 例：{@code sendError(ctx, NOT_FOUND)} 会让客户端收到：</p>
     * <pre>
     * HTTP/1.1 404 Not Found
     * content-type: text/plain; charset=UTF-8
     *
     * Failure: 404 Not Found
     * </pre>
     *
     * @param ctx    通道上下文
     * @param status HTTP 状态码
     */
    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        // copiedBuffer 把字符串按 UTF-8 编成字节并复制一份（Netty 不持有传入的数组引用）
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        // 设置头
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        // 关闭连接
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

}
