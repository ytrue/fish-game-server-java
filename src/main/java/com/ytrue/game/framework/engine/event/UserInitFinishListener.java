package com.ytrue.game.framework.engine.event;

import com.ytrue.game.framework.database.data.mapper.UserMapper;
import com.ytrue.game.framework.engine.data.ServerUser;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 用户初始化收尾监听器。
 *
 * <p>在所有业务初始化监听器执行完毕后，将用户实体置为在线状态（{@code onlineState = 0}）并
 * 回写数据库。</p>
 *
 * @since 1.0.0
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@AllArgsConstructor
public class UserInitFinishListener implements ApplicationListener<UserInitEvent> {

    /**
     * 用户数据库操作接口。
     */
    private final UserMapper userMapper;

    /**
     * 初始化收尾：置为在线并回写数据库。
     *
     * <p>{@code @Order(Ordered.LOWEST_PRECEDENCE)} 保证在所有业务初始化监听器之后执行。</p>
     *
     * @param event 初始化事件
     */
    @Override
    public void onApplicationEvent(UserInitEvent event) {
        ServerUser user = event.getUser();
        if (user.getEntity() != null) {
            // 置为在线状态并回写数据库
            user.getEntity().setOnlineState(0);
            userMapper.updateById(user.getEntity());
        }
    }

}
