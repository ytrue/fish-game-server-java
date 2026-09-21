package com.ytrue.game.framework.base.manager.gobang;

import com.ytrue.game.framework.base.data.BaseGamePlayer;
import com.ytrue.game.framework.base.data.gobang.BaseGobangPlayer;
import com.ytrue.game.framework.base.data.gobang.BaseGobangRoom;
import com.ytrue.game.framework.engine.utils.ThreadPoolFactory;

import java.util.concurrent.TimeUnit;

/**
 * 基础五子棋管理类。把对局流程（开局、轮转落子方、判胜负、落子超时）固定在框架层，
 * 各五子棋变体只需继承并实现 {@code on*} 系列回调去补玩法细节。
 *
 * <p>本类不加 {@code @Component}——抽象类，Spring 组件扫描会跳过。</p>
 *
 * <p><b>下面所有方法的行内注释都引用同一个例子，先记住它：</b></p>
 * <pre>
 * 房间 maxSize = 2
 *
 * 座位号    玩家     棋子编码
 * ──────────────────────────
 *   0      张三         1       ← 编码 = 座位号 + 1
 *   1      李四         2
 *  （空）               0
 *
 * 棋盘 int[15][15]，写作 chessboard[x][y]：
 *   x 是行（从上往下增大），y 是列（从左往右增大）
 *
 *         y=0  1  2  3  4  5  6  7  8
 *    x=5  │  │  │  │  │  │①│  │  │  │    ① = 张三(座位0)的子，格子值 = 0+1 = 1
 *    x=6  │  │  │  │  │  │  │②│  │  │    ② = 李四(座位1)的子，格子值 = 1+1 = 2
 * </pre>
 *
 * <p>棋子之所以存「座位号 + 1」而不是座位号本身：0 要留给空位，
 * 否则座位 0 的棋子（值 0）会和空位撞车，判胜负时分不清「有子」还是「没子」。</p>
 *
 * @since 1.0.0
 */
public abstract class BaseGobangManager {

    /**
     * 落子操作限制时间（秒）。超过这个时长没落子，由 {@link #playStepTimeout} 自动把出牌权交给下一位。
     *
     * <p>例：张三的回合在 12:00:00 开始，他 30 秒内没点棋盘，12:00:30 定时器触发，出牌权自动交给李四。</p>
     */
    public static final int PLAY_STEP_TIME = 30;

    /**
     * 判连线的四个方向，每个是 {@code {dx, dy}} 偏移：
     * <pre>
     * {1, -1}  右上/左下      行+1 列-1
     * {0,  1}  右  /左        行不变 列+1
     * {1,  0}  下  /上        行+1 列不变
     * {1,  1}  右下/左上      行+1 列+1
     * </pre>
     *
     * <p>只要 4 个方向而不是 8 个：一条直线是双向的，从中心往「右下」数到的子和往「左上」数到的子
     * 属于同一条线，所以每个方向只查半边，反向由代码取负号覆盖（见 {@link #checkWin} 第二段循环）。</p>
     */
    public static final int[][] LINE_DIRECTIONS = {{1, -1}, {0, 1}, {1, 0}, {1, 1}};

    /**
     * 游戏开始（由房间内最后一个座位上的玩家先手）。
     *
     * @param gameRoom 目标房间
     */
    public void gameStart(BaseGobangRoom gameRoom) {
        // 先手玩家。初始 null —— 房间里一个五子棋玩家都没有时就保持 null
        BaseGobangPlayer lastPlayer = null;
        // 遍历房间全部玩家。例：[张三(座位0), 李四(座位1)]
        for (BaseGamePlayer gamePlayer : gameRoom.getGamePlayers()) {
            // 只认五子棋玩家，跳过旁观者或其它玩法的玩家对象（它们的座位号和棋盘编码对不上）
            if (gamePlayer instanceof BaseGobangPlayer) {
                // 每命中一个就覆盖一次。例：第1轮 → 张三；第2轮 → 李四；循环走完 lastPlayer = 李四
                // 这里不能 break：要的就是让循环完整走完，停在最后一个元素上
                lastPlayer = (BaseGobangPlayer) gamePlayer;
            }
        }
        // 转交重载，李四先手
        gameStart(gameRoom, lastPlayer);
    }

    /**
     * 游戏开始（指定先手玩家）。先清空棋盘，再通知子类开局，最后把出牌权交给先手玩家。
     *
     * @param gameRoom    目标房间
     * @param startPlayer 先手玩家
     */
    public void gameStart(BaseGobangRoom gameRoom, BaseGobangPlayer startPlayer) {
        // 清空棋盘（15×15 全变 0）、nowStep=0、nowPlayerSeat=0，并 cancel 掉上一局遗留的定时器
        gameRoom.reset();
        // 子类回调：下发「开局了」的协议、放背景音乐等。框架不关心里面做什么，只管按顺序调
        onGameStart(gameRoom);
        // 把出牌权交给先手。第 3 个参数传「当前步数」（此刻是 0）作为凭据，
        // changeNowPlayer 内部拿它和房间当前步数比对，一致才生效
        changeNowPlayer(gameRoom, startPlayer, gameRoom.getNowStep());
    }

