package com.ytrue.game.framework.base.data.fishing;

import com.ytrue.game.framework.base.data.BaseGameRoom;
import lombok.Getter;
import lombok.Setter;

/**
 * 基础捕鱼房间。
 *
 * <p>在通用房间之上只加了一样东西——<b>房间时钟</b>：一个随帧循环自增的计数器。</p>
 *
 * <p>捕鱼是「持续跑帧」的玩法：怪物游动、刷新、开炮判定都挂在一根时间轴上。
 * 用房间自己的时钟来对齐客户端与服务端的节奏，比双方各自记时间戳更靠谱——
 * 时间戳会受网络延迟和机器时钟差影响，而「第 N 帧」是双方都算得出来的确定值。</p>
 *
 * <p>时钟由 {@code BaseFishingManager} 的循环任务推进，见
 * {@link #addRoomTick()}。</p>
 *
 * @since 1.0.0
 */
@Getter
@Setter
public abstract class BaseFishingRoom extends BaseGameRoom {

    /**
     * 房间时钟（每帧 +1）。
     */
    private long roomTick;

    /**
     * 房间时钟自增一次。
     *
     * <p>由 {@code BaseFishingManager.doFishingRoomTask} 在每个循环周期调用一次——
     * 遍历到本房间时先推一格时钟，再交给子类的 {@code doFishingRoomTask0} 处理。
     * 所以子类里读到的 {@link #getRoomTick()} 就是「当前帧」。</p>
     */
    public void addRoomTick() {
        roomTick++;
    }

}
