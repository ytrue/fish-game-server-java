package com.ytrue.game.framework.engine.data;

/**
 * 承载客户端连接的传输类型。
 *
 * <p>服务端向客户端发消息时，需要靠它决定走哪条网络：TCP 与 WebSocket 各有独立的连接表，
 * 走错表会查不到连接，消息被静默丢弃——这类问题极难排查，所以传输类型必须跟着会话走。</p>
 *
 * <p>注意与 {@link NetworkMsgType} 区分：后者描述的是「一条消息怎么编解码」（二进制还是 JSON），
 * 本枚举描述的是「这条连接架在哪种传输上」。两者互相独立。</p>
 *
 * @since 1.0.0
 */
public enum TransportType {

    /**
     * TCP 长连接（客户端主通道，承载 Protobuf 游戏协议）。
     */
    TCP,

    /**
     * WebSocket 长连接（承载同一套客户端协议，供浏览器等无法用原生 TCP 的场景使用）。
     */
    WEBSOCKET

}
