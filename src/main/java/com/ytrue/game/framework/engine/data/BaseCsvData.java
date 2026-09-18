package com.ytrue.game.framework.engine.data;

import lombok.Getter;
import lombok.Setter;

/**
 * 基础 CSV 数据类。
 *
 * <p>所有由 {@code .csv} 配置表加载的配置实体的统一父类，提供公共主键 {@code id}。</p>
 *
 * @since 1.0.0
 */
@Getter
@Setter
public class BaseCsvData {

    /**
     * 数据 id。
     */
    private long id;

}
