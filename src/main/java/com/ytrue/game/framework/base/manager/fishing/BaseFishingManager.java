package com.ytrue.game.framework.base.manager.fishing;

import com.ytrue.game.framework.base.container.GameContainer;
import com.ytrue.game.framework.base.data.BaseGameRoom;
import com.ytrue.game.framework.base.data.fishing.BaseFishingRoom;
import com.ytrue.game.framework.engine.utils.ThreadPoolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 基础捕鱼管理类。
 *
 * <p>捕鱼是「持续跑帧」的玩法：怪物的移动、刷新、开炮判定都需要按固定节奏推进。
 * 本类在构造时把自己注册进共享任务线程池，按固定周期遍历所有捕鱼房间、推进房间时钟，
 * 再把每个房间交给子类的 {@link #doFishingRoomTask0} 去做玩法相关的处理。</p>
 *
 * <p><b>怎么用</b>：子类调 {@code super(loopTime)} 指定帧间隔（毫秒），并实现
 * {@link #doFishingRoomTask0}。例如 {@code super(50)} 表示 20 帧/秒。</p>
 *
 * <p><b>注意：本类不加 {@code @Component}</b>——它是抽象的，且需要子类通过构造器传入帧间隔。
 * Spring 的组件扫描会跳过抽象类（候选组件要求 {@code isConcrete()}），
 * 给抽象类标 {@code @Component} 的后果是「不生成 bean 且不报错」，属于静默失效。</p>
 *
 * @since 1.0.0
 */
public abstract class BaseFishingManager {

    /**
     * 日志对象。
     *
     * <p>用 {@code getClass()} 而不是 {@code BaseFishingManager.class} 建 logger：
     * 这样日志里的类名是真正的子类（如 {@code FishingManager}），
     * 排查时能一眼看出是哪个玩法在报错。也正因如此，这个字段是<b>实例</b>字段而非 {@code static}，
     * 不能用 Lombok 的 {@code @Slf4j}（它生成的是静态 logger）。</p>
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 注册捕鱼房间的帧循环任务。
     *
     * <p>注册后立即开始执行（初始延迟为 0），之后每隔 {@code loopTime} 毫秒跑一次。</p>
     *
     * <p><b>注意 {@code this} 逸出</b>：构造器里就把 {@code this} 交给了线程池，
     * 而子类的构造器尚未执行完——理论上第一次任务可能在子类字段初始化之前就跑起来。
     * 因为初始延迟是 0，这个窗口是真实存在的。旧工程即如此，迁移时保持原样；
     * 若某个子类依赖构造器里赋值的字段，需要自行留意。</p>
     *
     * <p>任务跑在 {@link ThreadPoolFactory#TASK_SERVICE_POOL} 上，与所有玩法的帧循环共用同一个池，
     * 单个房间处理过慢会拖累其它房间的节奏。</p>
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
     * <b>任务抛出异常后就不再被调度</b>，某个房间的一次偶发异常会让整个玩法的帧循环永久停摆，
     * 因此这里必须把异常吞掉、保证周期任务持续运行。</p>
     */
    protected void doFishingRoomTask() {
        try {
            for (BaseGameRoom gameRoom : GameContainer.getGameRooms()) {
                // 房间里混着各种玩法，只挑捕鱼的出来处理
                if (gameRoom instanceof BaseFishingRoom) {
                    BaseFishingRoom fishingRoom = (BaseFishingRoom) gameRoom;
                    // 先走一格房间时钟，子类处理时读到的就是当前帧
                    fishingRoom.addRoomTick();
                    doFishingRoomTask0(fishingRoom);
                }
            }
        } catch (Exception e) {
            log.error("执行捕鱼房间循环任务时出现异常:[{}]", e.getMessage(), e);
        }
    }

    /**
     * 捕鱼房间回调任务，由子类实现具体的玩法逻辑。
     *
     * @param fishingRoom 本轮要处理的捕鱼房间
     */
    protected abstract void doFishingRoomTask0(BaseFishingRoom fishingRoom);

}
