package com.ytrue.game.framework.network.event;

import com.ytrue.game.framework.engine.data.ServerUser;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 用户退出事件。
 *
 * <p>承载触发退出事件的用户，由 {@code ApplicationEventPublisher} 发布。触发时机有四处：
 * 连接断开（{@code channelInactive}）、心跳超时被服务端踢下线、客户端主动登出、以及后台强制下线。</p>
 *
 * <p>发布后由各业务模块的监听器接手清理各自的数据（如登录模块补登出时间、游戏模块处理房间内玩家掉线），
 * 最后由 {@link NetExitFinishListener} 兜底：把用户置为离线并回写数据库。</p>
 *
 * @since 1.0.0
 */
@Getter
public class NetExitEvent extends ApplicationEvent {

    /**
     * 触发退出事件的用户。
     */
    private final ServerUser user;

    /**
     * 构造器。
     *
     * @param user 触发退出事件的用户
     */
    public NetExitEvent(ServerUser user) {
        super(user);
        this.user = user;
    }

}
