package com.ytrue.game.framework.network.netty.tcp;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.ytrue.game.framework.engine.container.UserContainer;
import com.ytrue.game.framework.engine.data.ServerUser;
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
        } else {
            log.info("netty - 连接[{}]断开，但未找到对应会话（可能尚未完成登记）", channelId);
        }

        // 从连接表移除，避免连接表随连接数增长而无限膨胀
        removeClientChannel(channelId);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 取出本条消息所属的连接标识
        String channelId = getChannelId(ctx.channel());

        // ProtobufDecoder 已把字节流解成信封对象（消息码 + 消息体）
        BaseMessage message = (BaseMessage) msg;
        // 按消息码查路由表，找对应的处理方法
        AppHandlerWrapper wrapper = register.getAppHandlerWrapperMap().get(message.getCode());
        if (wrapper == null) {
            // 未注册的消息码：只记录，不中断连接
            log.info("netty - 注册消息中未找到消息:[{}]", Integer.toHexString(message.getCode()));
            // 注意：这里不能 return，否则 super.channelRead 不会执行，Netty 的资源回收链会断
        } else {
            // 处理方法第一个参数的类型就是该消息的具体类型，用它反序列化消息体
            Class<?> paramType = wrapper.taskMethod().getParameterTypes()[0];
            Method parseFrom = paramType.getMethod("parseFrom", ByteString.class);

            // 取会话对象并刷新活跃时间——收到任何消息都算一次心跳
            ServerUser user = UserContainer.getUserByConnect(channelId);
            // 获取当前时间戳
            long currentTime = System.currentTimeMillis();
            if (user != null) {
                // 设置最后活跃时间
                user.setLastActiveTime(currentTime);
            }

            try {
                // 反序列化消息体，得到业务消息对象
                Message req = (Message) parseFrom.invoke(null, message.getBody());
                Method taskMethod = wrapper.taskMethod();

                if (wrapper.checkMethod() != null) {
                    // 有校验方法：交给它去决定是否执行任务方法（如登录校验、权限校验）
                    wrapper.checkMethod().invoke(wrapper.bean(), taskMethod, req, user, wrapper.exp());
                } else {
                    // 无校验方法：直接执行任务方法
                    taskMethod.invoke(wrapper.bean(), req, user, wrapper.exp());
                }
            } catch (Exception e) {
                // 单条消息处理失败不应拖垮整条连接，记录后继续
                log.error("处理客户端消息出错:[{}]", e.getMessage(), e);
            }

            // 耗时告警：处理过慢会阻塞该连接所在 EventLoop 上的其它连接
            long cost = System.currentTimeMillis() - currentTime;
            if (cost > INetwork.WARN_TIME) {
                log.warn("消息[{}]处理时间[{}]过长", Integer.toHexString(message.getCode()), cost);
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
        log.info("netty - 连接错误捕获:[{}]", channelId);
        log.info("netty - 错误信息:[{}][{}]", cause.getClass().getSimpleName(), cause.getMessage());

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
        log.info("netty - TcpSocket连接成功:[{}] 当前连接数量[{}]", channelId, channelMap.size());
    }

    /**
     * 移除一条连接。
     *
     * @param channelId 连接标识
     */
    public void removeClientChannel(String channelId) {
        ChannelHandlerContext context = channelMap.remove(channelId);
        if (context == null) {
            log.info("netty - TcpSocket移除失败，链接表不存在连接[{}], 当前连接数量[{}]", channelId, channelMap.size());
        } else {
            log.info("netty - TcpSocket断开并移除成功[{}] 当前连接数量[{}]", channelId, channelMap.size());
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
