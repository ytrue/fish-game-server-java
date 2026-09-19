package com.ytrue.game.framework.base.data.gobang;

/**
 * 五子棋房间状态。
 *
 * <p>三个常量互斥，表示一局棋走到哪一步。旧工程用的是「常量类」而不是枚举，
 * 迁移时保持原样：业务层大量代码直接拿 {@code int} 比较（{@code getGameState() == GOBANG_PLAY}），
 * 改成枚举会波及所有调用点，收益不抵风险。</p>
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
