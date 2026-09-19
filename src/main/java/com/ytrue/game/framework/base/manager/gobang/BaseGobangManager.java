package com.ytrue.game.framework.base.manager.gobang;

import com.ytrue.game.framework.base.data.BaseGamePlayer;
import com.ytrue.game.framework.base.data.gobang.BaseGobangPlayer;
import com.ytrue.game.framework.base.data.gobang.BaseGobangRoom;
import com.ytrue.game.framework.base.manager.BaseRoomManager;
import com.ytrue.game.framework.engine.utils.ThreadPoolFactory;

import java.util.concurrent.TimeUnit;

/**
 * 基础五子棋管理类。
 *
 * <p>把五子棋的对局流程（开局、轮转落子方、判胜负、落子超时）固定在框架层，
 * 各五子棋变体只需继承并实现 {@code *0} 系列回调去补玩法细节。</p>
 *
 * <p><b>胜负判定</b>：棋子用「座位号 + 1」编码（见 {@code BaseGobangRoom}），
 * 因此判断某格是否属于玩家 {@code p} 的条件是 {@code 格子值 - 1 == p.getSeat()}。</p>
 *
 * <p><b>注意：本类不加 {@code @Component}</b>——它是抽象类，Spring 组件扫描会跳过抽象类，
 * 标了注解也不会生成 bean。</p>
 *
 * @since 1.0.0
 */
public abstract class BaseGobangManager extends BaseRoomManager {

    /**
     * 落子操作限制时间（秒）。
     *
     * <p>超过这个时长没落子，由 {@link #playStepTimeout} 自动把出牌权交给下一位。</p>
     */
    public static final int PLAY_STEP_TIME = 30;

    /**
     * 判连线的四个方向（横、竖、两条斜线）。每个方向只需查半边，反向由代码取负号覆盖。
     */
    public static final int[][] LINE_DIRECTIONS = { { 1, -1 }, { 0, 1 }, { 1, 0 }, { 1, 1 } };

    /**
     * 游戏开始（由房间内最后一个座位上的玩家先手）。
     *
     * @param gameRoom 目标房间
     */
    public void gameStart(BaseGobangRoom gameRoom) {
        BaseGobangPlayer lastPlayer = null;
        for (BaseGamePlayer gamePlayer : gameRoom.getGamePlayers()) {
            if (gamePlayer instanceof BaseGobangPlayer) {
                // 不 break：循环走完拿到的就是「最后一个」五子棋玩家
                lastPlayer = (BaseGobangPlayer) gamePlayer;
            }
        }
        gameStart(gameRoom, lastPlayer);
    }

    /**
     * 游戏开始（指定先手玩家）。
     *
     * <p>先清空棋盘，再通知子类开局，最后把出牌权交给先手玩家。</p>
     *
     * <p><b>已知缺陷（保持旧行为，未修复）</b>：房间内没有五子棋玩家时，
     * 上面的重载会传入 {@code null}，走到 {@link #changeNowPlayer} 时对 {@code nowPlayer.getSeat()}
     * 解引用会抛 {@code NullPointerException}。旧工程同样如此。</p>
     *
     * @param gameRoom    目标房间
     * @param startPlayer 先手玩家
     */
    public void gameStart(BaseGobangRoom gameRoom, BaseGobangPlayer startPlayer) {
        gameRoom.reset();
        gameStart0(gameRoom);
        // 把 nowStep 一并传下去：changeNowPlayer 会用它和房间的当前步数比对
        changeNowPlayer(gameRoom, startPlayer, gameRoom.getNowStep());
    }

    /**
     * 玩家落子。
     *
     * <p>整段用 {@code synchronized (gameRoom)} 圈起来：判「是不是轮到你」、写棋盘、
     * 切下一手这三步必须原子完成，否则两个玩家同时落子会互相覆盖。
     * 锁对象是房间实例本身，与 {@link #playStepTimeout} 用的是同一把锁。</p>
     *
     * @param gameRoom   目标房间
     * @param gamePlayer 落子的玩家
     * @param x          横坐标
     * @param y          纵坐标
     */
    public void playChess(BaseGobangRoom gameRoom, BaseGobangPlayer gamePlayer, int x, int y) {
        synchronized (gameRoom) {
            // 不是你的回合，忽略本次落子（不报错，客户端可能因网络重发）
            if (gameRoom.getNowPlayerSeat() == gamePlayer.getSeat()) {
                setSeatChess(gameRoom, gamePlayer.getSeat(), x, y);
                playChess0(gameRoom, gamePlayer, x, y);
                if (!checkWin(gameRoom, gamePlayer, x, y)) {
                    changeNextPlayer(gameRoom, gameRoom.getNowStep());
                } else {
                    playerWin(gameRoom, gamePlayer);
                }
            }
        }
    }

