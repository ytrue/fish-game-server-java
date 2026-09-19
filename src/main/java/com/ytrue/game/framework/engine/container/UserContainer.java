package com.ytrue.game.framework.engine.container;

import com.ytrue.game.framework.database.data.mapper.UserMapper;
import com.ytrue.game.framework.engine.data.ServerUser;
import com.ytrue.game.framework.engine.event.UserInitEvent;
import com.ytrue.game.framework.engine.utils.SpringEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户容器。
 *
 * <p>维护所有活跃用户（{@link ServerUser}）的内存索引，支持按连接、id、openid、unionid、用户名
 * 等维度查询用户，并负责用户数据的初始化与一致性巡检。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class UserContainer {

    /**
     * 用户数据库操作接口。
     */
    private static UserMapper userMapper;

    /**
     * 活跃用户 connectId 表。
     */
    private static final Map<Object, ServerUser> USER_CONNECT_MAP = new ConcurrentHashMap<>();

    /**
     * 活跃用户 id 表。
     */
    private static final Map<Long, ServerUser> USER_ID_MAP = new ConcurrentHashMap<>();

    /**
     * 活跃用户 openid 表。
     */
    private static final Map<String, ServerUser> USER_OPENID_MAP = new ConcurrentHashMap<>();

    /**
     * 活跃用户 unionid 表。
     */
    private static final Map<String, ServerUser> USER_UNIONID_MAP = new ConcurrentHashMap<>();

    /**
     * 活跃用户用户名表。
     */
    private static final Map<String, ServerUser> USER_NAME_MAP = new ConcurrentHashMap<>();

    /**
     * 构造器注入用户数据库操作接口。
     *
     * @param userMapper 用户数据库操作接口
     */
    @Autowired
    public UserContainer(UserMapper userMapper) {
        UserContainer.userMapper = userMapper;
    }

    /**
     * 添加 / 更新用户索引。
     *
     * <p>只写入「键确实有效」的维度：</p>
     * <ul>
     *     <li>连接维度始终可写——{@code connect} 是每条连接唯一的 channelId；</li>
     *     <li>身份维度（id / openid / unionid / username）仅在用户已建号、且该字段有值时才写。</li>
     * </ul>
     *
     * <p>不判断会有两类问题：未登录用户没有实体，{@code getId()} 恒为 {@code 0}、
     * 其余恒为空串，所有连接会挤在同几个键上互相覆盖；而已登录用户也可能缺某一项
     * （账号密码用户没有 openid，微信用户可能没有 username），
     * 而 {@link ConcurrentHashMap} 不允许 null 键，直接写入会抛 {@code NullPointerException}。</p>
     *
     * @param user 用户
     */
    public static void putServerUser(ServerUser user) {
        // 连接索引：key 是每条连接唯一的 channelId，任何时候都安全
        USER_CONNECT_MAP.put(user.getConnect(), user);

        // 未建号的用户只有连接身份，到此为止
        if (user.getEntity() == null) {
            return;
        }

        // id 是 long 原生类型，建号后必定有效
        USER_ID_MAP.put(user.getId(), user);
        putIfPresent(USER_OPENID_MAP, user.getOpenid(), user);
        putIfPresent(USER_UNIONID_MAP, user.getUnionid(), user);
        putIfPresent(USER_NAME_MAP, user.getUsername(), user);
    }

    /**
     * 移除用户索引。
     *
     * <p>与 {@link #putServerUser} 对称，只处理当初写入过的维度。</p>
     *
     * <p>用两参 {@code remove(key, value)} 而非单参版本：只在「该键当前映射的正是这个实例」
     * 时才删除，避免误删他人索引。</p>
     *
     * @param user 用户
     */
    public static void removeServerUser(ServerUser user) {
        USER_CONNECT_MAP.remove(user.getConnect(), user);

        // 与写入侧对称：未建号时只写过连接索引
        if (user.getEntity() == null) {
            return;
        }

        USER_ID_MAP.remove(user.getId(), user);
        removeIfPresent(USER_OPENID_MAP, user.getOpenid(), user);
        removeIfPresent(USER_UNIONID_MAP, user.getUnionid(), user);
        removeIfPresent(USER_NAME_MAP, user.getUsername(), user);
    }

    /**
     * 键有效时写入索引。
     *
     * @param map  目标索引表
     * @param key  索引键（为 {@code null} 或空串时跳过）
     * @param user 用户
     */
    private static void putIfPresent(Map<String, ServerUser> map, String key, ServerUser user) {
        if (key != null && !key.isEmpty()) {
            map.put(key, user);
        }
    }

    /**
     * 键有效时移除索引。
     *
     * @param map  目标索引表
     * @param key  索引键（为 {@code null} 或空串时跳过）
     * @param user 用户
     */
    private static void removeIfPresent(Map<String, ServerUser> map, String key, ServerUser user) {
        if (key != null && !key.isEmpty()) {
            map.remove(key, user);
        }
    }

    /**
     * 根据连接获取用户。
     *
     * @param connect 连接标识
     * @return 用户；不存在时返回 {@code null}
     */
    public static ServerUser getUserByConnect(Object connect) {
        return USER_CONNECT_MAP.get(connect);
    }

    /**
     * 初始化用户（用户信息、玩家信息等数据）。
     *
     * @param user 用户
     */
    public static void initUser(ServerUser user) {
        // 用户实体存在时才发布初始化事件，各业务模块通过 @EventListener 完成自身初始化
        if (user.getEntity() != null) {
            SpringEventPublisher.publish(new UserInitEvent(user));
        }
    }

    /**
     * 根据 id 获取用户（内存未命中时回源数据库）。
     *
     * @param userId 用户 id
     * @return 用户；不存在时返回 {@code null}
     */
    public static ServerUser getUserById(long userId) {
        // 先在内存索引中命中
        ServerUser user = getActiveUserById(userId);
        if (user != null) {
            return user;
        }

        // 未命中则回源数据库，加锁避免并发重复建用户
        synchronized (USER_ID_MAP) {
            // 双重检查：等锁期间可能已有其它线程建好了同一个用户。
            // 不复查的话两个线程会各自查库、各自 new 出实例，后写入的覆盖前者，
            // 导致先返回的那个 ServerUser 成为孤儿——业务代码改它的字段对后续查询毫无影响。
            user = getActiveUserById(userId);
            if (user != null) {
                return user;
            }

            user = new ServerUser();
            user.setEntity(userMapper.selectById(userId));
            initUser(user);
            if (user.getEntity() != null) {
                // 建号成功则写入索引表
                UserContainer.putServerUser(user);
                return user;
            }
        }

        return null;
    }

    /**
     * 根据 id 获取活跃用户（仅查内存索引，不回源数据库）。
     *
     * @param userId 用户 id
     * @return 用户；不存在时返回 {@code null}
     */
    public static ServerUser getActiveUserById(long userId) {
        return USER_ID_MAP.get(userId);
    }

    /**
     * 根据 unionid 获取用户。
     *
     * @param unionid unionid
     * @return 用户；不存在时返回 {@code null}
     */
    public static ServerUser getUserByUnionid(String unionid) {
        return USER_UNIONID_MAP.get(unionid);
    }

    /**
     * 根据用户名获取用户。
     *
     * @param username 用户名
     * @return 用户；不存在时返回 {@code null}
     */
    public static ServerUser getUserByUsername(String username) {
        return USER_NAME_MAP.get(username);
    }

    /**
     * 获取活跃用户数量。
     *
     * @return 活跃用户数量
     */
    public static int getActiveUserCount() {
        return USER_CONNECT_MAP.size();
    }

    /**
     * 获取活跃用户列表。
     *
     * @return 活跃用户列表
     */
    public static List<ServerUser> getActiveServerUsers() {
        return new LinkedList<>(USER_CONNECT_MAP.values());
    }

    /**
     * 定时巡检：清理「id 与实体不匹配」的脏数据。
     * 项目启动后先等 30 秒，然后每隔 30 秒执行一次。
     */
    @Scheduled(initialDelay = 30000, fixedRate = 30000)
    public void checker() {
        // 清理「索引键与用户当前 id 不符」的脏条目。
        // 典型来源：连接刚建立时用户还没有实体（getId() 为 0），putServerUser 会以 0 为键建一条索引；
        // 登录拿到实体后 id 变成真实值，再 put 一次会新建条目，原来那个 0 键条目就此残留。
        // TODO 这个后面在理解
        // 必须按 entry 的 key 删除：removeServerUser 是按用户「当前」id/connect 删的，
        // 对不上这条残留的键，删了等于没删（同一批脏数据会被反复扫出来）。
        USER_ID_MAP.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(entry.getValue().getId()))
                .map(Entry::getKey)
                .toList()
                .forEach(USER_ID_MAP::remove);
    }
}
