package com.ytrue.game.framework.base.container;

import com.ytrue.game.framework.base.data.BaseGamePlayer;
import com.ytrue.game.framework.base.data.BaseGameRoom;
import com.ytrue.game.framework.database.data.mapper.UserMapper;
import com.ytrue.game.framework.engine.data.ServerUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 游戏容器。
 *
 * <p>全局唯一的「房间 + 玩家」内存索引，与 {@code UserContainer} 是同一套路子：
 * {@code UserContainer} 管「谁在线」，本类管「谁在哪个房间、坐在哪」。
 * 两者都是 {@code static} 字段 + {@code @Autowired} 构造器注入，供业务层以静态方法直接调用。</p>
 *
 * <p><b>为什么用静态字段而不是实例字段</b>：房间查询遍布业务层各处（管理器、定时任务、控制器），
 * 若做成实例 Bean，每个调用点都要注入一次 {@code GameContainer}。旧工程选择了静态访问，
 * 迁移时保持原样以降低业务层的改动量。</p>
 *
 * <p><b>线程安全</b>：{@link #ROOM_LOCK} 保护房间表，{@link #PLAYER_LOCK} 保护玩家表。
 * 加锁的目的是保住「查空位 → 放入 → 记账」这一串复合操作的原子性，
 * 而不是单次 {@code put}/{@code get}。因此两张表用 {@link ConcurrentHashMap}——
 * 查询方法（{@link #getGameRoomByCode} 等）是<b>不加锁</b>读的，
 * 若沿用旧工程的 {@code HashMap}，就会在读线程与写线程之间产生数据竞争
 * （轻则读到旧值，重则遍历时抛 {@code ConcurrentModificationException}）。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class GameContainer {

    /**
     * 房间表同步锁。
     */
    private static final Object ROOM_LOCK = new Object();

    /**
     * 玩家表同步锁。
     */
    private static final Object PLAYER_LOCK = new Object();

    /**
     * 创建房间时默认的人数的上限。
     */
    private static final int MIN_PLAYER_SIZE = 2;

    /**
     * 尚未使用的房间号（空闲池，取一个少一个）。
     *
     * <p>只在 {@link #ROOM_LOCK} 内读写，因此保持普通 {@link LinkedList} 即可。</p>
     */
    private static final List<Integer> unusedRoomCodes = new LinkedList<>();

    /**
     * 房间号 → 房间。
     *
     * <p>写操作在 {@link #ROOM_LOCK} 内，读操作不加锁，故用并发容器。</p>
     */
    private static final Map<Integer, BaseGameRoom> roomCodeMap = new ConcurrentHashMap<>();

    /**
     * 玩家 id → 玩家。
     *
     * <p>写操作在 {@link #PLAYER_LOCK} 内，读操作不加锁，故用并发容器。</p>
     */
    private static final Map<Long, BaseGamePlayer> playerIdMap = new ConcurrentHashMap<>();

    /**
     * 用户数据库操作接口。
     *
     * <p>用于把「玩家进了哪个房间」写回用户表，见 {@link #createGamePlayer}。</p>
     */
    private static UserMapper userMapper;

    /**
     * 构造器注入用户数据库操作接口。
     *
     * @param userMapper 用户数据库操作接口
     */
    @Autowired
    public GameContainer(UserMapper userMapper) {
        GameContainer.userMapper = userMapper;
    }

    // ==================== 房间 ====================

    /**
     * 创建游戏房间（人数上限取默认值 {@value MIN_PLAYER_SIZE}）。
     *
     * @param roomClass 房间类
     * @param <T>       房间实际类型
     * @return 新建的房间；创建失败返回 {@code null}
     */
    public static <T extends BaseGameRoom> T createGameRoom(Class<? extends T> roomClass) {
        return createGameRoom(roomClass, MIN_PLAYER_SIZE);
    }

    /**
     * 创建游戏房间。
     *
     * <p>流程：先从空闲池里取一个房间号 → 反射创建房间实例 → 设置房间号与人数上限 → 登记进房间表。</p>
     *
     * <p><b>房间号怎么来的</b>：池子空了就一次性随机生成最多 100 个 6 位号
     * （{@code 100000~999999}）放进去，每次只取走一个。外层的 10 次循环是旧工程留下的写法——
     * 第一次循环负责把池子填满，第二次必然取到号。</p>
     *
     * <p><b>已知缺陷（保持旧行为，未修复）</b>：内层 {@code while (count-- > 0)} 只做「不与池中、
     * 不与已用房间重复」的检查，<b>没有检查池内自身是否重复</b>——
     * 随机到同一个号两次时，池里会出现两个相同元素，生成两个同号房间，
     * 后创建的那个会静默覆盖 {@code roomCodeMap} 里的前一个。重号概率约为万分之五/千次生成，
     * 到一定规模才会显现。真要修的话，判重时应改为 {@code !unusedRoomCodes.contains(tmpCode)} 的判断
     * 加上一个「本次已生成」的临时集合。</p>
     *
     * @param roomClass 房间类（需具备可访问的无参构造器）
     * @param maxSize   房间人数上限
     * @param <T>       房间实际类型
     * @return 新建的房间；创建失败（如无无参构造器）返回 {@code null}，并记一条错误日志
     */
    public static <T extends BaseGameRoom> T createGameRoom(Class<? extends T> roomClass, int maxSize) {
        try {
            int roomCode = -1;
            synchronized (ROOM_LOCK) {
                for (int i = 0; i < 10; i++) {
                    if (!unusedRoomCodes.isEmpty()) {
                        roomCode = unusedRoomCodes.removeFirst();
                        break;
                    } else {
                        int count = 100;
                        while (count-- > 0) {
                            int tmpCode = ThreadLocalRandom.current().nextInt(100000, 1000000);
                            if (!unusedRoomCodes.contains(tmpCode) && !roomCodeMap.containsKey(tmpCode)) {
                                unusedRoomCodes.add(tmpCode);
                            }
                        }
                    }
                }

                if (roomCode > 0) {
                    // 旧工程写的是 roomClass.newInstance()，该方法自 Java 9 起已废弃
                    // （异常信息少、且会把构造器抛出的异常吞掉），改为显式取无参构造器再实例化
                    T gameRoom = roomClass.getDeclaredConstructor().newInstance();
                    gameRoom.setCode(roomCode);
                    gameRoom.setMaxSize(maxSize);

                    roomCodeMap.put(roomCode, gameRoom);
                    return gameRoom;
                }
            }
        } catch (Exception e) {
            // 格式说明：末尾那个 e 必须「多出来」才能被 SLF4J 识别为异常、打印堆栈。
            // 旧工程写成 "...[{}][{}]", e.getMessage(), e —— 占位符与实参个数正好相等，
            // e 会被当成普通参数用 toString() 拼进去，堆栈信息（真正有用的部分）反而丢了。
            log.error("创建游戏房间时出现异常:[{}]", e.getMessage(), e);
        }

        return null;
    }

    /**
     * 移除游戏房间，并把它所有座位上的玩家一并移除。
     *
     * @param gameRoom 待移除的房间
     */
    public static void removeGameRoom(BaseGameRoom gameRoom) {
        try {
            synchronized (ROOM_LOCK) {
                // 先清人再删房：玩家表里不能留下指向已消失房间的条目
                for (int i = 0; i < gameRoom.getMaxSize(); i++) {
                    if (gameRoom.getGamePlayerByIndex(i) != null) {
                        removeGamePlayer(gameRoom, i);
                    }
                }

                roomCodeMap.remove(gameRoom.getCode());
            }
        } catch (Exception e) {
            log.error("移除游戏房间时出现异常:[{}]", e.getMessage(), e);
        }
    }

    /**
     * 根据房间号获取房间。
     *
     * @param roomCode 房间号
     * @param <T>      房间实际类型
     * @return 房间；不存在时返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseGameRoom> T getGameRoomByCode(int roomCode) {
        try {
            return (T) roomCodeMap.get(roomCode);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前所有房间。
     *
     * <p>返回的是<b>快照副本</b>，遍历过程中房间的增删不影响本次结果，也不会抛
     * {@code ConcurrentModificationException}。</p>
     *
     * @return 房间列表
     */
    public static List<BaseGameRoom> getGameRooms() {
        return new LinkedList<>(roomCodeMap.values());
    }

    /**
     * 获取当前所有指定类型的房间（精确匹配类，不含子类）。
     *
     * <p>用的是 {@code room.getClass() == clazz} 而非 {@code instanceof}：
     * 传 {@code BaseFishingRoom.class} 只会拿到「直接就是该类」的房间（实际为 0 个），
     * 想按玩法大类筛选请用 {@code instanceof} 自行判断，或传具体的实现类。</p>
     *
     * @param clazz 房间类
     * @param <T>   房间实际类型
     * @return 房间列表
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseGameRoom> List<T> getGameRooms(Class<T> clazz) {
        List<T> result = (List<T>) roomCodeMap.values().stream()
                .filter(room -> (room.getClass() == clazz))
                .toList();
        return new LinkedList<>(result);
    }

    // ==================== 玩家 ====================

    /**
     * 创建游戏玩家（不随机座位）。
     *
     * @param gameRoom    所在房间
     * @param user        用户会话
     * @param playerClass 玩家类
     * @param <T>         玩家实际类型
     * @return 新建的玩家；创建失败返回 {@code null}
     */
    public static <T extends BaseGamePlayer> T createGamePlayer(BaseGameRoom gameRoom, ServerUser user, Class<? extends T> playerClass) {
        return createGamePlayer(gameRoom, user, playerClass, false);
    }

    /**
     * 创建游戏玩家，并指定是否随机座位。
     *
     * <p>流程：反射创建玩家实例 → 绑定用户会话与房间号 → 交给房间入座 → 登记进玩家表 →
     * 把「在玩哪个游戏」写回用户表。</p>
     *
     * <p><b>入座策略由房间决定</b>：本方法只调 {@code gameRoom.addPlayer(...)}，
     * 具体怎么排座位是房间自己的事（见 {@code BaseGameRoom.addPlayer} 的说明）。</p>
     *
     * <p><b>旧工程在这里做过什么，为什么删掉了</b>：旧代码是全限定类名字符串比对——</p>
     * <pre>{@code
     * if (playerClass.getName().equals("com.maple.game.osee.entity.fightten.FightTenPlayer")
     *  || playerClass.getName().equals("com.maple.game.osee.entity.fightten.FightTenRobotPlayer")) {
     *     gameRoom.addPlayer1(gamePlayer, randomSeat);              // 座位号 = 下标
     * } else {
     *     gameRoom.addPlayer(gamePlayer, randomSeat);               // 座位号取最小空闲号
     * }
     * }</pre>
     * <p>这段在包名一改（本项目已把 {@code com.maple.*} 整体改名为 {@code com.ytrue.game.*}）之后，
     * 比对结果就<b>永远为 false</b>，FightTen 会静默退回默认入座策略、座位分配行为改变，
     * 而编译器不会有任何提示。这类「改了包名就悄悄坏掉」的字符串硬编码不能带进新工程，
     * 因此改为由房间类覆写 {@code addPlayer} 来体现差异。业务层迁移时，
     * 房卡场的房间类覆写 {@code addPlayer} 并转调
     * {@code addPlayerWithSeatIndex}（即旧的 {@code addPlayer1}）即可复原原行为。</p>
     *
     * <p><b>房间满员时返回 {@code null}</b>：{@code addPlayer} 会返回 null 表示没坐上座位，
     * 此时本方法必须原样返回 null，不能把玩家登记进 {@code playerIdMap}——
     * 否则会出现「在玩家表里、却不在任何房间座位上」的幽灵玩家，
     * 后续按 id 反查房间会拿到 {@code null}，业务代码莫名其妙地空指针。</p>
     *
     * <p><b>与旧工程的差别</b>：旧代码不看 {@code addPlayer} 的返回值。房间满时它有两种表现——
     * 非随机入座会抛数组越界（被下面的 catch 兜住，最终也返回 null），
     * 随机入座则返回 null 但被忽略（于是造出幽灵玩家）。
     * 现在 {@code addPlayer} 统一返回 null，本方法也统一返回 null，两种路径行为一致。</p>
     *
     * @param gameRoom    所在房间
     * @param user        用户会话
     * @param playerClass 玩家类（需具备可访问的无参构造器）
     * @param randomSeat  是否随机座位
     * @param <T>         玩家实际类型
     * @return 新建的玩家；房间满员或创建失败返回 {@code null}
     */
    public static <T extends BaseGamePlayer> T createGamePlayer(BaseGameRoom gameRoom, ServerUser user, Class<? extends T> playerClass, boolean randomSeat) {
        try {
            synchronized (PLAYER_LOCK) {
                T gamePlayer = playerClass.getDeclaredConstructor().newInstance();
                gamePlayer.setUser(user);
                gamePlayer.setRoomCode(gameRoom.getCode());
                // 入座：座位号由房间的入座策略分配；返回 null 表示房间没位置，玩家没坐进去
                if (gameRoom.addPlayer(gamePlayer, randomSeat) == null) {
                    log.warn("玩家[{}]加入房间[{}]失败：房间已满或没有可用座位号", user.getId(), gameRoom.getCode());
                    // 关键：不能往下走。没有座位就不该进玩家表，否则玩家表里会留下一个
                    // 既不在房间里、也永远不会被清掉的对象
                    return null;
                }
                playerIdMap.put(user.getId(), gamePlayer);
                // 已登录用户才回写数据库：未登录时没有实体，且写入的 id 是 0 会污染其它记录
                if (user.isOnline() && user.getEntity() != null) {
                    // onlineState = 玩法编号，表示「正在玩哪个游戏」
                    user.getEntity().setOnlineState(gameRoom.getGameId());
                    userMapper.updateById(user.getEntity());
                }
                return gamePlayer;
            }
        } catch (Exception e) {
            log.error("创建游戏玩家时出现异常:[{}]", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 移除指定座位上的玩家。
     *
     * @param gameRoom 所在房间
     * @param seat     数组下标（注意不是座位号，见 {@code BaseGameRoom.getGamePlayerByIndex}）
     * @param <T>      玩家实际类型
     * @return 被移除的玩家；该座位为空时返回 {@code null}
     */
    public static <T extends BaseGamePlayer> T removeGamePlayer(BaseGameRoom gameRoom, int seat) {
        try {
            synchronized (PLAYER_LOCK) {
                T gamePlayer = gameRoom.getGamePlayerByIndex(seat);
                gameRoom.getGamePlayers()[seat] = null;
                if (gamePlayer != null) {
                    playerIdMap.remove(gamePlayer.getId());
                    ServerUser user = gamePlayer.getUser();
                    // 回到大厅，onlineState 复位为 1（1 = 在线但不在游戏中）
                    if (user.isOnline() && user.getEntity() != null) {
                        user.getEntity().setOnlineState(1);
                        userMapper.updateById(user.getEntity());
                    }
                    return gamePlayer;
                }
            }
        } catch (Exception e) {
            log.error("移除游戏玩家时出现异常:[{}]", e.getMessage(), e);
        }

        return null;
    }

    /**
     * 移除指定 id 的玩家（按用户 id 找座位）。
     *
     * <p>与 {@link #removeGamePlayer(BaseGameRoom, int)} 的区别：入口是用户 id 而不是座位下标，
     * 内部先遍历找出该玩家所在的数组下标。</p>
     *
     * <p><b>已知缺陷（保持旧行为，未修复）</b>：找不到该玩家时 {@code a} 保持初始值 {@code 0}，
     * 于是 {@code gameRoom.getGamePlayers()[0] = null} 会把 <b>0 号槽位的玩家踢出房间</b>
     * 而不是报错。随后 {@code gamePlayer} 为 {@code null}，方法走到最后返回 {@code null}，
     * 调用方看不出出过问题。这个「传错 id 就误踢 0 号位」的行为已保持原样。</p>
     *
     * @param gameRoom 所在房间
     * @param seat     用户 id（参数名沿用旧工程，实际是 id）
     * @param <T>      玩家实际类型
     * @return 被移除的玩家；该 id 不在房间时返回 {@code null}
     */
    public static <T extends BaseGamePlayer> T removeGamePlayer1(BaseGameRoom gameRoom, Long seat) {
        try {
            synchronized (PLAYER_LOCK) {
                T gamePlayer = gameRoom.getGamePlayerById(seat);
                int a = 0;
                for (int i = 0; i < gameRoom.getGamePlayers().length; i++) {
                    if (gameRoom.getGamePlayers()[i] != null) {
                        if (gameRoom.getGamePlayers()[i].getUser().getId() == seat) {
                            a = i;
                            break;
                        }
                    }
                }
                gameRoom.getGamePlayers()[a] = null;
                if (gamePlayer != null) {
                    playerIdMap.remove(gamePlayer.getId());
                    ServerUser user = gamePlayer.getUser();
                    if (user.isOnline() && user.getEntity() != null) {
                        user.getEntity().setOnlineState(1);
                        userMapper.updateById(user.getEntity());
                    }
                    return gamePlayer;
                }
            }
        } catch (Exception e) {
            log.error("移除游戏玩家时出现异常:[{}]", e.getMessage(), e);
        }

        return null;
    }

    /**
     * 移除玩家，并可选地在房间空掉时把房间一起删掉。
     *
     * @param gameRoom  所在房间
     * @param userId    用户 id
     * @param checkRoom 为 {@code true} 时，移除后若房间已无玩家则连同房间一并移除
     * @param <T>       玩家实际类型
     * @return 被移除的玩家
     */
    public static <T extends BaseGamePlayer> T removeGamePlayer(BaseGameRoom gameRoom, Long userId, boolean checkRoom) {
        T player = removeGamePlayer1(gameRoom, userId);
        if (checkRoom) {
            for (BaseGamePlayer roomPlayer : gameRoom.getGamePlayers()) {
                if (roomPlayer != null) {
                    return player;
                }
            }
            removeGameRoom(gameRoom);
        }

        return player;
    }

    /**
     * 根据用户 id 获取玩家数据。
     *
     * @param id 用户 id
     * @param <T> 玩家实际类型
     * @return 玩家；不在任何房间时返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseGamePlayer> T getPlayerById(long id) {
        try {
            return (T) playerIdMap.get(id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据玩家 id 获取其所在房间。
     *
     * @param id 用户 id
     * @param <T> 房间实际类型
     * @return 房间；玩家不在任何房间时返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseGameRoom> T getGameRoomByPlayerId(long id) {
        try {
            BaseGamePlayer gamePlayer = getPlayerById(id);
            return (T) (gamePlayer == null ? null : roomCodeMap.get(gamePlayer.getRoomCode()));
        } catch (Exception e) {
            return null;
        }
    }

}
