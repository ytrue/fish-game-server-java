package com.ytrue.game.framework.base.data.gobang;

import com.ytrue.game.framework.base.data.BaseGamePlayer;

/**
 * 基础五子棋玩家。
 *
 * <p>当前是 {@link BaseGamePlayer} 的<b>空壳子类</b>，一个字段都没有。
 * 它的作用是把「五子棋」这条玩法线在类型上标记出来——
 * {@code BaseGobangManager} 的轮转落子、胜负判定都要求玩家能转成
 * {@link BaseGobangPlayer}，靠的就是这层类型。</p>
 *
 * <p>五子棋专属的字段（悔棋次数、本局计时、头像等）由业务层的具体玩家类添加。</p>
 *
 * @since 1.0.0
 */
public abstract class BaseGobangPlayer extends BaseGamePlayer {

}
