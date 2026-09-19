package com.ytrue.game.framework.base.data.fishing;

import com.ytrue.game.framework.base.data.BaseGameRoom;

/**
 * 基础捕鱼房间。
 *
 * <p>在通用房间之上增加了「房间时钟」——一个随帧循环自增的计数器。
 * 捕鱼是持续跑帧的玩法，怪物的移动、刷新、开炮判定都挂在同一根时间轴上，
 * 用它来对齐客户端与服务端的节奏，比各自记时间戳更容易保持一致。</p>
 *
 * @since 1.0.0
 */
public abstract class BaseFishingRoom extends BaseGameRoom {

    /**
     * 房间时钟（每帧 +1）。
     */
    private long roomTick;

    /**
     * 房间时钟自增一次。
     *
     * <p>由 {@code BaseFishingManager.doFishingRoomTask} 在每个循环周期调用。</p>
     */
    public void addRoomTick() {
        roomTick++;
    }

    /**
     * 获取房间时钟。
     *
     * @return 房间时钟
     */
    public long getRoomTick() {
        return roomTick;
    }

    /**
     * 设置房间时钟。
     *
     * @param roomTick 房间时钟
     */
    public void setRoomTick(long roomTick) {
        this.roomTick = roomTick;
    }

}
