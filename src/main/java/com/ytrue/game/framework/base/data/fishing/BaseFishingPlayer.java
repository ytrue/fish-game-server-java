package com.ytrue.game.framework.base.data.fishing;

import com.ytrue.game.framework.base.data.BaseGamePlayer;

/**
 * 基础捕鱼玩家。
 *
 * <p>当前只是 {@link BaseGamePlayer} 的空壳子类，作用是把「捕鱼」这条玩法线在类型上标记出来，
 * 让捕鱼的房间、管理器、控制器可以按类型筛选玩家（{@code instanceof BaseFishingPlayer}），
 * 而不必污染通用父类。</p>
 *
 * <p>捕鱼专属的字段（炮台等级、子弹、命中判定等）由业务层的具体玩家类添加。</p>
 *
 * @since 1.0.0
 */
public abstract class BaseFishingPlayer extends BaseGamePlayer {

}
