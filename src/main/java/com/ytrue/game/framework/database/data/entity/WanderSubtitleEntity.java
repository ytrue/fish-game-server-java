package com.ytrue.game.framework.database.data.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytrue.game.framework.database.DbEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Date;

/**
 * 游走字幕实体（表 {@code app_wander_subtitle}）。
 *
 * @since 1.0.0
 */
@Getter
@Setter
@TableName("app_wander_subtitle")
public class WanderSubtitleEntity extends DbEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字幕内容
     */
    private String content;

    /**
     * 间隔时间（分钟）
     */
    private Integer intervalTime;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 下次推送时间（毫秒时间戳，由推送定时器在内存里维护）。
     *
     * <p><b>不是数据库列</b>：建表脚本 {@code app_wander_subtitle} 只有
     * {@code id / create_time / content / interval_time / start_time / end_time} 六列，
     * 没有 {@code next_send_time}。旧工程 mapper 是手写 SQL、列名写死，多一个字段无所谓；
     * 换成 {@code BaseMapper} 后字段会被自动拼进 SELECT / INSERT，必须显式排除，
     * 否则运行时报「列不存在」。</p>
     *
     * <p>类型必须是 {@code Long} 而不是 {@code Integer}：它存的是毫秒时间戳（约 1.7e12），
     * {@code Integer} 装不下，取模后的值会恒小于当前时间，导致字幕每轮都发且永不判过期。</p>
     */
    @TableField(exist = false)
    private Long nextSendTime;

}
