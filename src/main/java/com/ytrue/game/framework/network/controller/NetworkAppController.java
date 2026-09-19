package com.ytrue.game.framework.network.controller;

import com.google.protobuf.Message;
import com.ytrue.game.framework.engine.annotation.AppController;
import com.ytrue.game.framework.engine.annotation.AppHandler;
import com.ytrue.game.framework.engine.data.ServerUser;
import com.ytrue.game.framework.network.ClientSender;
import com.ytrue.game.framework.network.proto.NetworkMessage.HeartBeatRequest;
import com.ytrue.game.framework.network.proto.NetworkMessage.NetworkMsgCode;
import com.ytrue.game.framework.network.proto.NetworkMessage.PingRequest;
import com.ytrue.game.framework.network.proto.NetworkMessage.PingResponse;

import java.lang.reflect.Method;

/**
 * 网络模块的客户端控制器。
 *
 * <p>提供连接层面的两个基础协议：心跳与网络诊断。请求路径与消息码的对应关系由
 * {@code @AppHandler.msgCode} 声明，框架启动时自动装配路由表。</p>
 *
 * <p>方法签名约定为 {@code (业务消息, ServerUser)}，由本类的 {@link #checker} 负责调用。</p>
 *
 * @since 1.0.0
 */
@AppController
public class NetworkAppController {

    /**
     * 控制器统一的校验与调用方法。
     *
     * <p>框架在调用具体的 {@code @AppHandler} 方法前先走这里，把「处理方法本身」当参数传进来，
     * 由本方法决定怎么调。当前不做任何校验，直接以 {@code (业务消息, 用户)} 两个参数调用。</p>
     *
     * <p>正因如此，本类里处理方法的参数是 2 个而不是 3 个——框架直接调的那条路径
     * （控制器没配校验方法时）固定传 3 个参数 {@code (消息, 用户, exp)}。</p>
     *
     * @param taskMethod 本次要执行的处理方法
     * @param msg        业务消息
     * @param user       消息所属的用户会话
     * @param exp        附加标记（对应 {@code @AppHandler.exp}）
     * @throws Exception 处理方法内部抛出异常时向外传递
     */
    public void checker(Method taskMethod, Message msg, ServerUser user, Long exp) throws Exception {
        taskMethod.invoke(this, msg, user);
    }

    /**
     * 心跳请求。
     *
     * <p><b>本方法故意留空，不是忘了写。</b></p>
     *
     * <p>心跳要做的事——刷新该会话的最后活跃时间——已经由框架在收到**任意**消息时统一完成
     * （见 {@code TcpServerHandler.channelRead}），不需要在这里重复。而心跳**响应**也不由这里发：
     * 它由 {@code HeartBeatTask} 每 5 秒对全部存活会话统一下发（项目里除了玩家操作，
     * 没有别的地方需要客户端定时确认连接，统一由服务端推更省事）。</p>
     *
     * <p>所以客户端发这条消息的唯一作用，就是让服务端知道「我还活着、别踢我」。</p>
     *
     * @param msg  心跳请求（无字段）
     * @param user 消息所属的用户会话
     */
    @AppHandler(msgCode = NetworkMsgCode.C_S_HEART_BEAT_REQUEST_VALUE)
    public void doHeartBeatTask(HeartBeatRequest msg, ServerUser user) {
        // 见上：活跃时间已由框架刷新，响应由 HeartBeatTask 定时下发
    }

    /**
     * 网络诊断请求。
     *
     * <p>把请求里带的时间戳原样回传，客户端据此算出往返延迟（RTT）。
     * 例：客户端发 {@code pingTime = 1234567890123}，服务端回同一个值，
     * 客户端用「收到时刻 − 1234567890123」得到往返耗时。</p>
     *
     * @param msg  诊断请求（含客户端发出时的时间戳）
     * @param user 消息所属的用户会话
     */
    @AppHandler(msgCode = NetworkMsgCode.C_S_PING_REQUEST_VALUE)
    public void doPingTask(PingRequest msg, ServerUser user) {
        // 原样回传时间戳，不做任何加工——往返延迟要由客户端自己算才准确
        PingResponse response = PingResponse.newBuilder().setPingTime(msg.getPingTime()).build();
        // 返回
        ClientSender.sendMessage(NetworkMsgCode.S_C_PING_RESPONSE_VALUE, response, user);
    }

}
