package com.ytrue.game.framework.network;

import com.google.protobuf.Message;
import com.ytrue.game.framework.engine.container.UserContainer;
import com.ytrue.game.framework.engine.data.NetworkMsgType;
import com.ytrue.game.framework.engine.data.ServerUser;
import com.ytrue.game.framework.engine.data.TransportType;
import com.ytrue.game.framework.engine.utils.SpringUtils;
import com.ytrue.game.framework.network.netty.tcp.TcpNetwork;
import com.ytrue.game.framework.network.netty.websocket.WebSocketNetwork;
import com.ytrue.game.framework.network.proto.NetworkMessage.HintMessageResponse;
import com.ytrue.game.framework.network.proto.NetworkMessage.NetworkMsgCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 客户端消息发送入口。
 *
 * <p>业务层「服务端 → 客户端」方向的所有操作都从这里走：单发消息、群发、发提示框、取客户端 IP、
 * 断开连接。与 {@code TcpServerHandler}（客户端 → 服务端，收）在方向上对称。</p>
 *
 * <p><b>传输路由</b>：客户端可能通过不同传输接入（TCP / WebSocket），各自维护独立的连接表。
 * 本类按 {@link ServerUser#getTransportType()} 决定走哪条网络——走错表会查不到连接、
 * 消息被静默丢弃，因此传输类型必须跟着会话走，而不能写死。</p>
 *
 * <p>本类是纯静态工具类，内部通过 {@link SpringUtils} 取网络实例，自身不持有任何静态状态。
 * （旧工程的 {@code NetManager} 用 {@code public static NetManager instance} 在构造器里赋值，
 * 属于静态单例反模式，已弃用该做法。）</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * ClientSender.sendMessage(NetworkMsgCode.S_C_PING_RESPONSE_VALUE, response, user);
 * ClientSender.sendHintMessage("金币不足", user);
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public final class ClientSender {

    /**
     * 私有构造器，禁止实例化（纯工具类）。
     */
    private ClientSender() {
    }

    // ==================== 单发 ====================

    /**
     * 向指定用户发送消息（接受未构建完成的 Builder，省去调用方一次 build）。
     *
     * <p>注意参数类型是 {@link Message.Builder} 而非 {@code GeneratedMessage.Builder<?>}：
     * 前者是所有生成类 Builder 的公共父接口且<b>不带泛型参数</b>，写 {@code Message.Builder<?>}
     * 会编译不过（该类型不接受类型参数）。</p>
     *
     * @param msgCode    消息码
     * @param msgBuilder 消息构建器
     * @param user       目标用户
     */
    public static void sendMessage(int msgCode, Message.Builder msgBuilder, ServerUser user) {
        sendMessage(msgCode, msgBuilder.build(), user);
    }

    /**
     * 向指定用户发送消息。
     *
     * @param msgCode 消息码
     * @param msg     业务消息
     * @param user    目标用户
     */
    public static void sendMessage(int msgCode, Message msg, ServerUser user) {
        sendMessage(msgCode, msg, user.getConnect(), user.getTransportType(), user.getMsgType());
    }

    /**
     * 向指定连接发送消息。
     *
     * @param msgCode   消息码
     * @param msg       业务消息
     * @param connect   连接标识
     * @param transport 该连接所属的传输类型
     */
    public static void sendMessage(int msgCode, Message msg, String connect, TransportType transport) {
        // 只有 BINARY 一种编解码方式被实现（见 BaseNetwork.sendMessageToClient），
        // 所以这个重载不对外暴露 msgType 参数，避免调用方误以为还有别的选择
        sendMessage(msgCode, msg, connect, transport, NetworkMsgType.BINARY);
    }

    /**
     * 向指定连接发送消息（完整参数版本）。
     *
     * @param msgCode   消息码
     * @param msg       业务消息
     * @param connect   连接标识
     * @param transport 该连接所属的传输类型
     * @param msgType   消息编解码类型
     */
    public static void sendMessage(int msgCode, Message msg, String connect,
                                   TransportType transport, NetworkMsgType msgType) {
        log.debug("向客户端[{}]发送消息[{}][{}]", connect, msgCode, msg.getClass().getSimpleName());
        network(transport).sendMessageToClient(msgCode, msg, connect, msgType);
    }

    // ==================== 群发 ====================

    /**
     * 向所有在线用户发送消息。
     *
     * <p>逐个按各自的传输类型发送——同一批用户可能来自不同传输，不能整批走同一条网络。</p>
     *
     * @param msgCode 消息码
     * @param msg     业务消息
     */
    public static void sendMessageToAll(int msgCode, Message msg) {
        List<ServerUser> serverUsers = UserContainer.getActiveServerUsers();
        for (ServerUser user : serverUsers) {
            // 只发给真正登录成功的用户：未登录的会话没有业务身份，收业务广播没有意义
            if (user.isOnline() && user.getId() > 0) {
                sendMessage(msgCode, msg, user);
            }
        }
    }

    // ==================== 提示消息 ====================

    /**
     * 向客户端发送提示消息（普通提示）。
     *
     * @param content 提示内容
     * @param user    目标用户
     */
    public static void sendHintMessage(String content, ServerUser user) {
        sendHintBoxMessage(content, user, 0);
    }

    /**
     * 向客户端发送警告消息。
     *
     * @param content 警告内容
     * @param user    目标用户
     */
    public static void sendWarnMessage(String content, ServerUser user) {
        sendHintBoxMessage(content, user, 1);
    }

    /**
     * 向客户端发送错误消息。
     *
     * @param content 错误内容
     * @param user    目标用户
     */
    public static void sendErrorMessage(String content, ServerUser user) {
        sendHintBoxMessage(content, user, 2);
    }

    /**
     * 向客户端发送弹框消息。
     *
     * @param content 消息内容
     * @param user    目标用户
     * @param level   消息级别：0 提示、1 警告、2 错误
     */
    public static void sendHintBoxMessage(String content, ServerUser user, int level) {
        HintMessageResponse.Builder builder = HintMessageResponse.newBuilder();
        builder.setContent(content);
        builder.setLevel(level);
        sendMessage(NetworkMsgCode.S_C_HINT_MESSAGE_RESPONSE_VALUE, builder.build(), user);
    }

    // ==================== 连接操作 ====================

    /**
     * 获取用户所在连接的客户端 IP。
     *
     * @param user 目标用户
     * @return 客户端 IP；取不到时返回空串
     */
    public static String getIpAddress(ServerUser user) {
        return getIpAddress(user.getConnect(), user.getTransportType());
    }

    /**
     * 获取指定连接的客户端 IP。
     *
     * @param connect   连接标识
     * @param transport 该连接所属的传输类型
     * @return 客户端 IP；取不到时返回空串
     */
    public static String getIpAddress(String connect, TransportType transport) {
        return network(transport).getClientIp(connect);
    }

    /**
     * 关闭用户的连接，并把它标记为不在线。
     *
     * <p>置为不在线是必要的：连接关闭本身也会触发 channelInactive 的清理，但在那之前，
     * 心跳任务等仍可能读到该用户并尝试向已关闭的连接发消息。</p>
     *
     * @param user 目标用户
     */
    public static void closeClientConnect(ServerUser user) {
        closeClientConnect(user.getConnect(), user.getTransportType());
        user.setOnline(false);
    }

    /**
     * 关闭指定连接。
     *
     * @param connect   连接标识
     * @param transport 该连接所属的传输类型
     */
    public static void closeClientConnect(String connect, TransportType transport) {
        network(transport).closeClientConnect(connect);
    }

    // ==================== 内部 ====================

    /**
     * 按传输类型取对应的网络实现。
     *
     * @param transport 传输类型
     * @return 网络实现
     */
    private static INetwork network(TransportType transport) {
        return switch (transport) {
            case TCP -> SpringUtils.getBean(TcpNetwork.BEAN_NAME, INetwork.class);
            case WEBSOCKET -> SpringUtils.getBean(WebSocketNetwork.BEAN_NAME, INetwork.class);
        };
    }

}
