package com.ytrue.game.framework.network;

import com.google.protobuf.Message;
import com.ytrue.game.framework.engine.data.NetworkMsgType;

/**
 * 网络监听接口。
 *
 * <p>抽象出「打开监听 / 关闭监听 / 向客户端发消息 / 断开客户端 / 取客户端 IP」这组能力，
 * 由具体实现（TCP Socket、HTTP 等）提供。</p>
 *
 * <p>连接对象 {@code connect} 对上层统一是 {@link Object}，具体类型由实现自行定义
 * （如 TCP 实现里是连接标识字符串），上层只负责透传，不关心其内部结构。</p>
 *
 * @since 1.0.0
 */
public interface INetwork {

    /**
     * 消息处理耗时告警阈值（毫秒）。
     *
     * <p>单条消息的处理时长超过该值时输出告警日志。</p>
     */
    long WARN_TIME = 100;

    /**
     * 打开网络监听。
     */
    void open();

    /**
     * 关闭网络监听。
     */
    void close();

    /**
     * 向客户端发送消息。
     *
     * @param msgCode 消息码
     * @param msg     消息体
     * @param connect 客户端连接标识
     * @param msgType 消息编解码类型
     */
    void sendMessageToClient(int msgCode, Message msg, Object connect, NetworkMsgType msgType);

    /**
     * 关闭客户端的连接。
     *
     * @param connect 客户端连接标识
     */
    void closeClientConnect(Object connect);

    /**
     * 获取客户端 IP 地址。
     *
     * @param connect 客户端连接标识
     * @return 客户端 IP 地址；连接不存在时返回空串
     */
    String getClientIp(Object connect);

}