    /**
     * 玩家获胜。
     *
     * <p>先通知子类结算（发奖励、记战绩等），再重置棋盘为下一局做准备。</p>
     *
     * <p>注意本方法<b>不设置 {@code gameState}</b>：把阶段置为 {@code GOBANG_OVER}
     * 应由子类的 {@link #playerWin0} 自行完成。</p>
     *
     * @param gameRoom   目标房间
     * @param gamePlayer 获胜玩家
     */
    public void playerWin(BaseGobangRoom gameRoom, BaseGobangPlayer gamePlayer) {
        playerWin0(gameRoom, gamePlayer);
        gameRoom.reset();
    }

    /**
     * 检查玩家这一手是否连成五子。
     *
     * <p>以刚落下的 {@code (x, y)} 为中心，沿四个方向各向两侧数同色棋子，
     * 任一方向合计达到 5 颗即为获胜。</p>
     *
     * @param gameRoom   目标房间
     * @param gamePlayer 落子的玩家
     * @param x          刚落子的横坐标
     * @param y          刚落子的纵坐标
     * @return 连成五子返回 {@code true}
     */
    private boolean checkWin(BaseGobangRoom gameRoom, BaseGobangPlayer gamePlayer, int x, int y) {
        for (int[] lineDirection : LINE_DIRECTIONS) {
            // 中心那一颗已经算在内，因此从 1 开始
            int lineCount = 1;

            // 正方向最多再数 4 颗（合计 5 颗即获胜，多数无意义）
            for (int i = 1; i < 5; i++) {
                int seatChess = getSeatChess(gameRoom, x + lineDirection[0] * i, y + lineDirection[1] * i);
                if (seatChess - 1 == gamePlayer.getSeat()) {
                    lineCount++;
                } else {
                    // 遇到空位、对方棋子或出界就停，不能跳过断点继续数
                    break;
                }
            }

            // 反方向同理
            for (int i = 1; i < 5; i++) {
                int seatChess = getSeatChess(gameRoom, x - lineDirection[0] * i, y - lineDirection[1] * i);
                if (seatChess - 1 == gamePlayer.getSeat()) {
                    lineCount++;
                } else {
                    break;
                }
            }

            if (lineCount >= 5) {
                return true;
            }
        }

        return false;
    }

    /**
     * 在指定位置放置棋子。
     *
     * <p>存的是「座位号 + 1」，因为 0 要表示空位。本方法不做边界检查。</p>
     *
     * @param gameRoom 目标房间
     * @param seat     落子玩家的座位号
     * @param x        横坐标
     * @param y        纵坐标
     */
    public void setSeatChess(BaseGobangRoom gameRoom, int seat, int x, int y) {
        gameRoom.getChessboard()[x][y] = seat + 1;
    }

    /**
     * 获取指定位置的棋子。
     *
     * @param gameRoom 目标房间
     * @param x        横坐标
     * @param y        纵坐标
     * @return 该格的值：{@code 0} 空位、{@code 座位号 + 1} 有子、{@code -1} 坐标出界
     */
    public int getSeatChess(BaseGobangRoom gameRoom, int x, int y) {
        // 出界返回 -1：与「空位 0」「座位 1~N」都不冲突，判连线时自然断在这里
        if (x < 0 || x >= BaseGobangRoom.CHESSBOARD_WIDTH || y < 0 || y >= BaseGobangRoom.CHESSBOARD_WIDTH) {
            return -1;
        }

        return gameRoom.getChessboard()[x][y];
    }

    /**
     * 落子超时回调：把出牌权交给下一位。
     *
     * <p>传入的 {@code nowStep} 是「挂这个定时器时」的步数，
     * {@link #changeNowPlayer} 会拿它和房间当前步数比对——一致才切换。
     * 这样即使玩家抢先落子后旧定时器才触发，也不会把出牌权多切一手。</p>
     *
     * @param gameRoom 目标房间
     * @param nowStep  挂定时器时的步数
     */
    private void playStepTimeout(BaseGobangRoom gameRoom, int nowStep) {
        synchronized (gameRoom) {
            changeNextPlayer(gameRoom, nowStep);
        }
    }

