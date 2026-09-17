package com.ytrue.game.common.dao.data.entity;

import com.ytrue.game.common.dao.data.DbEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Date;

/**
 * 游走字幕实体（表 {@code tbl_app_wander_subtitle}）。
 *
 * @since 1.0.0
 */
@Getter
@Setter
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
    private int intervalTime;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 下次推送时间
     */
    private long nextSendTime;

}
