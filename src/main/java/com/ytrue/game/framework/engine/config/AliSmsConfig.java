package com.ytrue.game.framework.engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云短信配置（对应 {@code application.yml} 中 {@code ali-sms} 前缀的配置）。
 *
 * <p>绑定阿里云短信服务（dysmsapi）所需的访问凭证、短信签名及模板编码，
 * 供 {@code SmsUtils} 发送短信验证码 / 找回密码时使用。</p>
 *
 * <p>注意：{@code accessKeyId} / {@code accessSecret} 属于敏感凭证，不应提交进仓库，
 * 应通过 {@code application-local.yml}、环境变量或配置中心注入。</p>
 *
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "ali-sms")
@Data
public class AliSmsConfig {

    /** 阿里云 AccessKey ID。 */
    private String accessKeyId;

    /** 阿里云 AccessKey Secret。 */
    private String accessSecret;

    /** 短信签名（阿里云控制台配置的签名名称）。 */
    private String sign;

    /** 验证码短信模板编码。 */
    private String authCode;

    /** 找回密码短信模板编码。 */
    private String passCode;

}
