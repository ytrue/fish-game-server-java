package com.ytrue.game.framework.base.manager;

import com.google.protobuf.Message;
import com.ytrue.game.framework.base.data.BaseGamePlayer;
import com.ytrue.game.framework.base.data.BaseGameRoom;
import com.ytrue.game.framework.network.ClientSender;

/**
 * 房间广播器。
 *
 * <p>只做一件事：<b>把一条消息发给房间内的所有在线玩家</b>。</p>
 *
 * <p><b>与 {@link ClientSender} 的分工</b>：{@code ClientSender} 面向「一个连接」，
 * 本类面向「一个房间」，负责把房内玩家逐个展开成连接再交出去。
 * 一个是连接维度的发送入口，一个是房间维度的。</p>
 *
 * <p><b>为什么是纯静态工具类、而不是「所有玩法管理器的父类」</b>：
 * 它不管理任何东西——没有状态、没有生命周期、不持有房间，就一个转发动作。
 * 旧工程里它叫 {@code BaseRoomManager}，各玩法的管理器通过
 * {@code extends BaseRoomManager} 继承它，但那只是为了「裸调静态方法、省掉类名前缀」，
 * 并没有继承到任何实例状态——而且旧工程的 {@code TEMessageService} 早就在用
 * 更合适的写法：</p>
 * <pre>{@code
 * import static com.ytrue.game.framework.base.manager.RoomBroadcaster.sendRoomMessage;
 * }</pre>
 * <p>静态方法用静态导入，不需要建立继承关系。所以本类加了私有构造器，
 * 明确「不该被继承、也不该被实例化」。</p>
 *
 * @since 1.0.0
 */
public final class RoomBroadcaster {

    /**
     * 私有构造器，禁止实例化（纯工具类）。
     */
    private RoomBroadcaster() {
    }

    /**
     * 发送消息到房间内的所有在线玩家。
     *
     * <p><b>只发给在线玩家</b>：房间座位上可能留着已断线、但尚未被清理的玩家，
     * 向这种玩家发消息会查不到连接、消息被静默丢弃
     * （见 {@link ClientSender} 的传输路由），因此先判 {@code isOnline()} 过滤掉。</p>
     *
     * @param msgCode  消息码
     * @param msg      业务消息
     * @param gameRoom 目标房间
     */
    public static void sendRoomMessage(int msgCode, Message msg, BaseGameRoom gameRoom) {
        for (BaseGamePlayer gamePlayer : gameRoom.getGamePlayers()) {
            // 座位上可能没人；玩家也可能已断线（下线时不会立刻从座位上摘掉）
            if (gamePlayer != null && gamePlayer.getUser().isOnline()) {
                ClientSender.sendMessage(msgCode, msg, gamePlayer.getUser());
            }
        }
    }

    /**
     * 发送消息到房间内的所有在线玩家（接受未构建完成的 Builder，省去调用方一次 build）。
     *
     * @param msgCode  消息码
     * @param builder  消息构建器
     * @param gameRoom 目标房间
     */
    public static void sendRoomMessage(int msgCode, Message.Builder builder, BaseGameRoom gameRoom) {
        sendRoomMessage(msgCode, builder.build(), gameRoom);
    }

}
