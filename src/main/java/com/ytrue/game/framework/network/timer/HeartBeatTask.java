package com.ytrue.game.framework.network.timer;

import com.ytrue.game.framework.engine.container.UserContainer;
import com.ytrue.game.framework.engine.data.ServerUser;
import com.ytrue.game.framework.network.ClientSender;
import com.ytrue.game.framework.network.proto.NetworkMessage.HeartBeatResponse;
import com.ytrue.game.framework.network.proto.NetworkMessage.NetworkMsgCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 心跳任务。
 *
 * <p>周期性巡检所有在线用户：</p>
 * <ul>
 *     <li>距上次活跃未超时 → 主动下发一条心跳返回，让客户端知道连接还活着；</li>
 *     <li>超过阈值没有收到任何消息 → 判定为掉线，关闭其连接。</li>
 * </ul>
 *
 * <p><b>注意「活跃」的判定</b>：{@code lastActiveTime} 是在收到客户端**任意**消息时刷新的
 * （见 {@code TcpServerHandler.channelRead}），不局限于心跳请求——玩家在打鱼、发聊天，
 * 都算活跃。因此本任务判的是「这条连接是不是死了」，而不是「客户端有没有按时发心跳」。</p>
 *
 * <p>调度方式：走 Spring 的 {@code @Scheduled}，由 {@code ScheduleConfig} 统一挂到
 * {@code ThreadPoolFactory.TIMER_SERVICE_POOL} 上。旧工程是在 {@code @AppInit} 里手工
 * 创建单线程池再 {@code scheduleAtFixedRate}，那样会多出一个线程池，现已不需要。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class HeartBeatTask {

    /**
     * 心跳超时时间（毫秒）。
     *
     * <p>超过这个时长没收到该用户的任何消息，就判定为掉线。取 60 秒是留了足够余量：
     * 客户端网络抖动、短暂卡顿都不至于被误踢。</p>
     */
    private static final long OFFLINE_HEART_BEAT_TIME = 60 * 1000;

    /**
     * 巡检间隔（毫秒）。
     */
    private static final long CHECK_INTERVAL = 5 * 1000;

    /**
     * 巡检一次：给正常连接的客户端回心跳，把超时的踢下线。
     */
    @Scheduled(fixedRate = CHECK_INTERVAL)
    public void checkHeartBeat() {
        // 获取所有的连接
        List<ServerUser> onlineUsers = UserContainer.getActiveServerUsers();
        // 获取当前13位时间戳
        long now = System.currentTimeMillis();
        // 循环处理
        for (ServerUser user : onlineUsers) {
            // 未登录的会话（只是连上了还没登录）不参与心跳：
            // online 只在登录成功时被置为 true，它们既不回心跳也无所谓超时
            if (!user.isOnline()) {
                continue;
            }

            if (user.getLastActiveTime() + OFFLINE_HEART_BEAT_TIME > now) {
                // 还活着：主动回一条心跳，让客户端确认连接可用
                ClientSender.sendMessage(NetworkMsgCode.S_C_HEART_BEAT_RESPONSE_VALUE, HeartBeatResponse.getDefaultInstance(), user);
            } else {
                // 超时：踢下线。
                //
                // 这里只负责关连接，不再单独发布退出事件——关闭会触发 channelInactive，
                // 由它作为「断连」的唯一事实来源统一发布。否则同一次掉线会发两遍事件，
                // 业务监听器（如补登出时间、结算房间数据）就会被执行两次。
                // 带上用户 id 与连接标识：只记连接标识的话，排查时不知道掉线的是谁
                // （此处 user 必定已登录——未登录的会话在上面的 isOnline 判断里被跳过了）
                log.info("用户[{}]心跳超时，断开连接，连接:[{}]", user.getId(), user.getConnect());
                ClientSender.closeClientConnect(user);
            }
        }
    }

}
