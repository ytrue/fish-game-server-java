package com.ytrue.game.framework.base.data.gobang;

import com.ytrue.game.framework.base.data.BaseGameRoom;

import java.util.concurrent.ScheduledFuture;

/**
 * 基础五子棋房间。
 *
 * <p>在通用房间之上增加了棋盘、对局阶段、当前落子方、步数与落子倒计时。
 * 棋盘是 {@link #CHESSBOARD_WIDTH} × {@link #CHESSBOARD_WIDTH} 的二维数组，
 * 每格存 {@code 0}（空）、{@code 座位号 + 1}（该座位玩家的棋子）。</p>
 *
 * <p><b>为什么存「座位号 + 1」而不是座位号本身</b>：0 要留给「空位」，
 * 而 {@code getSeatChess} 越界时返回 {@code -1} 表示不可落子，
 * 于是 {@code 0/1/2/3} 恰好构成「空 + 三个座位」的编码。</p>
 *
 * @since 1.0.0
 */
public abstract class BaseGobangRoom extends BaseGameRoom {

    /**
     * 棋盘宽度（同时也是高度，棋盘为正方形）。
     */
    public static final int CHESSBOARD_WIDTH = 15;

    /**
     * 棋盘（行 x 列，0 表示空位）。
     */
    private int[][] chessboard;

    /**
     * 游戏阶段，取值见 {@link GoBangRoomState}。
     */
    private int gameState = GoBangRoomState.GOBANG_READY;

    /**
     * 当前该落子的玩家座位号。
     */
    private int nowPlayerSeat = 0;

    /**
     * 当前步数（每落一子 +1，用来判断超时回调是否已经过期）。
     */
    private int nowStep = 0;

    /**
     * 当前这一手的超时定时器，由 {@code BaseGobangManager.changeNowPlayer} 挂上。
     */
    private ScheduledFuture<?> stepFuture = null;

    /**
     * 获取棋盘。
     *
     * @return 棋盘二维数组
     */
    public int[][] getChessboard() {
        return chessboard;
    }

    /**
     * 设置棋盘。
     *
     * @param chessboard 棋盘二维数组
     */
    public void setChessboard(int[][] chessboard) {
        this.chessboard = chessboard;
    }

    /**
     * 获取游戏阶段。
     *
     * @return 游戏阶段，取值见 {@link GoBangRoomState}
     */
    public int getGameState() {
        return gameState;
    }

    /**
     * 设置游戏阶段。
     *
     * @param gameState 游戏阶段，取值见 {@link GoBangRoomState}
     */
    public void setGameState(int gameState) {
        this.gameState = gameState;
    }

    /**
     * 获取当前该落子的玩家座位号。
     *
     * @return 当前落子玩家的座位号
     */
    public int getNowPlayerSeat() {
        return nowPlayerSeat;
    }

    /**
     * 设置当前该落子的玩家座位号。
     *
     * @param nowPlayerSeat 当前落子玩家的座位号
     */
    public void setNowPlayerSeat(int nowPlayerSeat) {
        this.nowPlayerSeat = nowPlayerSeat;
    }

    /**
     * 获取当前步数。
     *
     * @return 当前步数
     */
    public int getNowStep() {
        return nowStep;
    }

    /**
     * 设置当前步数。
     *
     * @param nowStep 当前步数
     */
    public void setNowStep(int nowStep) {
        this.nowStep = nowStep;
    }

    /**
     * 获取落子超时定时器。
     *
     * @return 落子超时定时器；未挂载时返回 {@code null}
     */
    public ScheduledFuture<?> getStepFuture() {
        return stepFuture;
    }

    /**
     * 设置落子超时定时器。
     *
     * @param stepFuture 落子超时定时器
     */
    public void setStepFuture(ScheduledFuture<?> stepFuture) {
        this.stepFuture = stepFuture;
    }

    /**
     * 重置房间，开始新的一局。
     *
     * <p>清空棋盘、步数归零，并取消上一局遗留的落子超时定时器。
     * <b>注意本方法不重置 {@link #gameState}</b>——阶段由调用方（{@code BaseGobangManager}）
     * 在合适的时机自行设置。</p>
     *
     * <p>{@code cancel(false)} 表示不打断正在执行的超时回调；
     * 由于回调内部按「步数是否一致」判断是否过期（见 {@code BaseGobangManager.playStepTimeout}），
     * 即使旧回调抢先跑了一会儿也不会误改新一局的状态。</p>
     */
    public void reset() {
        chessboard = new int[CHESSBOARD_WIDTH][CHESSBOARD_WIDTH];
        nowStep = 0;
        nowPlayerSeat = 0;

        if (stepFuture != null) {
            stepFuture.cancel(false);
        }
    }

}
