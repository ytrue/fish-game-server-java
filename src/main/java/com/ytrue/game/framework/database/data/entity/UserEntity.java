package com.ytrue.game.framework.database.data.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ytrue.game.framework.database.DbEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 用户数据实体（表 {@code app_user}）。
 *
 * @since 1.0.0
 */
@Getter
@Setter
@TableName("app_user")
public class UserEntity extends DbEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phonenum;

    /**
     * 第三方 openid
     */
    private String openid;

    /**
     * 第三方 unionid
     */
    private String unionid;

    /**
     * 密码（32 位 md5）
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像序号（0：使用头像地址）
     */
    private Integer headIndex = 1;

    /**
     * 头像地址
     */
    private String headUrl;

    /**
     * 性别（0：男 1：女）
     */
    private Integer sex;

    /**
     * 我的邀请码
     */
    private Long myInviteCode;

    /**
     * 我绑定的邀请码
     */
    private Long inviteCode;

    /**
     * 用户状态
     */
    private Integer userState;

    /**
     * 在线状态
     */
    private Integer onlineState;

}
