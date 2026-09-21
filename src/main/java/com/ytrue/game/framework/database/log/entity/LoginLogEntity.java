package com.ytrue.game.framework.database.log.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ytrue.game.framework.database.DbEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Date;

/**
 * 登录日志实体（表 {@code app_login_log}）。
 *
 * @since 1.0.0
 */
@Getter
@Setter
@TableName("app_login_log")
public class LoginLogEntity extends DbEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 退出游戏时间
     */
    private Date exitTime;

}
