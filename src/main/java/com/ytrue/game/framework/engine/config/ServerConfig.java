package com.ytrue.game.framework.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 服务器配置（对应 {@code application.yml} 中 {@code game} 前缀的配置）。
 *
 * <p>通过 Spring Boot 的 {@code @ConfigurationProperties} 将 yml 配置绑定为类型安全的 Bean，
 * 替代原先基于 dom4j 解析 {@code server_setting.xml} 的实现。</p>
 *
 * <p>字段名与 yml 键采用「宽松绑定」对应（如 yml 的 {@code boss-size} 对应字段 {@code bossSize}），
 * 并天然支持 {@code application-dev.yml} / {@code application-prod.yml} 多环境配置。</p>
 *
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "game")
@Getter
@Setter
public class ServerConfig {

    /** 网络监听配置。 */
    private Network network = new Network();

    /** 服务端业务规则配置。 */
    private Server server = new Server();

    /**
     * 网络监听配置。
     */
    @Getter
    @Setter
    public static class Network {
        /** TCP Socket 监听配置。 */
        private TcpSocket tcpsocket = new TcpSocket();
        /** UDP Socket 监听配置。 */
        private UdpSocket udpsocket = new UdpSocket();
        /** HTTP 监听配置。 */
        private Http http = new Http();
        /** WebSocket 监听配置。 */
        private WebSocket websocket = new WebSocket();
    }

    /**
     * TCP Socket 监听配置。
     */
    @Getter
    @Setter
    public static class TcpSocket {
        /** 监听端口。 */
        private int port;
        /** boss 线程数。 */
        private int bossSize;
        /** worker 线程数。 */
        private int workerSize;
    }

    /**
     * UDP Socket 监听配置。
     */
    @Getter
    @Setter
    public static class UdpSocket {
        /** 监听端口。 */
        private int port;
        /** boss 线程数。 */
        private int bossSize;
        /** worker 线程数。 */
        private int workerSize;
    }

    /**
     * HTTP 监听配置。
     */
    @Getter
    @Setter
    public static class Http {
        /** 监听端口。 */
        private int port;
        /** boss 线程数。 */
        private int bossSize;
        /** worker 线程数。 */
        private int workerSize;
    }

    /**
     * WebSocket 监听配置。
     */
    @Getter
    @Setter
    public static class WebSocket {
        /** 监听端口。 */
        private int port;
        /** boss 线程数。 */
        private int bossSize;
        /** worker 线程数。 */
        private int workerSize;
    }

    /**
     * 服务端业务规则配置。
     */
    @Getter
    @Setter
    public static class Server {
        /** 昵称相关规则。 */
        private Nickname nickname = new Nickname();
    }

    /**
     * 昵称相关规则。
     */
    @Getter
    @Setter
    public static class Nickname {
        /** 昵称限制值。 */
        private int limit;
    }

}
