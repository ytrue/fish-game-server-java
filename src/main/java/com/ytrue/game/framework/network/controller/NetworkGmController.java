package com.ytrue.game.framework.network.controller;

import com.ytrue.game.framework.engine.annotation.GmController;
import com.ytrue.game.framework.engine.annotation.GmHandler;
import com.ytrue.game.framework.engine.utils.GsonUtils;
import com.ytrue.game.framework.engine.utils.JsonMapUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 网络模块的后台控制器。
 *
 * <p>提供网络诊断等运维接口。请求路径与处理方法的对应关系由 {@code @GmHandler.key} 声明，
 * 框架启动时自动装配，新增接口只需加一个带注解的方法。</p>
 *
 * <p>方法签名约定为
 * {@code (Map&lt;String, Object&gt; param, Map&lt;String, Object&gt; resultMap)}：
 * 从 {@code param} 取入参，往 {@code resultMap} 塞返回值，由校验方法统一转 JSON 输出。</p>
 *
 * @since 1.0.0
 */
@GmController
public class NetworkGmController {

    /**
     * 统一校验与调用方法。
     *
     * <p>框架在调用具体的 {@code @GmHandler} 方法前先走这里：方法本身作为参数传入，
     * 由本方法负责准备返回容器、反射调用、并序列化成 JSON。</p>
     *
     * @param taskMethod 本次要执行的处理方法
     * @param param      请求参数
     * @return 处理结果的 JSON 字符串
     * @throws Exception 处理方法内部抛出异常时向外传递，由 HTTP 层统一捕获
     */
    public String checker(Method taskMethod, Map<String, Object> param) throws Exception {
        // 处理方法把结果写进这个 Map，而不是用返回值
        Map<String, Object> resultMap = new HashMap<>();
        taskMethod.invoke(this, param, resultMap);
        return GsonUtils.toJson(resultMap);
    }

    /**
     * 网络诊断请求。
     *
     * <p>把请求里带的时间戳原样回传，调用方据此计算往返延迟（RTT）。
     * 例：{@code GET /ping?pingTime=1234567890123} → {@code {"pingTime":1234567890123}}</p>
     *
     * <p>注意这里必须用 {@link JsonMapUtils#parseObject} 取值：请求参数经 JSON 反序列化后
     * 数字会变成 {@code Double}，直接强转 {@code Long} 会抛 {@link ClassCastException}。</p>
     *
     * @param param     请求参数
     * @param resultMap 返回结果容器
     * @throws Exception 类型转换失败时抛出
     */
    @GmHandler(key = "/ping")
    public void doPingTask(Map<String, Object> param, Map<String, Object> resultMap) throws Exception {
        resultMap.put("pingTime", JsonMapUtils.parseObject(param, "pingTime", JsonMapUtils.JsonInnerType.TYPE_LONG));
    }

}
