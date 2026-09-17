package com.ytrue.game.common.dao.log.entity;

import com.ytrue.game.common.dao.data.DbEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Date;

/**
 * 登录日志实体（表 {@code tbl_app_login_log}）。
 *
 * @since 1.0.0
 */
@Getter
@Setter
public class LoginLogEntity extends DbEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 id
     */
    private long userId;

    /**
     * 退出游戏时间
     */
    private Date exitTime;

}
