package com.ytrue.game.framework.base.data;

import com.ytrue.game.framework.engine.data.ServerUser;
import lombok.Getter;
import lombok.Setter;

/**
 * 基础游戏玩家。
 *
 * <p>承载一名玩家「在某个房间里、坐在某个座位上」这层身份，是所有玩法玩家类的公共父类。
 * 玩法相关的字段（金币、炮台、手牌等）由各业务模块的子类自行添加。</p>
 *
 * <p><b>本类与 {@link ServerUser} 的分工</b>：{@code ServerUser} 是「连接 + 账号」层面的会话，
 * 生命周期与网络连接一致；{@code BaseGamePlayer} 是「这一局里的我」，
 * 生命周期与房间一致——同一名玩家退出房间再加入会得到新的 {@code BaseGamePlayer}，
 * 但 {@code ServerUser} 始终是同一个实例。</p>
 *
 * <p>字段访问器由 Lombok 的 {@code @Getter}/{@code @Setter} 生成（{@code user}/{@code roomCode}/{@code seat}），
 * <b>但 {@link #getId()} 是手写的</b>：它不是字段读取，而是转发到 {@code user.getId()}。
 * 本类没有 {@code id} 字段，Lombok 也就不会生成同名的 {@code getId()}，两者不会打架。</p>
 *
 * @since 1.0.0
 */
@Getter
@Setter
public abstract class BaseGamePlayer {

    /**
     * 用户会话。
     *
     * <p>由 {@code GameContainer.createGamePlayer} 在创建玩家时写入，
     * 是本类与网络层之间唯一的联系（发消息走 {@code user}）。</p>
     */
    private ServerUser user;

    /**
     * 所在房间号。
     */
    private int roomCode;

    /**
     * 所在座位号（从 0 开始）。
     */
    private int seat;

    /**
     * 获取玩家 id。
     *
     * <p>直接取用户会话的 id，因此本类<b>没有</b>自己的 id 字段——
     * 玩家 id 与用户 id 恒等。若 {@code user} 尚未设置，本方法会抛 {@code NullPointerException}。</p>
     *
     * @return 玩家 id
     */
    public long getId() {
        return user.getId();
    }
}
