package com.ytrue.game.framework.network.event;

import com.ytrue.game.framework.database.data.mapper.UserMapper;
import com.ytrue.game.framework.engine.data.ServerUser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 用户退出收尾监听器。
 *
 * <p>在所有业务退出监听器执行完毕后，把用户置为离线（{@code onlineState = 0}）并回写数据库。</p>
 *
 * <p>{@code online_state} 取值约定：{@code 0} 离线、{@code 1} 大厅、
 * {@code 2~10} 在对应游戏中（见 {@code GameEnum}）。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@AllArgsConstructor
public class NetExitFinishListener implements ApplicationListener<NetExitEvent> {

    /**
     * 用户数据库操作接口。
     */
    private final UserMapper userMapper;

    /**
     * 退出收尾：置为离线并回写数据库。
     *
     * <p>{@code @Order(Ordered.LOWEST_PRECEDENCE)} 保证在所有业务退出监听器之后执行——
     * 业务监听器可能还要读取用户当前状态（例如判断是否在房间内、在哪个场次）。</p>
     *
     * @param event 退出事件
     */
    @Override
    public void onApplicationEvent(NetExitEvent event) {
        ServerUser user = event.getUser();
        // 只有真正建号成功的用户才需要回写；纯连接阶段的占位用户没有实体
        if (user.getEntity() != null) {
            try {
                // 置为离线状态并回写数据库
                user.getEntity().setOnlineState(0);
                userMapper.updateById(user.getEntity());
            } catch (Exception e) {
                // 退出流程由连接断开触发，可能发生在容器关闭过程中；
                // 此处吞掉异常，保证后面的「置为不在线」一定会执行，避免用户永远停留在在线
                log.error("退出时回写用户状态出错:[{}]", e.getMessage(), e);
            }
        }
        // 内存中的在线标记：无论有没有实体都要置为不在线
        user.setOnline(false);
    }

}