    /**
     * 玩家落子。
     *
     * @param gameRoom   目标房间
     * @param gamePlayer 落子的玩家
     * @param x          横坐标
     * @param y          纵坐标
     */
    public void playChess(BaseGobangRoom gameRoom, BaseGobangPlayer gamePlayer, int x, int y) {
        // 锁住房间对象。判回合 + 写棋盘 + 切下一手必须原子完成，
        // 否则两个玩家同时落子会互相覆盖。锁对象是房间本身，与 playStepTimeout 用的是同一把
        synchronized (gameRoom) {
            // 判断是不是他的回合。例：房间 nowPlayerSeat=1，李四 seat=1 → 1==1 成立
            // 不成立就整段跳过，不报错也不回复——客户端网络重发或玩家连点时会走到这里
            if (gameRoom.getNowPlayerSeat() == gamePlayer.getSeat()) {
                // 把棋子写进棋盘。例：李四(座位1)落(7,7) → chessboard[7][7] = 1+1 = 2
                setSeatChess(gameRoom, gamePlayer.getSeat(), x, y);
                // 子类回调：下发「有人落子」的协议，客户端据此在 (7,7) 画出李四的棋子
                onPlayChess(gameRoom, gamePlayer, x, y);
                // 检查这一手是否连成五子（沿四个方向数同色棋子，过程见 checkWin）
                if (!checkWin(gameRoom, gamePlayer, x, y)) {
                    // 没赢 → 把出牌权交给下一位。
                    // 传 gameRoom.getNowStep()（此刻=1）当凭据，changeNowPlayer 会校验它没被改过
                    changeNextPlayer(gameRoom, gameRoom.getNowStep());
                } else {
                    // 赢了 → 先回调子类结算奖励，再清空棋盘准备下一局
                    playerWin(gameRoom, gamePlayer);
                }
            }
        }
    }

    /**
     * 玩家获胜。先通知子类结算（发奖励、记战绩等），再重置棋盘为下一局做准备。
     *
     * @param gameRoom   目标房间
     * @param gamePlayer 获胜玩家
     */
    public void playerWin(BaseGobangRoom gameRoom, BaseGobangPlayer gamePlayer) {
        // 子类回调：发金币奖励、写战绩日志、把 gameState 置为 GOBANG_OVER
        // 必须在 reset() 之前——若反过来，子类在回调里读棋盘会看到已经清空的数据，结算无从谈起
        onPlayerWin(gameRoom, gamePlayer);
        // 再清空棋盘，开始新的一局
        gameRoom.reset();
    }

    /**
     * 检查玩家这一手是否连成五子：以刚落下的 {@code (x, y)} 为中心，沿四个方向各向两侧数同色棋子，
     * 任一方向合计达到 5 颗即为获胜。
     *
     * @param gameRoom   目标房间
     * @param gamePlayer 落子的玩家
     * @param x          刚落子的横坐标
     * @param y          刚落子的纵坐标
     * @return 连成五子返回 {@code true}
     */
    private boolean checkWin(BaseGobangRoom gameRoom, BaseGobangPlayer gamePlayer, int x, int y) {
        // 依次试 4 个方向。下面以「张三在 (4,4) 落子，且 (5,5)(6,6)(7,7)(8,8) 都是他的子」为例走一遍
        // {{1, -1}, {0, 1}, {1, 0}, {1, 1}};
        for (int[] lineDirection : LINE_DIRECTIONS) {
            // 本方向累计的同色棋子数。中心那颗 (x,y) 在第 ② 步已经写进棋盘了，
            // 但两个循环都是从相邻格（i=1）开始数的，所以这里先算 1 颗把它补上
            int lineCount = 1;

            // ===== 正方向：从中心往外走 1~4 格 =====
            // i 最多到 4，因为 5 颗就赢了，再数没有意义
            for (int i = 1; i < 5; i++) {
                // 算出第 i 个格子的坐标并取值。例（方向 {1,1}，中心 (4,4)，i=1）：
                //   x + 1*1 = 5，y + 1*1 = 5 → 取 (5,5) 的值 = 1
                // 出界时 getSeatChess 返回 -1
                int seatChess = getSeatChess(gameRoom, x + lineDirection[0] * i, y + lineDirection[1] * i);
                // 判「这格是不是我的」：格子值 - 1 就是座位号。
                // 例：取值 1，1 - 1 = 0，张三座位正是 0 → 相等，是己方子
                if (seatChess - 1 == gamePlayer.getSeat()) {
                    // 是己方子，计数 +1。i=1,2,3,4 依次命中 → lineCount 涨到 2,3,4,5
                    lineCount++;
                } else {
                    // 不是己方子就断开：空位(0)、对方子(对方座位+1)、出界(-1) 都会走到这里
                    // 必须 break 不能 continue——五子棋要求 5 颗「连着」，
                    // 跳过去继续数的话「①①①空①①」这种断开的形状会被误判成 6 连
                    break;
                }
            }

            // ===== 反方向：偏移量取负号，往另一侧数 =====
            // 例（方向 {1,1}，中心 (4,4)，i=1）：x - 1*1 = 3，y - 1*1 = 3 → 取 (3,3)
            for (int i = 1; i < 5; i++) {
                // 取另一侧第 i 格的值，坐标算法与正方向一样，只是把 dx、dy 都取了负号
                int seatChess = getSeatChess(gameRoom, x - lineDirection[0] * i, y - lineDirection[1] * i);
                // 同样是「格子值 - 1 == 座位号」判己方子
                if (seatChess - 1 == gamePlayer.getSeat()) {
                    // 同为己方子，计数 +1
                    lineCount++;
                } else {
                    // 例：(3,3) 是空位 0 → 走到这里 break，这一侧不再往远处数
                    break;
                }
            }

            // 本方向累计够 5 颗 → 获胜。例：lineCount 数到 5，5 >= 5 成立
            if (lineCount >= 5) {
                // 直接返回 true，后面还没试的方向不用再看了
                return true;
            }
        }

        // 四个方向全试完都没凑够 5 颗 → 这一手没赢
        return false;
    }

