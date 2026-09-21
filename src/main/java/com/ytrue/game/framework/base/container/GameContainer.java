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

/**
 * 游戏容器。
 *
 * <p>全局唯一的「房间 + 玩家」内存索引，和 {@code UserContainer} 是一套路子：
 * {@code UserContainer} 管「谁在线」，本类管「谁在哪个房间、坐在哪」。
 * 两者都是静态字段 + 构造器注入，供业务层以静态方法直接调用。</p>
 *
 * <p><b>为什么用静态字段而不是实例字段</b>：房间查询遍布业务层各处（管理器、定时任务、控制器），
 * 做成实例 Bean 的话每个调用点都得注入一次 {@code GameContainer}。
 * 旧工程选择了静态访问，迁移时保持原样以降低业务层的改动量。</p>
 *
 * <p><b>线程安全</b>：{@link #ROOM_LOCK} 保护房间表，{@link #PLAYER_LOCK} 保护玩家表。
 * 加锁保护的是「查空位 → 放入 → 记账」这一串复合操作的原子性，
 * 而不是单次 put/get。因此两张表用 {@link ConcurrentHashMap}——
 * 查询方法（{@link #getGameRoomByCode} 等）都是<b>不加锁</b>读的，
 * 沿用旧工程的 {@code HashMap} 会在读线程和写线程之间产生数据竞争
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
     * 创建房间时默认的人数上限。
     */
    private static final int MIN_PLAYER_SIZE = 2;

    /**
     * 尚未使用的房间号（空闲池，取一个少一个）。
     *
     * <p>只在 {@link #ROOM_LOCK} 内读写，所以保持普通 {@link LinkedList} 即可。</p>
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
     * <p>用于把「玩家进了哪个房间」写回用户表，见
     * {@link #createGamePlayer(BaseGameRoom, ServerUser, Class, boolean)}。</p>
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
     * <p>流程：从空闲池里取一个房间号 → 反射创建房间实例 → 设置房间号与人数上限 → 登记进房间表。</p>
     *
     * <p><b>房间号怎么来的</b>：池子空了就一次性随机生成最多 100 个 6 位号
     * （{@code 100000~999999}）放进去，每次只取走一个。外层的 10 次循环是旧工程留下的写法——
     * 第一次循环负责把池子填满，第二次必然取到号。</p>
     *
     * <p><b>池子里的号不会重复</b>：每次生成后立刻入池，所以下一轮判重时
     * {@code contains} 就能看到它，「本轮刚生成的」和「池子里已有的」是同一批。
     * 实测 20 万轮补货，没有一次出现重复。</p>
     *
     * @param roomClass 房间类（需具备可访问的无参构造器）
     * @param maxSize   房间人数上限
     * @param <T>       房间实际类型
     * @return 新建的房间；创建失败（如无无参构造器）返回 {@code null}，并记一条错误日志
     */
    public static <T extends BaseGameRoom> T createGameRoom(Class<? extends T> roomClass, int maxSize) {
        // ========== 示例场景 ==========
        //   第一次建房：GameContainer.createGameRoom(FishingGameRoom.class, 4)
        //   此时房间号池 unusedRoomCodes 是空的（应用刚启动，还没建过房）
        //   期望结果：拿到一个 6 位房间号（比如 482913），房间登记进表，方法返回该房间

        try {
            // 房间号。先用 -1 占位表示「还没取到」，
            // 下面 if (roomCode > 0) 就是靠它判断到底取没取到
            // 例：本例最终会被赋成 482913
            int roomCode = -1;

            // 取号和登记要作为一个整体完成：中间被别的线程插进来的话，
            // 可能两个房间拿到同一个号，或者同一个号被写两次
            synchronized (ROOM_LOCK) {

                // 外层最多循环 10 次，实际用不到——第一次循环把池子填满，
                // 第二次必然能取到号，第 3 次起就多余了。这是旧工程留下的写法，保持原样
                for (int i = 0; i < 10; i++) {

                    if (!unusedRoomCodes.isEmpty()) {
                        // 池子里有现成的号，取走一个并跳出。
                        // removeFirst 是「取走」不是「查看」——取一个少一个
                        // 例：第一次调用走不到这里（池子空的）；
                        //     第二次调用时 i=0 就走到这里，取走池子里的第一个号
                        roomCode = unusedRoomCodes.removeFirst();
                        break;
                    } else {
                        // 池子空了，一次性补货：随机生成最多 100 个 6 位号放进去
                        int count = 100;
                        // count-- > 0 是先判断后自减，所以这里正好跑 100 次
                        while (count-- > 0) {
                            // 6 位号取 100000~999999。
                            // nextInt 的上界是开区间，所以要写 1000000 而不是 999999
                            int tmpCode = ThreadLocalRandom.current().nextInt(100000, 1000000);
                            // 两个判重：不能和池子里已有的重、不能和已经发出去的重。
                            // 「本轮刚生成的」不用单独再判一次——add 就在下面这一行，
                            // 第一个号进来时立刻入了池，下一轮再随机到它，contains 就命中得到
                            // 例：本例这一轮跑完，池子里 100 个号，互不重复
                            if (!unusedRoomCodes.contains(tmpCode) && !roomCodeMap.containsKey(tmpCode)) {
                                unusedRoomCodes.add(tmpCode);
                            }
                        }
                        // 补完货不 break，回到 for 再走一轮——下一轮 i=1 时池子非空，
                        // 就从上一条分支取走一个号
                    }
                }

                // 取到号了才继续。roomCode 还是 -1 说明 10 轮一次都没取到
                // （理论上不可能，因为第一轮就把池子填满了）
                // 例：本例 roomCode = 482913 > 0，继续往下
                if (roomCode > 0) {
                    // 反射创建房间实例，要求有无参构造器。
                    // 旧工程写的是 roomClass.newInstance()，该方法自 Java 9 起已废弃
                    // （异常信息少、还会把构造器抛出的异常吞掉），改为显式取无参构造器再实例化
                    // 例：roomClass = FishingGameRoom.class  ->  new FishingGameRoom()
                    T gameRoom = roomClass.getDeclaredConstructor().newInstance();

                    // 把房间号和人数上限写进房间。
                    // ★ setMaxSize 必须在任何 addPlayer 之前调用——座位数组是它创建的
                    // 例：gameRoom.setCode(482913)、gameRoom.setMaxSize(4)
                    gameRoom.setCode(roomCode);
                    gameRoom.setMaxSize(maxSize);

                    // 登记进房间表，之后 getGameRoomByCode(482913) 就能查到它
                    // 例：roomCodeMap 里多一条 482913 -> FishingGameRoom 实例
                    roomCodeMap.put(roomCode, gameRoom);

                    log.debug("创建房间成功: room={}, roomClass={}, maxSize={}", roomCode, roomClass.getSimpleName(), maxSize);

                    // 返回。return 写在 synchronized 块里，锁会自动释放
                    return gameRoom;
                }
            }
        } catch (Exception e) {
            // 反射失败（没有无参构造器、构造器自身抛异常等）会走到这里。
            // 不往外抛：建房失败不该把调用方的流程一起炸掉，返回 null 让它自己决定怎么办
            // e 会被当成普通参数用 toString() 拼进去，堆栈信息（真正有用的部分）反而丢了
            log.error("创建房间失败: roomClass={}, maxSize={}, error={}", roomClass.getSimpleName(), maxSize, e.getMessage(), e);
        }

        // 走到这里只有两种情况：反射失败（上面 catch 了），或 10 轮都没取到号
        return null;
    }

    /**
     * 移除游戏房间，并把它所有座位上的玩家一并移除。
     *
     * @param gameRoom 待移除的房间
     */
    public static boolean removeGameRoom(BaseGameRoom gameRoom) {
        try {
            synchronized (ROOM_LOCK) {
                // 先清人再删房：玩家表里不能留下指向已消失房间的条目
                for (int i = 0; i < gameRoom.getMaxSize(); i++) {
                    // 获取玩家
                    if (gameRoom.getGamePlayerBySeat(i) != null) {
                        // 删除玩家
                        removeGamePlayerBySeat(gameRoom, i);
                    }
                }
                roomCodeMap.remove(gameRoom.getCode());
                log.debug("移除房间: room={}, roomClass={}", gameRoom.getCode(), gameRoom.getClass().getSimpleName());
                return true;
            }
        } catch (Exception e) {
            int roomCode = gameRoom == null ? -1 : gameRoom.getCode();
            log.error("移除房间失败: room={}, error={}", roomCode, e.getMessage(), e);
            return false;
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
        return (T) roomCodeMap.get(roomCode);
    }

    /**
     * 获取当前所有房间。
     *
     * <p>返回的是<b>快照副本</b>：遍历过程中房间的增删不影响本次结果，
     * 也不会抛 {@code ConcurrentModificationException}。</p>
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
        List<T> result = (List<T>) roomCodeMap.values().stream().filter(room -> (room.getClass() == clazz)).toList();
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
     * @return 新建的玩家；创建失败或房间没位置返回 {@code null}
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
     * @param gameRoom    所在房间
     * @param user        用户会话
     * @param playerClass 玩家类（需具备可访问的无参构造器）
     * @param randomSeat  是否随机座位
     * @param <T>         玩家实际类型
     * @return 新建的玩家；房间没位置或创建失败返回 {@code null}
     */
    public static <T extends BaseGamePlayer> T createGamePlayer(BaseGameRoom gameRoom, ServerUser user, Class<? extends T> playerClass, boolean randomSeat) {
        try {
            synchronized (PLAYER_LOCK) {
                // 创建玩家
                T gamePlayer = playerClass.getDeclaredConstructor().newInstance();
                // 设置会话user
                gamePlayer.setUser(user);
                // 设置房间号码
                gamePlayer.setRoomCode(gameRoom.getCode());


                // TODO这个后面在看吧
                boolean seated;
                if (playerClass.getName().equals("com.ytrue.game.business.entity.fightten.FightTenPlayer") ||
                        playerClass.getName().equals("com.ytrue.game.business.entity.fightten.FightTenRobotPlayer")
                ) {
                    seated = gameRoom.addPlayerWithSeatIndex(gamePlayer, randomSeat);
                } else {
                    seated = gameRoom.addPlayerWithFreeSeatNumber(gamePlayer, randomSeat);
                }
                if (!seated) {
                    return null;
                }

                // 插入map
                playerIdMap.put(user.getId(), gamePlayer);
                // 回写「正在玩哪个游戏」。此时玩家已经真的坐进来了，内存是事实，
                // DB 写失败只记日志，不影响本方法的返回值
                updateOnlineState(user, gameRoom.getGameId());

                log.debug("创建玩家成功: room={}, player={}, slot={}, onlineState={}", gameRoom.getCode(), user.getId(), gamePlayer.getSeat(), gameRoom.getGameId());
                return gamePlayer;
            }
        } catch (Exception e) {
            // user / gameRoom 都可能为 null，catch 里的日志绝不能再直接引用它们——
            // 否则异常会在日志语句里二次抛出，直接逃出方法，把「返回 null」的契约破坏掉
            long userId = user == null ? -1L : user.getId();
            int roomCode = gameRoom == null ? -1 : gameRoom.getCode();
            log.error("创建玩家失败: room={}, player={}, playerClass={}, error={}", roomCode, userId, playerClass.getSimpleName(), e.getMessage(), e);
        }
        return null;
    }

    /**
     * 移除指定<b>槽位</b>上的玩家。
     *
     * <p><b>参数是槽位，但多数时候它就等于座位号</b>：策略二（座位号 = 下标）恒等；
     * 策略一在「非随机、且没人错位」时也相等——旧工程那句
     * {@code removeGamePlayer(room, player.getSeat())} 能跑通就是靠这个。</p>
     *
     * <p>只有「策略一 + 随机入座」会错开（捕鱼机器人就是随机进房的），
     * 那时把座位号当槽位传进来会删错人，详见 {@link BaseGameRoom#addPlayerWithFreeSeatNumber}。</p>
     *
     * @param gameRoom 所在房间
     * @param seat     数组槽位下标
     * @param <T>      玩家实际类型
     * @return 被移除的玩家；该槽位为空时返回 {@code null}
     */
    public static <T extends BaseGamePlayer> T removeGamePlayerBySeat(BaseGameRoom gameRoom, int seat) {
        try {
            synchronized (PLAYER_LOCK) {
                T gamePlayer = gameRoom.getGamePlayerBySeat(seat);
                gameRoom.getGamePlayers()[seat] = null;
                if (gamePlayer != null) {
                    ServerUser user = gamePlayer.getUser();
                    // user 可能为 null（手工塞进数组的玩家），而 getId() 内部直接解引用它。
                    // playerIdMap.remove 必须排在判空【之后】——否则 NPE 会让下面那个 return 被跳过，
                    // 人已经从数组里删掉了，调用方却收到 null
                    if (user != null) {
                        playerIdMap.remove(gamePlayer.getId());
                    }
                    // 回到大厅，onlineState 复位为 1（1 = 在线但不在游戏中）
                    updateOnlineState(user, 1);
                }
                return gamePlayer;
            }
        } catch (Exception e) {
            int roomCode = gameRoom == null ? -1 : gameRoom.getCode();
            log.error("移除玩家失败(按槽位): room={}, index={}, error={}", roomCode, seat, e.getMessage(), e);
        }

        return null;
    }

    /**
     * 移除指定<b>玩家 id</b> 的玩家。
     *
     * <p>{@code BaseGameRoom} 的入座策略下，座位号和数组槽位<b>可能不相等</b>
     * （策略一随机入座时必然错开），所以「按 id 找人」不能直接拿下标去取，
     * 必须先遍历找出他所在的槽位。</p>
     *
     * <p><b>旧工程里这个方法叫 {@code removeGamePlayer1}</b>，参数名还写成 {@code seat}——
     * 名字完全看不出它是按 id 删的，传座位号进来也不会报错，只会静默删错人。已改名。</p>
     *
     * <p><b>找不到该玩家时返回 {@code null}，什么都不动</b>：{@code slot} 初始值是 {@code -1}，
     * 循环没命中就一直保持 {@code -1}，直接返回，不会误删别人。</p>
     *
     * <p>旧工程这里没有这道检查——{@code slot} 初值是 {@code 0}，找不到人就会执行
     * {@code gamePlayers[0] = null}，把 <b>0 号槽位的玩家</b>误踢出房间。
     * 迁移时已经修正。</p>
     *
     * @param gameRoom 所在房间
     * @param playerId 玩家 id（= 用户 id）
     * @param <T>      玩家实际类型
     * @return 被移除的玩家；该 id 不在房间时返回 {@code null}
     */
    public static <T extends BaseGamePlayer> T removeGamePlayerById(BaseGameRoom gameRoom, long playerId) {
        try {
            synchronized (PLAYER_LOCK) {
                // 先按 id 找出他所在的槽位
                int slot = -1;
                for (int i = 0; i < gameRoom.getGamePlayers().length; i++) {
                    BaseGamePlayer player = gameRoom.getGamePlayers()[i];
                    // 用户 id 等于玩家 id，找到了
                    if (player != null && player.getUser() != null && player.getId() == playerId) {
                        slot = i;
                        break;
                    }
                }
                // 说明没有找到
                if (slot == -1) {
                    return null;
                }

                // 删除
                return removeGamePlayerBySeat(gameRoom, slot);
            }
        } catch (Exception e) {
            int roomCode = gameRoom == null ? -1 : gameRoom.getCode();
            log.error("移除玩家失败(按 id): room={}, player={}, error={}", roomCode, playerId, e.getMessage(), e);
        }

        return null;
    }

    /**
     * 按玩家 id 移除，并可选地在房间空掉时把房间一起删掉。
     *
     * <p>房间空了还留着的话，它会一直挂在 {@code roomCodeMap} 里被各种定时任务遍历到，
     * 所以「最后一个玩家走了」的调用点应该传 {@code checkRoom = true}。</p>
     *
     * @param gameRoom  所在房间
     * @param playerId  玩家 id
     * @param checkRoom 为 {@code true} 时，移除后若房间已无玩家则连同房间一并移除
     * @param <T>       玩家实际类型
     * @return 被移除的玩家
     */
    public static <T extends BaseGamePlayer> T removeGamePlayerById(BaseGameRoom gameRoom, long playerId, boolean checkRoom) {
        T player = removeGamePlayerById(gameRoom, playerId);
        // 是否也删除房间
        if (checkRoom) {
            // 房间空了才把房间也删掉
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
     * @param id  用户 id
     * @param <T> 玩家实际类型
     * @return 玩家；不在任何房间时返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseGamePlayer> T getPlayerById(long id) {
        return (T) playerIdMap.get(id);
    }

    /**
     * 根据玩家 id 获取其所在房间。
     *
     * @param id  用户 id
     * @param <T> 房间实际类型
     * @return 房间；玩家不在任何房间时返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseGameRoom> T getGameRoomByPlayerId(long id) {
        BaseGamePlayer gamePlayer = getPlayerById(id);
        return (T) (gamePlayer == null ? null : roomCodeMap.get(gamePlayer.getRoomCode()));
    }

    // ==================== 内部 ====================

    /**
     * 回写用户的 {@code onlineState}。
     *
     * <p><b>本方法永不抛异常，这是刻意的。</b></p>
     *
     * <p>它只是内存状态的一次「尽力回写」——调用它的地方，玩家已经<b>真的</b>入座或退出了，
     * 内存才是事实。DB 写不进去（连接池耗尽、抖动、超时、死锁重试）不该把整个操作判成失败：
     * 一旦异常冒到外层 catch，方法会返回 {@code null}，调用方以为操作没成功，
     * 而内存早已改完——两边就此不一致。表现出来就是「玩家卡在『加入失败』界面，
     * 服务端却认为他坐好了，他收不到房间消息也不会被踢，直到心跳超时」。</p>
     *
     * <p>顺带把判空也收在这里：{@code user} 为空、未登录、没有实体时直接跳过，
     * 免得每个调用点各写一遍还写漏。三种情况都不该写库——未登录用户没有实体，
     * 而 {@code onlineState} 是写在实体上的。</p>
     *
     * @param user        用户会话；为 {@code null}、未登录或没有实体时直接跳过
     * @param onlineState 要写入的状态值（玩法编号表示在游戏中，1 表示回到大厅）
     */
    private static void updateOnlineState(ServerUser user, int onlineState) {
        if (user == null || !user.isOnline() || user.getEntity() == null) {
            return;
        }
        try {
            user.getEntity().setOnlineState(onlineState);
            // 旧工程写的是 userMapper.update(entity)，该单参重载在 MyBatis-Plus 3.5.7+ 已被移除，
            // 对应的替代就是 updateById（同样是「按主键更新、跳过 null 字段」）
            userMapper.updateById(user.getEntity());
        } catch (Exception e) {
            // user.getId() 在日志里是安全的：ServerUser#getId 内部是
            // `entity == null ? 0 : entity.getId()`，不会 NPE
            log.error("回写 onlineState 失败: player={}, onlineState={}, error={}",
                    user.getId(), onlineState, e.getMessage(), e);
        }
    }

}
