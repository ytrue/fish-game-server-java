package com.ytrue.game.framework.engine.event;

import com.ytrue.game.framework.engine.data.ServerUser;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 用户初始化事件。
 *
 * <p>承载触发初始化事件的用户，由 {@code ApplicationEventPublisher} 发布，业务模块通过
 * {@code @EventListener} 监听并完成各自的初始化。</p>
 *
 * @since 1.0.0
 */
@Getter
public class UserInitEvent extends ApplicationEvent {

    /**
     * 触发初始化事件的用户。
     */
    private final ServerUser user;

    /**
     * 构造器。
     *
     * @param user 触发初始化事件的用户
     */
    public UserInitEvent(ServerUser user) {
        super(user);
        this.user = user;
    }

}
