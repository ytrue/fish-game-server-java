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
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
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

/**
 * HTTP 请求处理器（后台 GM 接口）。
 *
 * <p>处理流程：</p>
 * <pre>
 * 取真实 IP → 解析 URI 与查询串 → 按路径查 GM 路由表 → 反射调用处理方法 → 返回 JSON
 * </pre>
 *
 * <p>约定：GM 处理方法的签名固定为
 * {@code (Map&lt;String, Object&gt; param, Map&lt;String, Object&gt; resultMap)}，
 * 由控制器上的「校验方法」负责调用并返回 JSON 字符串（见 {@code NetworkGmController.checker}）。
 * 请求参数无论 GET 还是 POST 都会被整理成 {@code param} 传入。</p>
 *
 * <p>本类被 {@link Sharable} 标注，是全局唯一实例，自身不持有任何连接状态，
 * 故可被所有连接共享。</p>
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
     * 处理一次完整的 HTTP 请求。
     *
     * @param ctx 通道上下文
     * @param msg 聚合后的完整请求（由 {@link HttpChannelInitializer} 装配的 HttpObjectAggregator 产出）
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        // ① 解码失败的请求直接拒掉（协议格式错误、超长等）
        if (!msg.decoderResult().isSuccess()) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        // ② 取真实客户端 IP。经 Nginx 等反向代理时真实 IP 在 x-forwarded-for 头里，
        //    取不到才回退到 TCP 对端地址。
        String ip = msg.headers().get("x-forwarded-for");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        }
        if (ip == null) {
            // 拿不到来源地址，拒绝服务。注：当前未把 ip 传给业务方法，此处仅作准入判断
            sendError(ctx, HttpResponseStatus.FORBIDDEN);
            return;
        }

        // ③ 解析 URI
        //    URL 解码：把 %E4%B8%AD 这类转义还原成中文
        String uri = URLDecoder.decode(msg.uri(), StandardCharsets.UTF_8);

        //    查询串起始位置（取第一个 '?'），-1 表示无参数
        int queryIndex = uri.indexOf('?');

        //    路由键 = 去掉查询串的那部分。
        //    注意顺序：必须先剥离查询串、再处理结尾斜杠。
        //    反过来的话 "/ping/?a=1" 这种 URI 不以 '/' 结尾（结尾是 '1'），
        //    斜杠剥不掉，路由键就成了 "/ping/"，查路由表必然落空。
        String path = queryIndex < 0 ? uri : uri.substring(0, queryIndex);
        //    去掉结尾多余的斜杠；根路径 "/" 要保留，不能剥成空串
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // ④ 按请求方法分派
        String result;
        if (HttpMethod.GET.equals(msg.method())) {
            // GET 的参数在查询串里
            String queryStr = queryIndex < 0 ? "" : uri.substring(queryIndex + 1);
            result = handleGet(ctx, path, queryStr);
        } else if (HttpMethod.POST.equals(msg.method())) {
            // POST 的参数在请求体里
            result = handlePost(ctx, msg, path);
        } else {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        // ⑤ 写出响应。result 为 null 说明处理过程已经自行发过响应（如 404），无需再写
        if (result != null) {
            sendJson(ctx, result);
        }
    }

    /**
     * 处理 GET 请求：参数写在 URL 查询串里。
     *
     * <p>例：{@code GET /ping?pingTime=123} → 路由键 {@code /ping}，
     * 参数 {@code {pingTime: "123"}}（注意值都是字符串，需要时用
     * {@code JsonMapUtils.parseObject} 转成目标类型）。</p>
     *
     * @param ctx      通道上下文
     * @param routeKey 已归一化的路由键（不含查询串与结尾斜杠）
     * @param queryStr 查询串（不含问号），无参数时为空串
     * @return 响应 JSON 字符串；已自行发送错误响应时返回 {@code null}
     */
    private String handleGet(ChannelHandlerContext ctx, String routeKey, String queryStr) {
        Map<String, Object> paramMap = new HashMap<>();
        if (!queryStr.isEmpty()) {
            // 查询串形如 name=zzz&age=20，按 & 拆键值对、再按 = 拆名值
            for (String kv : queryStr.split("&")) {
                String[] kav = kv.split("=");
                // 只接受完整的 key=value 形式，忽略残缺片段
                if (kav.length == 2) {
                    paramMap.put(kav[0], kav[1]);
                }
            }
        }

        GmHandlerWrapper wrapper = register.getGmHandlerWrapperMap().get(routeKey);
        if (wrapper == null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            log.info("GM - 消息未找到[{}]", routeKey);
            return null;
        }

        try {
            return invoke(wrapper, paramMap);
        } catch (Exception e) {
            log.error("GM - 消息处理出错:[{}]", e.getMessage(), e);
            return errorResult(e);
        }
    }

    /**
     * 处理 POST 请求：参数写在请求体里（JSON 格式）。
     *
     * @param ctx      通道上下文
     * @param msg      完整请求
     * @param routeKey 已归一化的路由键（不含查询串与结尾斜杠）
     * @return 响应 JSON 字符串；已自行发送错误响应时返回 {@code null}
     */
    private String handlePost(ChannelHandlerContext ctx, FullHttpRequest msg, String routeKey) {
        GmHandlerWrapper wrapper = register.getGmHandlerWrapperMap().get(routeKey);
        if (wrapper == null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            log.info("GM - 消息未找到[{}]", routeKey);
            return null;
        }

        Map<String, Object> paramMap = new HashMap<>();
        ByteBuf content = msg.content();
        String jsonStr = content.toString(CharsetUtil.UTF_8);
        log.info("后台消息:[{}]", jsonStr);
        try {
            // 请求体解析成 Map。注意：Gson 会把所有数字解析成 Double，
            // 业务侧取值需走 JsonMapUtils.parseObject 做类型转换
            Map<String, Object> parsed = GsonUtils.fromJsonToMap(jsonStr);
            if (parsed != null) {
                paramMap.putAll(parsed);
            }
        } catch (Exception e) {
            // 请求体不是合法 JSON（或为空）时按「无参数」继续，
            // 由业务方法自己校验必填参数——这样空 body 的 POST 也能给出更明确的业务错误
            log.info("后台请求体解析失败，按无参数处理:[{}]", e.getMessage());
        }

        try {
            return invoke(wrapper, paramMap);
        } catch (Exception e) {
            log.error("GM - 消息处理出错:[{}]", e.getMessage(), e);
            return errorResult(e);
        }
    }

    /**
     * 反射调用 GM 处理方法。
     *
     * <p>由控制器上的校验方法负责实际调用：框架把「处理方法本身」交给它，
     * 它可以先做权限校验、再决定用哪组参数调用任务方法，最后返回 JSON 字符串。</p>
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
            throw new IllegalStateException("GM 控制器缺少校验方法，无法处理请求: "
                    + wrapper.bean().getClass().getSimpleName() + "#" + wrapper.taskMethod().getName());
        }
        return (String) wrapper.checkMethod().invoke(wrapper.bean(), wrapper.taskMethod(), paramMap);
    }

    /**
     * 构造异常情况下的响应内容。
     *
     * <p>把异常信息与堆栈一并返回，便于后台页面直接看到出错原因。注意这会把内部细节
     * 暴露给调用方，仅适用于内网后台接口。</p>
     *
     * @param e 处理过程中抛出的异常
     * @return 描述异常的 JSON 字符串
     */
    private String errorResult(Exception e) {
        return e.getMessage() + " --- " + GsonUtils.toJson(e.getStackTrace());
    }

    /**
     * 写回一个 JSON 响应并关闭连接。
     *
     * @param ctx    通道上下文
     * @param result 响应体
     */
    private static void sendJson(ChannelHandlerContext ctx, String result) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        // 显式用 UTF-8 编码：getBytes() 走平台默认字符集，在 GBK 环境下会把中文写坏
        response.content().writeBytes(result.getBytes(StandardCharsets.UTF_8));

        // 规定返回值为 json 字符串
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        // 允许跨域访问（后台页面与接口域名不同时需要）
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS,
                "Origin, X-Requested-With, Content-Type, Accept");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE");

        HttpUtil.setContentLength(response, response.content().readableBytes());
        // 写完即关：本接口不保持长连接
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.info("netty http - 连接错误捕获:[{}]", ctx.channel().id().asShortText());
        log.info("netty http - 错误信息:[{}][{}]", cause.getClass().getSimpleName(), cause.getMessage());
        // 关闭出错的连接。原实现只把异常抛给后续处理器，连接不会被关闭
        ctx.close();
    }

    /**
     * 发送错误响应并关闭连接。
     *
     * @param ctx    通道上下文
     * @param status HTTP 状态码
     */
    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

}
