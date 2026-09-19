package com.ytrue.game.framework.base.manager;

import com.google.protobuf.Message;
import com.ytrue.game.framework.base.data.BaseGamePlayer;
import com.ytrue.game.framework.base.data.BaseGameRoom;
import com.ytrue.game.framework.network.ClientSender;

/**
 * 基础房间管理类。
 *
 * <p>提供「把一条消息发给房间内所有人」这一最常用的动作，供各玩法的管理器继承
 * （如 {@code TwoEightManager}）或静态导入（如 {@code TEMessageService}）。</p>
 *
 * <p>与 {@code ClientSender} 的分工：{@code ClientSender} 面向「一个连接」，
 * 本类面向「一个房间」，负责把房内玩家逐个展开成连接。</p>
 *
 * @since 1.0.0
 */
public class BaseRoomManager {

    /**
     * 发送消息到房间内的所有在线玩家。
     *
     * <p><b>只发给在线玩家</b>：房间座位上可能留着已断线、但尚未被清理的玩家，
     * 向这种玩家发消息会查不到连接、消息被静默丢弃（见 {@code ClientSender} 的传输路由），
     * 因此先判 {@code isOnline()} 过滤掉。</p>
     *
     * @param msgCode  消息码
     * @param msg      业务消息
     * @param gameRoom 目标房间
     */
    public static void sendRoomMessage(int msgCode, Message msg, BaseGameRoom gameRoom) {
        for (BaseGamePlayer gamePlayer : gameRoom.getGamePlayers()) {
            // 座位可能为空；玩家也可能已断线（下线时不会立刻从座位上摘掉）
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
