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
     *
     * <p><b>必须保持 {@code null} 初值，不要给它赋默认值。</b>
     * {@code IdType.AUTO} 只声明「主键由数据库自增生成」，<b>并不会</b>把 id 列从 INSERT 里排除——
     * MyBatis-Plus 仍按「字段非 null 才写」的策略决定是否带上这一列。
     * 所以只要这里给了初值，生成的 SQL 就会变成
     * {@code INSERT INTO xxx (id, ...) VALUES (100001, ...)}，
     * 第一条能插进去，<b>第二条就主键冲突</b>，且显式写 id 不会推进数据库的序列。</p>
     *
     * <p>旧工程写的是 {@code protected long id = 100001;}，但那时 mapper 是<b>手写 INSERT</b>、
     * 列清单里根本不含 id（靠 {@code @Options(useGeneratedKeys = true)} 回填），
     * 所以初值无副作用。换成 {@code BaseMapper.insert} 后这个初值才有害。</p>
     */
    @TableId(type = IdType.AUTO)
    protected Long id;

    /**
     * 创建时间（仅插入时写入，更新时忽略）。
     */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    protected Date createTime = new Date();

}
