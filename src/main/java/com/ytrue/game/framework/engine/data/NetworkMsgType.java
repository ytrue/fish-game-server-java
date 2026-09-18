package com.ytrue.game.framework.engine.data;

/**
 * 通信消息类型。
 *
 * <p>描述客户端与服务端之间一条消息的编解码格式。</p>
 *
 * @since 1.0.0
 */
public enum NetworkMsgType {

    /**
     * 二进制流（Protobuf 编解码）。
     */
    BINARY,

    /**
     * JSON 字符串。
     */
    JSON,

}
