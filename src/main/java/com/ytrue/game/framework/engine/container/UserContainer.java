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
     * 添加 / 更新用户。
     *
     * @param user 用户
     */
    public static void putServerUser(ServerUser user) {
        // 同时写入五张索引表，保证各维度都能查到同一用户
        USER_CONNECT_MAP.put(user.getConnect(), user);
        USER_ID_MAP.put(user.getId(), user);
        USER_OPENID_MAP.put(user.getOpenid(), user);
        USER_UNIONID_MAP.put(user.getUnionid(), user);
        USER_NAME_MAP.put(user.getUsername(), user);
    }

    /**
     * 移除用户。
     *
     * @param user 用户
     */
    public static void removeServerUser(ServerUser user) {
        // 从五张索引表中同步移除
        USER_CONNECT_MAP.remove(user.getConnect());
        USER_ID_MAP.remove(user.getId());
        USER_OPENID_MAP.remove(user.getOpenid());
        USER_UNIONID_MAP.remove(user.getUnionid());
        USER_NAME_MAP.remove(user.getUsername());
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
        //
        // 必须按 entry 的 key 删除：removeServerUser 是按用户「当前」id/connect 删的，
        // 对不上这条残留的键，删了等于没删（同一批脏数据会被反复扫出来）。
        USER_ID_MAP.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(entry.getValue().getId()))
                .map(Entry::getKey)
                .toList()
                .forEach(USER_ID_MAP::remove);
    }
}
