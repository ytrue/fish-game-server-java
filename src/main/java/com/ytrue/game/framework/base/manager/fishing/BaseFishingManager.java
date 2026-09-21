package com.ytrue.game.framework.base.manager.fishing;

import com.ytrue.game.framework.base.container.GameContainer;
import com.ytrue.game.framework.base.data.BaseGameRoom;
import com.ytrue.game.framework.base.data.fishing.BaseFishingRoom;
import com.ytrue.game.framework.engine.utils.ThreadPoolFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 基础捕鱼管理类。
 *
 * <p>捕鱼是「持续跑帧」的玩法：怪物游动、刷新、开炮判定都要按固定节奏推进。
 * 本类在构造时把自己注册进共享任务线程池，按固定周期遍历所有捕鱼房间、推进房间时钟，
 * 再把每个房间交给子类的 {@link #onFishingRoomTick} 去做玩法相关的处理。</p>
 *
 * <p><b>怎么用</b>：子类调 {@code super(loopTime)} 指定帧间隔（毫秒），并实现
 * {@link #onFishingRoomTick}。例如 {@code super(50)} 表示 20 帧/秒。</p>
 *
 * <p><b>本类不加 {@code @Component}</b>——它是抽象的，而且需要子类通过构造器传入帧间隔。
 * Spring 的组件扫描会跳过抽象类（候选组件要求 {@code isConcrete()}），
 * 给抽象类标 {@code @Component} 的后果是「不生成 bean 且不报错」，属于静默失效。</p>
 *
 * @since 1.0.0
 */
@Slf4j
public abstract class BaseFishingManager {
    /**
     * 注册捕鱼房间的帧循环任务。
     *
     * <p>注册后立即开始执行（初始延迟为 0），之后每隔 {@code loopTime} 毫秒跑一次。</p>
     *
     * <p><b>注意 {@code this} 逸出</b>：构造器里就把 {@code this} 交给了线程池，
     * 而子类的构造器还没执行完——初始延迟是 0，所以第一次任务理论上可能在子类字段
     * 初始化之前就跑起来。旧工程即如此，迁移时保持原样；子类若依赖构造器里赋值的字段，
     * 需要自行留意。</p>
     *
     * <p>任务跑在 {@link ThreadPoolFactory#TASK_SERVICE_POOL} 上，与所有玩法的帧循环
     * 共用同一个池，单个房间处理过慢会拖累其它房间的节奏。</p>
     *
     * @param loopTime 帧间隔（毫秒）
     */
    public BaseFishingManager(long loopTime) {
        ThreadPoolFactory.TASK_SERVICE_POOL.scheduleAtFixedRate(this::doFishingRoomTask, 0, loopTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 捕鱼房间循环任务：遍历所有捕鱼房间，推进时钟并回调子类。
     *
     * <p>整体包了一层 try/catch：{@code scheduleAtFixedRate} 的规则是
     * <b>任务抛出异常后就不再被调度</b>，某个房间的一次偶发异常会让整个玩法的帧循环
     * 永久停摆，因此这里必须把异常吞掉、保证周期任务持续运行。</p>
     */
    protected void doFishingRoomTask() {
        for (BaseGameRoom gameRoom : GameContainer.getGameRooms()) {
            try {
                // 房间里混着各种玩法，只挑捕鱼的出来处理
                if (gameRoom instanceof BaseFishingRoom fishingRoom) {
                    // 先走一格房间时钟，子类处理时读到的就是当前帧
                    fishingRoom.addRoomTick();
                    onFishingRoomTick(fishingRoom);
                }
            } catch (Exception e) {
                // 格式说明：末尾那个 e 必须「多出来」才能被 SLF4J 识别为异常、打印堆栈
                log.error("执行捕鱼房间循环任务时出现异常:[{}]", e.getMessage(), e);
            }
        }
    }

    /**
     * 捕鱼房间帧回调，由子类实现具体的玩法逻辑。
     *
     * <p>每帧调一次（帧间隔由构造器传入的 {@code loopTime} 决定）。
     * 进来时 {@link BaseFishingRoom#getRoomTick()} <b>已经自增过</b>，
     * 所以这里读到的就是当前帧号。</p>
     *
     * <p>旧工程里这个方法叫 {@code doFishingRoomTask0}——{@code 0} 后缀看不出它是什么，
     * 改成 {@code on} 前缀：它就是「每帧发生一次」的回调。子类实现时会带 {@code @Override}，
     * 名字对不上会直接编译失败，不存在漏改。</p>
     *
     * @param fishingRoom 本轮要处理的捕鱼房间
     */
    protected abstract void onFishingRoomTick(BaseFishingRoom fishingRoom);

}