    /**
     * 在指定位置放置棋子。存的是「座位号 + 1」，因为 0 要表示空位。本方法不做边界检查。
     *
     * @param gameRoom 目标房间
     * @param seat     落子玩家的座位号
     * @param x        横坐标
     * @param y        纵坐标
     */
    public void setSeatChess(BaseGobangRoom gameRoom, int seat, int x, int y) {
        // 例：李四(座位1)落(7,7) → chessboard[7][7] = 1 + 1 = 2
        // 不做边界检查：调用前坐标已经校验过，正常路径不会越界。
        // 真正需要兜越界的是读取侧的 getSeatChess——判连线要按偏移量算坐标，贴边落子必然算出界外
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
        // 越界返回 -1。例：x = -1 → -1；y = 15（棋盘宽 15，合法列 0~14）→ -1
        // 选 -1 是因为它和「空位 0」「棋子 1~N」都不冲突：
        // 判连线写的是「格子值 - 1 == 座位号」，-1 算出来是 -2，不等于任何合法座位号，
        // 于是连线自然在这里断开，checkWin 里不必再单独判一次边界
        if (x < 0 || x >= BaseGobangRoom.CHESSBOARD_WIDTH || y < 0 || y >= BaseGobangRoom.CHESSBOARD_WIDTH) {
            // 出界统一用 -1 表示，调用方只需判这一个值
            return -1;
        }

        // 界内就直接返回该格的值。例：(5,5) → 1（张三的子）；(5,6) → 0（空位）
        return gameRoom.getChessboard()[x][y];
    }

