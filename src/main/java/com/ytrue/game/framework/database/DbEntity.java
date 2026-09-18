package com.ytrue.game.framework.database;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 数据实体基类。
 *
 * <p>所有数据库实体（主库实体、日志库实体）的统一父类，提供主键 {@code id}
 * 与创建时间 {@code createTime} 两个公共字段。</p>
 *
 * @since 1.0.0
 */
@Getter
@Setter
public abstract class DbEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据主键 id（数据库自增）。
     */
    @TableId(type = IdType.AUTO)
    protected Long id = 100001L;

    /**
     * 创建时间（仅插入时写入，更新时忽略）。
     */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    protected Date createTime = new Date();

}
