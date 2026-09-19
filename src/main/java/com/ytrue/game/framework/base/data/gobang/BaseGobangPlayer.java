package com.ytrue.game.framework.base.data.gobang;

import com.ytrue.game.framework.base.data.BaseGamePlayer;

/**
 * 基础五子棋玩家。
 *
 * <p>当前只是 {@link BaseGamePlayer} 的空壳子类，作用是把「五子棋」这条玩法线在类型上标记出来，
 * 让五子棋的管理器可以按类型筛选玩家。</p>
 *
 * <p>五子棋专属的字段（悔棋次数、计时、头像等）由业务层的具体玩家类添加。</p>
 *
 * @since 1.0.0
 */
public abstract class BaseGobangPlayer extends BaseGamePlayer {

}