    /**
     * 落子超时回调：把出牌权交给下一位。
     *
     * @param gameRoom 目标房间
     * @param nowStep  挂定时器时的步数
     */
    private void playStepTimeout(BaseGobangRoom gameRoom, int nowStep) {
        // 与 playChess 共用房间这把锁：防「玩家刚落子」和「超时触发」同时发生，
        // 否则可能出现两个人同时被当成当前落子方
        synchronized (gameRoom) {
            // 把「挂定时器时记下的步数」原样传下去，交给 changeNowPlayer 校验是否已过期。
            // 例：12:00:00 张三的回合开始时 nowStep=2，12:00:30 触发，这里传的就是 2
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
        // 锁住房间，防止「玩家落子」和「出牌权切换」同时进行
        synchronized (gameRoom) {
            // 校验凭据：传进来的步数 == 房间当前步数？
            // 例A 通过：传入 1，房间 nowStep 也是 1（李四落子时置的）→ 切换生效
            // 例B 过期：传入 2，房间 nowStep 已经是 3（玩家抢先落子了）→ 整段跳过，安静返回，不算异常
            if (nowStep == gameRoom.getNowStep()) {
                // 把「当前该落子的座位号」改成新玩家。例：李四(1) → 张三(0)
                gameRoom.setNowPlayerSeat(nowPlayer.getSeat());
                // 步数 +1。例：1 → 2
                // nowStep 记的是「第几手」，每切换一手就 +1；开局也会 +1，所以开局后它就是 1
                gameRoom.setNowStep(gameRoom.getNowStep() + 1);
                // 上一位玩家遗留的定时器要取消，否则多个超时回调会同时存在。
                // 第一次进来时 stepFuture 还是 null，所以必须先判空
                if (gameRoom.getStepFuture() != null) {
                    // cancel(false) = 不打断正在执行的回调；
                    // 就算它已经开始跑了，也会被上面那个步数校验挡掉
                    gameRoom.getStepFuture().cancel(false);
                }
                // 挂新的 30 秒超时：到点还没落子就自动过手。
                // 传的是自增后的 nowStep（= 2），和房间当前步数一致，
                // 所以这个定时器不会一挂上就被当成过期作废
                gameRoom.setStepFuture(ThreadPoolFactory.TASK_SERVICE_POOL.schedule(
                        () -> playStepTimeout(gameRoom, gameRoom.getNowStep()), PLAY_STEP_TIME, TimeUnit.SECONDS)
                );
                // 子类回调：下发「该张三下了，剩余 30 秒」的协议
                onChangeNowPlayer(gameRoom, nowPlayer);
            }
        }
    }

    /**
     * 切换到下一个玩家落子。先检查棋盘是否已下满（满则判和棋，房主获胜），再把出牌权交给下一位。
     *·
     * @param gameRoom 目标房间
     * @param nowStep  调用方观测到的步数
     */
    public void changeNextPlayer(BaseGobangRoom gameRoom, int nowStep) {
        // 锁住房间
        synchronized (gameRoom) {
            // 棋盘是否下满。先假定满了，下面只要找到一个空位就推翻
            boolean finish = true;
            // 逐行扫棋盘（chessboard[x] 就是第 x 行）
            for (int[] chessLine : gameRoom.getChessboard()) {
                // 再逐格扫这一行
                for (int chess : chessLine) {
                    // 只要还有一格是 0（空位），就说明没满
                    if (chess == 0) {
                        // 一旦发现有空格，finish 就被置为 false 且之后不会再变回 true
                        finish = false;
                    }
                }
            }

            // 下满了 → 判和棋。「平局房主赢」即 0 号座位获胜
            if (finish) {
                // 取 0 号座位上的玩家对象当赢家传进回调
                onPlayerWin(gameRoom, gameRoom.getGamePlayerBySeat(0));
                // 已判和棋，直接收工，不再往下切出牌权
                return;
            }

            // 从「当前座位的下一格」开始绕圈找，并跳过空座位。
            // i 从 1 走到 maxSize-1，正好把除自己以外的座位都看一遍
            for (int i = 1; i < gameRoom.getMaxSize(); i++) {
                // 取模实现绕圈：座位 2 之后回到 0，不会越界
                int seat = (gameRoom.getNowPlayerSeat() + i) % gameRoom.getMaxSize();
                // 取该座位上的玩家对象；座位空着时返回 null
                BaseGobangPlayer nextPlayer = gameRoom.getGamePlayerBySeat(seat);
                // 空座位跳过。旧实现没判空，房内只剩 1 人时会把 null 传进 changeNowPlayer 抛 NPE
                if (nextPlayer != null) {
                    // 找到人即切换；nowStep 原样传下去，由 changeNowPlayer 校验凭据
                    changeNowPlayer(gameRoom, nextPlayer, nowStep);
                    // 只切一个人
                    return;
                }
            }
            // 绕完一圈都没找到别人（房里只剩当前这一个玩家）→ 什么都不做。
            // 注意此时旧定时器已被上一手 cancel 掉，也没有挂新的，所以这一局会停在这儿，
            // 等该玩家落子、或房间被销毁
            // 一般来说不会有这个问题的
        }
    }

    /**
     * 房间游戏开始回调，由子类实现。
     *
     * @param gameRoom 目标房间
     */
    public abstract void onGameStart(BaseGobangRoom gameRoom);

    /**
     * 玩家落子成功回调，由子类实现（下发落子消息、播放音效等）。
     *
     * @param gameRoom  目标房间
     * @param nowPlayer 落子的玩家
     * @param x         横坐标
     * @param y         纵坐标
     */
    public abstract void onPlayChess(BaseGobangRoom gameRoom, BaseGobangPlayer nowPlayer, int x, int y);

    /**
     * 玩家获胜回调，由子类实现（结算奖励、记录战绩、设置房间状态等）。
     *
     * @param gameRoom  目标房间
     * @param nowPlayer 获胜玩家
     */
    public abstract void onPlayerWin(BaseGobangRoom gameRoom, BaseGobangPlayer nowPlayer);

    /**
     * 改变当前落子玩家回调，由子类实现（下发「该谁下了」以及剩余时间）。
     *
     * @param gameRoom  目标房间
     * @param nowPlayer 新的落子玩家
     */
    public abstract void onChangeNowPlayer(BaseGobangRoom gameRoom, BaseGobangPlayer nowPlayer);

}
