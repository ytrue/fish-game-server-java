package com.ytrue.game.framework.network.netty.websocket;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.ytrue.game.framework.engine.container.UserContainer;
import com.ytrue.game.framework.engine.data.ServerUser;
import com.ytrue.game.framework.engine.data.TransportType;
import com.ytrue.game.framework.engine.register.AppHandlerRegister;
import com.ytrue.game.framework.engine.utils.SpringEventPublisher;
import com.ytrue.game.framework.engine.wrapper.AppHandlerWrapper;
import com.ytrue.game.framework.network.INetwork;
import com.ytrue.game.framework.network.event.NetExitEvent;
import com.ytrue.game.framework.network.proto.AppMessage.BaseMessage;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 的客户端消息处理器。
 *
 * <p>本类与 {@code TcpServerHandler} 是同构的——连接管理、消息路由、异常处理三段逻辑完全对应，
 * 差别只在「收到的东西」与「怎么解析」：</p>
 *
 * <table border="1">
 *     <caption>与 TCP 处理器的差异</caption>
 *     <tr><th></th><th>{@code TcpServerHandler}</th><th>本类</th></tr>
 *     <tr><td>基类</td><td>{@code ChannelInboundHandlerAdapter}</td>
 *         <td>{@code SimpleChannelInboundHandler&lt;BinaryWebSocketFrame&gt;}</td></tr>
 *     <tr><td>入站参数</td><td>{@code Object msg}，需强转成 {@code BaseMessage}</td>
 *         <td>直接是 {@code BinaryWebSocketFrame}</td></tr>
 *     <tr><td>解码位置</td><td>上游 {@code ProtobufDecoder} 已解好</td>
 *         <td><b>本类自己解</b>——WebSocket 没有对应的 Protobuf 解码器</td></tr>
 *     <tr><td>消息边界</td><td>靠长度前缀切帧</td><td>帧本身就是边界</td></tr>
 * </table>
 *
 * <p>本类被 {@link Sharable} 标注，是全局唯一实例：所有连接共用同一个处理器，
 * 因此实例字段 {@link #channelMap} 就是服务端的全局连接表。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Sharable
@Component
@RequiredArgsConstructor
public class WebSocketServerHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {

    /**
     * 消息路由表（由框架启动时扫描 {@code @AppHandler} 装配）。
     *
     * <p>注意：与 TCP 用的是<b>同一张表</b>——同一个 {@code @AppHandler} 方法
     * 既能处理 TCP 来的消息，也能处理 WebSocket 来的，业务层无需为两种传输各写一份。</p>
     */
    private final AppHandlerRegister register;

    /**
     * 全局连接表：连接标识（channelId 短文本）→ 通道上下文。
     */
    private final Map<String, ChannelHandlerContext> channelMap = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 建立会话对象：此刻还没登录，只是「连上了」的占位用户
        ServerUser user = new ServerUser();
        user.setConnect(getChannelId(ctx.channel()));
        user.setLastActiveTime(System.currentTimeMillis());
        // 标记本会话架在哪条网络上。服务端主动下发时（ClientSender）靠它决定走哪条连接表，
        // 走错会查不到连接、消息被静默丢弃——所以必须显式打标，不能依赖默认值
        user.setTransportType(TransportType.WEBSOCKET);
        // 登记到连接表，使其能被 sendMessageToClient 等主动下发操作找到
        putClientChannel(getChannelId(ctx.channel()), ctx);
        // 登记到用户容器，使其能被按连接维度查到
        UserContainer.putServerUser(user);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String channelId = getChannelId(ctx.channel());

        // 取出会话对象。此处可能为 null——例如连接还没走到 channelActive 就断了，
        // 或该连接已被其它路径清理过，因此必须判空
        ServerUser user = UserContainer.getUserByConnect(channelId);
        if (user != null) {
            // 先在内存中标记为不在线，避免心跳任务在这之后仍向已断开的连接发消息
            user.setOnline(false);
            // 发布退出事件：各业务模块清理自身数据，最后由 NetExitFinishListener 兜底回写数据库
            SpringEventPublisher.publish(new NetExitEvent(user));
            // 不会出现「监听器想 getUserById 却已经被摘掉」。
            // 旧工程断线不摘，USER_CONNECT_MAP 会随「不再回来的连接数」单调增长，
            // 拖慢每 5 秒一次的心跳全量拷贝与每次全服群发的遍历
            UserContainer.removeServerUser(user);
        } else {
            log.warn("webSocketServer - 连接[{}]断开，但未找到对应会话（可能尚未完成登记）", channelId);
        }

        // 从连接表移除，避免连接表随连接数增长而无限膨胀
        removeClientChannel(channelId);
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        String channelId = getChannelId(ctx.channel());

        // ① 解帧。与 TCP 不同，WebSocket 没有现成的 Protobuf 解码器可用，
        //    需要自己把帧里的字节取出来反序列化。
        //    frame.content() 是 ByteBuf；ByteBufUtil.getBytes 会从 readerIndex 读到 writerIndex，
        //    返回纯粹的负载字节（WebSocket 的帧头已被上游的 WebSocketFrameDecoder 剥掉了）
        BaseMessage message;
        try {
            message = BaseMessage.parseFrom(ByteBufUtil.getBytes(frame.content()));
        } catch (Exception e) {
            // 收到解不出信封的帧：可能是客户端发错、或协议版本不匹配。
            // 只记录并丢弃这一帧，不断开连接——单帧异常不该拖垮整条连接。
            //
            // 只打异常消息、不挂堆栈：解析失败的原因就那么几种，堆栈每次都是同一串
            // Netty 调用链，除了刷屏没有别的用处；需要深挖时再把 e 挂上即可
            log.warn("webSocketServer - 消息解析失败，已丢弃该帧:[{}] 原因: {}", channelId, e.getMessage());
            return;
        }

        // ② 按消息码查路由表。这一步与 TCP 完全一致——路由表是按消息码组织的，
        //    与消息从哪种传输进来无关
        AppHandlerWrapper wrapper = register.getAppHandlerWrapperMap().get(message.getCode());
        if (wrapper == null) {
            // 未注册的消息码：只记录，不中断连接
            log.warn("webSocketServer - 注册消息中未找到消息:[{}]", Integer.toHexString(message.getCode()));
            return;
        }

        // ③ 反序列化消息体。处理方法的第一个参数类型就是这条消息的具体类型，
        //    例：doPingTask(PingRequest msg, ...) -> PingRequest.parseFrom(body)
        Class<?> paramType = wrapper.taskMethod().getParameterTypes()[0];
        Method parseFrom;
        try {
            parseFrom = paramType.getMethod("parseFrom", ByteString.class);
        } catch (NoSuchMethodException e) {
            log.error("webSocketServer - 处理方法的首个参数不是 Protobuf 消息类型:[{}#{}]",
                    wrapper.bean().getClass().getSimpleName(), wrapper.taskMethod().getName(), e);
            return;
        }

        // ④ 取会话对象并刷新活跃时间——收到任何消息都算一次心跳
        ServerUser user = UserContainer.getUserByConnect(channelId);
        long currentTime = System.currentTimeMillis();
        if (Objects.nonNull(user)) {
            user.setLastActiveTime(currentTime);
        }

        try {
            Message req = (Message) parseFrom.invoke(null, message.getBody());
            Method taskMethod = wrapper.taskMethod();

            if (Objects.nonNull(wrapper.checkMethod())) {
                // 有校验方法：交给它决定是否执行、以及怎么执行任务方法
                wrapper.checkMethod().invoke(wrapper.bean(), taskMethod, req, user, wrapper.exp());
            } else {
                // 无校验方法：直接执行，固定传 3 个参数
                taskMethod.invoke(wrapper.bean(), req, user, wrapper.exp());
            }
        } catch (Exception e) {
            // 单条消息处理失败不应拖垮整条连接，记录后继续
            log.error("webSocketServer - 处理客户端消息出错:[{}]", e.getMessage(), e);
        }

        // ⑤ 耗时告警：处理逻辑跑在 EventLoop 线程上，过慢会阻塞同一 EventLoop 上的其它连接
        long cost = System.currentTimeMillis() - currentTime;
        if (cost > INetwork.WARN_TIME) {
            log.warn("webSocketServer - 消息[{}]处理时间[{}]过长", Integer.toHexString(message.getCode()), cost);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String channelId = getChannelId(ctx.channel());
        // 合并成一条，与 TCP 侧保持一致：拆两条打容易被误读成两个独立问题。
        // 用 warn 而非 error——多数情况是客户端半路断开，不是服务端的错；
        // 堆栈也不挂在这里，否则客户端正常断开会把日志淹掉
        log.warn("webSocketServer - 连接异常，已关闭:[{}] 原因: {}: {}",
                channelId, cause.getClass().getSimpleName(), cause.getMessage());

        // 关闭出错的连接。关闭会触发 channelInactive，由它统一做连接表清理与退出事件
        ctx.close();
    }

    /**
     * 获取连接对应的通道上下文。
     *
     * @param channelId 连接标识
     * @return 通道上下文；不存在时返回 {@code null}
     */
    public ChannelHandlerContext getClientChannel(String channelId) {
        return channelMap.get(channelId);
    }

    /**
     * 登记一条连接。
     *
     * @param channelId 连接标识
     * @param channel   通道上下文
     */
    public void putClientChannel(String channelId, ChannelHandlerContext channel) {
        channelMap.put(channelId, channel);
        log.debug("webSocketServer - 连接成功:[{}] 当前连接数量[{}]", channelId, channelMap.size());
    }

    /**
     * 移除一条连接。
     *
     * @param channelId 连接标识
     */
    public void removeClientChannel(String channelId) {
        ChannelHandlerContext context = channelMap.remove(channelId);
        if (context == null) {
            // 连接表中已没有这条连接——正常路径，不是异常：
            // closeClientConnect 先关闭再移除（第 1 次命中），
            // 关闭触发的 channelInactive 又会移除一次（第 2 次落空）。
            // 用 debug，否则每次关闭连接都会打出一条假的「移除失败」警告
            log.debug("webSocketServer - 连接[{}]已不在连接表中（可能已被清理），当前连接数量[{}]",
                    channelId, channelMap.size());
        } else {
            log.debug("webSocketServer - 断开并移除成功[{}] 当前连接数量[{}]", channelId, channelMap.size());
        }
    }

    /**
     * 获取连接标识。
     *
     * @param channel 通道
     * @return 连接标识
     */
    private String getChannelId(Channel channel) {
        return channel.id().asShortText();
    }

}
