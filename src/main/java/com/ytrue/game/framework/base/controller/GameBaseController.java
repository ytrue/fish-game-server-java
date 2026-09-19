package com.ytrue.game.framework.base.controller;

import com.google.protobuf.Message;
import com.ytrue.game.framework.base.container.GameContainer;
import com.ytrue.game.framework.base.data.BaseGamePlayer;
import com.ytrue.game.framework.base.data.BaseGameRoom;
import com.ytrue.game.framework.base.manager.BaseRoomManager;
import com.ytrue.game.framework.base.proto.GameBaseMessage.ChatInRoomRequest;
import com.ytrue.game.framework.base.proto.GameBaseMessage.ChatInRoomResponse;
import com.ytrue.game.framework.base.proto.GameBaseMessage.GameBaseMsgCode;
import com.ytrue.game.framework.base.proto.GameBaseMessage.VoiceInRoomRequest;
import com.ytrue.game.framework.base.proto.GameBaseMessage.VoiceInRoomResponse;
import com.ytrue.game.framework.engine.annotation.AppController;
import com.ytrue.game.framework.engine.annotation.AppHandler;
import com.ytrue.game.framework.engine.data.ServerUser;

import java.lang.reflect.Method;

/**
 * 游戏基础控制器。
 *
 * <p>提供所有玩法共用的房间内通信协议：文字聊天与语音聊天。
 * 两条协议都只做一件事——把发起者的内容广播给房间内其余玩家，不涉及具体玩法规则。</p>
 *
 * <p><b>关于 {@code exp}</b>：本控制器的处理方法签名是三个参数
 * {@code (消息, 玩家, 房间)}，而框架在「控制器没配校验方法」时只会传两个参数。
 * 之所以能跑通，是因为下面实现了 {@code checker}——框架把「处理方法本身」交给 checker，
 * 由 checker 决定怎么调。{@code exp} 就是 checker 用来区分调用方式的标记：
 * 0 表示只传用户，1 表示要先把玩家和房间查出来再传。</p>
 *
 * @since 1.0.0
 */
@AppController
public class GameBaseController {

    /**
     * 控制器统一的校验与调用方法。
     *
     * <p>框架在调用具体的 {@code @AppHandler} 方法前先走这里，
     * 因此本方法承担了「按 {@code exp} 组装参数并反射调用」的职责。
     * 各 {@code @AppHandler} 方法不再各自重复这段逻辑。</p>
     *
     * <p>旧工程的 checker 里没有做任何额外校验，本方法保持同样的行为，只是把注释补全。</p>
     *
     * @param taskMethod 本次要执行的处理方法
     * @param msg        业务消息
     * @param user       消息所属的用户会话
     * @param exp        附加标记（对应 {@code @AppHandler.exp}）
     * @throws Exception 处理方法内部抛出异常时向外传递
     */
    public void checker(Method taskMethod, Message msg, ServerUser user, Long exp) throws Exception {
        if (exp == 0) {
            // exp = 0：不依赖房间，直接以 (消息, 用户) 调用
            taskMethod.invoke(this, msg, user);
        } else if (exp == 1) {
            // exp = 1：要求玩家必须在房间里。
            // 先按用户 id 反查房间——玩家不在房间时房间为 null，此时静默丢弃这条消息，
            // 不报错也不回包（客户端此时通常已切场景，回包反而会造成困扰）
            BaseGameRoom gameRoom = GameContainer.getGameRoomByPlayerId(user.getId());
            if (gameRoom != null) {
                BaseGamePlayer gamePlayer = gameRoom.getGamePlayerById(user.getId());
                taskMethod.invoke(this, msg, gamePlayer, gameRoom);
            }
        }
    }

    /**
     * 房间聊天任务。
     *
     * <p>把发送者的玩家 id 填进响应包，再广播给房间内所有人（<b>包含发送者自己</b>，
     * 客户端据此确认消息已发出并归位到聊天列表）。</p>
     *
     * @param req      聊天请求（聊天内容、内容类型）
     * @param player   发送者
     * @param gameRoom 所在房间
     */
    @AppHandler(msgCode = GameBaseMsgCode.C_S_CHAT_IN_ROOM_REQUEST_VALUE, exp = 1)
    public void doChatInRoomTask(ChatInRoomRequest req, BaseGamePlayer player, BaseGameRoom gameRoom) {
        ChatInRoomResponse.Builder builder = ChatInRoomResponse.newBuilder();
        // 玩家 id 由服务端填，不信任客户端传来的值——否则可以伪造成别人发言
        builder.setPlayerId(player.getId());
        builder.setChatContent(req.getChatContent());
        builder.setContentType(req.getContentType());
        BaseRoomManager.sendRoomMessage(GameBaseMsgCode.S_C_CHAT_IN_ROOM_RESPONSE_VALUE, builder, gameRoom);
    }

    /**
     * 房间语音聊天任务。
     *
     * <p>与文字聊天的差别仅仅是少了内容类型、多了二进制语音体；
     * 服务端不解析语音内容，只做转发。</p>
     *
     * @param req      语音请求（语音内容）
     * @param player   发送者
     * @param gameRoom 所在房间
     */
    @AppHandler(msgCode = GameBaseMsgCode.C_S_VOICE_IN_ROOM_REQUEST_VALUE, exp = 1)
    public void doVoiceInRoomTask(VoiceInRoomRequest req, BaseGamePlayer player, BaseGameRoom gameRoom) {
        VoiceInRoomResponse.Builder builder = VoiceInRoomResponse.newBuilder();
        builder.setPlayerId(player.getId());
        builder.setBody(req.getBody());
        BaseRoomManager.sendRoomMessage(GameBaseMsgCode.S_C_VOICE_IN_ROOM_RESPONSE_VALUE, builder, gameRoom);
    }
}
