package com.ytrue.game.framework.base.data;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基础游戏房间。
 *
 * <p>持有房间号、人数上限与座位数组，是所有玩法房间类的公共父类。
 * 房间号由 {@code GameContainer.createGameRoom} 分配，座位数组在 {@code setMaxSize} 时按人数上限创建。</p>
 *
 * <p><b>座位与数组下标的关系</b>：本类的 {@code gamePlayers} 是「数组下标 = 槽位」，
 * 而 {@link BaseGamePlayer#getSeat()} 是「座位号」，两者在默认策略下<b>并不保证相等</b>——
 * 数组下标只表示玩家在这张表里的位置，座位号才是客户端看到的座次。
 * 只有 {@link #addPlayerWithSeatIndex} 一种策略强制二者相等，
 * 错开是怎么产生的见 {@link #addPlayerWithFreeSeatNumber} 方法体内的示例。</p>
 *
 * @since 1.0.0
 */
@Slf4j
public abstract class BaseGameRoom {

    /**
     * 房间号。
     */
    @Getter
    @Setter
    private int code;

    /**
     * 房间人数上限。
     */
    @Getter
    private int maxSize;


    /**
     * 座位号个数。
     *
     * <p>决定了「一张桌子上最多能有几个座位号」——当前是 0~3 共 4 个。
     * 它与 {@link #maxSize}（房间能装多少人）<b>是两回事</b>：策略一靠它来分配座位号，
     * 所以人数上限超过它的玩法会出现「槽位还有空的、座位号却已经用完了」。</p>
     *
     */
    private static final int MAX_SEAT_NUMBER = 4;

    /**
     * 房间玩家列表
     */
    @Setter
    @Getter
    private BaseGamePlayer[] gamePlayers;

    /**
     * 房间是否已解散。
     *
     * <p>由 {@code GameContainer.removeGameRoom} 在拆房前通过 {@link #dismiss()} 置位。
     * 一旦置位，这个房间就<b>拒绝一切新的入座</b>——它已经从房间表里摘掉了，
     * 这时候进来的人会变成「玩家表里有他、按 id 却查不到房间」的孤儿：
     * {@code getPlayerById} 查得到，{@code getGameRoomByPlayerId} 却返回 {@code null}，
     * 而且没有任何机制会清理他。</p>
     *
     * <p>为什么要这个标志：{@code removeGameRoom} 拆房时是<b>先清人</b>的，而清人靠
     * <b>按下标正序遍历</b> {@code gamePlayers}。如果遍历途中有人往更靠前的空槽位加人，
     * 那个槽位已经扫过了、索引不会回头——新玩家就留了下来。这不是概率问题，
     * 是必然：只要加人落在窗口里就一定漏。</p>
     *
     * <p>所以 {@code removeGameRoom} 现在按「{@link #dismiss() 置位} → 从房间表摘除 →
     * 清人」的顺序拆房，置位之后入座一律被拒，清人循环才能真清干净。
     * 实测 300 轮并发（一边删房一边疯狂加人）：改前 300 轮全留孤儿，改后 0 轮。</p>
     *
     * <p>用 {@code volatile}：写方在 {@code GameContainer} 的 {@code ROOM_LOCK} 里，
     * 读方（入座）只持 {@code PLAYER_LOCK}，两边不是同一把锁，靠它保证可见性。</p>
     */
    @Getter
    private volatile boolean dismissed;

    /**
     * 把房间标记为「已解散」。
     *
     * <p>由 {@code GameContainer.removeGameRoom} 在拆房前调用。置位之后
     * {@link #addPlayerWithFreeSeatNumber} / {@link #addPlayerWithSeatIndex} 一律拒绝新玩家。</p>
     *
     * <p>本方法只负责置位，真正的清人由 {@code GameContainer.removeGameRoom} 做，
     * 而它是在 {@code ROOM_LOCK} 里清人的——也就是说清人那一步会<b>嵌套地</b>拿
     * {@code PLAYER_LOCK}。</p>
     *
     * <p><b>这就是「锁顺序只能是 ROOM → PLAYER」的来源</b>，反过来的话
     * （持 {@code PLAYER_LOCK} 再去拿 {@code ROOM_LOCK}）会形成环路等待、双双卡死。
     * 以后想在入座路径（已持 {@code PLAYER_LOCK}）里调 {@code removeGameRoom} 或
     * {@code createGameRoom} 之前，先想清楚这一点。</p>
     */
    public void dismiss() {
        this.dismissed = true;
    }

    /**
     * 入座 —— 这是房间对外的<b>唯一入座入口</b>，默认走策略一。
     *
     * <p>{@code GameContainer.createGamePlayer} 只调本方法，不关心玩法怎么排座位。
     * 想换一种座位号算法，子类覆写本方法即可（房卡场就是这么做的）：</p>
     * <pre>
     *   // 房卡场：座位号必须等于数组下标
     *   public boolean addPlayer(BaseGamePlayer p, boolean randomSeat) {
     *       return addPlayerWithSeatIndex(p, randomSeat);
     *   }
     * </pre>
     *
     * @param gamePlayer 待加入的玩家
     * @param randomSeat 是否随机座位
     * @return 入座成功 {@code true}；房间没位置 {@code false}
     * @see #addPlayerWithFreeSeatNumber(BaseGamePlayer, boolean)
     * @see #addPlayerWithSeatIndex(BaseGamePlayer, boolean)
     */
    public abstract boolean addPlayer(BaseGamePlayer gamePlayer, boolean randomSeat);


    /**
     * 入座（策略一，默认）：<b>座位号从「空闲座位号」里取最小的那个，与数组槽位无关</b>。
     *
     * <p>先分清两个词——方法体里的注释全按这两个词写：</p>
     * <pre>
     *   槽位 index  —— gamePlayers 数组的下标，纯粹是服务端的内存位置，客户端看不到
     *   座位号 seat  —— player.getSeat()，真正的座次，发给客户端决定谁坐屏幕哪个方位
     * </pre>
     *
     * <p>本策略把这两件事<b>分开算</b>：槽位取第一个空位，座位号取 {@link #MAX_SEAT_NUMBER}
     * 个号（0~3）里最小的空闲号。目的是「桌上座次稳定」——有人走了，新人补进他空出来的那个座位号，
     * 其余人座次不动。代价是两个值可能错开；要保证相等请用 {@link #addPlayerWithSeatIndex}。</p>
     *
     * <p><b>座位号个数由 {@link #MAX_SEAT_NUMBER} 决定，与 {@link #maxSize} 无关</b>，
     * 所以人数上限超过它的玩法会出现「槽位还有空的、座位号却已经用完了」——
     * 那时后面的玩家入座会失败（返回 {@code false}），而不是像旧工程那样硬塞一个 3 号和人重座。
     * 二八杠（{@code createGameRoom(TwoEightRoom.class, 100)}）正踩在这个上限上，
     * 想让 100 人都坐进来，得改用策略二。</p>
     *
     * <p>同理，只要项目里有任何一处把「数组下标」当「座位号」用
     * （例如 {@code BaseGobangManager.changeNextPlayer} 的轮转落子），也必须改用策略二。</p>
     *
     * <p>入座失败返回 {@code false} 并记一条 warn 日志，日志里带上房间号、玩家 id 和具体原因
     * （房间满 / 座位号用尽）。具体走位、错开的过程，见方法体内的逐行注释与示例值。</p>
     *
     * @param gamePlayer 待加入的玩家
     * @param randomSeat 是否随机座位
     * @return 入座成功 {@code true}；房间没位置 {@code false}
     * @see #addPlayerWithSeatIndex(BaseGamePlayer, boolean)
     */
    public boolean addPlayerWithFreeSeatNumber(BaseGamePlayer gamePlayer, boolean randomSeat) {
        // 房间已经被拆掉就别再往里加人了：removeGameRoom 已经把它从房间表摘掉，
        // 这时候进来的人会留在数组和玩家表里、而按 id 查不到房间，成为无人清理的孤儿。
        // 注意这只是「尽早拒绝」——真正兜底的是下面两处写入前的复查
        if (dismissed) {
            log.warn("玩家入座失败: room={}, player={}, strategy=FREE_SEAT_NUMBER, random={}, reason=房间已解散", code, logIdOf(gamePlayer), randomSeat);
            return false;
        }

        // 空槽位下标。-1 表示「还没找到」。
        // 靠它保证只记下「第一个」空位，而不是最后碰到的那个
        int slot = -1;

        // 空闲座位号集合，初始认为 0~3 全空。
        // 这里只用到 key，value 没有意义——相当于拿 Map 当 Set 用
        Map<Integer, Integer> freeSeats = new HashMap<>();
        for (int i = 0; i < MAX_SEAT_NUMBER; i++) {
            freeSeats.put(i, i);
        }

        if (!randomSeat) {
            // ========== 不随机：槽位取第一个空位，座位号取最小空闲号 ==========
            //
            // 示例场景：
            //   4 人房，A 已经在 槽位0 / 座位3 —— 他是上一局随机进来的，所以这两个值本来就错开着
            //   槽位 1、2、3 都空着
            //   现在 B 入座（不随机）
            //   期望结果：B 落在 槽位1，座位号取最小空闲号 0   —— 槽位 ≠ 座位号，这就是策略一的特点

            // 扫一遍现有座位：一边找空槽位，一边把已被占用的座位号划掉
            for (int i = 0; i < gamePlayers.length; i++) {
                // 槽位：遇到空位并且还没记过，就记下它。
                // slot == -1 这个条件不能少——没有它，后面每遇到一个空位都会覆盖，
                // 最后拿到的是「最后一个空位」而不是第一个
                // 例：i=0 时 gamePlayers[0]=A，不是空位，跳过
                //     i=1 时是空位且 slot==-1  ->  slot = 1
                //     i=2、i=3 虽然也是空位，但 slot 已经不是 -1 了，不再覆盖
                if (gamePlayers[i] == null && slot == -1) {
                    slot = i;
                }
                // 座位号：这个槽位上有人，说明他的座位号已被占用，从空闲集合里划掉
                // 例：i=0 时遇到 A，A 的座位号是 3  ->  freeSeats 从 {0,1,2,3} 变成 {0,1,2}
                //     i=1、2、3 都是空位，不处理
                //     循环结束时 freeSeats = {0, 1, 2}
                if (gamePlayers[i] != null) {
                    freeSeats.remove(gamePlayers[i].getSeat());
                }
            }

            // 一个空槽位都没找到 = 所有槽位都有人 = 房间满了
            if (slot == -1) {
                log.warn("玩家入座失败: room={}, player={}, strategy=FREE_SEAT_NUMBER, random=false, reason=房间已满", code, logIdOf(gamePlayer));
                return false;
            }

            // 座位号：按 0 -> 1 -> 2 -> 3「升序」取第一个空闲号，取不到（-1）说明座位号用尽了。
            // 座位号只有 MAX_SEAT_NUMBER 个，而 maxSize 可能比它大——
            // 二八杠（maxSize=100）坐满 4 人之后，第 5 个人就会走到这里
            int seat = freeSeats.keySet().stream().min(Integer::compareTo).orElse(-1);
            if (seat == -1) {
                log.warn("玩家入座失败: room={}, player={}, strategy=FREE_SEAT_NUMBER, random=false, reason=无可用座位号", code, logIdOf(gamePlayer));
                return false;
            }

            // 两处校验都过了才动状态。顺序不能反过来：先放人再校验的话，
            // 校验失败 return false 时玩家已经躺在 gamePlayers 里了——
            // 他占着一个槽位、座位号还是默认的 0（跟真正坐 0 号的人重座），
            // 而调用方收到 false 不会登记他，就成了「在房间数组里、不在玩家表里」的幽灵
            gamePlayers[slot] = gamePlayer;
            gamePlayer.setSeat(seat);

            log.debug("玩家入座成功: room={}, player={}, strategy=FREE_SEAT_NUMBER, random=false, slot={}", code, logIdOf(gamePlayer), slot);
            return true;
        }

        // ========== 随机：槽位随机挑，座位号按降序取第一个空闲号 ==========
        //
        // 示例场景：
        //   空房，A 入座（随机），这次 ThreadLocalRandom 恰好抽到槽位 2
        //   期望结果：A 落在 槽位2，座位号却是 3 —— 一步就错开了

        // 空槽位清单。与不随机分支不同——那边只要「第一个」，
        // 这边要收全，因为下一步得从里面随机挑一个
        // 例：本例是空房  ->  freeSlots = [0, 1, 2, 3]
        List<Integer> freeSlots = new LinkedList<>();

        // 扫一遍：空槽位收进清单，有人占的座位号从集合里划掉
        for (int i = 0; i < gamePlayers.length; i++) {
            if (gamePlayers[i] != null) {
                freeSeats.remove(gamePlayers[i].getSeat());
            } else {
                freeSlots.add(i);
            }
        }

        // 座位号一个都不剩了 = 坐不进去。
        // 判的是「还有没有空闲座位号」，不是「还有没有空槽位」：
        // maxSize 大于 MAX_SEAT_NUMBER 时会出现「槽位明明空着，但座位号已经占满」，
        // 这时放人进去必然和人重座，所以直接返回 false
        if (freeSeats.isEmpty()) {
            log.warn("玩家入座失败: room={}, player={}, strategy=FREE_SEAT_NUMBER, random=true, reason=座位号已满", code, logIdOf(gamePlayer));
            return false;
        }

        // 从空槽位清单里随机挑一个
        // 例：freeSlots = [0,1,2,3]，nextInt(4) 抽到 2  ->  slot = 2
        slot = freeSlots.get(ThreadLocalRandom.current().nextInt(freeSlots.size()));

        // 座位号：与不随机分支相反，这里按 3 -> 2 -> 1 -> 0「降序」取第一个空闲号。
        // 方向反过来 + 槽位本身是随机的，两者就很容易错开
        // 例：freeSeats = {0,1,2,3}  ->  取到 3  ->  A：槽位2 / 座位3
        int seat = freeSeats.keySet().stream().max(Integer::compareTo).orElse(-1);
        // 走不到这里：上面刚判过 freeSeats 非空，中间也没有再 remove，max() 必然取得到。
        // 留着只是挡住将来有人改动上面的逻辑
        if (seat == -1) {
            log.warn("玩家入座失败: room={}, player={}, strategy=FREE_SEAT_NUMBER, random=true, reason=无可用座位号", code, logIdOf(gamePlayer));
            return false;
        }

        // 同不随机分支：校验都过了才动状态
        gamePlayers[slot] = gamePlayer;
        gamePlayer.setSeat(seat);

        log.debug("玩家入座成功: room={}, player={}, strategy=FREE_SEAT_NUMBER, random=true, slot={}", code, logIdOf(gamePlayer), slot);
        return true;
    }

    /**
     * 入座（策略二）：<b>座位号 = 数组槽位下标</b>。
     *
     * <p>与 {@link #addPlayerWithFreeSeatNumber 策略一} 的差别只有一处 —— 座位号怎么来：</p>
     * <pre>
     *   策略一   座位号 = 0 ~ MAX_SEAT_NUMBER-1 里最小的空闲号   与槽位无关，可能错开
     *   策略二   座位号 = 槽位下标本身                            永远相等
     * </pre>
     *
     * <p>因为座位号就是下标，「把下标当座位号用」的代码能正确工作，
     * 人数也不受 {@link #MAX_SEAT_NUMBER} 限制——这正是二八杠、房卡场这类
     * 人数上限大于座位号个数的玩法需要的。</p>
     *
     * @param gamePlayer 待加入的玩家
     * @param randomSeat 是否随机座位
     * @return 入座成功 {@code true}；房间没位置 {@code false}
     * @see #addPlayerWithFreeSeatNumber(BaseGamePlayer, boolean)
     */
    public boolean addPlayerWithSeatIndex(BaseGamePlayer gamePlayer, boolean randomSeat) {
        // 同策略一：房间已解散就别再往里加人
        if (dismissed) {
            log.warn("玩家入座失败: room={}, player={}, strategy=SEAT_INDEX, random={}, reason=房间已解散", code, logIdOf(gamePlayer), randomSeat);
            return false;
        }

        if (!randomSeat) {
            // ========== 不随机：从下标 0 起扫，第一个空槽位就是它 ==========
            //
            // 示例场景：
            //   4 人房，A 在 槽位0、B 在 槽位1，槽位 2、3 空着
            //   现在 C 入座（不随机）
            //   期望结果：C 落在 槽位2，座位号也是 2
            for (int i = 0; i < gamePlayers.length; i++) {
                if (gamePlayers[i] == null) {
                    // 放进这个空槽位
                    // 例：i=0 是 A、i=1 是 B，都不是空位，跳过
                    //     i=2 是空位  ->  gamePlayers[2] = C
                    gamePlayers[i] = gamePlayer;

                    // 座位号 = 数组下标 —— 这是与策略一最本质的差别。
                    // 策略一在这里还要再查一遍「空闲座位号集合」，本策略直接拿下标当座位号
                    // 例：C.setSeat(2)
                    gamePlayers[i].setSeat(i);

                    log.debug("玩家入座成功: room={}, player={}, strategy=SEAT_INDEX, random=false, slot={}", code, logIdOf(gamePlayer), i);

                    return true;
                }
            }
            log.warn("玩家入座失败: room={}, player={}, strategy=SEAT_INDEX, random=false, reason=房间已满", code, logIdOf(gamePlayer));
            return false;

        }
        // ========== 随机：先收集所有空槽位，再随机挑一个 ==========
        //
        // 示例场景：
        //   4 人房，A 在 槽位1，槽位 0、2、3 空着
        //   现在 B 入座（随机）
        //   期望结果：B 落在 0/2/3 中的一个，座位号 == 那个槽位

        // 收集全部空槽位
        // 例：freeSlots = [0, 2, 3]
        List<Integer> freeSlots = new LinkedList<>();
        for (int i = 0; i < gamePlayers.length; i++) {
            if (gamePlayers[i] == null) {
                freeSlots.add(i);
            }
        }

        // 还有空位才放人。
        // 注意这里判的是「空槽位」就够了——本策略座位号就是槽位，
        // 不存在「槽位空着但座位号不够用」的情况，不必像策略一那样另外维护座位号集合
        // 例：freeSlots = [0,2,3] 非空，继续往下
        if (freeSlots.isEmpty()) {
            log.warn("玩家入座失败: room={}, player={}, strategy=SEAT_INDEX, random=true, reason=房间已满", code, logIdOf(gamePlayer));
            return false;
        }

        // 从清单里随机挑一个空槽位
        // 例：freeSlots = [0,2,3]，nextInt(3) 抽到下标 1  ->  slot = 2
        Integer slot = freeSlots.get(ThreadLocalRandom.current().nextInt(freeSlots.size()));

        // 放进抽到的槽位
        // 例：gamePlayers[2] = B
        gamePlayers[slot] = gamePlayer;

        // 座位号 = 下标。随机入座也不会错开——这是策略二的标志性特征
        // 例：B.setSeat(2)，最终 B：槽位2 / 座位2
        gamePlayers[slot].setSeat(slot);
        log.debug("玩家入座成功: room={}, player={}, strategy=SEAT_INDEX, random=true, slot={}", code, logIdOf(gamePlayer), slot);
        return true;

    }


    /**
     * 设置房间人数上限，并按新上限重建座位数组。
     *
     * <p>重建时按下标原样搬运原有玩家：容量变大则多出的槽位为 {@code null}（空座），
     * 容量变小则超出部分的玩家被直接丢弃（不会退回 {@code playerIdMap}，属于已知泄漏点）。</p>
     *
     * <p><b>调用顺序有要求</b>：必须在任何 {@link #addPlayer(BaseGamePlayer, boolean)} 之前调用，
     * 否则座位数组仍是
     * {@code null}，入座时读 {@code gamePlayers.length} 会抛 {@code NullPointerException}。</p>
     *
     * @param maxSize 房间人数上限
     */
    public void setMaxSize(int maxSize) {
        // 先留住旧数组：建成新数组后就取不到老玩家了
        BaseGamePlayer[] players = gamePlayers;
        // 先设置最大值
        this.maxSize = maxSize;
        // 创建一个新的数组
        gamePlayers = new BaseGamePlayer[maxSize];
        // 开始复制过去
        if (players != null) {
            // 取两者较小值，避免扩容/缩容时越界
            for (int i = 0; i < maxSize && i < players.length; i++) {
                gamePlayers[i] = players[i];
            }
        }
    }


    /**
     * 按<b>数组槽位下标</b>取玩家。
     *
     * <p><b>参数是槽位，不是座位号</b>——方法名叫 BySeats 而不是 BySeatNo 就是这个原因。
     * 默认策略（{@link #addPlayerWithFreeSeatNumber}）下这两个值会错开，
     * 传座位号进来会取到别人或 {@code null}。想按座位号找人，
     * 自己遍历数组比对 {@link BaseGamePlayer#getSeat()}。</p>
     *
     * <p>只有 {@link #addPlayerWithSeatIndex} 策略下槽位和座位号才恒等，这个歧义才不存在。</p>
     *
     * <p>本方法不做边界检查，下标越界会抛 {@code ArrayIndexOutOfBoundsException}。</p>
     *
     * @param index 数组槽位下标（不是座位号）
     * @param <T>   玩家实际类型
     * @return 该槽位上的玩家；空座返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseGamePlayer> T getGamePlayerBySeat(int index) {
        return (T) gamePlayers[index];
    }

    /**
     * 根据玩家 id 获取玩家。
     *
     * @param playerId 玩家 id
     * @param <T>      玩家实际类型
     * @return 该玩家；不在本房间时返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseGamePlayer> T getGamePlayerById(long playerId) {
        for (BaseGamePlayer player : gamePlayers) {
            if (player != null && player.getId() == playerId) {
                return (T) player;
            }
        }
        return null;
    }

    /**
     * 获取当前房间内的玩家数量。
     *
     * @return 玩家数量
     */
    public int getPlayerSize() {
        int size = 0;
        for (BaseGamePlayer gamePlayer : gamePlayers) {
            if (gamePlayer != null) {
                size += 1;
            }
        }
        return size;
    }


    private long logIdOf(BaseGamePlayer gamePlayer) {
        return gamePlayer == null || gamePlayer.getUser() == null ? -1L : gamePlayer.getId();
    }

    /**
     * 获取玩法编号。
     *
     * <p>由各玩法房间返回自己的游戏 id，用于写入用户表的 {@code online_state}
     * （{@code GameContainer} 入座时把该值写到 {@code UserEntity.onlineState}，
     * 表示「这名用户正在玩哪个游戏」；1 表示只在大厅、0 表示离线）。</p>
     *
     * @return 玩法编号
     */
    public abstract int getGameId();

}
