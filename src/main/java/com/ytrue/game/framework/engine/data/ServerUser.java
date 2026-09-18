package com.ytrue.game.framework.engine.data;

import com.ytrue.game.framework.database.DbEntity;
import com.ytrue.game.framework.database.data.entity.UserEntity;
import com.ytrue.game.framework.database.log.entity.LoginLogEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 活跃用户（在线玩家会话）。
 *
 * <p>代表一个已建立连接的玩家，聚合了其连接信息、用户数据实体、登录日志、通信消息类型等
 * 运行时状态，是框架层在内存中维护玩家上下文的核心对象。</p>
 *
 * @since 1.0.0
 */
@Getter
@Setter
public class ServerUser {

    /**
     * 用户连接标识。
     */
    private String connect = "";

    /**
     * 连接时间（毫秒时间戳）。
     */
    private long connectTime = System.currentTimeMillis();

    /**
     * 最后活跃时间（毫秒时间戳）。
     */
    private long lastActiveTime;

    /**
     * 验证码。
     */
    private String checkCode;

    /**
     * 获取验证码的等待时间。
     */
    private long checkCodeTime;

    /**
     * 用户数据实体。
     */
    private UserEntity entity;

    /**
     * 额外数据表。
     */
    private Map<String, DbEntity> entityMap = new HashMap<>();

    /**
     * 在线标识。
     */
    private boolean online;

    /**
     * 版本验证标识。
     */
    private boolean versionCheck;

    /**
     * 登录日志实体。
     */
    private LoginLogEntity loginLogEntity;

    /**
     * 通信消息类型。
     */
    private NetworkMsgType msgType = NetworkMsgType.BINARY;

    /**
     * 是否登录成功。
     */
    private boolean loginSuccess = true;

    /**
     * 获取用户 id。
     *
     * @return 用户 id；用户实体为空时返回 {@code 0}
     */
    public long getId() {
        return entity == null ? 0 : entity.getId();
    }

    /**
     * 获取用户 openid。
     *
     * @return 用户 openid；用户实体为空时返回空串
     */
    public String getOpenid() {
        return entity == null ? "" : entity.getOpenid();
    }

    /**
     * 获取用户 unionid。
     *
     * @return 用户 unionid；用户实体为空时返回空串
     */
    public String getUnionid() {
        return entity == null ? "" : entity.getUnionid();
    }

    /**
     * 获取用户昵称。
     *
     * @return 用户昵称；用户实体为空时返回空串
     */
    public String getNickname() {
        return entity == null ? "" : entity.getNickname();
    }

    /**
     * 获取用户名。
     *
     * @return 用户名；用户实体为空时返回空串
     */
    public String getUsername() {
        return entity == null ? "" : entity.getUsername();
    }

    /**
     * 获取用户手机号。
     *
     * @return 用户手机号；用户实体为空时返回空串
     */
    public String getPhonenum() {
        return entity == null ? "" : entity.getPhonenum();
    }

    /**
     * 添加额外数据。
     *
     * @param entityId 额外数据标识
     * @param entity   额外数据实体
     */
    public void putExpertData(String entityId, DbEntity entity) {
        entityMap.put(entityId, entity);
    }

    /**
     * 获取额外数据。
     *
     * @param entityId 额外数据标识
     * @param <T>      额外数据实体类型
     * @return 额外数据实体；不存在时返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T extends DbEntity> T getExpertData(String entityId) {
        return (T) entityMap.get(entityId);
    }

}
