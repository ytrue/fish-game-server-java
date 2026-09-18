package com.ytrue.game.framework.database.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ytrue.game.framework.database.data.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * 用户数据交互接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，获得 {@code selectById}/{@code updateById}
 * 等通用 CRUD 能力；其中 {@code updateById} 默认跳过值为 {@code null} 的字段，
 * 实现「部分更新」（null 表示未设置，不覆盖对应列）。</p>
 *
 * @since 1.0.0
 */
@Component
public interface UserMapper extends BaseMapper<UserEntity> {

}
