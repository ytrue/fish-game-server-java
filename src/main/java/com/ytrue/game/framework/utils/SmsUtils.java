package com.ytrue.game.framework.utils;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.reflect.TypeToken;
import com.ytrue.game.framework.config.AliSmsConfig;
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
 * @since 1.0.0
 */
@Slf4j
@Component
public final class SmsUtils implements BeanFactoryAware {

    private static AliSmsConfig aliSmsConfig;

    /**
     * 阿里云短信接口响应中表示调用成功的返回码。
     */
    private static final String SUCCESS_CODE = "OK";

    /**
     * 私有构造器，禁止实例化（纯工具类）。
     */
    private SmsUtils() {
    }

    /**
     * 发送阿里云短信。
     *
     * @param phone         接收短信的手机号
     * @param templateCode  短信模板编码
     * @param templateParam 模板参数（JSON 字符串，形如 {@code {"code":"123456"}}）
     * @return 发送成功返回 {@code true}，否则返回 {@code false}
     */
    public static boolean sendSms(String phone, String templateCode, String templateParam) {
        DefaultProfile profile = DefaultProfile.getProfile("default",
                aliSmsConfig.getAccessKeyId(), aliSmsConfig.getAccessSecret());
        IAcsClient client = new DefaultAcsClient(profile);

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
            CommonResponse response = client.getCommonResponse(request);
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

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        aliSmsConfig = beanFactory.getBean(AliSmsConfig.class);
    }
}
