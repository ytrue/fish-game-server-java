package com.ytrue.game.framework.database.data.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ytrue.game.framework.database.DbEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 用户实名认证数据实体（表 {@code app_user_authentication}）。
 *
 * @since 1.0.0
 */
@Getter
@Setter
@TableName("app_user_authentication")
public class UserAuthenticationEntity extends DbEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 id
     */
    private long userId;

    /**
     * 真实姓名
     */
    private String name;

    /**
     * 身份证号
     */
    private String idcardNo;

    /**
     * 手机号码
     */
    private String phoneNo;

}
