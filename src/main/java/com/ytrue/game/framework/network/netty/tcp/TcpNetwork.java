package com.ytrue.game.framework.network.netty.tcp;

import com.ytrue.game.framework.engine.config.ServerConfig;
import com.ytrue.game.framework.network.netty.BaseNetwork;
import com.ytrue.game.framework.network.proto.AppMessage.BaseMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * TCP Socket 网络实现。
 *
 * <p>客户端游戏协议的主通道：Protobuf 二进制编解码，承载登录、大厅、各玩法房间的实时通信。
 * 监听参数取自 {@code application.yml} 的 {@code game.network.tcpsocket} 配置。</p>
 *
 * <p>本类是 {@link BaseNetwork} 的具体实现，负责三件事：</p>
 * <ol>
 *     <li>从配置读取端口与线程数并启动监听（{@link #open()}）；</li>
 *     <li>提供连接的流水线装配器（{@link #getChannelHandler()}）；</li>
 *     <li>把连接表的存取委托给 {@link TcpServerHandler}（它持有全局连接表）。</li>
 * </ol>
 *
 * <p>标注 {@link Primary}：存在多个 {@code INetwork} 实现时，未指定限定符的注入默认取本类。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Primary
@Component(TcpNetwork.BEAN_NAME)
@Qualifier("tcpSocket")
@RequiredArgsConstructor
public class TcpNetwork extends BaseNetwork {

    /**
     * 本实现在容器中的 bean 名。
     *
     * <p>{@code ClientSender} 需要按传输类型找到对应的网络实现（TCP 与 WebSocket 各有独立连接表），
     * 显式声明名字比依赖类名推导出的默认名更可靠——默认名容易随类改名而变，且不易检索。</p>
     */
    public static final String BEAN_NAME = "tcpNetwork";

    /**
     * TCP 消息处理器（同时持有全局连接表）。
     */
    private final TcpServerHandler serverHandler;

    /**
     * 服务器配置。
     */
    private final ServerConfig serverConfig;

    /**
     * 单帧最大长度（约 1.9 GB，实际由业务消息大小决定，这里只作为防御上限）。
     */
    private static final int MAX_FRAME_LENGTH = 2036334592;

    /**
     * 长度字段的起始偏移量：从帧首开始。
     */
    private static final int LENGTH_FIELD_OFFSET = 0;

    /**
     * 长度字段占用字节数：4 字节 int。
     */
    private static final int LENGTH_FIELD_LENGTH = 4;

    /**
     * 长度修正值：长度字段的值就是整帧长度，无需修正。
     */
    private static final int LENGTH_ADJUSTMENT = 0;

    /**
     * 解码后跳过的字节数：剥掉 4 字节长度前缀。
     */
    private static final int INITIAL_BYTES_TO_STRIP = 4;

    @Override
    public void open() {
        // 从配置读取监听参数（对应 yml 中 game.network.tcpsocket 下的 port / boss-size / worker-size）
        ServerConfig.TcpSocket tcpSocket = serverConfig.getNetwork().getTcpsocket();
        // 获取端口
        int port = tcpSocket.getPort();
        // master thread number
        int bossSize = tcpSocket.getBossSize();
        // work thread number
        int workerSize = tcpSocket.getWorkerSize();

        // 委托给基类的启动流程；重复调用会因已启动而返回 false
        if (open(port, bossSize, workerSize)) {
            log.info("TcpSocket网络服务启动成功~ port:[{}]", port);
        } else {
            log.error("TcpSocket网络服务启动失败~ port:[{}]", port);
        }
    }

    @Override
    protected ChannelHandler getChannelHandler() {
        // 取空实例用于解码：ProtobufDecoder 需要它来拿到消息类型的描述符
        BaseMessage appMessage = BaseMessage.getDefaultInstance();
        return new TcpChannelInitializer(serverHandler, appMessage, MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP);
    }

    @Override
    protected ChannelHandlerContext getClientChannel(Object connect) {
        return serverHandler.getClientChannel(connect.toString());
    }

    @Override
    protected void removeClientChannel(Object connect) {
        serverHandler.removeClientChannel(connect.toString());
    }

}
