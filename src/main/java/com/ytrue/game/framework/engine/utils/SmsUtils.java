package com.ytrue.game.framework.engine.utils;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.reflect.TypeToken;
import com.ytrue.game.framework.engine.config.AliSmsConfig;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 短信工具类。
 *
 * <p>封装阿里云短信服务（dysmsapi）的调用，通过 {@code CommonRequest} 发送短信验证码、
 * 找回密码等通知短信，并以 {@link GsonUtils} 解析接口响应。</p>
 *
 * <p>对外是静态方法（业务代码以 {@code SmsUtils.sendSms(...)} 调用），内部所需的
 * {@link AliSmsConfig} 由 Spring 在初始化本类时注入到静态字段。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public final class SmsUtils implements BeanFactoryAware {

    /**
     * 短信配置（由 Spring 注入）。
     */
    private static AliSmsConfig aliSmsConfig;

    /**
     * 阿里云短信客户端（懒加载 + 复用）。
     *
     * <p>客户端内部持有连接池，每次调用都新建会白白浪费连接建立开销。
     * 用 {@code volatile} 配合双重检查保证只创建一次且安全发布。</p>
     */
    private static volatile IAcsClient acsClient;

    /**
     * 阿里云短信接口响应中表示调用成功的返回码。
     */
    private static final String SUCCESS_CODE = "OK";

    /**
     * 发送阿里云短信。
     *
     * @param phone         接收短信的手机号
     * @param templateCode  短信模板编码
     * @param templateParam 模板参数（JSON 字符串，形如 {@code {"code":"123456"}}）
     * @return 发送成功返回 {@code true}，否则返回 {@code false}
     */
    public static boolean sendSms(String phone, String templateCode, String templateParam) {
        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain("dysmsapi.aliyuncs.com");
        request.setVersion("2017-05-25");
        request.setAction("SendSms");
        request.putQueryParameter("PhoneNumbers", phone);
        request.putQueryParameter("SignName", aliSmsConfig.getSign());
        request.putQueryParameter("TemplateCode", templateCode);
        request.putQueryParameter("TemplateParam", templateParam);

        try {
            CommonResponse response = acsClient().getCommonResponse(request);
            Map<String, Object> resultMap = GsonUtils.fromJson(response.getData(), new TypeToken<Map<String, Object>>() {
            }.getType());
            if (resultMap != null && SUCCESS_CODE.equals(resultMap.get("Code"))) {
                return true;
            }
            log.error("阿里云短信发送失败: {}", resultMap != null ? resultMap.get("Message") : "空响应");
        } catch (Exception e) {
            log.error("阿里云短信发送异常", e);
        }
        return false;
    }

    /**
     * 获取短信客户端（首次调用时创建）。
     *
     * <p>懒加载而非在 {@code setBeanFactory} 里创建：AccessKey 来自环境变量，
     * 未配置时 {@code DefaultProfile} 可能直接抛异常，放在启动阶段会拖垮整个服务；
     * 放在首次发短信时失败，影响面小得多。</p>
     *
     * @return 阿里云短信客户端
     */
    private static IAcsClient acsClient() {
        if (acsClient == null) {
            synchronized (SmsUtils.class) {
                if (acsClient == null) {
                    DefaultProfile profile = DefaultProfile.getProfile("default",
                            aliSmsConfig.getAccessKeyId(), aliSmsConfig.getAccessSecret());
                    acsClient = new DefaultAcsClient(profile);
                }
            }
        }
        return acsClient;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        aliSmsConfig = beanFactory.getBean(AliSmsConfig.class);
    }

}
