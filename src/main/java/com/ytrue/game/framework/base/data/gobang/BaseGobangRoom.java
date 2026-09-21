package com.ytrue.game.framework.base.data.gobang;

import com.ytrue.game.framework.base.data.BaseGameRoom;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ScheduledFuture;

/**
 * 基础五子棋房间。
 *
 * <p>在通用房间之上加了棋盘、对局阶段、当前落子方、步数与落子倒计时。</p>
 *
 * <p><b>棋盘怎么存的</b>：{@link #CHESSBOARD_WIDTH} × {@link #CHESSBOARD_WIDTH} 的二维数组，
 * 每格的值是「<b>座位号 + 1</b>」：</p>
 * <pre>
 *   0        —— 空位
 *   1 ~ N    —— 该座位号玩家的棋子（1 = 座位0，2 = 座位1，……）
 * </pre>
 *
 * <p>之所以 +1，是为了给「空位」留出 0 这个值。判胜负时判断「这格是不是我的」写的是
 * {@code 格子值 - 1 == player.getSeat()}；如果直接存座位号，座位 0 的棋子就和空位撞了，
 * 整个判断都得推倒重来。</p>
 *
 * <p><b>注意座位号的取值范围</b>：座位号来自 {@code BaseGameRoom} 的入座策略。
 * 策略一给的是「0 ~ MAX_SEAT_NUMBER-1 里最小的空闲号」，策略二给的是「槽位下标」。
 * 本类本身两种都支持，但 {@code BaseGobangManager} 的轮转落子是<b>拿下标当座位号</b>用的，
 * 所以五子棋房间应当覆写 {@code addPlayer} 走策略二。</p>
 *
 * @since 1.0.0
 */
@Getter
@Setter
public abstract class BaseGobangRoom extends BaseGameRoom {

    /**
     * 棋盘宽度（同时也是高度，棋盘是正方形）。
     */
    public static final int CHESSBOARD_WIDTH = 15;

    /**
     * 棋盘（行 × 列，0 表示空位，座位号 + 1 表示该座位的棋子）。
     *
     * <p>由 {@link #reset()} 创建，所以开局前是 {@code null}——
     * 直接读 {@link #getChessboard()} 而不先开局会拿到 null。</p>
     *
     * int[][] chessboard = {
     *     {0, 1, 0, 0},
     *     {2, 0, 0, 3},
     *     {0, 0, 1, 0}
     * };
     *       0  1  2  3
     *     ┌──┬──┬──┬──┐
     * 0   │  │①│  │  │
     *     ├──┼──┼──┼──┤
     * 1   │②│  │  │③│
     *     ├──┼──┼──┼──┤
     * 2   │  │  │①│  │
     *     └──┴──┴──┴──┘
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
     * 当前步数。
     *
     * <p>每落一子 +1。它还是「落子超时定时器是否过期」的凭据：
     * 挂定时器时把当时的步数一起记下来，回调触发时比对步数——
     * 对不上说明玩家已经落过子了，这次超时作废。</p>
     */
    private int nowStep = 0;

    /**
     * 当前这一手的超时定时器，由 {@code BaseGobangManager.changeNowPlayer} 挂上。
     */
    private ScheduledFuture<?> stepFuture = null;

    /**
     * 重置房间，开始新的一局。
     *
     * <p>清空棋盘、步数归零、取消上一局遗留的落子超时定时器。</p>
     *
     * <p><b>注意本方法不重置 {@link #gameState}</b>——阶段由调用方自行设置
     * （{@code BaseGobangManager.gameStart} 也不设，最终是子类的
     * {@code onPlayerWin} 负责把阶段改成 {@link GoBangRoomState#GOBANG_OVER}）。</p>
     *
     * <p>{@code cancel(false)} 表示不打断正在执行的超时回调。
     * 由于回调内部会按「步数是否一致」判断过期（见 {@link #nowStep}），
     * 即使旧回调抢先跑了一会儿，也不会误改新一局的状态。</p>
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