    /**
     * 改变当前落子玩家，并重挂落子超时定时器。
     *
     * @param gameRoom  目标房间
     * @param nowPlayer 新的落子玩家
     * @param nowStep   调用方观测到的步数
     */
    public void changeNowPlayer(BaseGobangRoom gameRoom, BaseGobangPlayer nowPlayer, int nowStep) {
        synchronized (gameRoom) {
            // 步数对不上说明这个调用已经过期（例如旧超时定时器延迟触发），直接丢弃
            if (nowStep == gameRoom.getNowStep()) {
                gameRoom.setNowPlayerSeat(nowPlayer.getSeat());
                gameRoom.setNowStep(gameRoom.getNowStep() + 1);
                // 取消上一个玩家遗留的定时器，避免多个超时回调同时存在
                if (gameRoom.getStepFuture() != null) {
                    gameRoom.getStepFuture().cancel(false);
                }
                // 挂新的超时：到点还没落子就自动过手。
                // 传的是自增后的 nowStep，与房间当前步数一致，所以这个定时器不会立刻失效
                gameRoom.setStepFuture(ThreadPoolFactory.TASK_SERVICE_POOL.schedule(
                        () -> playStepTimeout(gameRoom, gameRoom.getNowStep()), PLAY_STEP_TIME, TimeUnit.SECONDS));
                changeNowPlayer0(gameRoom, nowPlayer);
            }
        }
    }

    /**
     * 切换到下一个玩家落子。
     *
     * <p>先检查棋盘是否已下满（满则判和棋，房主获胜），再把出牌权交给下一位。</p>
     *
     * <p><b>已知缺陷（保持旧行为，未修复）</b>，有两处：</p>
     * <ol>
     *     <li><b>可能选中空座位</b>：选人的逻辑是「取第一个下标不等于当前落子座位号的座位」，
     *         并没有判断该座位上有没有人。房内只有 1 人时，会取到空座位的 {@code null}，
     *         传进 {@link #changeNowPlayer} 后抛 {@code NullPointerException}。
     *         棋力正常的对局走不到这里，但玩家中途退出就会触发；</li>
     *     <li><b>和棋判断后会继续切人</b>：{@code finish} 为真时调用了 {@code playerWin0}，
     *         但没有 {@code return}，紧接着仍会往下找下一位玩家切换出牌权。
     *         依赖子类的 {@code playerWin0} 内部改掉房间状态来兜住。</li>
     * </ol>
     *
     * @param gameRoom 目标房间
     * @param nowStep  调用方观测到的步数
     */
    public void changeNextPlayer(BaseGobangRoom gameRoom, int nowStep) {
        synchronized (gameRoom) {
            boolean finish = true;
            for (int[] chessLine : gameRoom.getChessboard()) {
                for (int chess : chessLine) {
                    if (chess == 0) {
                        finish = false;
                    }
                }
            }

            // 平局房主赢
            if (finish) {
                playerWin0(gameRoom, gameRoom.getGamePlayerByIndex(0));
            }

            for (int seat = 0; seat < gameRoom.getMaxSize(); seat++) {
                if (seat != gameRoom.getNowPlayerSeat()) {
                    changeNowPlayer(gameRoom, gameRoom.getGamePlayerByIndex(seat), nowStep);
                    return;
                }
            }
        }
    }

    /**
     * 房间游戏开始回调，由子类实现。
     *
     * @param gameRoom 目标房间
     */
    public abstract void gameStart0(BaseGobangRoom gameRoom);

    /**
     * 玩家落子成功回调，由子类实现（下发落子消息、播放音效等）。
     *
     * @param gameRoom  目标房间
     * @param nowPlayer 落子的玩家
     * @param x         横坐标
     * @param y         纵坐标
     */
    public abstract void playChess0(BaseGobangRoom gameRoom, BaseGobangPlayer nowPlayer, int x, int y);

    /**
     * 玩家获胜回调，由子类实现（结算奖励、记录战绩、设置房间阶段等）。
     *
     * @param gameRoom  目标房间
     * @param nowPlayer 获胜玩家
     */
    public abstract void playerWin0(BaseGobangRoom gameRoom, BaseGobangPlayer nowPlayer);

    /**
     * 改变当前落子玩家回调，由子类实现（下发「该谁下了」以及剩余时间）。
     *
     * @param gameRoom  目标房间
     * @param nowPlayer 新的落子玩家
     */
    public abstract void changeNowPlayer0(BaseGobangRoom gameRoom, BaseGobangPlayer nowPlayer);

}
