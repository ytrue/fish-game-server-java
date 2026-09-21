package com.ytrue.game.framework.base.data.gobang;

/**
 * 五子棋房间状态。
 *
 * <p>三个常量互斥，表示一局棋走到了哪一步。</p>
 *
 * <p><b>为什么是常量类而不是枚举</b>：旧工程用的就是常量类，业务层大量代码直接拿
 * {@code int} 比较（例如 {@code room.getGameState() == GoBangRoomState.GOBANG_PLAY}），
 * 改成枚举会波及所有调用点，收益不抵风险。迁移时保持原样。</p>
 *
 * @since 1.0.0
 */
public class GoBangRoomState {

    /**
     * 准备阶段：玩家已入座，等待开局。
     */
    public static final int GOBANG_READY = 0;

    /**
     * 落子阶段：对局进行中，轮到某一方落子。
     */
    public static final int GOBANG_PLAY = 1;

    /**
     * 结束阶段：已分出胜负或和棋。
     */
    public static final int GOBANG_OVER = 2;

}
