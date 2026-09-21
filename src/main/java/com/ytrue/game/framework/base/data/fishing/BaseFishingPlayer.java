package com.ytrue.game.framework.base.data.fishing;

import com.ytrue.game.framework.base.data.BaseGamePlayer;

/**
 * 基础捕鱼玩家。
 *
 * <p>当前是 {@link BaseGamePlayer} 的<b>空壳子类</b>，一个字段都没有。
 * 它存在的意义不是装字段，而是把「捕鱼」这条玩法线在<b>类型上标记出来</b>：
 * 捕鱼的管理器、定时任务需要从房间里一堆玩家中挑出捕鱼的来处理，
 * 靠的就是 {@code instanceof BaseFishingPlayer}。没有这层类型，
 * 就只能靠 {@code getGameId()} 之类的运行时值去猜，既慢又容易漏。</p>
 *
 * <p>捕鱼专属的字段（炮台等级、子弹、命中判定等）由业务层的具体玩家类添加。</p>
 *
 * @since 1.0.0
 */
public abstract class BaseFishingPlayer extends BaseGamePlayer {

}
