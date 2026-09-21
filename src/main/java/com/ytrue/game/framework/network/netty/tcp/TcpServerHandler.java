package com.ytrue.game.framework.network.netty.tcp;

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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP Socket 的客户端消息处理器。
 *
 * <p>作为 Netty 的入站处理器，负责一条连接的完整生命周期：</p>
 * <ul>
 *     <li>{@link #channelActive}——连接建立，登记连接并创建用户会话；</li>
 *     <li>{@link #channelRead}——收到消息，按消息码查路由表并反射调用处理方法；</li>
 *     <li>{@link #channelInactive}——连接断开，清理连接并发布退出事件；</li>
 *     <li>{@link #exceptionCaught}——发生异常，关闭该连接。</li>
 * </ul>
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
public class TcpServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 消息路由表（由框架启动时扫描 {@code @AppHandler} 装配）。
     */
    private final AppHandlerRegister register;

    /**
     * 全局连接表：连接标识（channelId 短文本）→ 通道上下文。
     */
    private final Map<String, ChannelHandlerContext> channelMap = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 连接建立：先算出本连接的标识
        String channelId = getChannelId(ctx.channel());

        // 建立会话对象：此刻还没登录，只是「连接上了」的占位用户
        ServerUser user = new ServerUser();
        // 记录连接标识，后续收发消息都靠它在连接表里定位
        user.setConnect(channelId);
        // 置上最后活跃时间，心跳检测据此判断是否超时
        user.setLastActiveTime(System.currentTimeMillis());
        // 标记本会话架在哪条网络上。服务端主动下发时（ClientSender）靠它决定走哪条连接表，
        // 走错会查不到连接、消息被静默丢弃——所以必须显式打标，不能依赖默认值
        user.setTransportType(TransportType.TCP);
        // 登记到连接表，使其能被 sendMessageToClient 等主动下发操作找到
        putClientChannel(channelId, ctx);
        // 登记到用户容器，使其能被按连接维度查到
        UserContainer.putServerUser(user);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 连接断开：先算出连接标识
        String channelId = getChannelId(ctx.channel());

        // 取出会话对象。此处可能为 null——例如连接还没走到 channelActive 就断了，
        // 或该连接已被其它路径（如异常处理）清理过，因此必须判空
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
            // 用 warn：正常断连一定找得到会话，找不到说明 channelActive 没跑完就被断，
            // 或该连接已被其它路径清理过——两种情况都值得关注。
            // 与 WebSocket 侧保持同一级别（同一件事不应在两个传输上表现不同）
            log.warn("tcpServer - 连接[{}]断开，但未找到对应会话（可能尚未完成登记）", channelId);
        }

        // 从连接表移除，避免连接表随连接数增长而无限膨胀
        removeClientChannel(channelId);
        // 把事件继续往流水线后面传。ChannelInboundHandlerAdapter.channelInactive 的实现
        // 就是 ctx.fireChannelInactive()，本处理器是流水线的最后一环，所以这里实际是空转。
        // 但要转发的是 channelInactive 本身：若写成 super.channelUnregistered(ctx)，
        // 往下游发的是 channelUnregistered——将来在它之后再加处理器（埋点、访问日志等）
        // 就只会收到 channelUnregistered，永远等不到 channelInactive
        super.channelInactive(ctx);
    }

    /**
     * 收到客户端消息：按消息码路由到对应的处理方法。
     *
     * <p>整条链路（以客户端发一次网络诊断为例）：</p>
     * <pre>
     * 客户端                     服务端
     *   |  PingRequest{pingTime=1234567890123}
     *   |  ── Protobuf 序列化 ──> 08cb89ec8ff723
     *   |  ── 套信封 ──> BaseMessage{code=0x10000000, body=08cb89ec8ff723}
     *   |  ── 加 4 字节长度前缀 ──> 网络传输
     *   v
     *                        LengthFieldBasedFrameDecoder  按长度切出完整一帧
     *                              ↓
     *                        ProtobufDecoder               解成 BaseMessage 对象
     *                              ↓
     *                        channelRead（本方法）          查路由表 → 反射调用
     *                              ↓
     *                        NetworkAppController.doPingTask(PingRequest, ServerUser)
     * </pre>
     *
     * <p>路由表由框架在启动时扫描 {@code @AppHandler} 装配，本方法只负责「查表 + 反射调用」，
     * 不关心任何具体业务。新增协议只需写一个带注解的方法，无需改动这里。</p>
     *
     * @param ctx 通道上下文（可用它发消息、关连接）
     * @param msg 解码后的消息对象，此处即 {@link BaseMessage}
     * @throws Exception 由父类声明，本方法内部已捕获全部业务异常
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // ① 这条消息是从哪条连接发来的。
        //    例：channelId = "3a6ee7b3"（Netty 为每条连接生成的短标识，与 channelActive 里登记的一致）
        String channelId = getChannelId(ctx.channel());

        // ② 上游的 ProtobufDecoder 已经把字节流解成了「信封」对象，这里直接强转即可。
        //    信封结构见 AppMessage.proto 的 BaseMessage：
        //        code = 消息码（整数，客户端与服务端约定的常量）
        //        body = 业务消息序列化后的字节
        //    例：客户端发 PingRequest{pingTime=1234567890123}，解出来是
        //        code = 0x10000000（C_S_PING_REQUEST，即十进制的 268435456）
        //        body = 十六进制 08cb89ec8ff723（PingRequest 的 protobuf 编码）
        BaseMessage message = (BaseMessage) msg;

        // ③ 拿消息码去路由表里找处理方法。
        //    路由表在容器启动时由 BaseHandlerRegister 扫描所有 @AppHandler 方法装配好，
        //    结构是「消息码 -> AppHandlerWrapper」。以 0x10000000 为例，命中的包装器内容是：
        //        bean        = NetworkAppController 实例（处理方法所在的控制器对象）
        //        checkMethod = NetworkAppController.checker(Method, Message, ServerUser, Long)
        //        taskMethod  = NetworkAppController.doPingTask(PingRequest, ServerUser)
        //        exp         = 0（@AppHandler.exp 的默认值，供业务当附加标记用）
        AppHandlerWrapper wrapper = register.getAppHandlerWrapperMap().get(message.getCode());
        if (wrapper == null) {
            // 客户端发来了没注册过的消息码：只记一条日志，不中断连接。
            // 例：日志里出现 "未找到消息:[7fffffff]"，说明客户端协议版本和服务端对不上
            log.warn("tcpServer - 注册消息中未找到消息:[{}]", Integer.toHexString(message.getCode()));
            // 注意：这里不能 return，否则下面的 super.channelRead 不会执行，Netty 的资源回收链会断
        } else {
            // ④ 反序列化消息体。
            //    关键点：处理后方法的「第一个参数类型」就是这条消息的具体类型。
            //    例：doPingTask(PingRequest msg, ...) 的第一个参数是 PingRequest.class，
            //        于是调用 PingRequest.parseFrom(body 字节) 还原出消息对象。
            //    这也是为什么 body 里不需要带类型信息——方法签名本身已经说明了该解成什么。
            Class<?> paramType = wrapper.taskMethod().getParameterTypes()[0];
            Method parseFrom = paramType.getMethod("parseFrom", ByteString.class);

            // ⑤ 取出这条连接的会话对象，并刷新最后活跃时间。
            //    收到任何消息都算一次心跳，心跳任务据此判断该连接是否超时（不会因超时被踢）。
            //    user 可能为 null：连接还没走到 channelActive 就发消息的极端情况，故需判空。
            ServerUser user = UserContainer.getUserByConnect(channelId);
            long currentTime = System.currentTimeMillis();
            if (user != null) {
                user.setLastActiveTime(currentTime);
            }

            try {
                // ⑥ 字节 -> 业务消息对象。
                //    parseFrom 是静态方法，所以 invoke 的第一个参数传 null。
                //    例：body = 08cb89ec8ff723 -> req = PingRequest{pingTime=1234567890123}
                Message req = (Message) parseFrom.invoke(null, message.getBody());
                Method taskMethod = wrapper.taskMethod();

                // ⑦ 执行处理方法。这里分两条路径，取决于控制器有没有配校验方法
                //    （@AppController.checkMethod，默认 "checker"；@AppHandler.checker 可覆盖）。
                //
                //    路径 A：有校验方法
                //      框架不直接调处理方法，而是把「处理方法本身」当参数交给校验方法：
                //          checker.invoke(控制器实例, taskMethod, req, user, exp)
                //      注意参数是 4 个：taskMethod / 业务消息 / 用户 / exp。
                //      由校验方法自己决定要不要执行、以及怎么执行。
                //      例：NetworkAppController.checker 的实现是 taskMethod.invoke(this, msg, user)，
                //          只传了 2 个参数——所以 doPingTask(PingRequest, ServerUser) 写成 2 参就能跑通。
                //          校验方法可以在这里插入「先判断用户是否已登录」之类的逻辑。
                //
                //    路径 B：无校验方法
                //      框架直接调处理方法，固定传 3 个参数：(业务消息, user, exp)。
                //      所以这种情况下处理方法必须声明成 3 个参数，否则会抛参数个数不匹配。
                if (wrapper.checkMethod() != null) {
                    wrapper.checkMethod().invoke(wrapper.bean(), taskMethod, req, user, wrapper.exp());
                } else {
                    taskMethod.invoke(wrapper.bean(), req, user, wrapper.exp());
                }
            } catch (Exception e) {
                // 单条消息处理失败不应拖垮整条连接：记录后继续，连接保持存活。
                // 业务方法里抛出的异常（如空指针、参数非法）都会被这里捕获。
                log.error("tcpServer - 处理客户端消息出错:[{}]", e.getMessage(), e);
            }

            // ⑧ 耗时告警。处理逻辑跑在 Netty 的 EventLoop 线程上，
            //    单条消息处理过慢会阻塞同一条 EventLoop 上的其它连接，因此超过阈值就告警。
            //    例：阈值 100ms，某条消息耗时 350ms，就会打出
            //        "消息[10000000]处理时间[350]过长"
            long cost = System.currentTimeMillis() - currentTime;
            if (cost > INetwork.WARN_TIME) {
                log.warn("tcpServer - 消息[{}]处理时间[{}]过长", Integer.toHexString(message.getCode()), cost);
            }
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // 一批消息读完统一冲刷一次，比每条都 flush 更省系统调用
        ctx.flush();
        super.channelReadComplete(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String channelId = getChannelId(ctx.channel());
        // 合并成一条：拆两条打容易被误读成两个独立问题。
        // 用 warn 而非 error——多数情况是客户端半路断开，不是服务端的错。
        // 堆栈不挂在这里：客户端正常断开也会走到这，挂堆栈会把日志淹掉
        log.warn("tcpServer - 连接异常，已关闭:[{}] 原因: {}: {}",
                channelId, cause.getClass().getSimpleName(), cause.getMessage());

        // 关闭出错的连接。关闭会触发 channelInactive，由它统一做连接表清理与退出事件，
        // 这里不再重复清理，避免同一连接被处理两次
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
        log.debug("tcpServer - TcpSocket连接成功:[{}] 当前连接数量[{}]", channelId, channelMap.size());
    }

    /**
     * 移除一条连接。
     *
     * @param channelId 连接标识
     */
    public void removeClientChannel(String channelId) {
        ChannelHandlerContext context = channelMap.remove(channelId);
        if (context == null) {
            log.debug("tcpServer - 连接[{}]已不在连接表中（可能已被清理），当前连接数量[{}]", channelId, channelMap.size());
        } else {
            log.debug("tcpServer - TcpSocket断开并移除成功[{}] 当前连接数量[{}]", channelId, channelMap.size());
        }
    }

    /**
     * 获取连接标识。
     *
     * <p>用 {@code asShortText()} 而非完整 id，是为了让日志与连接表 key 更短更好读。</p>
     *
     * @param channel 通道
     * @return 连接标识
     */
    private String getChannelId(Channel channel) {
        return channel.id().asShortText();
    }

}
